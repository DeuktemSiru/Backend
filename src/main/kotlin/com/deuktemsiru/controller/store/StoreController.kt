package com.deuktemsiru.controller.store

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.StoreResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

// ── Response DTOs (spec-aligned) ─────────────────────────────────────────────

data class StoreListItem(
    val storeId: Long,
    val name: String,
    val thumbnailUrl: String?,          // TODO: 엔티티에 썸네일 필드 추가 후 연결
    val distanceM: Int?,                // TODO: 위치 기반 거리 계산 구현 후 연결
    val category: String,
    val ratingAvg: Float,
    val reviewCount: Int,               // TODO: 리뷰 집계 구현 후 연결
    val availableProductCount: Int,
) {
    companion object {
        fun from(store: StoreResponse) = StoreListItem(
            storeId = store.id,
            name = store.name,
            thumbnailUrl = null,
            distanceM = null,
            category = store.category.name,
            ratingAvg = store.rating,
            reviewCount = 0,
            availableProductCount = store.menus.count { !it.isSoldOut },
        )
    }
}

data class StoreListResponse(val stores: List<StoreListItem>, val hasNext: Boolean)

data class ProductSummary(
    val productId: Long,
    val name: String,
    val discountPrice: Int,
    val quantityRemaining: Int,
    val pickupEnd: String,
    val status: String,
)

data class StoreDetailResponse(
    val storeId: Long,
    val name: String,
    val description: String?,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val thumbnailUrl: String?,
    val images: List<String>,
    val categories: List<String>,
    val ratingAvg: Float,
    val reviewCount: Int,
    val products: List<ProductSummary>,
) {
    companion object {
        fun from(store: StoreResponse) = StoreDetailResponse(
            storeId = store.id,
            name = store.name,
            description = null,
            address = store.address,
            latitude = store.latitude,
            longitude = store.longitude,
            phone = store.phone,
            thumbnailUrl = null,
            images = emptyList(),
            categories = listOf(store.category.name),
            ratingAvg = store.rating,
            reviewCount = 0,
            products = store.menus.map {
                ProductSummary(
                    productId = it.id,
                    name = it.name,
                    discountPrice = it.discountedPrice,
                    quantityRemaining = it.remainingItems,
                    pickupEnd = it.pickupTimeSlot.substringAfter("-").trim(),
                    status = if (it.isSoldOut) "SOLD_OUT" else "AVAILABLE",
                )
            },
        )
    }
}

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/stores")
class StoreController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/stores
     * 주변 가게 목록 (위치 기반·리스트 뷰)
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
        val stores = storeService.getStores(category, memberId)
            .map { StoreListItem.from(it) }
        return ApiResponse.success(StoreListResponse(stores = stores, hasNext = false))
    }

    /**
     * GET /api/v1/stores/{storeId}
     * 가게 상세 조회
     */
    @GetMapping("/{storeId}")
    fun getStore(
        @PathVariable storeId: Long,
    ): ApiResponse<StoreDetailResponse> {
        val memberId = authContext.getCurrentMemberId()
        val store = storeService.getStore(storeId, memberId)
        return ApiResponse.success(StoreDetailResponse.from(store))
    }
}
