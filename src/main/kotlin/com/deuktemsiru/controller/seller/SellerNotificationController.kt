package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.created
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.SendNotificationRequest
import com.deuktemsiru.dto.SellerNotificationResponse
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.SellerAppService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Controller ────────────────────────────────────────────────────────────────

@Tag(name = "Seller Notifications", description = "판매자 고객 알림 발송 API")
@RestController
@RequestMapping("/api/v1/sellers/notifications")
class SellerNotificationController(
    private val sellerAppService: SellerAppService,
) {

    /**
     * POST /api/v1/sellers/notifications
     * 찜한 소비자들에게 알림 발송
     */
    @PostMapping
    fun sendNotification(
        @CurrentMemberId sellerId: Long,
        @RequestBody req: SendNotificationRequest,
    ): ResponseEntity<ApiResponse<SellerNotificationResponse>> {
        return created(sellerAppService.sendNotification(sellerId, req), "알림이 발송되었습니다.")
    }

    @GetMapping
    fun getNotifications(@CurrentMemberId sellerId: Long): ApiResponse<List<SellerNotificationResponse>> {
        return ok(sellerAppService.getSellerNotifications(sellerId))
    }
}
