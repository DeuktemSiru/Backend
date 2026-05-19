package com.deuktemsiru.controller.cart

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.created
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.CartAddRequest
import com.deuktemsiru.dto.CartItem
import com.deuktemsiru.dto.CartResponse
import com.deuktemsiru.dto.CartUpdateRequest
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.CartService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/cart")
class CartController(
    private val cartService: CartService,
) {

    /**
     * POST /api/v1/cart
     * 장바구니 상품 추가
     */
    @PostMapping
    fun addToCart(
        @CurrentMemberId memberId: Long,
        @RequestBody req: CartAddRequest,
    ): ResponseEntity<ApiResponse<CartItem>> {
        return created(cartService.addToCart(memberId, req.productId, req.quantity), "장바구니에 담았습니다.")
    }

    /**
     * GET /api/v1/cart
     * 장바구니 목록 조회
     */
    @GetMapping
    fun getCart(
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CartResponse> {
        return ok(cartService.getCart(memberId))
    }

    /**
     * DELETE /api/v1/cart/{cartItemId}
     * 장바구니 특정 상품 제거
     */
    @DeleteMapping("/{cartItemId}")
    fun removeFromCart(
        @CurrentMemberId memberId: Long,
        @PathVariable cartItemId: Long,
    ): ApiResponse<Unit> {
        cartService.removeFromCart(memberId, cartItemId)
        return ok(Unit, "장바구니 상품을 삭제했습니다.")
    }

    @PatchMapping("/{cartItemId}")
    fun updateCartItem(
        @CurrentMemberId memberId: Long,
        @PathVariable cartItemId: Long,
        @RequestBody req: CartUpdateRequest,
    ): ApiResponse<CartItem> {
        return ok(cartService.updateQuantity(memberId, cartItemId, req.quantity), "장바구니 수량을 변경했습니다.")
    }

    /**
     * DELETE /api/v1/cart
     * 장바구니 전체 비우기 (주문 완료 후 자동 호출)
     */
    @DeleteMapping
    fun clearCart(
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        cartService.clearCart(memberId)
        return ok(Unit, "장바구니를 비웠습니다.")
    }
}
