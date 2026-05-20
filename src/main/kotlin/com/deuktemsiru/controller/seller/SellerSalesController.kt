package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.ok
import com.deuktemsiru.common.toLocalDateOrThrow
import com.deuktemsiru.dto.SalesResponse
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.OrderService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.time.Clock
import java.time.LocalDate

// ── Controller ────────────────────────────────────────────────────────────────

@Tag(name = "Seller Sales", description = "판매자 매출 요약 API")
@RestController
@RequestMapping("/api/v1/sellers/sales")
class SellerSalesController(
    private val orderService: OrderService,
    private val clock: Clock,
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
        @CurrentMemberId sellerId: Long,
        @RequestParam(defaultValue = "DAY") period: String,
        @RequestParam(required = false) date: String?,
    ): ApiResponse<SalesResponse> {
        val targetDate = date?.toLocalDateOrThrow() ?: LocalDate.now(clock)
        return ok(orderService.getSalesStats(sellerId, period, targetDate))
    }
}
