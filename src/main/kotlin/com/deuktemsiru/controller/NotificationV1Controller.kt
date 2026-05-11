package com.deuktemsiru.controller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.AppNotificationResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.NotificationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationV1Controller(
    private val notificationService: NotificationService,
    private val authContext: AuthContext,
) {

    @GetMapping
    fun getNotifications(): ApiResponse<List<AppNotificationResponse>> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(notificationService.getNotificationEntities(memberId).map { AppNotificationResponse.from(it) })
    }

    @PatchMapping("/{notificationId}/read")
    fun markAsRead(@PathVariable notificationId: Long): ApiResponse<Unit> {
        notificationService.markAsRead(authContext.getCurrentMemberId(), notificationId)
        return ApiResponse.success(Unit)
    }
}
