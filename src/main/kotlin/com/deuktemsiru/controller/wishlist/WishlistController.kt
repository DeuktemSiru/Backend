package com.deuktemsiru.controller.wishlist

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.WishlistItemResponse
import com.deuktemsiru.security.AuthContext
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
    private val authContext: AuthContext,
) {
    /**
     * POST /api/v1/wishlist/{storeId}
     * 찜 토글 (추가 시 isWishlisted=true·201, 제거 시 isWishlisted=false·200)
     */
    @PostMapping("/{storeId}")
    fun toggleWishlist(
        @PathVariable storeId: Long,
    ): ResponseEntity<ApiResponse<WishlistToggleResponse>> {
        val memberId = authContext.getCurrentMemberId()
        val isWishlisted = storeService.toggleWishlist(memberId, storeId)
        val status = if (isWishlisted) HttpStatus.CREATED else HttpStatus.OK
        val message = if (isWishlisted) "찜 등록 성공" else "찜 해제 성공"
        return ResponseEntity.status(status)
            .body(ApiResponse.success(WishlistToggleResponse(storeId = storeId, isWishlisted = isWishlisted), message))
    }

    /**
     * DELETE /api/v1/wishlist/{storeId}
     * 찜 해제 (명시적 제거)
     */
    @DeleteMapping("/{storeId}")
    fun removeWishlist(
        @PathVariable storeId: Long,
    ): ApiResponse<Unit> {
        val memberId = authContext.getCurrentMemberId()
        storeService.removeWishlist(memberId, storeId)
        return ApiResponse.success(Unit, "찜 해제 성공")
    }

    /**
     * GET /api/v1/wishlist
     * 찜 목록 조회
     */
    @GetMapping
    fun getWishlist(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<WishlistListResponse> {
        val memberId = authContext.getCurrentMemberId()
        val wishlists = storeService.getWishlistBuyer(memberId)
        return ApiResponse.success(WishlistListResponse(wishlists))
    }
}
