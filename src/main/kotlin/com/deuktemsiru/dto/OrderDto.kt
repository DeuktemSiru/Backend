package com.deuktemsiru.dto

import com.deuktemsiru.entity.Order
import com.deuktemsiru.entity.OrderItem
import com.deuktemsiru.entity.OrderStatus
import java.time.LocalDateTime

data class OrderItemRequest(
    val menuItemId: Long,
    val quantity: Int,
)

data class CreateOrderRequest(
    val storeId: Long,
    val items: List<OrderItemRequest>,
    val pickupTime: String,
)

data class OrderItemResponse(
    val menuItemId: Long,
    val name: String,
    val emoji: String,
    val quantity: Int,
    val price: Int,
) {
    companion object {
        fun from(item: OrderItem) = OrderItemResponse(
            menuItemId = item.menuItem.id,
            name = item.menuItem.name,
            emoji = item.menuItem.emoji,
            quantity = item.quantity,
            price = item.price,
        )
    }
}

data class OrderResponse(
    val id: Long,
    val orderNumber: String,
    val storeId: Long,
    val storeName: String,
    val status: OrderStatus,
    val pickupCode: String,
    val pickupTime: String,
    val totalAmount: Int,
    val createdAt: LocalDateTime,
    val items: List<OrderItemResponse>,
) {
    companion object {
        fun from(order: Order) = OrderResponse(
            id = order.id,
            orderNumber = "OM-%04d".format(order.id),
            storeId = order.store.id,
            storeName = order.store.name,
            status = order.status,
            pickupCode = order.pickupCode,
            pickupTime = order.pickupTime,
            totalAmount = order.totalAmount,
            createdAt = order.createdAt,
            items = order.items.map { OrderItemResponse.from(it) },
        )
    }
}

data class UpdateOrderStatusRequest(
    val status: OrderStatus,
)

data class SalesResponse(
    val todaySales: Int,
    val todayOrderCount: Int,
    val salesData: List<DailySales>,
    val topMenus: List<TopMenu>,
)

data class DailySales(val date: String, val amount: Int)
data class TopMenu(val name: String, val emoji: String, val count: Int)
