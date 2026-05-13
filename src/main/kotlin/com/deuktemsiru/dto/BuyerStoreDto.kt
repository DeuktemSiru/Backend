package com.deuktemsiru.dto

import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 구매자 앱에서 기대하는 메뉴(상품) 응답 형식.
 * MenuItem(정적 메뉴 정의)이 아닌 Product(오늘의 마감 할인 상품) 기반.
 */
@Schema(description = "구매자 앱 메뉴 응답")
data class BuyerMenuResponse(
    @field:Schema(description = "상품 ID", example = "1")
    val id: Long,
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
    @field:Schema(description = "품절 여부", example = "false")
    val isSoldOut: Boolean,
    @field:Schema(description = "픽업 가능 시간대", example = "18:00-20:00")
    val pickupTimeSlot: String,
) {
    companion object {
        fun from(product: Product, emoji: String): BuyerMenuResponse {
            val discountRate = if (product.originalPrice > 0)
                ((1.0 - product.discountPrice.toDouble() / product.originalPrice) * 100).toInt()
            else 0
            return BuyerMenuResponse(
                id = product.productId,
                name = product.name,
                emoji = emoji,
                originalPrice = product.originalPrice,
                discountedPrice = product.discountPrice,
                discountRate = discountRate,
                remainingItems = product.quantityRemaining,
                isSoldOut = product.status == ProductStatus.SOLD_OUT || product.quantityRemaining <= 0,
                pickupTimeSlot = "${product.pickupStart}-${product.pickupEnd}",
            )
        }
    }
}

/**
 * 구매자 앱에서 기대하는 가게 응답 형식.
 */
@Schema(description = "구매자 앱 가게 응답")
data class BuyerStoreResponse(
    @field:Schema(description = "가게 ID", example = "1")
    val id: Long,
    @field:Schema(description = "가게명", example = "시루 베이커리")
    val name: String,
    @field:Schema(description = "대표 카테고리", allowableValues = ["BAKERY", "RESTAURANT", "CAFE", "GROCERY", "OTHER"], example = "BAKERY")
    val category: String,
    @field:Schema(description = "카테고리 이모지", example = "🥐")
    val emoji: String,
    @field:Schema(description = "평점", example = "4.7")
    val rating: Float,
    @field:Schema(description = "주소", example = "서울시 강남구 테헤란로 1")
    val address: String,
    @field:Schema(description = "전화번호", example = "02-1234-5678")
    val phone: String,
    @field:Schema(description = "위도", example = "37.5665")
    val latitude: Double,
    @field:Schema(description = "경도", example = "126.9780")
    val longitude: Double,
    @field:Schema(description = "영업 종료 또는 픽업 종료 시간", example = "21:00")
    val closingTime: String,
    @field:Schema(description = "찜 여부", example = "true")
    val isWishlisted: Boolean,
    @field:Schema(description = "판매 중인 메뉴 목록")
    val menus: List<BuyerMenuResponse>,
) {
    companion object {
        fun from(store: Store, products: List<Product>, isWishlisted: Boolean): BuyerStoreResponse {
            val category = store.categories.firstOrNull()?.category?.name ?: "OTHER"
            val emoji = categoryEmoji(category)
            val menus = products.map { BuyerMenuResponse.from(it, emoji) }
            val closingTime = products.maxOfOrNull { it.pickupEnd }?.toString() ?: "21:00"
            return BuyerStoreResponse(
                id = store.storeId,
                name = store.name,
                category = category,
                emoji = emoji,
                rating = store.ratingAvg.toFloat(),
                address = store.address,
                phone = store.phone ?: "",
                latitude = store.latitude,
                longitude = store.longitude,
                closingTime = closingTime,
                isWishlisted = isWishlisted,
                menus = menus,
            )
        }

        private fun categoryEmoji(category: String) = when (category) {
            "BAKERY"     -> "🥐"
            "CAFE"       -> "☕"
            "RESTAURANT" -> "🍱"
            "GROCERY"    -> "🥦"
            else         -> "🍽️"
        }
    }
}
