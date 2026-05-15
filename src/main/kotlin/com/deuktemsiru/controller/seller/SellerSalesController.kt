package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.SalesResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/sales")
class SellerSalesController(
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/sellers/sales/summary
     * 매출 통계 조회 (기간별 그래프, TOP 상품, 탄소 저감량)
     * @param period DAY | WEEK | MONTH (기본값: DAY)
     *   - DAY  : 지정 날짜의 시간대별(0~23시) 매출 차트
     *   - WEEK : 지정 날짜가 속한 주(월~일) 일별 매출 차트
     *   - MONTH: 지정 날짜가 속한 월의 주별 매출 차트
     * @param date 기준 날짜 (yyyy-MM-dd, 기본값: 오늘)
     */
    @GetMapping("/summary")
    fun getSalesSummary(
        @RequestParam(defaultValue = "DAY") period: String,
        @RequestParam(required = false) date: String?,
    ): ApiResponse<SalesResponse> {
        val sellerId = authContext.getCurrentMemberId()
        val targetDate = date
            ?.let { runCatching { LocalDate.parse(it) }.getOrElse { throw IllegalArgumentException("날짜 형식은 yyyy-MM-dd 이어야 합니다.") } }
            ?: LocalDate.now()
        val stats = orderService.getSalesStats(sellerId, period, targetDate)
        return ApiResponse.success(stats)
    }
}
