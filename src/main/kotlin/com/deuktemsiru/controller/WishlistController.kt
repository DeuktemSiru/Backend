package com.deuktemsiru.controller

import com.deuktemsiru.dto.StoreResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/wishlist")
class WishlistController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    // POST /api/wishlist/{storeId}?userId=1
    @PostMapping("/{storeId}")
    fun toggleWishlist(
        @PathVariable storeId: Long,
        @RequestParam userId: Long,
    ): Map<String, Any> {
        authContext.requireCurrentUserId(userId)
        val isWishlisted = storeService.toggleWishlist(userId, storeId)
        return mapOf("isWishlisted" to isWishlisted)
    }

    // GET /api/wishlist?userId=1
    @GetMapping
    fun getWishlist(@RequestParam userId: Long): List<StoreResponse> {
        authContext.requireCurrentUserId(userId)
        return storeService.getWishlist(userId)
    }
}
