package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.SalesResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import org.springframework.web.bind.annotation.*

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/sales")
class SellerSalesController(
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/sellers/sales/summary
     * 매출 통계 조회 (오늘 매출, 주문 수, 기간별 그래프, TOP 상품)
     * @param period DAY | WEEK | MONTH (기본값: DAY) — spec v2.0 파라미터
     * @param offset 0 = 이번 기간, 1 = 지난 기간
     */
    @GetMapping("/summary")
    fun getSalesSummary(
        @RequestParam(defaultValue = "WEEK") period: String,
        @RequestParam(defaultValue = "0") offset: Int,
    ): ApiResponse<SalesResponse> {
        val sellerId = authContext.getCurrentMemberId()
        // spec: DAY/WEEK/MONTH → service 내부: weekly/monthly
        val normalizedPeriod = when (period.uppercase()) {
            "MONTH" -> "monthly"
            else    -> "weekly"   // DAY, WEEK 모두 weekly 집계
        }
        val stats = orderService.getSalesStats(sellerId, normalizedPeriod, offset)
        return ApiResponse.success(stats)
    }
}
