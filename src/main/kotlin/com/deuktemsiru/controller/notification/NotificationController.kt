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
        val notifications = notificationService.getBuyerNotifications(memberId)
        return ApiResponse.success(NotificationListResponse(notifications))
    }

    /**
     * PATCH /api/v1/notifications/{notificationId}/read
     * 알림 읽음 처리
     * TODO: Notification 엔티티에 isRead 필드 추가 및 읽음 처리 로직 구현 필요
     */
    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @PathVariable notificationId: Long,
    ): ApiResponse<Unit> {
        throw UnsupportedOperationException("알림 읽음 처리: 미구현 — Notification.isRead 필드 및 서비스 메서드 구현 필요")
    }
}
