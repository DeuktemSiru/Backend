package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
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
class SellerSettlementController {

    /**
     * GET /api/v1/sellers/settlements
     * 정산 내역 조회
     * TODO: Settlement 엔티티 및 정산 처리 로직 구현 필요
     */
    @GetMapping
    fun getSettlements(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<SettlementListResponse> {
        throw UnsupportedOperationException("정산 내역 조회: 미구현 — Settlement 엔티티 및 서비스 구현 필요")
    }
}
