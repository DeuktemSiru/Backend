package com.deuktemsiru.service

import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.DailySales
import com.deuktemsiru.dto.OrderResponse
import com.deuktemsiru.dto.SalesResponse
import com.deuktemsiru.dto.TopMenu
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.entity.Order
import com.deuktemsiru.entity.OrderItem
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.UserRole
import com.deuktemsiru.repository.MenuItemRepository
import com.deuktemsiru.repository.OrderRepository
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
    private val menuItemRepository: MenuItemRepository,
    private val userService: UserService,
    private val storeService: StoreService,
) {

    @Transactional
    fun createOrder(buyerId: Long, req: CreateOrderRequest): OrderResponse {
        val buyer = userService.findUser(buyerId)
        require(buyer.role == UserRole.CONSUMER || buyer.role == UserRole.BUYER) { "구매자 계정만 주문할 수 있습니다." }
        require(req.items.isNotEmpty()) { "주문 항목이 없습니다." }

        val store = storeService.findStore(req.storeId)

        val order = Order(
            buyer = buyer,
            store = store,
            pickupCode = generatePickupCode(),
            pickupTime = req.pickupTime,
        )

        var total = 0
        var savedAmount = 0
        for (itemReq in req.items) {
            require(itemReq.quantity > 0) { "주문 수량은 1개 이상이어야 합니다." }

            val menuItem = menuItemRepository.findByIdForUpdate(itemReq.menuItemId)
                .orElseThrow { NoSuchElementException("메뉴를 찾을 수 없습니다.") }
            require(menuItem.store.id == store.id) { "선택한 가게의 메뉴만 주문할 수 있습니다." }
            require(!menuItem.isSoldOut) { "${menuItem.name}은(는) 품절되었습니다." }
            require(menuItem.remainingItems >= itemReq.quantity) { "${menuItem.name} 재고가 부족합니다." }

            menuItem.remainingItems -= itemReq.quantity
            if (menuItem.remainingItems == 0) menuItem.isSoldOut = true

            val lineTotal = menuItem.discountedPrice * itemReq.quantity
            total += lineTotal
            savedAmount += (menuItem.originalPrice - menuItem.discountedPrice) * itemReq.quantity
            order.items.add(OrderItem(order = order, menuItem = menuItem, quantity = itemReq.quantity, price = lineTotal))
        }
        order.totalAmount = total

        val saved = orderRepository.save(order)
        userService.updateSavings(buyerId, savedAmount)
        return OrderResponse.from(saved)
    }

    fun getMyOrders(buyerId: Long): List<OrderResponse> {
        val buyer = userService.findUser(buyerId)
        return orderRepository.findByBuyerOrderByCreatedAtDesc(buyer).map { OrderResponse.from(it) }
    }

    fun getOrder(orderId: Long): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        return OrderResponse.from(order)
    }

    fun getStoreOrders(sellerId: Long): List<OrderResponse> {
        val seller = userService.findUser(sellerId)
        require(seller.role == UserRole.SELLER) { "판매자 계정만 주문을 조회할 수 있습니다." }
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        return orderRepository.findByStoreOrderByCreatedAtDesc(store).map { OrderResponse.from(it) }
    }

    @Transactional
    fun updateOrderStatus(sellerId: Long, orderId: Long, req: UpdateOrderStatusRequest): OrderResponse {
        val seller = userService.findUser(sellerId)
        require(seller.role == UserRole.SELLER) { "판매자 계정만 주문 상태를 변경할 수 있습니다." }
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val order = orderRepository.findById(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다.") }
        require(order.store.id == store.id) { "접근 권한이 없습니다." }
        require(canTransition(order.status, req.status)) {
            "주문 상태를 ${order.status}에서 ${req.status}(으)로 변경할 수 없습니다."
        }
        order.status = req.status
        return OrderResponse.from(order)
    }

    fun getSalesStats(sellerId: Long, period: String = "weekly", offset: Int = 0): SalesResponse {
        val seller = userService.findUser(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val allOrders = orderRepository.findByStoreOrderByCreatedAtDesc(store)
            .filter { it.status != OrderStatus.REJECTED }

        val today = LocalDate.now()
        val todayOrders = allOrders.filter { it.createdAt.toLocalDate() == today }
        val todaySales = todayOrders.sumOf { it.totalAmount }

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
                        .sumOf { it.totalAmount }
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
                        .sumOf { it.totalAmount }
                    DailySales("${month}월", amount)
                }
            }
            else -> {
                // weekly: 최근 7일 기준, offset 1 = 지난주
                val endDay = today.minusDays((offset * 7).toLong())
                val startDay = endDay.minusDays(6)
                val formatter = DateTimeFormatter.ofPattern("MM/dd")
                (0..6).map { i ->
                    val date = startDay.plusDays(i.toLong())
                    val amount = allOrders
                        .filter { it.createdAt.toLocalDate() == date }
                        .sumOf { it.totalAmount }
                    DailySales(date.format(formatter), amount)
                }
            }
        }

        val menuCount = mutableMapOf<Long, Triple<String, String, Int>>()
        allOrders.flatMap { it.items }.forEach { item ->
            val id = item.menuItem.id
            val (name, emoji, count) = menuCount.getOrDefault(id, Triple(item.menuItem.name, item.menuItem.emoji, 0))
            menuCount[id] = Triple(name, emoji, count + item.quantity)
        }
        val topMenus = menuCount.values
            .sortedByDescending { it.third }
            .take(3)
            .map { (name, emoji, count) -> TopMenu(name, emoji, count) }

        return SalesResponse(
            todaySales = todaySales,
            todayOrderCount = todayOrders.size,
            salesData = salesData,
            topMenus = topMenus,
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
            OrderStatus.NEW -> next == OrderStatus.PREPARING || next == OrderStatus.REJECTED
            OrderStatus.PREPARING -> next == OrderStatus.READY || next == OrderStatus.REJECTED
            OrderStatus.READY -> next == OrderStatus.COMPLETED
            OrderStatus.COMPLETED,
            OrderStatus.REJEC