package com.deuktemsiru.controller.store

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.BuyerStoreResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

// ── Response DTOs ─────────────────────────────────────────────────────────────

data class StoreListResponse(val stores: List<BuyerStoreResponse>, val hasNext: Boolean)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/stores")
class StoreController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/stores
     * 주변 가게 목록 (구매자용 — Product 기반)
     * TODO: latitude, longitude, radius, sort, keyword, page, size 필터 구현 필요
     */
    @GetMapping
    fun getStores(
        @RequestParam(required = false) latitude: Double?,
        @RequestParam(required = false) longitude: Double?,
        @RequestParam(required = false, defaultValue = "1000") radius: Int,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false, defaultValue = "distance") sort: String,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<StoreListResponse> {
        val memberId = authContext.getCurrentMemberId()
        val stores = storeService.getStoresBuyer(category, memberId)
        return ApiResponse.success(StoreListResponse(stores = stores, hasNext = false))
    }

    /**
     * GET /api/v1/stores/{storeId}
     * 가게 상세 조회 (구매자용 — Product 기반)
     */
    @GetMapping("/{storeId}")
    fun getStore(
        @PathVariable storeId: Long,
    ): ApiResponse<BuyerStoreResponse> {
        val memberId = authContext.getCurrentMemberId()
        val store = storeService.getStoreBuyer(storeId, memberId)
        return ApiResponse.success(store)
    }
}
