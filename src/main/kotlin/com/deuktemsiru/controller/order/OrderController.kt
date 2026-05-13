package com.deuktemsiru.controller.order

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.OrderResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Request DTOs ──────────────────────────────────────────────────────────────

data class OrderCancelRequest(val reason: String?)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {

    /**
     * POST /api/v1/orders
     * 주문 생성
     */
    @PostMapping
    fun createOrder(
        @RequestBody req: CreateOrderRequest,
    ): ResponseEntity<ApiResponse<OrderResponse>> {
        val memberId = authContext.getCurrentMemberId()
        val order = orderService.createOrder(memberId, req)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(order, "주문이 생성되었습니다."))
    }

    /**
     * GET /api/v1/orders/my
     * 내 주문 목록 조회 (진행중·픽업완료 탭)
     * @param status PENDING|CONFIRMED|PICKED_UP|CANCELLED (optional)
     */
    @GetMapping("/my")
    fun getMyOrders(
        @RequestParam(required = false) status: String?,
    ): ApiResponse<List<OrderResponse>> {
        val memberId = authContext.getCurrentMemberId()
        val orders = orderService.getMyOrders(memberId, status)
        return ApiResponse.success(orders)
    }

    /**
     * GET /api/v1/orders/{orderId}
     * 주문 상세 조회 (픽업 코드 포함)
     */
    @GetMapping("/{orderId}")
    fun getOrder(
        @PathVariable orderId: Long,
    ): ApiResponse<OrderResponse> {
        val order = orderService.getOrder(orderId)
        return ApiResponse.success(order)
    }

    /**
     * PATCH /api/v1/orders/{orderId}/cancel
     * 주문 취소 (픽업 전)
     * TODO: 취소 정책 및 환불 로직 구현 필요
     */
    @PatchMapping("/{orderId}/cancel")
    fun cancelOrder(
        @PathVariable orderId: Long,
        @Reques