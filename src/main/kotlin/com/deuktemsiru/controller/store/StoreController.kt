package com.deuktemsiru.controller.store

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.StoreDetailResponse
import com.deuktemsiru.dto.StoreListItemResponse
import com.deuktemsiru.security.JwtUser
import com.deuktemsiru.service.StoreService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

data class StoreListResponse(val stores: List<StoreListItemResponse>, val hasNext: Boolean)

@Tag(name = "Buyer Stores", description = "구매자 매장 목록 및 상세 조회 API")
@RestController
@RequestMapping("/api/v1/stores")
class StoreController(
    private val storeService: StoreService,
) {
    /**
     * GET /api/v1/stores
     * 주변 가게 목록 — category·keyword·radius 필터와 거리 정렬 지원
     */
    @Operation(summary = "매장 목록 조회", description = "현재 위치 기준 주변 매장을 거리, 카테고리, 검색어 조건으로 조회합니다.")
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
            memberId = currentMemberId(),
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            sort = sort,
            page = page,
            size = size,
        )
        return ok(StoreListResponse(stores = stores.items, hasNext = stores.hasNext))
    }

    /**
     * GET /api/v1/stores/{storeId}
     * 가게 상세 조회 (상품 목록 포함)
     */
    @Operation(summary = "매장 상세 조회", description = "매장 상세 정보와 판매 중인 상품 목록을 조회합니다.")
    @GetMapping("/{storeId}")
    fun getStore(
        @PathVariable storeId: Long,
    ): ApiResponse<StoreDetailResponse> {
        return ok(storeService.getStoreDetailBuyer(storeId, currentMemberId()))
    }

    private fun currentMemberId(): Long? =
        (SecurityContextHolder.getContext().authentication?.principal as? JwtUser)?.memberId
}
