package com.deuktemsiru.service

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
import com.deuktemsiru.dto.categoryEmoji
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.OrderItem
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Orders
import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import com.deuktemsiru.repository.OrderRepository
import com.deuktemsiru.repository.ProductRepository
import com.deuktemsiru.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository,
    private val memberService: MemberService,
    private val storeService: StoreService,
) {

    @Transactional
    fun createOrder(consumerId: Long, req: CreateOrderRequest): CreateOrderResponse {
        val consumer = memberService.findMember(consumerId)
        require(consumer.role == MemberRole.CONSUMER) { "소비자 계정만 주문할 수 있습니다." }
        require(req.items.isNotEmpty()) { "주문 항목이 없습니다." }

        // 첫 번째 상품에서 가게 결정
        val firstProduct = productRepository.findById(req.items.first().productId)
            .orElseThrow { NoSuchElementException("상품을 찾을 수 없습니다.") }
        val store = firstProduct.store

        val order = Orders(
            consumer = consumer,
            store = store,
            pickupCode = generatePickupCode(),
        )

        order.items += req.items.map { itemReq ->
            require(itemReq.quantity > 0) { "주문 수량은 1개 이상이어야 합니다." }
            val product = productRepository.findByIdForUpdate(itemReq.productId)
                .orElseThrow { NoSuchElementException("상품을 찾을 수 없습니다.") }
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
        return CreateOrderResponse.from(saved, req.paymentMethod)
    }

    fun getMyOrders(consumerId: Long, statusFilter: String? = null): List<OrderListItemResponse> {
        val consumer = memberService.findMember(consumerId)
        var orders = orderRepository.findByConsumerOrderByCreatedAtDesc(consumer)
        if (statusFilter != null) {
            val target = runCatching { OrderStatus.valueOf(statusFilter.uppercase()) }
                .getOrElse { throw IllegalArgumentException("지원하지 않는 주문 상태: $statusFilter") }
            orders = orders.filter { it.status == target }
        }
        return orders.map { OrderListItemResponse.from(it) }
    }

    fun getOrder(orderId: Long): OrderDetailResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        return OrderDetailResponse.from(order)
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

        order.status = OrderStatus.CANCELLED
        // 재고 복구
        order.items.forEach { item ->
            item.product.quantityRemaining += item.quantity
            if (item.product.status == ProductStatus.SOLD_OUT) {
                item.product.status = ProductStatus.AVAILABLE
            }
        }
        return OrderDetailResponse.from(order)
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
            val target = runCatching { OrderStatus.valueOf(it.uppercase()) }
                .getOrElse { throw IllegalArgumentException("지원하지 않는 주문 상태: $status") }
            orders = orders.filter { order -> order.status == target }
        }
        date?.let {
            val targetDate = runCatching { LocalDate.parse(it) }
                .getOrElse { throw IllegalArgumentException("날짜 형식은 yyyy-MM-dd 이어야 합니다.") }
            orders = orders.filter { order -> order.createdAt.toLocalDate() == targetDate }
        }

        val safeSize = size.coerceAtLeast(1)
        val fromIndex = page.coerceAtLeast(0) * safeSize
        if (fromIndex >= orders.size) return emptyList()
        val toIndex = min(fromIndex + safeSize, orders.size)
        return orders.subList(fromIndex, toIndex).map { OrderDetailResponse.from(it) }
    }

    fun getStoreOrder(sellerId: Long, orderId: Long): OrderDetailResponse {
        val seller = memberService.findMember(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        require(order.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        return OrderDetailResponse.from(order)
    }

    fun getStoreOrderEntities(sellerId: Long): List<Orders> {
        val seller = memberService.findMember(sellerId)
        require(seller.role == MemberRole.SELLER) { "판매자 계정만 주문을 조회할 수 있습니다." }
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        return orderRepository.findByStoreOrderByCreatedAtDesc(store)
    }

    @Transactional
    fun updateOrderStatus(sellerId: Long, orderId: Long, req: UpdateOrderStatusRequest): OrderDetailResponse {
        val seller = memberService.findMember(sellerId)
        require(seller.role == MemberRole.SELLER) { "판매자 계정만 주문 상태를 변경할 수 있습니다." }
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        require(order.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        require(canTransition(order.status, req.status)) {
            "주문 상태를 ${order.status}에서 ${req.status}(으)로 변경할 수 없습니다."
        }
        order.status = req.status
        return OrderDetailResponse.from(order)
    }

    @Transactional
    fun verifyPickupCode(sellerId: Long, pickupCode: String): OrderDetailResponse {
        val seller = memberService.findMember(sellerId)
        require(seller.role == MemberRole.SELLER) { "판매자 계정만 픽업 코드를 검증할 수 있습니다." }
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val order = orderRepository.findByPickupCode(pickupCode)
            ?: throw NoSuchElementException("픽업 코드를 찾을 수 없습니다.")
        require(order.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        if (order.status == OrderStatus.CONFIRMED) order.status = OrderStatus.PICKED_UP
        return OrderDetailResponse.from(order)
    }

    fun getSalesStats(sellerId: Long, period: String = "DAY", targetDate: LocalDate = LocalDate.now()): SalesResponse {
        val seller = memberService.findMember(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val allOrders = orderRepository.findByStoreOrderByCreatedAtDesc(store)
            .filter { it.status != OrderStatus.CANCELLED }
        val targetDayOrders = allOrders.filter { it.createdAt.toLocalDate() == targetDate }
        return SalesResponse(
            totalAmount = targetDayOrders.sumOf { it.totalPrice },
            totalOrders = targetDayOrders.size,
            chartData = salesData(period, targetDate, allOrders),
            topProducts = topProducts(allOrders),
            carbonSavedKg = 0.0,
        )
    }

    fun getSellerSalesStats(sellerId: Long, period: String = "DAY", targetDate: LocalDate = LocalDate.now()): SellerSalesResponse {
        val sales = getSalesStats(sellerId, period, targetDate)
        return SellerSalesResponse(
            totalAmount = sales.totalAmount,
            totalOrders = sales.totalOrders,
            chartData = sales.chartData,
            topMenus = topMenus(getStoreOrderEntities(sellerId).filter { it.status != OrderStatus.CANCELLED }),
        )
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────────────

    private fun salesData(period: String, targetDate: LocalDate, orders: List<Orders>): List<DailySales> =
        when (period.uppercase()) {
            "MONTH" -> monthlySales(targetDate, orders)
            "WEEK"  -> weeklySales(targetDate, orders)
            else    -> dailySales(targetDate, orders)  // DAY (기본값)
        }

    private fun dailySales(date: LocalDate, orders: List<Orders>): List<DailySales> =
        (0..23).map { hour ->
            val amount = orders.sumOf { order ->
                if (order.createdAt.toLocalDate() == date && order.createdAt.hour == hour) order.totalPrice else 0
            }
            DailySales("${hour}시", amount)
        }

    private fun monthlySales(targetMonth: LocalDate, orders: List<Orders>): List<DailySales> {
        val firstDay = targetMonth.withDayOfMonth(1)
        val lastDay = targetMonth.with(TemporalAdjusters.lastDayOfMonth())
        return generateSequence(firstDay) { it.plusDays(7) }
            .takeWhile { !it.isAfter(lastDay) }
            .mapIndexed { index, weekStart ->
                val weekEnd = minOf(weekStart.plusDays(6), lastDay)
                DailySales("${index + 1}주", orders.sumBetween(weekStart, weekEnd))
            }
            .toList()
    }

    private fun yearlySales(year: Int, orders: List<Orders>): List<DailySales> =
        (1..12).map { month ->
            DailySales("${month}월", orders.sumOf { order ->
                if (order.createdAt.year == year && order.createdAt.monthValue == month) order.totalPrice else 0
            })
        }

    private fun weeklySales(referenceDate: LocalDate, orders: List<Orders>): List<DailySales> {
        val startDay = referenceDate.minusDays(referenceDate.dayOfWeek.value.toLong() - 1) // 해당 주 월요일
        val formatter = DateTimeFormatter.ofPattern("MM/dd")
        return (0..6).map { i ->
            val date = startDay.plusDays(i.toLong())
            DailySales(date.format(formatter), orders.sumOn(date))
        }
    }

    private fun List<Orders>.sumOn(date: LocalDate) =
        sumOf { if (it.createdAt.toLocalDate() == date) it.totalPrice else 0 }

    private fun List<Orders>.sumBetween(start: LocalDate, end: LocalDate) =
        sumOf {
            val d = it.createdAt.toLocalDate()
            if (!d.isBefore(start) && !d.isAfter(end)) it.totalPrice else 0
        }

    private fun topProducts(orders: List<Orders>): List<TopProduct> =
        orders.flatMap { it.items }
            .groupBy { it.product.productId }
            .values
            .map { items -> TopProduct(productName = items.first().product.name, soldCount = items.sumOf { it.quantity }) }
            .sortedByDescending { it.soldCount }
            .take(3)

    private fun topMenus(orders: List<Orders>): List<TopMenu> =
        orders.flatMap { it.items }
            .groupBy { it.product.productId }
            .values
            .map { items ->
                TopMenu(
                    name = items.first().product.name,
                    count = items.sumOf { it.quantity },
                )
            }
            .sortedByDescending { it.count }
            .take(3)

    private fun generatePickupCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        repeat(10) {
            val code = (1..4).map { chars.random() }.joinToString("")
            if (!orderRepository.existsByPickupCode(code)) return code
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
