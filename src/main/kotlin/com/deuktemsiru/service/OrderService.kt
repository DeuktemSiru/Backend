package com.deuktemsiru.service

import com.deuktemsiru.common.toEnumOrThrow
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
import com.deuktemsiru.repository.MemberStatsRepository
import com.deuktemsiru.repository.OrderRepository
import com.deuktemsiru.repository.PaymentRepository
import com.deuktemsiru.repository.ProductRepository
import com.deuktemsiru.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
    private val memberStatsRepository: MemberStatsRepository,
    private val memberService: MemberService,
) {
    private companion object {
        private const val PICKUP_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private val secureRandom = SecureRandom()
    }

    @Transactional
    fun createOrder(consumerId: Long, req: CreateOrderRequest): CreateOrderResponse {
        val consumer = memberService.findMember(consumerId)
        require(consumer.role == MemberRole.CONSUMER) { "소비자 계정만 주문할 수 있습니다." }
        require(req.items.isNotEmpty()) { "주문 항목이 없습니다." }

        // B7: 모든 상품을 먼저 잠근 후 재고 차감 (lost-update 방지)
        val lockedProducts = req.items.associate { itemReq ->
            itemReq.productId to productRepository.findByIdForUpdate(itemReq.productId)
                .orElseThrow { NoSuchElementException("상품을 찾을 수 없습니다.") }
        }

        // 첫 번째 상품에서 가게 결정
        val firstProduct = lockedProducts[req.items.first().productId]!!
        val store = firstProduct.store

        val order = Orders(
            consumer = consumer,
            store = store,
            pickupCode = generatePickupCode(),
        )

        order.items += req.items.map { itemReq ->
            require(itemReq.quantity > 0) { "주문 수량은 1개 이상이어야 합니다." }
            val product = lockedProducts[itemReq.productId]!!
            require(product.store.storeId == store.storeId) { "같은 가게의 상품만 주문할 수 있습니다." }
            require(product.status == ProductStatus.AVAILABLE) { "${product.name}은(는) 구매 불가 상태입니다." }
            require(product.quantityRemaining >= itemReq.quantity) { "${product.name} 재고가 부족합니다." }

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
        var orders = orderRepository.findByConsumerOrderByCreatedAtDesc(consumer)
        if (statusFilter != null) {
            val target = statusFilter.toEnumOrThrow<OrderStatus>("주문 상태")
            orders = orders.filter { it.status == target }
        }
        // S2: 수동 fromIndex/toIndex 계산을 drop/take 로 대체
        val safeSize = size.coerceAtLeast(1).coerceAtMost(100)
        val fromIndex = page.coerceAtLeast(0) * safeSize
        return orders.drop(fromIndex).take(safeSize).map { OrderListItemResponse.from(it) }
    }

    fun getOrder(consumerId: Long, orderId: Long): OrderDetailResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        require(order.consumer.memberId == consumerId) { "접근 권한이 없습니다." }
        return OrderDetailResponse.from(order, latestPayment(order))
    }

    @Transactional
    fun cancelOrder(consumerId: Long, orderId: Long): OrderDetailResponse {
        val consumer = memberService.findMember(consumerId)
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        require(order.consumer.memberId == consumerId) { "접근 권한이 없습니다." }
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
        var orders = getStoreOrderEntities(sellerId)
        status?.let {
            val target = it.toEnumOrThrow<OrderStatus>("주문 상태")
            orders = orders.filter { order -> order.status == target }
        }
        date?.let {
            val targetDate = runCatching { LocalDate.parse(it) }
                .getOrElse { throw IllegalArgumentException("날짜 형식은 yyyy-MM-dd 이어야 합니다.") }
            orders = orders.filter { order -> order.createdAt.toLocalDate() == targetDate }
        }

        // S2: drop/take 로 페이지네이션
        val safeSize = size.coerceAtLeast(1).coerceAtMost(100)
        val fromIndex = page.coerceAtLeast(0) * safeSize
        return orders.drop(fromIndex).take(safeSize).map { OrderDetailResponse.from(it, latestPayment(it)) }
    }

    fun getStoreOrder(sellerId: Long, orderId: Long): OrderDetailResponse {
        val store = sellerStore(sellerId)
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        require(order.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        return OrderDetailResponse.from(order, latestPayment(order))
    }

    fun getStoreOrderEntities(sellerId: Long): List<Orders> {
        return orderRepository.findByStoreOrderByCreatedAtDesc(sellerStore(sellerId))
    }

    @Transactional
    fun updateOrderStatus(sellerId: Long, orderId: Long, req: UpdateOrderStatusRequest): OrderDetailResponse {
        val store = sellerStore(sellerId)
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        require(order.store.storeId == store.storeId) { "접근 권한이 없습니다." }
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
        val store = sellerStore(sellerId)
        val order = orderRepository.findByPickupCode(pickupCode)
            ?: throw NoSuchElementException("픽업 코드를 찾을 수 없습니다.")
        require(order.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        require(order.status == OrderStatus.CONFIRMED) { "픽업 대기 중인 주문만 확인할 수 있습니다." }
        return OrderDetailResponse.from(order, latestPayment(order))
    }

    fun getSalesStats(sellerId: Long, period: String = "DAY", targetDate: LocalDate = LocalDate.now()): SalesResponse {
        val agg = aggregateSales(sellerId, period, targetDate)
        return SalesResponse(
            totalAmount = agg.totalAmount,
            totalOrders = agg.totalOrders,
            chartData = agg.chartData,
            topProducts = agg.topCounts.map { (name, count) -> TopProduct(productName = name, soldCount = count) },
            carbonSavedKg = 0.0,
        )
    }

    fun getSellerSalesStats(sellerId: Long, period: String = "DAY", targetDate: LocalDate = LocalDate.now()): SellerSalesResponse {
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
        val (_, pickedUpOrders) = fetchStoreOrders(sellerId)
        val targetDayOrders = pickedUpOrders.filter { it.createdAt.toLocalDate() == targetDate }
        return SalesAggregation(
            totalAmount = targetDayOrders.sumOf { it.totalPrice },
            totalOrders = targetDayOrders.size,
            chartData = salesData(period, targetDate, pickedUpOrders),
            topCounts = topProductCounts(pickedUpOrders),
        )
    }

    private fun fetchStoreOrders(sellerId: Long): Pair<List<Orders>, List<Orders>> {
        val store = sellerStore(sellerId)
        val all = orderRepository.findByStoreOrderByCreatedAtDesc(store)
        return all to all.filter { it.status == OrderStatus.PICKED_UP }
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
                paidAt = java.time.LocalDateTime.now(),
                externalTransactionId = "${method.name}-${order.orderId}-${System.currentTimeMillis()}",
            )
        )
    }

    private fun latestPayment(order: Orders): Payment? =
        paymentRepository.findFirstByOrderOrderByPaymentIdDesc(order).orElse(null)

    // S3: SellerAppService 에서도 재사용할 수 있도록 internal 로 공개
    internal fun sellerStore(sellerId: Long): com.deuktemsiru.entity.Store {
        val seller = memberService.findMember(sellerId)
        require(seller.role == MemberRole.SELLER) { "판매자 계정만 주문을 처리할 수 있습니다." }
        return storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
    }

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
                    paidAt = java.time.LocalDateTime.now(),
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
    ): List<DailySales> = buckets.map { (label, matches) ->
        DailySales(label, orders.sumOf { if (matches(it)) it.totalPrice else 0 })
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
}
