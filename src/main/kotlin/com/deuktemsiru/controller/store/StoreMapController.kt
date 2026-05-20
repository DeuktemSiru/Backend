package com.deuktemsiru.controller.store

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.StoreMarkerResponse
import com.deuktemsiru.service.StoreService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

data class StoreMapResponse(val markers: List<StoreMarkerResponse>)

@Tag(name = "Buyer Stores", description = "구매자 매장 지도 API")
@RestController
@RequestMapping("/api/v1/stores/map")
class StoreMapController(
    private val storeService: StoreService,
) {
    /**
     * GET /api/v1/stores/map
     * 지도 뷰용 가게 마커 목록
     */
    @Operation(summary = "매장 지도 마커 조회", description = "지도 화면에 표시할 주변 매장 마커 목록을 조회합니다.")
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
