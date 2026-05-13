package com.deuktemsiru.controller.cart

import com.deuktemsiru.common.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Request / Response DTOs ───────────────────────────────────────────────────

data class CartAddRequest(
    val productId: Long,
    val quantity: Int,
)

data class CartItem(
    val cartItemId: Long,
    val productId: Long,
    val productName: String,
    val storeId: Long,
    val storeName: String,
    val discountPrice: Int,
    val quantity: Int,
    val imageUrl: String?,
)

data class CartResponse(
    val items: List<CartItem>,
    val totalPrice: Int,
)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/cart")
class CartController {

    /**
     * POST /api/v1/cart
     * 장바구니 상품 추가
     * TODO: Cart 엔티티 및 CartService 구현 필요
     */
    @PostMapping
    fun addToCart(
        @RequestBody req: CartAddRequest,
    ): ResponseEntity<ApiResponse<CartItem>> {
        throw UnsupportedOperationException("장바구니 추가: 미구현 — Cart 엔티티 및 CartService 구현 필요")
    }

    /**
     * GET /api/v1/cart
     * 장바구니 목록 조회
     * TODO: Cart 엔티티 및 CartService 구현 필요
     */
    @GetMapping
    fun getCart(): ApiResponse<CartResponse> {
        throw UnsupportedOperationException("장바구니 조회: 미구현 — Cart 엔티티 및 CartService 구현 필요")
    }

    /**
     * DELETE /api/v1/cart/{cartItemId}
     * 장바구니 특정 상품 제거
     * TODO: Cart 엔티티 및 CartService 구현 필요
     */
    @DeleteMapping("/{cartItemId}")
    fun removeFromCart(
        @PathVariable cartItemId: Long,
    ): ApiResponse<Unit> {
        throw UnsupportedOperationException("장바구니 삭제: 미구현 — Cart 엔티티 및 CartService 구현 필요")
 