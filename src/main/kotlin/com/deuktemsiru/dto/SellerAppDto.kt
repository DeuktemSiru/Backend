package com.deuktemsiru.dto

import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import java.time.LocalTime

data class SaleItemRequest(
    val menuItemId: Long,
    val discountRate: Int,
    val quantity: Int,
    val pickupTimeSlot: String,
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
    val emoji: String = "",
    val originalPrice: Int,
    val costPrice: Int? = null,
    val allergyInfo: String? = null,
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
