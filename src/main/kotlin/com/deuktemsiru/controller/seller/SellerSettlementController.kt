package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.SettlementService
import org.springframework.web.bind.annotation.*

// ── Response DTOs ─────────────────────────────────────────────────────────────

data class SettlementItem(
    val settlementId: Long,
    val periodStart: String,
    val periodEnd: String,
    val totalSales: Int,
    val platformFee: Int,
    val settlementAmount: Int,
    val status: String,   // PENDING | COMPLETED
    val settledAt: String?,
)

data class SettlementListResponse(val settlements: List<SettlementItem>)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/settlements")
class SellerSettlementController(
    private val settlementService: SettlementService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/sellers/settlements
     * 정산 내역 조회
     */
    @GetMapping
    fun getSettlements(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<SettlementListResponse> {
        val response = settlementService.getSettlements(authContext.getCurrentMemberId(), page, size)
        return ApiResponse.success(response)
    }
}
