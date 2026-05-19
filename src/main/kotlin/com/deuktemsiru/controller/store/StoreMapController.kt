package com.deuktemsiru.controller.store

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.StoreMarkerResponse
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

data class StoreMapResponse(val markers: List<StoreMarkerResponse>)

@RestController
@RequestMapping("/api/v1/stores/map")
class StoreMapController(
    private val storeService: StoreService,
) {
    /**
     * GET /api/v1/stores/map
     * 지도 뷰용 가게 마커 목록
     */
    @GetMapping
    fun getMapMarkers(
        @RequestParam(required = true) latitude: Double,
        @RequestParam(required = true) longitude: Double,
        @RequestParam(required = false, defaultValue = "2000") radius: Int,
        @RequestParam(required = false) category: String?,
    ): ApiResponse<StoreMapResponse> {
        return ok(StoreMapResponse(storeService.getMapMarkers(category)))
    }
}
