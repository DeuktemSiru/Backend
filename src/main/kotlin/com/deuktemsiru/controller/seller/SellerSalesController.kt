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
     * 매출 통계 조회 (오늘 매출, 주문 수, 주간/월간/연간 그래프, TOP 메뉴)
     * @param period weekly | monthly | yearly
     * @param offset 0 = 이번 주(월/년), 1 = 지난 주(월/년)
     */
    @GetMapping("/summary")
    fun getSalesSummary(
        @RequestParam(defaultValue = "weekly") period: String,
        @RequestParam(defaultValue = "0") offset: Int,
    ): ApiResponse<SalesResponse> {
        val sellerId = authContext.getCurrentMemberId()
        val stats = orderService.getSalesStats(sellerId, period, offset)
        return ApiResponse.success(stats)
    }
}
