package com.deuktemsiru.controller.wishlist

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.WishlistItemResponse
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.StoreService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class WishlistToggleResponse(val storeId: Long, val isWishlisted: Boolean)
data class WishlistListResponse(val wishlists: List<WishlistItemResponse>)

@RestController
@RequestMapping("/api/v1/wishlist")
class WishlistController(
    private val storeService: StoreService,
) {
    /**
     * POST /api/v1/wishlist/{storeId}
     * 찜 토글 (추가 시 isWishlisted=true·201, 제거 시 isWishlisted=false·200)
     */
    @PostMapping("/{storeId}")
    fun toggleWishlist(
        @CurrentMemberId memberId: Long,
        @PathVariable storeId: Long,
    ): ResponseEntity<ApiResponse<WishlistToggleResponse>> {
        val isWishlisted = storeService.toggleWishlist(memberId, storeId)
        return wishlistToggleResponse(storeId, isWishlisted)
    }

    /**
     * DELETE /api/v1/wishlist/{storeId}
     * 찜 해제 (명시적 제거)
     */
    @DeleteMapping("/{storeId}")
    fun removeWishlist(
        @CurrentMemberId memberId: Long,
        @PathVariable storeId: Long,
    ): ApiResponse<Unit> {
        storeService.removeWishlist(memberId, storeId)
        return ok(Unit, "찜 해제 성공")
    }

    /**
     * GET /api/v1/wishlist
     * 찜 목록 조회
     */
    @GetMapping
    fun getWishlist(
        @CurrentMemberId memberId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<WishlistListResponse> {
        val wishlists = storeService.getWishlistBuyer(memberId)
        return ok(WishlistListResponse(wishlists))
    }

    private fun wishlistToggleResponse(
        storeId: Long,
        isWishlisted: Boolean,
    ): ResponseEntity<ApiResponse<WishlistToggleResponse>> =
        ResponseEntity
            .status(if (isWishlisted) HttpStatus.CREATED else HttpStatus.OK)
            .body(
                ok(
                    WishlistToggleResponse(storeId = storeId, isWishlisted = isWishlisted),
                    if (isWishlisted) "찜 등록 성공" else "찜 해제 성공",
                )
            )
}
