package com.deuktemsiru.controller

import com.deuktemsiru.dto.NotificationResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.NotificationService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val authContext: AuthContext,
) {

    @GetMapping
    fun getMyNotifications(@RequestParam memberId: Long): List<NotificationResponse> {
        authContext.requireCurrentMemberId(memberId)
        return notificationService.getNotifications(memberId)
    }

    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @RequestParam memberId: Long,
        @PathVariable notificationId: Long,
    ) {
        authContext.requireCurrentMemberId(memberId)
        notificationService.markAsRead(memberId, notificationId)
    }
}
