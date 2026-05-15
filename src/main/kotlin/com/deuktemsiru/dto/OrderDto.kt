package com.deuktemsiru.dto

import com.deuktemsiru.entity.OrderItem
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Orders
import com.deuktemsiru.entity.Payment
import java.time.LocalDateTime

// ── 요청 ─────────────────────────────────────────────────────────────────────

data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
)

data class CreateOrderRequest(
    val items: List<OrderItemRequest>,
    val paymentMethod: String? = null,  // SIRU / CARD / CASH (TBD)
)

// ── 응답 공통 ─────────────────────────────────────────────────────────────────

data class PaymentInfo(
    val method: String,
    val status: String,
)

// ── POST /orders 응답 ─────────────────────────────────────────────────────────
data class CreateOrderResponse(
    val orderId: Long,
    val pickupCode: String?,
    val status: OrderStatus,
    val totalPrice: Int,
    val payment: PaymentInfo,
) {
    companion object {
        fun from(order: Orders, payment: Payment?) = CreateOrderResponse(
            orderId = order.orderId,
            pickupCode = order.pickupCode,
            status = order.status,
            totalPrice = order.totalPrice,
            payment = PaymentInfo(
                method = payment?.method?.name ?: "CASH",
                status = payment?.status?.name ?: "PENDING",
            ),
        )
    }
}

// ── GET /orders/{orderId} 응답 ────────────────────────────────────────────────
data class OrderItemDetailResponse(
    val productName: String,
    val quantity: Int,
    val unitPrice: Int,
) {
    companion object {
        fun from(item: OrderItem) = OrderItemDetailResponse(
            productName = item.product.name,
            quantity = item.quantity,
            unitPrice = item.unitPrice,
        )
    }
}

data class OrderDetailResponse(
    val orderId: Long,
    val pickupCode: String?,
    val status: OrderStatus,
    val totalPrice: Int,
    val storeName: String,
    val items: List<OrderItemDetailResponse>,
    val payment: PaymentInfo,
) {
    companion object {
        fun from(order: Orders, payment: Payment? = null) = OrderDetailResponse(
            orderId = order.orderId,
            pickupCode = order.pickupCode,
            status = order.status,
            totalPrice = order.totalPrice,
            storeName = order.store.name,
            items = order.items.map { OrderItemDetailResponse.from(it) },
            payment = PaymentInfo(
                method = payment?.method?.name ?: "CASH",
                status = payment?.status?.name
                    ?: if (order.status == OrderStatus.CANCELLED) "REFUNDED" else "PENDING",
            ),
        )
    }
}

// ── GET /orders/my 목록 아이템 ────────────────────────────────────────────────
data class OrderListItemResponse(
    val orderId: Long,
    val storeName: String,
    val status: OrderStatus,
    val totalPrice: Int,
    val pickupCode: String?,
    val createdAt: LocalDateTime,
    val itemCount: Int,
) {
    companion object {
        fun from(order: Orders) = OrderListItemResponse(
            orderId = order.orderId,
            storeName = order.store.name,
            status = order.status,
            totalPrice = order.totalPrice,
            pickupCode = order.pickupCode,
            createdAt = order.createdAt,
            itemCount = order.items.size,
        )
    }
}

// ── 판매자/기타 내부 용도 ─────────────────────────────────────────────────────
data class UpdateOrderStatusRequest(
    val status: OrderStatus,
)

data class DailySales(val date: String, val amount: Int)
data class TopProduct(val productName: String, val soldCount: Int)
data class TopMenu(val name: String, val count: Int)

data class SalesResponse(
    val totalAmount: Int,
    val totalOrders: Int,
    val chartData: List<DailySales>,
    val topProducts: List<TopProduct>,
    val carbonSavedKg: Double = 0.0,
)

data class SellerSalesResponse(
    val totalAmount: Int,
    val totalOrders: Int,
    val chartData: List<DailySales>,
    val topMenus: List<TopMenu>,
)

internal fun categoryEmoji(category: String?) = when (category) {
    "BAKERY" -> "🥐"
    "CAFE" -> "☕"
    "RESTAURANT" -> "🍱"
    "GROCERY" -> "🥦"
    else -> "🍽️"
}
