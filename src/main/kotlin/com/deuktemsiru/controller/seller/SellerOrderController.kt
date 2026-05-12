package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.OrderResponse
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import org.springframework.web.bind.annotation.*

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
     * PATCH /api/v1/sellers/orders/{orderId}/status
     * 주문 상태 변경 (PREPARING → READY → COMPLETED / REJECTED)
     */
    @PatchMapping("/{orderId}/status")
    fun updateOrderStatus(
        @PathVariable orderId: Long,
        @RequestBody req: UpdateOrderStatusRequest,
    ): ApiResponse<OrderResponse> {
        val sellerId = authContext.getCurrentMemberId()
        val order = orderService.updateOrderStatus(sellerId, orderId, req)
        return ApiResponse.success(order)
    }
}
