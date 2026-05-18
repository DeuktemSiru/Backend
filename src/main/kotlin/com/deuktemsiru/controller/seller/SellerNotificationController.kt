package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.SendNotificationRequest
import com.deuktemsiru.dto.SellerNotificationResponse
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.SellerAppService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Controller ────────────────────────────────────────────────────────────────

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
        val notification = sellerAppService.sendNotification(sellerId, req)
        return ApiResponse.createdEntity(notification, "알림이 발송되었습니다.")
    }

    @GetMapping
    fun getNotifications(@CurrentMemberId sellerId: Long): ApiResponse<List<SellerNotificationResponse>> {
        return ApiResponse.success(sellerAppService.getSellerNotifications(sellerId))
    }
}
