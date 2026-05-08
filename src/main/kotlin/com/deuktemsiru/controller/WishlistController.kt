package com.deuktemsiru.controller

import com.deuktemsiru.dto.StoreResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/wishlist")
class WishlistController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    @PostMapping("/{storeId}")
    fun toggleWishlist(
        @PathVariable storeId: Long,
        @RequestParam userId: Long,
    ): Map<String, Any> {
        authContext.requireCurrentMemberId(userId)
        val isWishlisted = storeService.toggleWishlist(userId, storeId)
        return mapOf("isWishlisted" to isWishlisted)
    }

    @GetMapping
    fun getWishlist(@RequestParam userId: Long): List<StoreResponse> {
        authContext.requireCurrentMemberId(userId)
        return storeService.getWishlist(userId)
    }
}
