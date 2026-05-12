package com.deuktemsiru.dto

import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store

/**
 * 구매자 앱에서 기대하는 메뉴(상품) 응답 형식.
 * MenuItem(정적 메뉴 정의)이 아닌 Product(오늘의 마감 할인 상품) 기반.
 */
data class BuyerMenuResponse(
    val id: Long,
    val name: String,
    val emoji: String,
    val originalPrice: Int,
    val discountedPrice: Int,
    val discountRate: Int,
    val remainingItems: Int,
    val isSoldOut: Boolean,
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
data class BuyerStoreResponse(
    val id: Long,
    val name: String,
    val category: String,
    val emoji: String,
    val rating: Float,
    val address: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val closingTime: String,
    val isWishlisted: Boolean,
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
