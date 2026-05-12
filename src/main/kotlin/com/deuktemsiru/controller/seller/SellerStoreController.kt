package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.StoreResponse
import com.deuktemsiru.dto.UpdateStoreRequest
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Request / Response DTOs ───────────────────────────────────────────────────

data class StoreCreateRequest(
    val name: String,
    val category: String,
    val address: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val closingTime: String,
)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/stores")
class SellerStoreController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    /**
     * POST /api/v1/sellers/stores
     * 가게 등록
     * TODO: 가게 등록 서비스 메서드 구현 필요
     */
    @PostMapping
    fun createStore(
        @RequestBody req: StoreCreateRequest,
    ): ResponseEntity<ApiResponse<StoreResponse>> {
        throw UnsupportedOperationException("가게 등록: 미구현 — StoreService.createStore() 구현 필요")
    }

    /**
     * GET /api/v1/sellers/stores/my
     * 내 가게 조회
     */
    @GetMapping("/my")
    fun getMyStore(): ApiResponse<StoreResponse> {
        val sellerId = authContext.getCurrentMemberId()
        val store = storeService.getSellerStore(sellerId)
        return ApiResponse.success(store)
    }

    /**
     * PUT /api/v1/sellers/stores/{storeId}
     * 가게 정보 수정 (주소, 전화번호, 마감 시간)
     */
    @PutMapping("/{storeId}")
    fun updateStore(
        @PathVariable storeId: Long,
        @RequestBody req: UpdateStoreRequest,
    ): ApiResponse<StoreResponse> {
        val sellerId = authContext.getCurrentMemberId()
        val store = storeService.updateStore(sellerId, req)
        return ApiResponse.success(store)
    }
}
