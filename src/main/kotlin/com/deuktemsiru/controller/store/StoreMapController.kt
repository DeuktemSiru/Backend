package com.deuktemsiru.controller.store

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

data class StoreMarker(
    val storeId: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val availableProductCount: Int,
    val category: String,
)

data class StoreMapResponse(val markers: List<StoreMarker>)

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
        val stores = storeService.getStores(category, null)
        val markers = stores.map {
            StoreMarker(
                storeId = it.id,
                name = it.name,
                latitude = it.latitude,
                longitude = it.longitude,
                availableProductCount = it.menus.count { m -> !m.isSoldOut },
                category = it.category.name,
            )
        }
        return ApiResponse.success(StoreMapResponse(markers))
    }
}
