package com.deuktemsiru.dto

import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.Store

// ── 가게 목록 아이템 (GET /stores) ───────────────────────────────────────────
data class StoreListItemResponse(
    val storeId: Long,
    val name: String,
    val thumbnailUrl: String?,
    val distanceM: Int,          // 위치 기반 미구현 시 0
    val category: String,
    val ratingAvg: Double,
    val reviewCount: Int,
    val availableProductCount: Int,
) {
    companion object {
        fun from(store: Store, availableProductCount: Int, distanceM: Int = 0) = StoreListItemResponse(
            storeId = store.storeId,
            name = store.name,
            thumbnailUrl = store.thumbnailUrl,
            distanceM = distanceM,
            category = store.categories.firstOrNull()?.category?.name ?: "OTHER",
            ratingAvg = store.ratingAvg,
            reviewCount = store.reviewCount,
            availableProductCount = availableProductCount,
        )
    }
}

// ── 가게 상세 내 상품 항목 (GET /stores/{storeId} > products[]) ─────────────
data class StoreProductItem(
    val productId: Long,
    val name: String,
    val discountPrice: Int,
    val quantityRemaining: Int,
    val pickupEnd: String,
    val status: String,
) {
    companion object {
        fun from(product: Product) = StoreProductItem(
            productId = product.productId,
            name = product.name,
            discountPrice = product.discountPrice,
            quantityRemaining = product.quantityRemaining,
            pickupEnd = product.pickupEnd.toString(),
            status = product.status.name,
        )
    }
}

// ── 가게 상세 응답 (GET /stores/{storeId}) ───────────────────────────────────
data class StoreDetailResponse(
    val storeId: Long,
    val name: String,
    val description: String?,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val thumbnailUrl: String?,
    val images: List<String>,
    val categories: List<String>,
    val ratingAvg: Double,
    val reviewCount: Int,
    val products: List<StoreProductItem>,
) {
    companion object {
        fun from(store: Store, products: List<Product>) = StoreDetailResponse(
            storeId = store.storeId,
            name = store.name,
            description = store.description,
            address = store.address,
            latitude = store.latitude,
            longitude = store.longitude,
            phone = store.phone,
            thumbnailUrl = store.thumbnailUrl,
            images = store.images.sortedBy { it.displayOrder }.map { it.imageUrl },
            categories = store.categories.map { it.category.name },
            ratingAvg = store.ratingAvg,
            reviewCount = store.reviewCount,
            products = products.map { StoreProductItem.from(it) },
        )
    }
}

// ── 상품 목록 아이템 (GET /products) ─────────────────────────────────────────
data class ProductListItemResponse(
    val productId: Long,
    val name: String,
    val thumbnailUrl: String?,
    val originalPrice: Int,
    val discountPrice: Int,
    val quantityRemaining: Int,
    val pickupEnd: String,
    val status: String,
    val storeName: String,
    val distanceM: Int,          // 위치 기반 미구현 시 0
) {
    companion object {
        fun from(product: Product, distanceM: Int = 0) = ProductListItemResponse(
            productId = product.productId,
            name = product.name,
            thumbnailUrl = product.thumbnailUrl,
            originalPrice = product.originalPrice,
            discountPrice = product.discountPrice,
            quantityRemaining = product.quantityRemaining,
            pickupEnd = product.pickupEnd.toString(),
            status = product.status.name,
            storeName = product.store.name,
            distanceM = distanceM,
        )
    }
}

// ── 상품 상세 내 가게 정보 ─────────────────────────────────────────────────
data class ProductStoreInfo(
    val storeId: Long,
    val name: String,
    val address: String,
)

// ── 상품 상세 응답 (GET /products/{productId}) ───────────────────────────────
data class ProductDetailResponse(
    val productId: Long,
    val name: String,
    val description: String?,
    val thumbnailUrl: String?,
    val images: List<String>,
    val originalPrice: Int,
    val discountPrice: Int,
    val quantityTotal: Int,
    val quantityRemaining: Int,
    val allergenInfo: String?,
    val madeAt: String?,
    val pickupStart: String,
    val pickupEnd: String,
    val availableDate: String,
    val status: String,
    val carbonSavedKg: Double,
    val store: ProductStoreInfo,
) {
    companion object {
        fun from(product: Product) = ProductDetailResponse(
            productId = product.productId,
            name = product.name,
            description = product.description,
            thumbnailUrl = product.thumbnailUrl,
            images = product.images.sortedBy { it.displayOrder }.map { it.imageUrl },
            originalPrice = product.originalPrice,
            discountPrice = product.discountPrice,
            quantityTotal = product.quantityTotal,
            quantityRemaining = product.quantityRemaining,
            allergenInfo = product.allergenInfo,
            madeAt = product.madeAt,
            pickupStart = product.pickupStart.toString(),
            pickupEnd = product.pickupEnd.toString(),
            availableDate = product.availableDate.toString(),
            status = product.status.name,
            carbonSavedKg = product.carbonSavedKg,
            store = ProductStoreInfo(
                storeId = product.store.storeId,
                name = product.store.name,
                address = product.store.address,
            ),
        )
    }
}

// ── 지도 마커 (GET /stores/map) ───────────────────────────────────────────────
data class StoreMarkerResponse(
    val storeId: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val availableProductCount: Int,
    val category: String,
) {
    companion object {
        fun from(store: Store, availableProductCount: Int) = StoreMarkerResponse(
            storeId = store.storeId,
            name = store.name,
            latitude = store.latitude,
            longitude = store.longitude,
            availableProductCount = availableProductCount,
            category = store.categories.firstOrNull()?.category?.name ?: "OTHER",
        )
    }
}

// ── 찜 목록 아이템 (GET /wishlist) ───────────────────────────────────────────
data class WishlistItemResponse(
    val wishlistId: Long,
    val storeId: Long,
    val name: String,
    val thumbnailUrl: String?,
    val ratingAvg: Double,
    val availableProductCount: Int,
)
