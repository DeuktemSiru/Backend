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
    val pickupTime: String? = null,
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
    val pickupTime: String?,
    val status: OrderStatus,
    val totalPrice: Int,
    val payment: PaymentInfo,
) {
    companion object {
        fun from(order: Orders, payment: Payment?) = CreateOrderResponse(
            orderId = order.orderId,
            pickupCode = order.pickupCode,
            pickupTime = order.pickupTime?.toString(),
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
    val productId: Long,
    val menuItemId: Long?,
    val productName: String,
    val quantity: Int,
    val unitPrice: Int,
) {
    companion object {
        fun from(item: OrderItem) = OrderItemDetailResponse(
            productId = item.product.productId,
            menuItemId = item.product.menuItem?.menuItemId,
            productName = item.product.name,
            quantity = item.quantity,
            unitPrice = item.unitPrice,
        )
    }
}

data class OrderDetailResponse(
    val orderId: Long,
    val orderNumber: String,
    val customerName: String,
    val pickupCode: String?,
    val pickupTime: String?,
    val status: OrderStatus,
    val totalPrice: Int,
    val storeName: String,
    val items: List<OrderItemDetailResponse>,
    val payment: PaymentInfo,
    val createdAt: String,
) {
    companion object {
        fun from(order: Orders, payment: Payment? = null): OrderDetailResponse {
            val firstProduct = order.items.firstOrNull()?.product
            val pickupTime = order.pickupTime?.toString()
                ?: firstProduct?.let { "${it.pickupStart}~${it.pickupEnd}" }
                ?: "정보 없음"
            return OrderDetailResponse(
                orderId = order.orderId,
                orderNumber = "#${order.orderId}",
                customerName = order.consumer.nickname,
                pickupCode = order.pickupCode,
                pickupTime = pickupTime,
                status = order.status,
                totalPrice = order.totalPrice,
                storeName = order.store.name,
                items = order.items.map { OrderItemDetailResponse.from(it) },
                payment = PaymentInfo(
                    method = payment?.method?.name ?: "CASH",
                    status = payment?.status?.name
                        ?: if (order.status == OrderStatus.CANCELLED) "REFUNDED" else "PENDING",
                ),
                createdAt = order.createdAt.toString(),
            )
        }
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

data class PickupConfirmRequest(val pickupCode: String)

data class DailySales(val date: String, val amount: Int)
data class TopProduct(val productName: String, val soldCount: Int)

data class SalesResponse(
    val totalAmount: Int,
    val totalOrders: Int,
    val chartData: List<DailySales>,
    val topProducts: List<TopProduct>,
    val carbonSavedKg: Double = 0.0,
)
