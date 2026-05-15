package com.deuktemsiru.controller.store

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.StoreDetailResponse
import com.deuktemsiru.dto.StoreListItemResponse
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

data class StoreListResponse(val stores: List<StoreListItemResponse>, val hasNext: Boolean)

@RestController
@RequestMapping("/api/v1/stores")
class StoreController(
    private val storeService: StoreService,
) {
    /**
     * GET /api/v1/stores
     * 주변 가게 목록 — category·keyword·radius 필터와 거리 정렬 지원
     */
    @GetMapping
    fun getStores(
        @RequestParam(required = true) latitude: Double,
        @RequestParam(required = true) longitude: Double,
        @RequestParam(required = false, defaultValue = "1000") radius: Int,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false, defaultValue = "distance") sort: String,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<StoreListResponse> {
        val stores = storeService.getStoreListBuyer(
            category = category,
            keyword = keyword,
            memberId = null,
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            sort = sort,
            page = page,
            size = size,
        )
        return ApiResponse.success(StoreListResponse(stores = stores.items, hasNext = stores.hasNext))
    }

    /**
     * GET /api/v1/stores/{storeId}
     * 가게 상세 조회 (상품 목록 포함)
     */
    @GetMapping("/{storeId}")
    fun getStore(
        @PathVariable storeId: Long,
    ): ApiResponse<StoreDetailResponse> {
        return ApiResponse.success(storeService.getStoreDetailBuyer(storeId))
    }
}
