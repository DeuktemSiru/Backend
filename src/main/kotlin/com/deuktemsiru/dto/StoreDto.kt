package com.deuktemsiru.dto

import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Store

data class MenuItemResponse(
    val menuItemId: Long,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val originalPrice: Int,
    val allergenInfo: String?,
    val isActive: Boolean,
) {
    companion object {
        fun from(item: MenuItem) = MenuItemResponse(
            menuItemId = item.menuItemId,
            name = item.name,
            description = item.description,
            imageUrl = item.imageUrl,
            originalPrice = item.originalPrice,
            allergenInfo = item.allergenInfo,
            isActive = item.isActive,
        )
    }
}

data class StoreResponse(
    val storeId: Long,
    val name: String,
    val description: String?,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val thumbnailUrl: String?,
    val ratingAvg: Double,
    val reviewCount: Int,
    val isActive: Boolean,
    val isWishlisted: Boolean,
    val categories: List<String>,
    val menuItems: List<MenuItemResponse>,
) {
    companion object {
        fun from(store: Store, isWishlisted: Boolean = false) = StoreResponse(
            storeId = store.storeId,
            name = store.name,
            description = store.description,
            address = store.address,
            latitude = store.latitude,
            longitude = store.longitude,
            phone = store.phone,
            thumbnailUrl = store.thumbnailUrl,
            ratingAvg = store.ratingAvg,
            reviewCount = store.reviewCount,
            isActive = store.isActive,
            isWishlisted = isWishlisted,
            categories = store.categories.map { it.category.name },
            menuItems = store.menuItems.map { MenuItemResponse.from(it) },
        )
    }
}

data class UpdateStoreRequest(
    val description: String? = null,
    val phone: String? = null,
)
