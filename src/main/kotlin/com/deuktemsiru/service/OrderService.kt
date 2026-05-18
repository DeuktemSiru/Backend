package com.deuktemsiru.service

import com.deuktemsiru.common.toEnumOrThrow
import com.deuktemsiru.common.toLocalDateOrThrow
import com.deuktemsiru.common.nowDateTime
import com.deuktemsiru.common.orNotFound
import com.deuktemsiru.common.safePage
import com.deuktemsiru.common.today
import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.CreateOrderResponse
import com.deuktemsiru.dto.DailySales
import com.deuktemsiru.dto.OrderDetailResponse
import com.deuktemsiru.dto.OrderListItemResponse
import com.deuktemsiru.dto.SalesResponse
import com.deuktemsiru.dto.SellerSalesResponse
import com.deuktemsiru.dto.TopMenu
import com.deuktemsiru.dto.TopProduct
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.OrderItem
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Orders
import com.deuktemsiru.entity.Payment
import com.deuktemsiru.entity.PaymentMethod
import com.deuktemsiru.entity.PaymentStatus
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.requirePurchasableOn
import com.deuktemsiru.repository.MemberStatsRepository
import com.deuktemsiru.repository.OrderRepository
import com.deuktemsiru.repository.PaymentRepository
import com.deuktemsiru.repository.ProductRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val productRepository: ProductRepository,
    private val memberStatsRepository: MemberStatsRepository,
    private val memberService: MemberService,
    private val storeOwnershipService: StoreOwnershipService,
    private val clock: Clock,
) {
    private companion object {
        private const val PICKUP_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private val secureRandom = SecureRandom()
    }

    @Transactional
    fun createOrder(consumerId: Long, req: CreateOrderRequest): CreateOrderResponse {
        val consumer = memberService.findMemberForUpdate(consumerId)
        require(consumer.role == MemberRole.CONSUMER) { "소비자 계정만 주문할 수 있습니다." }
        require(req.items.isNotEmpty()) { "주문 항목이 없습니다." }
        val orderItems = req.items
            .groupBy { it.productId }
            .map { (productId, items) ->
                val quantity = items.sumOf { item ->
                    require(item.quantity > 0) { "주문 수량은 1개 이상이어야 합니다." }
                    item.quantity
                }
                com.deuktemsiru.dto.OrderItemRequest(productId = productId, quantity = quantity)
            }

        val lockedProducts = orderItems
            .map { it.productId }
            .distinct()
            .sorted()
            .associateWith { productId ->
                productRepository.findByIdForUpdate(productId)
                    .orNotFound("상품을 찾을 수 없습니다.")
            }

        // 첫 번째 상품에서 가게 결정
        val firstProduct = lockedProducts[orderItems.first().productId]!!
        val store = firstProduct.store

        val order = Orders(
            consumer = consumer,
            store = store,
            pickupCode = generatePickupCode(),
        )

        order.items += orderItems.map { itemReq ->
            val product = lockedProducts[itemReq.productId]!!
            require(product.store.storeId == store.storeId) { "같은 가게의 상품만 주문할 수 있습니다." }
            product.requirePurchasableOn(today(), itemReq.quantity)

            product.quantityRemaining -= itemReq.quantity
            if (product.quantityRemaining == 0) product.status = ProductStatus.SOLD_OUT

            OrderItem(
                order = order,
                product = product,
                quantity = itemReq.quantity,
                unitPrice = product.discountPrice,
            )
        }
        order.totalPrice = order.items.sumOf { it.unitPrice * it.quantity }

        val saved = orderRepository.save(order)
        val payment = createPayment(saved, req.paymentMethod)
        return CreateOrderResponse.from(saved, payment)
    }

    fun getMyOrders(
        consumerId: Long,
        statusFilter: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): List<OrderListItemResponse> {
        val consumer = memberService.findMember(consumerId)
        val pageable = safePage(page, size)
        val orders = if (statusFilter != null) {
            val target = statusFilter.toEnumOrThrow<OrderStatus>("주문 상태")
            orderRepository.findByConsumerAndStatusOrderByCreatedAtDesc(consumer, target, pageable)
        } else {
            orderRepository.findByConsumerOrderByCreatedAtDesc(consumer, pageable)
        }
        return orders.map { OrderListItemResponse.from(it) }
    }

    fun getOrder(consumerId: Long, orderId: Long): OrderDetailResponse {
        val order = findConsumerOrder(consumerId, orderId)
        return OrderDetailResponse.from(order, latestPayment(order))
    }

    @Transactional
    fun cancelOrder(consumerId: Long, orderId: Long): OrderDetailResponse {
        val order = findConsumerOrderForUpdate(consumerId, orderId)
        require(
            order.status == OrderStatus.PENDING || order.status == OrderStatus.CONFIRMED
        ) { "픽업 완료되거나 이미 취소된 주문은 취소할 수 없습니다." }

        cancelAndRefund(order)
        return OrderDetailResponse.from(order, latestPayment(order))
    }

    // ── 판매자용 ──────────────────────────────────────────────────────────────

    fun getStoreOrders(
        sellerId: Long,
        status: String? = null,
        date: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): List<OrderDetailResponse> {
        val store = sellerStoreOrNull(sellerId) ?: return emptyList()
        val targetStatus = status?.toEnumOrThrow<OrderStatus>("주문 상태")
        val pageable = safePage(page, size)
        val targetDate = date?.let { parseDate(it) }
        val orderedPageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        val orders = orderRepository.findAll(
            buildOrderSpec(store, targetStatus, targetDate?.atStartOfDay(), targetDate?.plusDays(1)?.atStartOfDay()),
            orderedPageable,
        ).content

        val payments = latestPayments(orders)
        return orders.map { OrderDetailResponse.from(it, payments[it.orderId]) }
    }

    fun getStoreOrder(sellerId: Long, orderId: Long): OrderDetailResponse {
        val order = findSellerOrder(sellerId, orderId)
        return OrderDetailResponse.from(order, latestPayment(order))
    }

    fun getStoreOrderEntities(sellerId: Long): List<Orders> {
        return orderRepository.findByStoreOrderByCreatedAtDesc(sellerStore(sellerId))
    }

    @Transactional
    fun updateOrderStatus(sellerId: Long, orderId: Long, req: UpdateOrderStatusRequest): OrderDetailResponse {
        val order = findSellerOrderForUpdate(sellerId, orderId)
        val previousStatus = order.status
        require(canTransition(previousStatus, req.status)) {
            "주문 상태를 ${order.status}에서 ${req.status}(으)로 변경할 수 없습니다."
        }
        if (req.status == OrderStatus.CANCELLED) {
            cancelAndRefund(order)
        } else {
            order.status = req.status
        }
        if (previousStatus != OrderStatus.PICKED_UP && req.status == OrderStatus.PICKED_UP) {
            applyPickupStats(order)
        }
        return OrderDetailResponse.from(order, latestPayment(order))
    }

    @Transactional
    fun verifyPickupCode(sellerId: Long, pickupCode: String): OrderDetailResponse {
        val order = findConfirmablePickupOrder(sellerId, pickupCode)
        return OrderDetailResponse.from(order, latestPayment(order))
    }

    @Transactional
    fun confirmPickupCode(sellerId: Long, orderId: Long, pickupCode: String): OrderDetailResponse {
        val order = findConfirmablePickupOrder(sellerId, pickupCode)
        require(order.orderId == orderId) { "주문과 픽업 코드가 일치하지 않습니다." }
        order.status = OrderStatus.PICKED_UP
        applyPickupStats(order)
        return OrderDetailResponse.from(order, latestPayment(order))
    }

    fun getSalesStats(sellerId: Long, period: String = "DAY", targetDate: LocalDate = today()): SalesResponse {
        val agg = aggregateSales(sellerId, period, targetDate)
        return SalesResponse(
            totalAmount = agg.totalAmount,
            totalOrders = agg.totalOrders,
            chartData = agg.chartData,
            topProducts = agg.topCounts.map { (name, count) -> TopProduct(productName = name, soldCount = count) },
            carbonSavedKg = 0.0,
        )
    }

    fun getSellerSalesStats(sellerId: Long, period: String = "DAY", targetDate: LocalDate = today()): SellerSalesResponse {
        val agg = aggregateSales(sellerId, period, targetDate)
        return SellerSalesResponse(
            totalAmount = agg.totalAmount,
            totalOrders = agg.totalOrders,
            chartData = agg.chartData,
            topMenus = agg.topCounts.map { (name, count) -> TopMenu(name = name, count = count) },
        )
    }

    /** S4: getSalesStats / getSellerSalesStats 공통 집계 로직 */
    private data class SalesAggregation(
        val totalAmount: Int,
        val totalOrders: Int,
        val chartData: List<DailySales>,
        val topCounts: List<Pair<String, Int>>,
    )

    private fun aggregateSales(sellerId: Long, period: String, targetDate: LocalDate): SalesAggregation {
        val (start, endExclusive) = salesWindow(period, targetDate)
        val store = sellerStoreOrNull(sellerId)
            ?: return SalesAggregation(
                totalAmount = 0,
                totalOrders = 0,
                chartData = salesData(period, targetDate, emptyList()),
                topCounts = emptyList(),
            )
        val periodOrders = orderRepository.findAll(
            buildOrderSpec(store, OrderStatus.PICKED_UP, start.atStartOfDay(), endExclusive.atStartOfDay()),
        )
        return SalesAggregation(
            totalAmount = periodOrders.sumOf { it.totalPrice },
            totalOrders = periodOrders.size,
            chartData = salesData(period, targetDate, periodOrders),
            topCounts = topProductCounts(periodOrders),
        )
    }

    private fun salesWindow(period: String, targetDate: LocalDate): Pair<LocalDate, LocalDate> =
        when (period.uppercase()) {
            "YEAR" -> targetDate.withDayOfYear(1).let { it to it.plusYears(1) }
            "MONTH" -> targetDate.withDayOfMonth(1).let { it to it.plusMonths(1) }
            "WEEK" -> targetDate.minusDays(targetDate.dayOfWeek.value.toLong() - 1).let { it to it.plusDays(7) }
            else -> targetDate to targetDate.plusDays(1)
        }

    // ── 내부 유틸 ────────────────────────────────────────────────────────────

    private fun createPayment(order: Orders, requestedMethod: String?): Payment {
        val method = (requestedMethod ?: "CASH").toEnumOrThrow<PaymentMethod>("결제 수단")

        if (method == PaymentMethod.SIRU) {
            require(order.consumer.isSiruLinked) { "시루 계정 연동이 필요합니다." }
            require(order.consumer.siruBalance >= order.totalPrice) { "시루 잔액이 부족합니다." }
            order.consumer.siruBalance -= order.totalPrice
        }

        return paymentRepository.save(
            Payment(
                order = order,
                method = method,
                amount = order.totalPrice,
                status = PaymentStatus.COMPLETED,
                paidAt = now(),
                externalTransactionId = "${method.name}-${order.orderId}-${System.currentTimeMillis()}",
            )
        )
    }

    private fun latestPayment(order: Orders): Payment? =
        paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order).orElse(null)

    private fun latestPayments(orders: List<Orders>): Map<Long, Payment> {
        val orderIds = orders.map { it.orderId }
        if (orderIds.isEmpty()) return emptyMap()
        return paymentRepository.findLatestByOrderIds(orderIds).associateBy { it.order.orderId }
    }

    private fun sellerStore(sellerId: Long): com.deuktemsiru.entity.Store =
        storeOwnershipService.findSellerStore(sellerId)

    private fun sellerStoreOrNull(sellerId: Long): com.deuktemsiru.entity.Store? =
        storeOwnershipService.findSellerStoreOrNull(sellerId)

    private fun findConsumerOrder(consumerId: Long, orderId: Long): Orders =
        findOrder(orderId) { it.consumer.memberId == consumerId }

    private fun findConsumerOrderForUpdate(consumerId: Long, orderId: Long): Orders =
        findOrder(orderId, forUpdate = true) { it.consumer.memberId == consumerId }

    private fun findSellerOrder(sellerId: Long, orderId: Long): Orders {
        val store = sellerStore(sellerId)
        return findOrder(orderId) { it.store.storeId == store.storeId }
    }

    private fun findSellerOrderForUpdate(sellerId: Long, orderId: Long): Orders {
        val store = sellerStore(sellerId)
        return findOrder(orderId, forUpdate = true) { it.store.storeId == store.storeId }
    }

    private fun findOrder(orderId: Long, forUpdate: Boolean = false, canAccess: (Orders) -> Boolean): Orders {
        val order = if (forUpdate) orderRepository.findByIdForUpdate(orderId) else orderRepository.findById(orderId)
        return order
            .orNotFound("주문을 찾을 수 없습니다.")
            .also { require(canAccess(it)) { "접근 권한이 없습니다." } }
    }

    private fun findConfirmablePickupOrder(sellerId: Long, pickupCode: String): Orders {
        val store = sellerStore(sellerId)
        return (orderRepository.findByPickupCodeForUpdate(pickupCode)
            ?: throw NoSuchElementException("픽업 코드를 찾을 수 없습니다."))
            .also {
                require(it.store.storeId == store.storeId) { "접근 권한이 없습니다." }
                require(it.status == OrderStatus.CONFIRMED) { "픽업 대기 중인 주문만 확인할 수 있습니다." }
            }
    }

    private fun today(): LocalDate = clock.today()

    private fun now(): LocalDateTime = clock.nowDateTime()

    private fun cancelAndRefund(order: Orders) {
        if (order.status == OrderStatus.CANCELLED) return

        order.status = OrderStatus.CANCELLED
        val payment = latestPayment(order)
        if (payment != null && payment.status != PaymentStatus.REFUNDED) {
            if (payment.method == PaymentMethod.SIRU) {
                order.consumer.siruBalance += payment.amount
            }
            paymentRepository.save(
                Payment(
                    order = order,
                    method = payment.method,
                    amount = payment.amount,
                    status = PaymentStatus.REFUNDED,
                    paidAt = now(),
                    externalTransactionId = "REFUND-${payment.paymentId}-${System.currentTimeMillis()}",
                )
            )
        }

        order.items.forEach { item ->
            item.product.quantityRemaining += item.quantity
            if (item.product.status == ProductStatus.SOLD_OUT) {
                item.product.status = ProductStatus.AVAILABLE
            }
        }
    }

    private fun applyPickupStats(order: Orders) {
        val stats = memberStatsRepository.findByMember(order.consumer).orElseGet {
            com.deuktemsiru.entity.MemberStats(member = order.consumer)
        }
        val originalTotal = order.items.sumOf { it.product.originalPrice * it.quantity }
        val savedAmount = (originalTotal - order.totalPrice).coerceAtLeast(0)
        stats.totalOrders += 1
        stats.totalSavedAmount += savedAmount
        stats.totalCarbonSavedKg += order.items.sumOf { it.quantity } * 0.25
        memberStatsRepository.save(stats)
    }

    private fun salesData(period: String, targetDate: LocalDate, orders: List<Orders>): List<DailySales> =
        when (period.uppercase()) {
            "YEAR" -> yearlySales(targetDate.year, orders)
            "MONTH" -> monthlySales(targetDate, orders)
            "WEEK"  -> weeklySales(targetDate, orders)
            else    -> dailySales(targetDate, orders)  // DAY (기본값)
        }

    /**
     * 시간 버킷(label, predicate) 목록을 받아 DailySales 리스트를 생성하는 공통 헬퍼.
     * dailySales / weeklySales / monthlySales / yearlySales 모두 이 헬퍼를 통해 구성됩니다.
     */
    private fun buildSalesChart(
        orders: List<Orders>,
        buckets: List<Pair<String, (Orders) -> Boolean>>,
    ): List<DailySales> {
        return buckets.map { (label, matches) ->
            DailySales(label, orders.filter(matches).sumOf { it.totalPrice })
        }
    }

    private fun dailySales(date: LocalDate, orders: List<Orders>): List<DailySales> =
        buildSalesChart(orders, (0..23).map { hour ->
            "${hour}시" to { order: Orders ->
                order.createdAt.toLocalDate() == date && order.createdAt.hour == hour
            }
        })

    private fun monthlySales(targetMonth: LocalDate, orders: List<Orders>): List<DailySales> {
        val firstDay = targetMonth.withDayOfMonth(1)
        val lastDay = targetMonth.with(TemporalAdjusters.lastDayOfMonth())
        val buckets = generateSequence(firstDay) { it.plusDays(7) }
            .takeWhile { !it.isAfter(lastDay) }
            .mapIndexed { index, weekStart ->
                val weekEnd = minOf(weekStart.plusDays(6), lastDay)
                "${index + 1}주" to { order: Orders ->
                    val d = order.createdAt.toLocalDate()
                    !d.isBefore(weekStart) && !d.isAfter(weekEnd)
                }
            }.toList()
        return buildSalesChart(orders, buckets)
    }

    private fun yearlySales(year: Int, orders: List<Orders>): List<DailySales> =
        buildSalesChart(orders, (1..12).map { month ->
            "${month}월" to { order: Orders ->
                order.createdAt.year == year && order.createdAt.monthValue == month
            }
        })

    private fun weeklySales(referenceDate: LocalDate, orders: List<Orders>): List<DailySales> {
        val startDay = referenceDate.minusDays(referenceDate.dayOfWeek.value.toLong() - 1) // 해당 주 월요일
        val formatter = DateTimeFormatter.ofPattern("MM/dd")
        val buckets = (0..6).map { i ->
            val date = startDay.plusDays(i.toLong())
            date.format(formatter) to { order: Orders -> order.createdAt.toLocalDate() == date }
        }
        return buildSalesChart(orders, buckets)
    }

    private fun topProductCounts(orders: List<Orders>): List<Pair<String, Int>> =
        orders.flatMap { it.items }
            .groupBy { it.product.productId }
            .values
            .map { items -> items.first().product.name to items.sumOf { it.quantity } }
            .sortedByDescending { it.second }
            .take(3)

    // B2: existsByPickupCode 확인과 저장 사이의 race condition 대응 —
    //     고유 제약 위반(DataIntegrityViolationException)이 발생할 경우 호출 측(createOrder)
    //     트랜잭션이 롤백되므로, 여기서는 DB 에 아직 없는 코드를 반환하는 것에 집중한다.
    //     반복 생성으로 충돌 확률을 최소화하고, 나머지는 unique constraint 에 의존한다.
    private fun generatePickupCode(): String {
        repeat(10) {
            val candidate = (1..6)
                .map { PICKUP_CODE_CHARS[secureRandom.nextInt(PICKUP_CODE_CHARS.length)] }
                .joinToString("")
            if (!orderRepository.existsByPickupCode(candidate)) return candidate
        }
        // 6자리에서 반복 충돌 시 8자리 UUID 기반 코드로 재시도
        repeat(5) {
            val fallback = java.util.UUID.randomUUID().toString().replace("-", "").uppercase().take(8)
            if (!orderRepository.existsByPickupCode(fallback)) return fallback
        }
        error("픽업 코드를 생성할 수 없습니다.")
    }

    private fun canTransition(current: OrderStatus, next: OrderStatus): Boolean {
        if (current == next) return true
        return when (current) {
            OrderStatus.PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED
            OrderStatus.CONFIRMED -> next == OrderStatus.PICKED_UP || next == OrderStatus.CANCELLED
            OrderStatus.PICKED_UP, OrderStatus.CANCELLED -> false
        }
    }

    private fun parseDate(value: String): LocalDate = value.toLocalDateOrThrow()

    private fun buildOrderSpec(
        store: com.deuktemsiru.entity.Store,
        status: OrderStatus? = null,
        start: LocalDateTime? = null,
        end: LocalDateTime? = null,
    ): Specification<Orders> = Specification { root, _, cb ->
        val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()
        predicates.add(cb.equal(root.get<com.deuktemsiru.entity.Store>("store"), store))
        if (status != null) predicates.add(cb.equal(root.get<OrderStatus>("status"), status))
        if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start))
        if (end != null) predicates.add(cb.lessThan(root.get("createdAt"), end))
        cb.and(*predicates.toTypedArray())
    }
}
