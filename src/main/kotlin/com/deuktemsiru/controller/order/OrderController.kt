package com.deuktemsiru.controller.order

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.CreateOrderResponse
import com.deuktemsiru.dto.OrderDetailResponse
import com.deuktemsiru.dto.OrderListItemResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {
    /**
     * POST /api/v1/orders
     * 주문 생성 — items[].productId 기반으로 가게를 자동 결정
     */
    @PostMapping
    fun createOrder(
        @RequestBody req: CreateOrderRequest,
    ): ResponseEntity<ApiResponse<CreateOrderResponse>> {
        val memberId = authContext.getCurrentMemberId()
        val order = orderService.createOrder(memberId, req)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(order, "주문 성공"))
    }

    /**
     * GET /api/v1/orders/my
     * 내 주문 목록 (status 필터 선택)
     */
    @GetMapping("/my")
    fun getMyOrders(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<List<OrderListItemResponse>> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(orderService.getMyOrders(memberId, status))
    }

    /**
     * GET /api/v1/orders/{orderId}
     * 주문 상세 조회 (픽업 코드 포함)
     */
    @GetMapping("/{orderId}")
    fun getOrder(
        @PathVariable orderId: Long,
    ): ApiResponse<OrderDetailResponse> {
        return ApiResponse.success(orderService.getOrder(orderId))
    }

    /**
     * PATCH /api/v1/orders/{orderId}/cancel
     * 주문 취소 (픽업 전 — PENDING/CONFIRMED 상태만 가능)
     */
    @PatchMapping("/{orderId}/cancel")
    fun cancelOrder(
        @PathVariable orderId: Long,
    ): ApiResponse<OrderDetailResponse> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(orderService.cancelOrder(memberId, orderId), "주문이 취소되었습니다.")
    }
}
