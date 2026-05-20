package com.deuktemsiru.controller.notification

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.NotificationResponse
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.NotificationService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

data class NotificationListResponse(
    val notifications: List<NotificationResponse>,
    val unreadCount: Int,
)

@Tag(name = "Notifications", description = "구매자 알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {
    /**
     * GET /api/v1/notifications
     * 알림 목록 + 안 읽은 알림 수
     */
    @GetMapping
    fun getMyNotifications(
        @CurrentMemberId memberId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<NotificationListResponse> {
        val result = notificationService.getNotifications(memberId)
        return ok(
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
        @CurrentMemberId memberId: Long,
        @PathVariable notificationId: Long,
    ): ApiResponse<Unit> {
        notificationService.markAsRead(memberId, notificationId)
        return ok(Unit, "읽음 처리 완료")
    }

    /**
     * DELETE /api/v1/notifications/{notificationId}
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    fun deleteNotification(
        @CurrentMemberId memberId: Long,
        @PathVariable notificationId: Long,
    ): ApiResponse<Unit> {
        notificationService.deleteNotification(memberId, notificationId)
        return ok(Unit, "알림 삭제 완료")
    }
}
