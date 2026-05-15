package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.SendNotificationRequest
import com.deuktemsiru.dto.SellerNotificationResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.SellerAppService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/notifications")
class SellerNotificationController(
    private val sellerAppService: SellerAppService,
    private val authContext: AuthContext,
) {

    /**
     * POST /api/v1/sellers/notifications
     * 찜한 소비자들에게 알림 발송
     */
    @PostMapping
    fun sendNotification(
        @RequestBody req: SendNotificationRequest,
    ): ResponseEntity<ApiResponse<SellerNotificationResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val notification = sellerAppService.sendNotification(sellerId, req)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(notification, "알림이 발송되었습니다."))
    }
}
