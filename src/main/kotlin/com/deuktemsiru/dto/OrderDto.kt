package com.deuktemsiru.dto

import com.deuktemsiru.entity.OrderItem
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Orders
import java.time.LocalDateTime

data class OrderItemRequest(
    val productId: Long? = null,
    val menuItemId: Long? = null,
    val quantity: Int,
) {
    fun resolvedProductId(): Long =
        productId ?: menuItemId ?: throw IllegalArgumentException("상품 ID가 없습니다.")
}

data class CreateOrderRequest(
    val storeId: Long,
    val items: List<OrderItemRequest>,
    val pickupTime: String? = null,
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

data class AppOrderItemResponse(
    val productId: Long,
    val menuItemId: Long,
    val name: String,
    val emoji: String,
    val quantity: Int,
    val price: Int,
) {
    companion object {
        fun from(item: OrderItem) = AppOrderItemResponse(
            productId = item.product.productId,
            menuItemId = item.product.productId,
            name = item.product.name,
            emoji = categoryEmoji(item.product.store.categories.firstOrNull()?.category?.name),
            quantity = item.quantity,
            price = item.unitPrice,
        )
    }
}

data class AppOrderResponse(
    val id: Long,
    val orderNumber: String,
    val storeId: Long,
    val storeName: String,
    val status: String,
    val pickupCode: String,
    val pickupTime: String,
    val totalAmount: Int,
    val createdAt: String,
    val items: List<AppOrderItemResponse>,
) {
    companion object {
        fun from(order: Orders): AppOrderResponse {
            val pickupTime = order.items.maxOfOrNull { it.product.pickupEnd }?.toString() ?: ""
            return AppOrderResponse(
                id = order.orderId,
                orderNumber = "ORDER-%06d".format(order.orderId),
                storeId = order.store.storeId,
                storeName = order.store.name,
                status = order.status.name,
                pickupCode = order.pickupCode.orEmpty(),
                pickupTime = pickupTime,
                totalAmount = order.totalPrice,
                createdAt = order.createdAt.toString(),
                items = order.items.map { AppOrderItemResponse.from(it) },
            )
        }
    }
}

data class SalesResponse(
    val todaySales: Int,
    val todayOrderCount: Int,
    val salesData: List<DailySales>,
    val topProducts: List<TopProduct>,
)

data class DailySales(val date: String, val amount: Int)
data class TopProduct(val name: String, val count: Int)

data class TopMenu(val name: String, val emoji: String, val count: Int)

data class SellerSalesResponse(
    val todaySales: Int,
    val todayOrderCount: Int,
    val salesData: List<DailySales>,
    val topMenus: List<TopMenu>,
)

internal fun categoryEmoji(category: String?) = when (category) {
    "BAKERY" -> "🥐"
    "CAFE" -> "☕"
    "RESTAURANT" -> "🍱"
    "GROCERY" -> "🥦"
    else -> "🍽️"
}
