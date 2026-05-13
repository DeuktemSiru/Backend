package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.SellerStoreResponse
import com.deuktemsiru.dto.SellerUpdateStoreRequest
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.SellerAppService
import org.springframework.web.bind.annotation.*

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/stores")
class SellerStoreController(
    private val sellerAppService: SellerAppService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/sellers/stores/my
     * 내 가게 조회
     */
    @GetMapping("/my")
    fun getMyStore(): ApiResponse<SellerStoreResponse> {
        val sellerId = authContext.getCurrentMemberId()
        return ApiResponse.success(sellerAppService.getStore(sellerId))
    }

    /**
     * PUT /api/v1/sellers/stores/{storeId}
     * 가게 정보 수정 (주소, 전화번호, 마감 시간)
     */
    @PutMapping("/{storeId}")
    fun updateStore(
        @PathVariable storeId: Long,
        @RequestBody req: SellerUpdateStoreRequest,
    ): ApiResponse<SellerStoreResponse> {
        val sellerId = authContext.getCurrentMemberId()
        return ApiResponse.success(sellerAppService.updateStore(sellerId, req))
    }

    /**
     * POST /api/v1/sellers/stores
     * 가게 등록
     * TODO: 가게 등록 서비스 메서드 구현 필요
     */
    @PostMapping
    fun createStore(): ApiResponse<Unit> {
        throw UnsupportedOperationException("가게 등록: 미구현 — SellerAppService.createStore() 구현 필요")
    }
}
