package com.deuktemsiru.dto

import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store

// ── 상품 등록 요청 ─────────────────────────────────────────────────────────────
data class SaleItemRequest(
    val menuItemId: Long? = null,
    val name: String,
    val discountPrice: Int,
    val originalPrice: Int,
    val quantityTotal: Int,
    val madeAt: String? = null,
    val pickupStart: String,
    val pickupEnd: String,
    val availableDate: String,
    val allergenInfo: String? = null,
)

data class UpdateSaleStatusRequest(
    val status: String,
)

// ── 판매자 상품 목록 응답 (GET /sellers/products) ─────────────────────────────
data class SellerSaleItemResponse(
    val productId: Long,
    val name: String,
    val originalPrice: Int,
    val discountPrice: Int,
    val quantityTotal: Int,
    val quantityRemaining: Int,
    val status: String,
    val pickupStart: String,
    val pickupEnd: String,
) {
    companion object {
        fun from(product: Product) = SellerSaleItemResponse(
            productId = product.productId,
            name = product.name,
            originalPrice = product.originalPrice,
            discountPrice = product.discountPrice,
            quantityTotal = product.quantityTotal,
            quantityRemaining = product.quantityRemaining,
            status = product.status.name,
            pickupStart = product.pickupStart.toString(),
            pickupEnd = product.pickupEnd.toString(),
        )
    }
}

// ── 메뉴 마스터 요청/응답 ──────────────────────────────────────────────────────
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
    val menuItemId: Long,
    val name: String,
    val imageUrl: String?,
    val originalPrice: Int,
    val allergenInfo: String?,
    val isActive: Boolean,
) {
    companion object {
        fun from(item: MenuItem) = SellerMenuItemResponse(
            menuItemId = item.menuItemId,
            name = item.name,
            imageUrl = item.imageUrl,
            originalPrice = item.originalPrice,
            allergenInfo = item.allergenInfo,
            isActive = item.isActive,
        )
    }
}

// ── 판매자 가게 조회 응답 (GET /sellers/stores/my) ────────────────────────────
data class SellerStoreResponse(
    val storeId: Long,
    val name: String,
    val isActive: Boolean,
    val isVerified: Boolean,
    val todayProductCount: Int,
    val pendingOrderCount: Int,
    val ratingAvg: Double,
    val reviewCount: Int,
)

// ── 가게 수정 요청 ─────────────────────────────────────────────────────────────
data class SellerUpdateStoreRequest(
    val description: String? = null,
    val phone: String? = null,
)

// ── 가게 등록 요청 (POST /sellers/stores) ────────────────────────────────────
data class CreateStoreRequest(
    val name: String,
    val description: String? = null,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val categories: List<String>,
)

// ── 알림 발송 ─────────────────────────────────────────────────────────────────
data class SendNotificationRequest(
    val message: String,
    val targetType: String = "REGULAR",
    val radiusKm: Int? = null,
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
        .getOrElse { throw IllegalArgumentException("지원하지 않는 판매 상태: $status") }
