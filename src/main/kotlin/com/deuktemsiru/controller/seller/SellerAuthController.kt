package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.security.AuthContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Request / Response DTOs ───────────────────────────────────────────────────

data class BusinessInfoRequest(
    val businessName: String,
    val businessNumber: String,
    val representativeName: String,
    val address: String,
    val phone: String,
)

data class BusinessInfoResponse(
    val memberId: Long,
    val businessName: String,
    val businessNumber: String,
    val representativeName: String,
    val address: String,
    val phone: String,
)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers")
class SellerAuthController(
    private val authContext: AuthContext,
) {

    /**
     * POST /api/v1/sellers/business-info
     * 사업자 정보 등록 (판매자 등록)
     * TODO: BusinessInfo 엔티티 및 등록 로직 구현 필요
     */
    @PostMapping("/business-info")
    fun registerBusinessInfo(
        @RequestBody req: BusinessInfoRequest,
    ): ResponseEntity<ApiResponse<BusinessInfoResponse>> {
        throw UnsupportedOperationException("사업자 정보 등록: 미구현 — BusinessInfo 엔티티 및 서비스 구현 필요")
    }
}
