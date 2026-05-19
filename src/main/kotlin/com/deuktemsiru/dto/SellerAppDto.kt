package com.deuktemsiru.dto

import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import com.deuktemsiru.common.toEnumOrThrow

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

data class SaleItemForm(
    val name: String,
    val discountPrice: Int,
    val originalPrice: Int,
    val quantityTotal: Int,
    val pickupStart: String,
    val pickupEnd: String,
    val availableDate: String,
    val menuItemId: Long? = null,
    val madeAt: String? = null,
    val allergenInfo: String? = null,
) {
    fun toRequest() = SaleItemRequest(menuItemId, name, discountPrice, originalPrice, quantityTotal, madeAt, pickupStart, pickupEnd, availableDate, allergenInfo)
}

data class UpdateSaleStatusRequest(
    val status: String,
)

data class UpdateSaleItemRequest(
    val originalPrice: Int? = null,
    val discountPrice: Int? = null,
    val quantityRemaining: Int? = null,
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
            productId = product.summary.productId,
            name = product.summary.name,
            originalPrice = product.summary.originalPrice,
            discountPrice = product.summary.discountPrice,
            quantityTotal = product.summary.quantityTotal,
            quantityRemaining = product.summary.quantityRemaining,
            status = product.status.name,
            pickupStart = product.summary.pickupStart,
            pickupEnd = product.summary.pickupEnd,
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

data class SellerMenuItemForm(
    val name: String,
    val originalPrice: Int,
    val description: String? = null,
    val allergenInfo: String? = null,
) {
    fun toRequest() = SellerMenuItemRequest(name, description, originalPrice, allergenInfo)
}

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
    val address: String,
    val phone: String?,
    val closingTime: String?,
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
    val address: String? = null,
    val phone: String? = null,
    val closingTime: String? = null,
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

data class CreateStoreForm(
    val name: String,
    val description: String? = null,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val categories: List<String>,
) {
    fun toRequest() = CreateStoreRequest(name, description, address, latitude, longitude, phone, categories)
}

data class CreateStoreResponse(val storeId: Long, val name: String) {
    companion object {
        fun from(store: Store) = CreateStoreResponse(store.storeId, store.name)
    }
}

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

internal fun String.toProductStatus(): ProductStatus = toEnumOrThrow("판매 상태")

internal data class ProductSummary(
    val productId: Long,
    val name: String,
    val originalPrice: Int,
    val discountPrice: Int,
    val quantityTotal: Int,
    val quantityRemaining: Int,
    val pickupStart: String,
    val pickupEnd: String,
)

internal val Product.summary: ProductSummary
    get() = ProductSummary(
        productId = productId,
        name = name,
        originalPrice = originalPrice,
        discountPrice = discountPrice,
        quantityTotal = quantityTotal,
        quantityRemaining = quantityRemaining,
        pickupStart = pickupStart.toString(),
        pickupEnd = pickupEnd.toString(),
    )
