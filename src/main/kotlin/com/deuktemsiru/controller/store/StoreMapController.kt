package com.deuktemsiru.controller.store

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

// ── Response DTOs ─────────────────────────────────────────────────────────────

data class StoreMarker(
    val storeId: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val availableProductCount: Int,
    val category: String,
)

data class StoreMapResponse(val markers: List<StoreMarker>)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/stores/map")
class StoreMapController(
    private val storeService: StoreService,
) {

    /**
     * GET /api/v1/stores/map
     * 지도 뷰용 주변 가게 마커 목록
     * TODO: 위치 기반 필터링 구현 필요
     */
    @GetMapping
    fun getMapMarkers(
        @RequestParam(required = false) latitude: Double?,
        @RequestParam(required = false) longitude: Double?,
        @RequestParam(required = false, defaultValue = "2000") radius: Int,
        @RequestParam(required = false) category: String?,
    ): ApiResponse<StoreMapResponse> {
        val stores = storeService.getStores(null)
        val markers = stores.map { store ->
            StoreMarker(
                storeId = store.id,
                name = store.name,
                latitude = store.latitude,
                longitude = store.longitude,
                availableProductCount = store.menus.count { !it.isSoldOut },
                category = store.category.name,
            )
        }
        return ApiResponse.success(StoreMapResponse(markers))
    }
}
