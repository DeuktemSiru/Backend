package com.deuktemsiru.controller.cart

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.CartService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Request / Response DTOs ───────────────────────────────────────────────────

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
    val originalPrice: Int,
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
class CartController(
    private val cartService: CartService,
    private val authContext: AuthContext,
) {

    /**
     * POST /api/v1/cart
     * 장바구니 상품 추가
     */
    @PostMapping
    fun addToCart(
        @RequestBody req: CartAddRequest,
    ): ResponseEntity<ApiResponse<CartItem>> {
        val item = cartService.addToCart(authContext.getCurrentMemberId(), req.productId, req.quantity)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(item, "장바구니에 담았습니다."))
    }

    /**
     * GET /api/v1/cart
     * 장바구니 목록 조회
     */
    @GetMapping
    fun getCart(): ApiResponse<CartResponse> {
        return ApiResponse.success(cartService.getCart(authContext.getCurrentMemberId()))
    }

    /**
     * DELETE /api/v1/cart/{cartItemId}
     * 장바구니 특정 상품 제거
     */
    @DeleteMapping("/{cartItemId}")
    fun removeFromCart(
        @PathVariable cartItemId: Long,
    ): ApiResponse<Unit> {
        cartService.removeFromCart(authContext.getCurrentMemberId(), cartItemId)
        return ApiResponse.success(Unit, "장바구니 상품을 삭제했습니다.")
    }

    @PatchMapping("/{cartItemId}")
    fun updateCartItem(
        @PathVariable cartItemId: Long,
        @RequestBody req: CartUpdateRequest,
    ): ApiResponse<CartItem> {
        val item = cartService.updateQuantity(authContext.getCurrentMemberId(), cartItemId, req.quantity)
        return ApiResponse.success(item, "장바구니 수량을 변경했습니다.")
    }

    /**
     * DELETE /api/v1/cart
     * 장바구니 전체 비우기 (주문 완료 후 자동 호출)
     */
    @DeleteMapping
    fun clearCart(): ApiResponse<Unit> {
        cartService.clearCart(authContext.getCurrentMemberId())
        return ApiResponse.success(Unit, "장바구니를 비웠습니다.")
    }
}
