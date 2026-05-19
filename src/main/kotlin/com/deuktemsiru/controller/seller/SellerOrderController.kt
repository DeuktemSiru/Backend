package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.OrderDetailResponse
import com.deuktemsiru.dto.PickupConfirmRequest
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.OrderService
import org.springframework.web.bind.annotation.*

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/orders")
class SellerOrderController(
    private val orderService: OrderService,
) {

    /**
     * GET /api/v1/sellers/orders
     * 내 가게 주문 목록 조회
     */
    @GetMapping
    fun getStoreOrders(
        @CurrentMemberId sellerId: Long,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) date: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<List<OrderDetailResponse>> {
        return ok(orderService.getStoreOrders(sellerId, status, date, page, size))
    }

    /**
     * GET /api/v1/sellers/orders/{orderId}
     * 주문 상세 조회 (고객명·메뉴·수량·픽업시각)
     */
    @GetMapping("/{orderId}")
    fun getStoreOrder(
        @CurrentMemberId sellerId: Long,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderDetailResponse> {
        return ok(orderService.getStoreOrder(sellerId, orderId))
    }

    /**
     * PATCH /api/v1/sellers/orders/{orderId}/confirm
     * 픽업 코드 확인 → 픽업 완료 처리
     */
    @PatchMapping("/{orderId}/confirm")
    fun confirmPickup(
        @CurrentMemberId sellerId: Long,
        @PathVariable orderId: Long,
        @RequestBody req: PickupConfirmRequest,
    ): ApiResponse<OrderDetailResponse> {
        return ok(orderService.confirmPickupCode(sellerId, orderId, req.pickupCode))
    }

    /**
     * PATCH /api/v1/sellers/orders/{orderId}/status
     * 주문 상태 직접 변경 (PENDING → CONFIRMED → PICKED_UP / CANCELLED)
     */
    @PatchMapping("/{orderId}/status")
    fun updateOrderStatus(
        @CurrentMemberId sellerId: Long,
        @PathVariable orderId: Long,
        @RequestBody req: UpdateOrderStatusRequest,
    ): ApiResponse<OrderDetailResponse> {
        return ok(orderService.updateOrderStatus(sellerId, orderId, req))
    }

}

@RestController
@RequestMapping("/api/v1/sellers/pickup")
class SellerPickupController(
    private val orderService: OrderService,
) {
    @GetMapping("/verify")
    fun verifyPickupCode(
        @CurrentMemberId sellerId: Long,
        @RequestParam code: String,
    ): ApiResponse<OrderDetailResponse> {
        return ok(orderService.verifyPickupCode(sellerId, code))
    }
}
