package com.deuktemsiru.dto

data class CartAddRequest(
    val productId: Long,
    val quantity: Int,
)

data class CartUpdateRequest(
    val quantity: Int,
)

data class CartItem(
    val cartItemId: Long,
    val productId: Long,
    val productName: String,
    val storeId: Long,
    val storeName: String,
    val storeLatitude: Double,
    val storeLongitude: Double,
    val originalPrice: Int,
    val discountPrice: Int,
    val pickupStart: String,
    val pickupEnd: String,
    val quantity: Int,
    val imageUrl: String?,
)

data class CartResponse(
    val items: List<CartItem>,
    val totalPrice: Int,
)
