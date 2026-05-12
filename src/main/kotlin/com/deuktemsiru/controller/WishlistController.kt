package com.deuktemsiru.controller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.BuyerStoreResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/wishlist")
class WishlistController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    @PostMapping("/{storeId}")
    fun toggleWishlist(
        @PathVariable storeId: Long,
    ): ApiResponse<Map<String, Any>> {
        val memberId = authContext.getCurrentMemberId()
        val isWishlisted = storeService.toggleWishlist(memberId, storeId)
        return ApiResponse.success(mapOf("isWishlisted" to isWishlisted))
    }

    @GetMapping
    fun getWishlist(): ApiResponse<List<BuyerStoreResponse>> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(storeService.getWishlistBuyer(memberId))
    }
}
