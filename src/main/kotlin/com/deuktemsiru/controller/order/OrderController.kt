package com.deuktemsiru.controller.order

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.created
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.CreateOrderResponse
import com.deuktemsiru.dto.OrderDetailResponse
import com.deuktemsiru.dto.OrderListItemResponse
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.OrderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
) {
    /**
     * POST /api/v1/orders
     * 주문 생성 — items[].productId 기반으로 가게를 자동 결정
     */
    @PostMapping
    fun createOrder(
        @CurrentMemberId memberId: Long,
        @RequestBody req: CreateOrderRequest,
    ): ResponseEntity<ApiResponse<CreateOrderResponse>> {
        return created(orderService.createOrder(memberId, req), "주문 성공")
    }

    /**
     * GET /api/v1/orders/my
     * 내 주문 목록 (status 필터 선택, 페이지네이션)
     */
    @GetMapping("/my")
    fun getMyOrders(
        @CurrentMemberId memberId: Long,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<List<OrderListItemResponse>> {
        return myOrders(memberId, status, page, size)
    }

    /**
     * GET /api/v1/orders
     * 구매자 앱 호환용 내 주문 목록
     */
    @GetMapping
    fun getMyOrdersCompat(
        @CurrentMemberId memberId: Long,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<List<OrderListItemResponse>> {
        return myOrders(memberId, status, page, size)
    }

    /**
     * GET /api/v1/orders/{orderId}
     * 주문 상세 조회 (픽업 코드 포함)
     */
    @GetMapping("/{orderId}")
    fun getOrder(
        @CurrentMemberId memberId: Long,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderDetailResponse> {
        return ok(orderService.getOrder(memberId, orderId))
    }

    /**
     * PATCH /api/v1/orders/{orderId}/cancel
     * 주문 취소 (픽업 전 — PENDING/CONFIRMED 상태만 가능)
     */
    @PatchMapping("/{orderId}/cancel")
    fun cancelOrder(
        @CurrentMemberId memberId: Long,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderDetailResponse> {
        return ok(orderService.cancelOrder(memberId, orderId), "주문이 취소되었습니다.")
    }

    private fun myOrders(memberId: Long, status: String?, page: Int, size: Int): ApiResponse<List<OrderListItemResponse>> =
        ok(orderService.getMyOrders(memberId, status, page, size))
}
