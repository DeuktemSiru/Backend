package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.SettlementService
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

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
data class SettlementWithdrawRequest(val year: Int, val month: Int)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/settlements")
class SellerSettlementController(
    private val settlementService: SettlementService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/sellers/settlements
     * 정산 내역 조회 — 지정한 연월의 정산 내역 반환
     * @param year  조회 연도 (기본값: 현재 연도)
     * @param month 조회 월 (기본값: 현재 월)
     */
    @GetMapping
    fun getSettlements(
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?,
    ): ApiResponse<SettlementListResponse> {
        val now = LocalDate.now()
        val targetYear = year ?: now.year
        val targetMonth = month ?: now.monthValue
        require(targetMonth in 1..12) { "month는 1~12 사이 값이어야 합니다." }
        val response = settlementService.getSettlements(authContext.getCurrentMemberId(), targetYear, targetMonth)
        return ApiResponse.success(response)
    }

    @PostMapping("/withdrawals")
    fun requestWithdrawal(
        @RequestBody req: SettlementWithdrawRequest,
    ): ApiResponse<SettlementItem> {
        require(req.month in 1..12) { "month는 1~12 사이 값이어야 합니다." }
        return ApiResponse.success(
            settlementService.requestWithdrawal(authContext.getCurrentMemberId(), req.year, req.month),
            "출금 신청이 접수되었습니다.",
        )
    }
}
