package com.deuktemsiru.dto

import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store

data class SaleItemRequest(
    val menuItemId: Long? = null,       // 기존 메뉴 선택 시 사용 (optional)
    val name: String,                    // 상품명 (직접 입력 또는 메뉴 이름 사용)
    val discountPrice: Int,             // 할인가 (원)
    val originalPrice: Int,             // 정가 (원)
    val quantityTotal: Int,             // 판매 수량
    val madeAt: String? = null,         // 만들어진 시간 HH:mm
    val pickupStart: String,            // 픽업 시작 HH:mm
    val pickupEnd: String,              // 픽업 마감 HH:mm
    val availableDate: String,          // 판매 날짜 yyyy-MM-dd
    val allergenInfo: String? = null,   // 알레르기 성분
)

data class UpdateSaleStatusRequest(
    val status: String,
)

data class SellerSaleItemResponse(
    val id: Long,
    val menuItemId: Long,
    val name: String,
    val emoji: String,
    val originalPrice: Int,
    val discountedPrice: Int,
    val discountRate: Int,
    val remainingItems: Int,
    val totalItems: Int,
    val status: String,
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

data class SellerMenuItemRequest(
    val name: String,
    val description: String? = null,
    val originalPrice: Int,
    val allergenInfo: String? = null,
)

data class SellerMenuItemUpdateRequest(
    val name: String? = null,
    val originalPrice: Int? = null,
)

data class SellerMenuItemResponse(
    val id: Long,
    val name: String,
    val emoji: String,
    val imageUrl: String? = null,
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

data class SellerStoreResponse(
    val id: Long,
    val name: String,
    val category: String,
    val address: String,
    val phone: String,
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

data class SellerUpdateStoreRequest(
    val address: String? = null,
    val phone: String? = null,
    val closingTime: String? = null,
)

data class SendNotificationRequest(
    val message: String,
)

data class SellerNotificationResponse(
    val id: Long,
    val storeId: Long,
    val storeName: String,
    val message: String,
    val sentAt: String,
    val recipientCount: Int,
)

internal fun discountRate(originalPrice: Int, discountedPrice: Int): Int =
    if (originalPrice > 0) ((1.0 - discountedPrice.toDouble() / originalPrice) * 100).toInt() else 0

internal fun parseProductStatus(status: String): ProductStatus =
    runCatching { ProductStatus.valueOf(status.uppercase()) }
        .getOrElse { throw IllegalArgumentException("지원하지 않는 판매 상