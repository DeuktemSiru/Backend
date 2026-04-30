package com.deuktemsiru.dto

import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Store
import com.deuktemsiru.entity.StoreCategory

data class MenuItemResponse(
    val id: Long,
    val name: String,
    val emoji: String,
    val imageUrl: String?,
    val originalPrice: Int,
    val discountedPrice: Int,
    val discountRate: Int,
    val remainingItems: Int,
    val isSoldOut: Boolean,
    val pickupTimeSlot: String,
) {
    companion object {
        fun from(item: MenuItem) = MenuItemResponse(
            id = item.id,
            name = item.name,
            emoji = item.emoji,
            imageUrl = item.imageUrl,
            originalPrice = item.originalPrice,
            discountedPrice = item.discountedPrice,
            discountRate = item.discountRate,
            remainingItems = item.remainingItems,
            isSoldOut = item.isSoldOut,
            pickupTimeSlot = item.pickupTimeSlot,
        )
    }
}

data class StoreResponse(
    val id: Long,
    val name: String,
    val category: StoreCategory,
    val emoji: String,
    val rating: Float,
    val address: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val closingTime: String,
    val isWishlisted: Boolean,
    val menus: List<MenuItemResponse>,
) {
    companion object {
        fun from(store: Store, isWishlisted: Boolean = false) = StoreResponse(
            id = store.id,
            name = store.name,
            category = store.category,
            emoji = store.emoji,
            rating = store.rating,
            address = store.address,
            phone = store.phone,
            latitude = store.latitude,
            longitude = store.longitude,
            closingTime = store.closingTime,
            isWishlisted = isWishlisted,
            menus = store.menuItems.map { MenuItemResponse.from(it) },
        )
    }
}

data class MenuItemRequest(
    val name: String,
    val emoji: String,
    val imageUrl: String? = null,
    val originalPrice: Int,
    val discountRate: Int,
    val quantity: Int,
    val pickupTimeSlot: String,
)

data class MenuItemUpdateRequest(
    val remainingItems: Int? = null,
    val isSoldOut: Boolean? = null,
    val discountRate: Int? = null,
    val pickupTimeSlot: String? = null,
    val imageUrl: String? = null,
)

data class UpdateStoreRequest(
    val address: String? = null,
    val phone: String? = null,
    val closingTime: String? = null,
)
