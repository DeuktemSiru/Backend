package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.SettlementItem
import com.deuktemsiru.dto.SettlementListResponse
import com.deuktemsiru.dto.SettlementWithdrawRequest
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.SettlementService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.time.Clock
import java.time.LocalDate

// ── Controller ────────────────────────────────────────────────────────────────

@Tag(name = "Seller Settlements", description = "판매자 정산 API")
@RestController
@RequestMapping("/api/v1/sellers/settlements")
class SellerSettlementController(
    private val settlementService: SettlementService,
    private val clock: Clock,
) {

    /**
     * GET /api/v1/sellers/settlements
     * 정산 내역 조회 — 지정한 연월의 정산 내역 반환
     * @param year  조회 연도 (기본값: 현재 연도)
     * @param month 조회 월 (기본값: 현재 월)
     */
    @GetMapping
    fun getSettlements(
        @CurrentMemberId sellerId: Long,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?,
    ): ApiResponse<SettlementListResponse> {
        val now = LocalDate.now(clock)
        val targetYear = year ?: now.year
        val targetMonth = month ?: now.monthValue
        require(targetMonth in 1..12) { "month는 1~12 사이 값이어야 합니다." }
        return ok(settlementService.getSettlements(sellerId, targetYear, targetMonth))
    }

    @PostMapping("/withdrawals")
    fun requestWithdrawal(
        @CurrentMemberId sellerId: Long,
        @RequestBody req: SettlementWithdrawRequest,
    ): ApiResponse<SettlementItem> {
        require(req.month in 1..12) { "month는 1~12 사이 값이어야 합니다." }
        return ok(
            settlementService.requestWithdrawal(sellerId, req.year, req.month),
            "출금 신청이 접수되었습니다.",
        )
    }
}
