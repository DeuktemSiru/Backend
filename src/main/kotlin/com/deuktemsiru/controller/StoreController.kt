package com.deuktemsiru.controller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.BuyerStoreResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/stores")
class StoreController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    @GetMapping
    fun getStores(
        @RequestParam(required = false) category: String?,
    ): ApiResponse<List<BuyerStoreResponse>> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(storeService.getStoresBuyer(category, memberId))
    }

    @GetMapping("/{storeId}")
    fun getStore(
        @PathVariable storeId: Long,
    ): ApiResponse<BuyerStoreResponse> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(storeService.getStoreBuyer(storeId, memberId))
    }
}
