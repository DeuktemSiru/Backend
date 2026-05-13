package com.deuktemsiru.dto

import com.deuktemsiru.entity.OrderItem
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Orders
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "주문 상품 요청")
data class OrderItemRequest(
    @field:Schema(description = "상품 ID. menuItemId와 호환을 위해 둘 중 하나를 전달합니다.", example = "1", nullable = true)
    val productId: Long? = null,
    @field:Schema(description = "앱 호환용 메뉴 상품 ID. productId와 동일하게 처리됩니다.", example = "1", nullable = true)
    val menuItemId: Long? = null,
    @field:Schema(description = "주문 수량", example = "2")
    val quantity: Int,
) {
    fun resolvedProductId(): Long =
        productId ?: menuItemId ?: throw IllegalArgumentException("상품 ID가 없습니다.")
}

@Schema(description = "주문 생성 요청")
data class CreateOrderRequest(
    @field:Schema(description = "가게 ID", example = "1")
    val storeId: Long,
    @field:Schema(description = "주문 상품 목록")
    val items: List<OrderItemRequest>,
    @field:Schema(description = "희망 픽업 시간", example = "18:00", nullable = true)
    val pickupTime: String? = null,
)

@Schema(description = "주문 상품 응답")
data class OrderItemResponse(
    @field:Schema(description = "상품 ID", example = "1")
    val productId: Long,
    @field:Schema(description = "상품명", example = "크루아상")
    val name: String,
    @field:Schema(description = "수량", example = "2")
    val quantity: Int,
    @field:Schema(description = "단가", example = "2500")
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

@Schema(description = "주문 응답")
data class OrderResponse(
    @field:Schema(description = "주문 ID", example = "1")
    val orderId: Long,
    @field:Schema(description = "가게 ID", example = "1")
    val storeId: Long,
    @field:Schema(description = "가게 이름", example = "시루 베이커리")
    val storeName: String,
    @field:Schema(description = "주문 상태", allowableValues = ["PENDING", "PREPARING", "READY", "COMPLETED", "CANCELLED"], example = "PENDING")
    val status: OrderStatus,
    @field:Schema(description = "픽업 코드", example = "1234", nullable = true)
    val pickupCode: String?,
    @field:Schema(description = "총 결제 금액", example = "5000")
    val totalPrice: Int,
    @field:Schema(description = "주문 생성 시각")
    val createdAt: LocalDateTime,
    @field:Schema(description = "주문 상품 목록")
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

@Schema(description = "판매자 주문 상태 변경 요청")
data class UpdateOrderStatusRequest(
    @field:Schema(description = "변경할 주문 상태", allowableValues = ["PENDING", "PREPARING", "READY", "COMPLETED", "CANCELLED"], example = "READY")
    val status: OrderStatus,
)

@Schema(description = "앱 주문 상품 응답")
data class AppOrderItemResponse(
    @field:Schema(description = "상품 ID", example = "1")
    val productId: Long,
    @field:Schema(description = "앱 호환용 메뉴 상품 ID", example = "1")
    val menuItemId: Long,
    @field:Schema(description = "상품명", example = "크루아상")
    val name: String,
    @field:Schema(description = "카테고리 이모지", example = "🥐")
    val emoji: String,
    @field:Schema(description = "수량", example = "2")
    val quantity: Int,
    @field:Schema(description = "단가", example = "2500")
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

@Schema(description = "앱 주문 응답")
data class AppOrderResponse(
    @field:Schema(description = "주문 ID", example = "1")
    val id: Long,
    @field:Schema(description = "표시용 주문 번호", example = "ORDER-000001")
    val orderNumber: String,
    @field:Schema(description = "가게 ID", example = "1")
    val storeId: Long,
    @field:Schema(description = "가게 이름", example = "시루 베이커리")
    val storeName: String,
    @field:Schema(description = "주문 상태", allowableValues = ["PENDING", "PREPARING", "READY", "COMPLETED", "CANCELLED"], example = "PENDING")
    val status: String,
    @field:Schema(description = "픽업 코드", example = "1234")
    val pickupCode: String,
    @field:Schema(description = "픽업 가능 시간", example = "18:00")
    val pickupTime: String,
    @field:Schema(description = "총 주문 금액", example = "5000")
    val totalAmount: Int,
    @field:Schema(description = "주문 생성 시각", example = "2026-05-13T18:30:00")
    val createdAt: String,
    @field:Schema(description = "주문 상품 목록")
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

@Schema(description = "판매 통계 응답")
data class SalesResponse(
    @field:Schema(description = "오늘 매출", example = "52000")
    val todaySales: Int,
    @field:Schema(description = "오늘 주문 수", example = "13")
    val todayOrderCount: Int,
    @field:Schema(description = "일별 매출 데이터")
    val salesData: List<DailySales>,
    @field:Schema(description = "상위 상품 목록")
    val topProducts: List<TopProduct>,
)

@Schema(description = "일별 매출")
data class DailySales(
    @field:Schema(description = "날짜 또는 구간 라벨", example = "2026-05-13")
    val date: String,
    @field:Schema(description = "매출 금액", example = "52000")
    val amount: Int,
)

@Schema(description = "상위 상품")
data class TopProduct(
    @field:Schema(description = "상품명", example = "크루아상")
    val name: String,
    @field:Schema(description = "판매 수량", example = "7")
    val count: Int,
)

@Schema(description = "상위 메뉴")
data class TopMenu(
    @field:Schema(description = "메뉴명", example = "크루아상")
    val name: String,
    @field:Schema(description = "카테고리 이모지", example = "🥐")
    val emoji: String,
    @field:Schema(description = "판매 수량", example = "7")
    val count: Int,
)

@Schema(description = "판매자 매출 통계 응답")
data class SellerSalesResponse(
    @field:Schema(description = "오늘 매출", example = "52000")
    val todaySales: Int,
    @field:Schema(description = "오늘 주문 수", example = "13")
    val todayOrderCount: Int,
    @field:Schema(description = "일별 또는 기간별 매출 데이터")
    val salesData: List<DailySales>,
    @field:Schema(description = "판매 상위 메뉴")
    val topMenus: List<TopMenu>,
)

internal fun categoryEmoji(category: String?) = when (category) {
    "BAKERY" -> "🥐"
    "CAFE" -> "☕"
    "RESTAURANT" -> "🍱"
    "GROCERY" -> "🥦"
    else -> "🍽️"
}
