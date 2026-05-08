package com.deuktemsiru.service

import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.DailySales
import com.deuktemsiru.dto.OrderResponse
import com.deuktemsiru.dto.SalesResponse
import com.deuktemsiru.dto.TopProduct
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.OrderItem
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Orders
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.repository.OrderRepository
import com.deuktemsiru.repository.ProductRepository
import com.deuktemsiru.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

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
    fun createOrder(consumerId: Long, req: CreateOrderRequest): OrderResponse {
        val consumer = memberService.findMember(consumerId)
        require(consumer.role == MemberRole.CONSUMER) { "소비자 계정만 주문할 수 있습니다." }
        require(req.items.isNotEmpty()) { "주문 항목이 없습니다." }

        val store = storeService.findStore(req.storeId)

        val order = Orders(
            consumer = consumer,
            store = store,
            pickupCode = generatePickupCode(),
        )

        var total = 0
        for (itemReq in req.items) {
            require(itemReq.quantity > 0) { "주문 수량은 1개 이상이어야 합니다." }

            val product = productRepository.findByIdForUpdate(itemReq.productId)
                .orElseThrow { NoSuchElementException("상품을 찾을 수 없습니다.") }
            require(product.store.storeId == store.storeId) { "선택한 가게의 상품만 주문할 수 있습니다." }
            require(product.status == ProductStatus.AVAILABLE) { "${product.name}은(는) 구매 불가 상태입니다." }
            require(product.quantityRemaining >= itemReq.quantity) { "${product.name} 재고가 부족합니다." }

            product.quantityRemaining -= itemReq.quantity
            if (product.quantityRemaining == 0) product.status = ProductStatus.SOLD_OUT

            val lineTotal = product.discountPrice * itemReq.quantity
            total += lineTotal
            order.items.add(OrderItem(order = order, product = product, quantity = itemReq.quantity, unitPrice = product.discountPrice))
        }
        order.totalPrice = total

        return OrderResponse.from(orderRepository.save(order))
    }

    fun getMyOrders(consumerId: Long): List<OrderResponse> {
        val consumer = memberService.findMember(consumerId)
        return orderRepository.findByConsumerOrderByCreatedAtDesc(consumer).map { OrderResponse.from(it) }
    }

    fun getOrder(orderId: Long): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        return OrderResponse.from(order)
    }

    fun getStoreOrders(sellerId: Long): List<OrderResponse> {
        val seller = memberService.findMember(sellerId)
        require(seller.role == MemberRole.SELLER) { "판매자 계정만 주문을 조회할 수 있습니다." }
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        return orderRepository.findByStoreOrderByCreatedAtDesc(store).map { OrderResponse.from(it) }
    }

    @Transactional
    fun updateOrderStatus(sellerId: Long, orderId: Long, req: UpdateOrderStatusRequest): OrderResponse {
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
        return OrderResponse.from(order)
    }

    fun getSalesStats(sellerId: Long, period: String = "weekly", offset: Int = 0): SalesResponse {
        val seller = memberService.findMember(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val allOrders = orderRepository.findByStoreOrderByCreatedAtDesc(store)
            .filter { it.status != OrderStatus.CANCELLED }

        val today = LocalDate.now()
        val todayOrders = allOrders.filter { it.createdAt.toLocalDate() == today }
        val todaySales = todayOrders.sumOf { it.totalPrice }

        val salesData: List<DailySales> = when (period) {
            "monthly" -> {
                val targetMonth = today.minusMonths(offset.toLong())
                val firstDay = targetMonth.withDayOfMonth(1)
                val lastDay = targetMonth.with(TemporalAdjusters.lastDayOfMonth())
                val result = mutableListOf<DailySales>()
                var weekStart = firstDay
                var weekNum = 1
                while (!weekStart.isAfter(lastDay)) {
                    val weekEnd = minOf(weekStart.plusDays(6), lastDay)
                    val amount = allOrders
                        .filter {
                            val d = it.createdAt.toLocalDate()
                            !d.isBefore(weekStart) && !d.isAfter(weekEnd)
                        }
                        .sumOf { it.totalPrice }
                    result.add(DailySales("${weekNum}주", amount))
                    weekStart = weekStart.plusDays(7)
                    weekNum++
                }
                result
            }
            "yearly" -> {
                val targetYear = today.year - offset
                (1..12).map { month ->
                    val amount = allOrders
                        .filter {
                            val d = it.createdAt.toLocalDate()
                            d.year == targetYear && d.monthValue == month
                        }
                        .sumOf { it.totalPrice }
                    DailySales("${month}월", amount)
                }
            }
            else -> {
                val endDay = today.minusDays((offset * 7).toLong())
                val startDay = endDay.minusDays(6)
                val formatter = DateTimeFormatter.ofPattern("MM/dd")
                (0..6).map { i ->
                    val date = startDay.plusDays(i.toLong())
                    val amount = allOrders
                        .filter { it.createdAt.toLocalDate() == date }
                        .sumOf { it.totalPrice }
                    DailySales(date.format(formatter), amount)
                }
            }
        }

        val productCount = mutableMapOf<Long, Pair<String, Int>>()
        allOrders.flatMap { it.items }.forEach { item ->
            val id = item.product.productId
            val (name, count) = productCount.getOrDefault(id, Pair(item.product.name, 0))
            productCount[id] = Pair(name, count + item.quantity)
        }
        val topProducts = productCount.values
            .sortedByDescending { it.second }
            .take(3)
            .map { (name, count) -> TopProduct(name, count) }

        return SalesResponse(
            todaySales = todaySales,
            todayOrderCount = todayOrders.size,
            salesData = salesData,
            topProducts = topProducts,
        )
    }

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
            OrderStatus.PICKED_UP,
            OrderStatus.CANCELLED -> false
        }
    }
}
