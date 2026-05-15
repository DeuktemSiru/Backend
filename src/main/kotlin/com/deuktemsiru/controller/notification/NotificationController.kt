package com.deuktemsiru.controller.notification

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.NotificationResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.NotificationService
import org.springframework.web.bind.annotation.*

data class NotificationListResponse(
    val notifications: List<NotificationResponse>,
    val unreadCount: Int,
)

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val authContext: AuthContext,
) {
    /**
     * GET /api/v1/notifications
     * 알림 목록 + 안 읽은 알림 수
     */
    @GetMapping
    fun getMyNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<NotificationListResponse> {
        val memberId = authContext.getCurrentMemberId()
        val result = notificationService.getNotifications(memberId)
        return ApiResponse.success(
            NotificationListResponse(
                notifications = result.notifications,
                unreadCount = result.unreadCount,
            )
        )
    }

    /**
     * PATCH /api/v1/notifications/{notificationId}/read
     * 단건 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @PathVariable notificationId: Long,
    ): ApiResponse<Unit> {
        val memberId = authContext.getCurrentMemberId()
        notificationService.markAsRead(memberId, notificationId)
        return ApiResponse.success(Unit, "읽음 처리 완료")
    }

    /**
     * DELETE /api/v1/notifications/{notificationId}
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    fun deleteNotification(
        @PathVariable notificationId: Long,
    ): ApiResponse<Unit> {
        val memberId = authContext.getCurrentMemberId()
        notificationService.deleteNotification(memberId, notificationId)
        return ApiResponse.success(Unit, "알림 삭제 완료")
    }
}
