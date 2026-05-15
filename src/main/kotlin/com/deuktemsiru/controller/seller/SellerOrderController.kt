package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.OrderDetailResponse
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import org.springframework.data.domain.PageRequest
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
    fun getStoreOrders(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) date: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<List<OrderDetailResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val orders = orderService.getStoreOrders(sellerId, status, date, page, size)
        return ApiResponse.success(orders)
    }

    /**
     * GET /api/v1/sellers/orders/{orderId}
     * 주문 상세 조회 (고객명·메뉴·수량·픽업시각)
     */
    @GetMapping("/{orderId}")
    fun getStoreOrder(
        @PathVariable orderId: Long,
    ): ApiResponse<OrderDetailResponse> {
        val sellerId = authContext.getCurrentMemberId()
        val order = orderService.getStoreOrder(sellerId, orderId)
        return ApiResponse.success(order)
    }

    /**
     * PATCH /api/v1/sellers/orders/{orderId}/confirm
     * 픽업 코드 확인 → 픽업 완료 처리
     */
    @PatchMapping("/{orderId}/confirm")
    fun confirmPickup(
        @PathVariable orderId: Long,
        @RequestBody req: PickupConfirmRequest,
    ): ApiResponse<OrderDetailResponse> {
        val sellerId = authContext.getCurrentMemberId()
        val verified = orderService.verifyPickupCode(sellerId, req.pickupCode)
        require(verified.orderId == orderId) { "주문과 픽업 코드가 일치하지 않습니다." }
        val order = orderService.updateOrderStatus(sellerId, orderId, UpdateOrderStatusRequest(OrderStatus.PICKED_UP))
        return ApiResponse.success(order)
    }

    /**
     * PATCH /api/v1/sellers/orders/{orderId}/status
     * 주문 상태 직접 변경 (PENDING → CONFIRMED → PICKED_UP / CANCELLED)
     */
    @PatchMapping("/{orderId}/status")
    fun updateOrderStatus(
        @PathVariable orderId: Long,
        @RequestBody req: UpdateOrderStatusRequest,
    ): ApiResponse<OrderDetailResponse> {
        val sellerId = authContext.getCurrentMemberId()
        val order = orderService.updateOrderStatus(sellerId, orderId, req)
        return ApiResponse.success(order)
    }

}

@RestController
@RequestMapping("/api/v1/sellers/pickup")
class SellerPickupController(
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {
    @GetMapping("/verify")
    fun verifyPickupCode(
        @RequestParam code: String,
    ): ApiResponse<OrderDetailResponse> {
        val sellerId = authContext.getCurrentMemberId()
        return ApiResponse.success(orderService.verifyPickupCode(sellerId, code))
    }
}
