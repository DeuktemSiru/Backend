package com.deuktemsiru.controller.notification

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.NotificationResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.NotificationService
import org.springframework.web.bind.annotation.*

// ── Response DTOs ─────────────────────────────────────────────────────────────

data class NotificationListResponse(val notifications: List<NotificationResponse>)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/notifications
     * 내 알림 목록 조회 (찜한 가게에서 발송된 알림)
     */
    @GetMapping
    fun getMyNotifications(): ApiResponse<NotificationListResponse> {
        val memberId = authContext.getCurrentMemberId()
        val notifications = notificationService.getNotifications(memberId)
        return ApiResponse.success(NotificationListResponse(notifications))
    }

    /**
     * PATCH /api/v1/notifications/{notificationId}/read
     * 알림 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @PathVariable notificationId: Long,
    ): ApiResponse<Unit> {
        val memberId = authContext.getCurrentMemberId()
        notificationService.markAsRead(memberId, notificationId)
        return ApiResponse.success(Unit, "읽음 처리 완료")
    }
}
