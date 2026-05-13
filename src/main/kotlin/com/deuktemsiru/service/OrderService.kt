package com.deuktemsiru.service

import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.DailySales
import com.deuktemsiru.dto.OrderItemRequest
import com.deuktemsiru.dto.OrderResponse
import com.deuktemsiru.dto.SalesResponse
import com.deuktemsiru.dto.SellerSalesResponse
import com.deuktemsiru.dto.TopProduct
import com.deuktemsiru.dto.TopMenu
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
        return OrderResponse.from(createOrderEntity(consumerId, req))
    }

    @Transactional
    fun createOrderEntity(consumerId: Long, req: CreateOrderRequest): Orders {
        val consumer = memberService.findMember(consumerId)
        require(consumer.role == MemberRole.CONSUMER) { "소비자 계정만 주문할 수 있습니다." }
        require(req.items.isNotEmpty()) { "주문 항목이 없습니다." }

        val store = storeService.findStore(req.storeId)
        val order = Orders(
            consumer = consumer,
            store = store,
            pickupCode = generatePickupCode(),
        )

        order.items += req.items.map { createOrderItem(order, store, it) }
        order.totalPrice = order.items.sumOf { it.unitPrice * it.quantity }

        return orderRepository.save(order)
    }

    fun getMyOrders(consumerId: Long, statusFilter: String? = null): List<OrderResponse> {
        val orders = getMyOrderEntities(consumerId)
        val filtered = if (statusFilter != null) {
            val target = runCatching { OrderStatus.valueOf(statusFilter.uppercase()) }
                .getOrElse { throw IllegalArgumentException("지원하지 않는 주문 상태입니다: $statusFilter") }
            orders.filter { it.status == target }
        } else orders
        return filtered.map { OrderResponse.from(it) }
    }

    fun getMyOrderEntities(consumerId: Long): List<Orders> {
        val consumer = memberService.findMember(consumerId)
        return orderRepository.findByConsumerOrderByCreatedAtDesc(consumer)
    }

    fun getOrder(orderId: Long): OrderResponse {
        return OrderResponse.from(getOrderEntity(orderId))
    }

    fun getOrderEntity(orderId: Long): Orders {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        return order
    }

    fun getStoreOrders(sellerId: Long): List<OrderResponse> {
        return getStoreOrderEntities(sellerId).map { OrderResponse.from(it) }
    }

    fun getStoreOrder(sellerId: Long, orderId: Long): OrderResponse {
        val seller = memberService.findMember(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        require(order.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        return OrderResponse.from(order)
    }

    fun getStoreOrderEntities(sellerId: Long): List<Orders> {
        val seller = memberService.findMember(sellerId)
        require(seller.role == MemberRole.SELLER) { "판매자 계정만 주문을 조회할 수 있습니다." }
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        return orderRepository.findByStoreOrderByCreatedAtDesc(store)
    }

    @Transactional
    fun updateOrderStatus(sellerId: Long, orderId: Long, req: UpdateOrderStatusRequest): OrderResponse {
        return OrderResponse.from(updateOrderStatusEntity(sellerId, orderId, req))
    }

    @Transactional
    fun updateOrderStatusEntity(sellerId: Long, orderId: Long, req: UpdateOrderStatusRequest): Orders {
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
        return order
    }

    @Transactional
    fun verifyPickupCode(sellerId: Long, pickupCode: String): Orders {
        val seller = memberService.findMember(sellerId)
        require(seller.role == MemberRole.SELLER) { "판매자 계정만 픽업 코드를 검증할 수 있습니다." }
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val order = orderRepository.findByPickupCode(pickupCode)
            ?: throw NoSuchElementException("픽업 코드를 찾을 수 없습니다.")
        require(order.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        if (order.status == OrderStatus.CONFIRMED) order.status = OrderStatus.PICKED_UP
        return order
    }

    fun getSalesStats(sellerId: Long, period: String = "weekly", offset: Int = 0): SalesResponse {
        val seller = memberService.findMember(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val allOrders = orderRepository.findByStoreOrderByCreatedAtDesc(store)
            .filter { it.status != OrderStatus.CANCELLED }

        val today = LocalDate.now()
        val todayOrders = allOrders.filter { it.createdAt.toLocalDate() == today }

        return SalesResponse(
            todaySales = todayOrders.sumOf { it.totalPrice },
            todayOrderCount = todayOrders.size,
            salesData = salesData(period, offset, today, allOrders),
            topProducts = topProducts(allOrders),
        )
    }

    fun getSellerSalesStats(sellerId: Long, period: String = "weekly", offset: Int = 0): SellerSalesResponse {
        val sales = getSalesStats(sellerId, period, offset)
        return SellerSalesResponse(
            todaySales = sales.todaySales,
            todayOrderCount = sales.todayOrderCount,
            salesData = sales.salesData,
            topMenus = topMenus(getStoreOrderEntities(sellerId).filter { it.status != OrderStatus.CANCELLED }),
        )
    }

    private fun createOrderItem(
        order: Orders,
        store: Store,
        itemReq: OrderItemRequest,
    ): OrderItem {
        require(itemReq.quantity > 0) { "주문 수량은 1개 이상이어야 합니다." }

        val product = productRepository.findByIdForUpdate(itemReq.resolvedProductId())
            .orElseThrow { NoSuchElementException("상품을 찾을 수 없습니다.") }
        validateProduct(product, store, itemReq.quantity)

        product.quantityRemaining -= itemReq.quantity
        if (product.quantityRemaining == 0) product.status = ProductStatus.SOLD_OUT

        return OrderItem(
            order = order,
            product = product,
            quantity = itemReq.quantity,
            unitPrice = product.discountPrice,
        )
    }

    private fun validateProduct(product: Product, store: Store, quantity: Int) {
        require(product.store.storeId == store.storeId) { "선택한 가게의 상품만 주문할 수 있습니다." }
        require(product.status == ProductStatus.AVAILABLE) { "${product.name}은(는) 구매 불가 상태입니다." }
        require(product.quantityRemaining >= quantity) { "${product.name} 재고가 부족합니다." }
    }

    private fun salesData(
        period: String,
        offset: Int,
        today: LocalDate,
        orders: List<Orders>,
    ): List<DailySales> = when (period) {
        "monthly" -> monthlySales(today.minusMonths(offset.toLong()), orders)
        "yearly" -> yearlySales(today.year - offset, orders)
        else -> weeklySales(today.minusDays((offset * 7).toLong()), orders)
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

    private fun weeklySales(endDay: LocalDate, orders: List<Orders>): List<DailySales> {
        val startDay = endDay.minusDays(6)
        val formatter = DateTimeFormatter.ofPattern("MM/dd")
        return (0..6).map { i ->
            val date = startDay.plusDays(i.toLong())
            DailySales(date.format(formatter), orders.sumOn(date))
        }
    }

    private fun List<Orders>.sumOn(date: LocalDate): Int =
        sumOf { if (it.createdAt.toLocalDate() == date) it.totalPrice else 0 }

    private fun List<Orders>.sumBetween(start: LocalDate, end: LocalDate): Int =
        sumOf {
            val date = it.createdAt.toLocalDate()
            if (!date.isBefore(start) && !date.isAfter(end)) it.totalPrice else 0
        }

    private fun topProducts(orders: List<Orders>): List<TopProduct> =
        orders.flatMap { it.items }
            .groupBy { it.product.productId }
            .values
            .map { items -> TopProduct(items.first().product.name, items.sumOf { it.quantity }) }
            .sortedByDescending { it.count }
            .take(3)

    private fun topMenus(orders: List<Orders>): List<TopMenu> =
        orders.flatMap { it.items }
            .groupBy { it.product.productId }
            .values
            .map { items ->
                val product = items.first().product
                TopMenu(
                    name = product.name,
                    emoji = categoryEmoji(product.store.categories.firstOrNull()?.category?.name),
                    count = items.sumOf { it.quan