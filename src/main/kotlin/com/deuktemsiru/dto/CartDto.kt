package com.deuktemsiru.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "장바구니 상품 추가 요청")
data class CartAddRequest(
    @field:Schema(description = "상품 ID", example = "1")
    val productId: Long,
    @field:Schema(description = "담을 수량", example = "2", minimum = "1")
    val quantity: Int,
)

@Schema(description = "장바구니 상품 수량 변경 요청")
data class CartUpdateRequest(
    @field:Schema(description = "변경할 수량", example = "3", minimum = "1")
    val quantity: Int,
)

@Schema(description = "장바구니 항목")
data class CartItem(
    @field:Schema(description = "장바구니 항목 ID", example = "10")
    val cartItemId: Long,
    @field:Schema(description = "상품 ID", example = "1")
    val productId: Long,
    @field:Schema(description = "상품명", example = "오늘 마감할인 소금빵 4개입")
    val productName: String,
    @field:Schema(description = "매장 ID", example = "1")
    val storeId: Long,
    @field:Schema(description = "매장명", example = "오이도굽는집")
    val storeName: String,
    val storeLatitude: Double,
    val storeLongitude: Double,
    @field:Schema(description = "정가", example = "12000")
    val originalPrice: Int,
    @field:Schema(description = "할인가", example = "7200")
    val discountPrice: Int,
    @field:Schema(description = "픽업 시작 시각", example = "18:30")
    val pickupStart: String,
    @field:Schema(description = "픽업 종료 시각", example = "20:30")
    val pickupEnd: String,
    @field:Schema(description = "수량", example = "2")
    val quantity: Int,
    @field:Schema(description = "상품 이미지 URL", nullable = true)
    val imageUrl: String?,
)

@Schema(description = "장바구니 조회 응답")
data class CartResponse(
    val items: List<CartItem>,
    @field:Schema(description = "총 결제 예정 금액", example = "14400")
    val totalPrice: Int,
)
