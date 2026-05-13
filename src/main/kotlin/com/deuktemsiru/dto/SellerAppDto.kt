package com.deuktemsiru.dto

import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

@Schema(description = "판매 상품 등록 요청")
data class SaleItemRequest(
    @field:Schema(description = "기준 메뉴 ID", example = "1")
    val menuItemId: Long,
    @field:Schema(description = "할인율(%)", example = "30")
    val discountRate: Int,
    @field:Schema(description = "판매 수량", example = "10")
    val quantity: Int,
    @field:Schema(description = "픽업 가능 시간대", example = "18:00-20:00")
    val pickupTimeSlot: String,
)

@Schema(description = "판매 상품 상태 변경 요청")
data class UpdateSaleStatusRequest(
    @field:Schema(description = "판매 상태", allowableValues = ["AVAILABLE", "SOLD_OUT", "EXPIRED", "CANCELLED"], example = "SOLD_OUT")
    val status: String,
)

@Schema(description = "판매자 상품 응답")
data class SellerSaleItemResponse(
    @field:Schema(description = "상품 ID", example = "1")
    val id: Long,
    @field:Schema(description = "메뉴 ID", example = "1")
    val menuItemId: Long,
    @field:Schema(description = "상품명", example = "크루아상")
    val name: String,
    @field:Schema(description = "카테고리 이모지", example = "🥐")
    val emoji: String,
    @field:Schema(description = "정상가", example = "5000")
    val originalPrice: Int,
    @field:Schema(description = "할인가", example = "3500")
    val discountedPrice: Int,
    @field:Schema(description = "할인율(%)", example = "30")
    val discountRate: Int,
    @field:Schema(description = "남은 수량", example = "4")
    val remainingItems: Int,
    @field:Schema(description = "총 수량", example = "10")
    val totalItems: Int,
    @field:Schema(description = "판매 상태", allowableValues = ["AVAILABLE", "SOLD_OUT", "EXPIRED", "CANCELLED"], example = "AVAILABLE")
    val status: String,
    @field:Schema(description = "픽업 가능 시간대", example = "18:00-20:00")
    val pickupTimeSlot: String,
) {
    companion object {
        fun from(product: Product) = SellerSaleItemResponse(
            id = product.productId,
            menuItemId = product.menuItem?.menuItemId ?: product.productId,
            name = product.name,
            emoji = categoryEmoji(product.store.categories.firstOrNull()?.category?.name),
            originalPrice = product.originalPrice,
            discountedPrice = product.discountPrice,
            discountRate = discountRate(product.originalPrice, product.discountPrice),
            remainingItems = product.quantityRemaining,
            totalItems = product.quantityTotal,
            status = product.status.name,
            pickupTimeSlot = "${product.pickupStart}-${product.pickupEnd}",
        )
    }
}

@Schema(description = "판매자 메뉴 등록 요청")
data class SellerMenuItemRequest(
    @field:Schema(description = "메뉴명", example = "크루아상")
    val name: String,
    @field:Schema(description = "앱 표시용 이모지", example = "🥐")
    val emoji: String = "",
    @field:Schema(description = "정상가", example = "5000")
    val originalPrice: Int,
    @field:Schema(description = "원가", example = "2500", nullable = true)
    val costPrice: Int? = null,
    @field:Schema(description = "알레르기 정보", example = "밀, 우유", nullable = true)
    val allergyInfo: String? = null,
)

@Schema(description = "판매자 메뉴 수정 요청")
data class SellerMenuItemUpdateRequest(
    @field:Schema(description = "메뉴명", example = "크루아상", nullable = true)
    val name: String? = null,
    @field:Schema(description = "정상가", example = "5000", nullable = true)
    val originalPrice: Int? = null,
)

@Schema(description = "판매자 메뉴 응답")
data class SellerMenuItemResponse(
    @field:Schema(description = "메뉴 ID", example = "1")
    val id: Long,
    @field:Schema(description = "메뉴명", example = "크루아상")
    val name: String,
    @field:Schema(description = "카테고리 이모지", example = "🥐")
    val emoji: String,
    @field:Schema(description = "메뉴 이미지 URL", example = "/uploads/menus/menu.png", nullable = true)
    val imageUrl: String? = null,
    @field:Schema(description = "정상가", example = "5000")
    val originalPrice: Int,
) {
    companion object {
        fun from(item: MenuItem) = SellerMenuItemResponse(
            id = item.menuItemId,
            name = item.name,
            emoji = categoryEmoji(item.store.categories.firstOrNull()?.category?.name),
            imageUrl = item.imageUrl,
            originalPrice = item.originalPrice,
        )
    }
}

@Schema(description = "판매자 가게 응답")
data class SellerStoreResponse(
    @field:Schema(description = "가게 ID", example = "1")
    val id: Long,
    @field:Schema(description = "가게명", example = "시루 베이커리")
    val name: String,
    @field:Schema(description = "대표 카테고리", allowableValues = ["BAKERY", "RESTAURANT", "CAFE", "GROCERY", "OTHER"], example = "BAKERY")
    val category: String,
    @field:Schema(description = "주소", example = "서울시 강남구 테헤란로 1")
    val address: String,
    @field:Schema(description = "전화번호", example = "02-1234-5678")
    val phone: String,
    @field:Schema(description = "영업 종료 또는 픽업 종료 시간", example = "21:00")
    val closingTime: String,
) {
    companion object {
        fun from(store: Store): SellerStoreResponse {
            val closingTime = store.products.maxOfOrNull { it.pickupEnd }?.toString() ?: "21:00"
            return SellerStoreResponse(
                id = store.storeId,
                name = store.name,
                category = store.categories.firstOrNull()?.category?.name ?: "OTHER",
                address = store.address,
                phone = store.phone.orEmpty(),
                closingTime = closingTime,
            )
        }
    }
}

@Schema(description = "판매자 가게 수정 요청")
data class SellerUpdateStoreRequest(
    @field:Schema(description = "주소", example = "서울시 강남구 테헤란로 1", nullable = true)
    val address: String? = null,
    @field:Schema(description = "전화번호", example = "02-1234-5678", nullable = true)
    val phone: String? = null,
    @field:Schema(description = "영업 종료 또는 픽업 종료 시간", example = "21:00", nullable = true)
    val closingTime: String? = null,
)

@Schema(description = "구독자 알림 발송 요청")
data class SendNotificationRequest(
    @field:Schema(description = "알림 메시지", example = "오늘 마감 할인 상품이 추가되었습니다.")
    val message: String,
)

@Schema(description = "판매자 알림 응답")
data class SellerNotificationResponse(
    @field:Schema(description = "알림 ID", example = "1")
    val id: Long,
    @field:Schema(description = "가게 ID", example = "1")
    val storeId: Long,
    @field:Schema(description = "가게명", example = "시루 베이커리")
    val storeName: String,
    @field:Schema(description = "알림 메시지", example = "오늘 마감 할인 상품이 추가되었습니다.")
    val message: String,
    @field:Schema(description = "발송 시각", example = "2026-05-13T18:30:00")
    val sentAt: String,
    @field:Schema(description = "수신자 수", example = "12")
    val recipientCount: Int,
)

internal fun discountRate(originalPrice: Int, discountedPrice: Int): Int =
    if (originalPrice > 0) ((1.0 - discountedPrice.toDouble() / originalPrice) * 100).toInt() else 0

internal fun discountedPrice(originalPrice: Int, discountRate: Int): Int =
    (originalPrice * (100 - discountRate) / 100).coerceAtLeast(0)

internal fun parsePickupTimeSlot(slot: String): Pair<LocalTime, LocalTime> {
    val parts = slot.split("-", "~").map { it.trim() }.filter { it.isNotBlank() }
    val start = parts.getOrNull(0)?.let { LocalTime.parse(it) } ?: LocalTime.of(17, 0)
    val end = parts.getOrNull(1)?.let { LocalTime.parse(it) } ?: start.plusHours(2)
    return start to end
}

internal fun parseProductStatus(status: String): ProductStatus =
    runCatching { ProductStatus.valueOf(status.uppercase()) }
        .getOrElse { throw IllegalArgumentException("지원하지 않는 판매 상태입니다: $status") }
