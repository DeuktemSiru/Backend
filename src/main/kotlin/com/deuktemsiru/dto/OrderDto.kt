package com.deuktemsiru.dto

import com.deuktemsiru.entity.OrderItem
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Orders
import java.time.LocalDateTime

data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
)

data class CreateOrderRequest(
    val storeId: Long,
    val items: List<OrderItemRequest>,
)

data class OrderItemResponse(
    val productId: Long,
    val name: String,
    val quantity: Int,
    val unitPrice: Int,
) {
    companion object {
        fun from(item: OrderItem) = OrderItemResponse(
            productId = item.product.productId,
            name = item.product.name,
            quantity = item.quantity,
            unitPrice = item.unitPrice,
        )
    }
}

data class OrderResponse(
    val orderId: Long,
    val storeId: Long,
    val storeName: String,
    val status: OrderStatus,
    val pickupCode: String?,
    val totalPrice: Int,
    val createdAt: LocalDateTime,
    val items: List<OrderItemResponse>,
) {
    companion object {
        fun from(order: Orders) = OrderResponse(
            orderId = order.orderId,
            storeId = order.store.storeId,
            storeName = order.store.name,
            status = order.status,
            pickupCode = order.pickupCode,
            totalPrice = order.totalPrice,
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
    val topProducts: List<TopProduct>,
)

data class DailySales(val date: String, val amount: Int)
data class TopProduct(val name: String, val count: Int)
