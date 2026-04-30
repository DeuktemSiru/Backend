package com.deuktemsiru.controller

import com.deuktemsiru.dto.StoreResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stores")
class StoreController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    // GET /api/stores?category=BAKERY&userId=1
    @GetMapping
    fun getStores(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) userId: Long?,
    ): List<StoreResponse> {
        userId?.let { authContext.requireCurrentUserId(it) }
        return storeService.getStores(category, userId)
    }

    // GET /api/stores/{storeId}?userId=1
    @GetMapping("/{storeId}")
    fun getStore(
        @PathVariable storeId: Long,
        @RequestParam(required = false) userId: Long?,
    ): StoreResponse {
        userId?.let { authContext.requireCurrentUserId(it) }
        return storeService.getStore(storeId, userId)
    }
}
