package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.OrderDetailResponse
import com.deuktemsiru.dto.PickupConfirmRequest
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

// ── Controller ────────────────────────────────────────────────────────────────

@Tag(name = "Seller Orders", description = "판매자 주문 관리 API")
@RestController
@RequestMapping("/api/v1/sellers/orders")
class SellerOrderController(
    private val orderService: OrderService,
) {

    /**
     * GET /api/v1/sellers/orders
     * 내 가게 주문 목록 조회
     */
    @Operation(summary = "판매자 주문 목록 조회", description = "판매자의 매장으로 들어온 주문 목록을 조회합니다.")
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
    @Operation(summary = "판매자 주문 상세 조회", description = "판매자의 매장 주문 상세 정보를 조회합니다.")
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
    @Operation(summary = "픽업 확정", description = "구매자가 제시한 픽업 코드로 주문을 픽업 완료 처리합니다.")
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
    @Operation(summary = "주문 상태 변경", description = "판매자가 주문 상태를 변경합니다.")
    @PatchMapping("/{orderId}/status")
    fun updateOrderStatus(
        @CurrentMemberId sellerId: Long,
        @PathVariable orderId: Long,
        @RequestBody req: UpdateOrderStatusRequest,
    ): ApiResponse<OrderDetailResponse> {
        return ok(orderService.updateOrderStatus(sellerId, orderId, req))
    }

}

@Tag(name = "Seller Orders", description = "판매자 픽업 코드 검증 API")
@RestController
@RequestMapping("/api/v1/sellers/pickup")
class SellerPickupController(
    private val orderService: OrderService,
) {
    @Operation(summary = "픽업 코드 검증", description = "픽업 코드로 주문을 조회해 판매자 매장 주문인지 확인합니다.")
    @GetMapping("/verify")
    fun verifyPickupCode(
        @CurrentMemberId sellerId: Long,
        @RequestParam code: String,
    ): ApiResponse<OrderDetailResponse> {
        return ok(orderService.verifyPickupCode(sellerId, code))
    }
}
