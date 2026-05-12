package com.deuktemsiru.controller.wishlist

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.StoreResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

// ── Response DTOs ─────────────────────────────────────────────────────────────

data class WishlistToggleResponse(val storeId: Long, val isWishlisted: Boolean)

data class WishlistStoreItem(
    val storeId: Long,
    val name: String,
    val category: String,
    val ratingAvg: Float,
    val availableProductCount: Int,
    val thumbnailUrl: String?,
) {
    companion object {
        fun from(store: StoreResponse) = WishlistStoreItem(
            storeId = store.id,
            name = store.name,
            category = store.category.name,
            ratingAvg = store.rating,
            availableProductCount = store.menus.count { !it.isSoldOut },
            thumbnailUrl = null,
        )
    }
}

data class WishlistResponse(val stores: List<WishlistStoreItem>)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/wishlist")
class WishlistController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    /**
     * POST /api/v1/wishlist/{storeId}
     * 찜 토글 (추가 시 true, 제거 시 false 반환)
     */
    @PostMapping("/{storeId}")
    fun toggleWishlist(
        @PathVariable storeId: Long,
    ): ApiResponse<WishlistToggleResponse> {
        val memberId = authContext.getCurrentMemberId()
        val isWishlisted = storeService.toggleWishlist(memberId, storeId)
        return ApiResponse.success(WishlistToggleResponse(storeId = storeId, isWishlisted = isWishlisted))
    }

    /**
     * GET /api/v1/wishlist
     * 찜 목록 조회
     */
    @GetMapping
    fun getWishlist(): ApiResponse<WishlistResponse> {
        val memberId = authContext.getCurrentMemberId()
        val stores = storeService.getWishlist(memberId).map { WishlistStoreItem.from(it) }
        return ApiResponse.success(WishlistResponse(stores))
    }
}
