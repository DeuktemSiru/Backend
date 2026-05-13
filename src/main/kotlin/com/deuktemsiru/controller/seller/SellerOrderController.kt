package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.OrderResponse
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import org.springframework.web.bind.annotation.*

// ── Request DTOs ──────────────────────────────────────────────────────────────

data class PickupConfirmRequest(val pickupCode: String)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/orders")
class SellerOrderController(
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/sellers/orders
     * 내 가게 주문 목록 조회
     */
    @GetMapping
    fun getStoreOrders(): ApiResponse<List<OrderResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val orders = orderService.getStoreOrders(sellerId)
        return ApiResponse.success(orders)
    }

    /**
     * GET /api/v1/sellers/orders/{orderId}
     * 주문 상세 조회 (고객명·메뉴·수량·픽업시각)
     */
    @GetMapping("/{orderId}")
    fun getStoreOrder(
        @PathVariable orderId: Long,
    ): ApiResponse<OrderResp