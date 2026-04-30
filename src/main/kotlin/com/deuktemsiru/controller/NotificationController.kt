package com.deuktemsiru.controller

import com.deuktemsiru.dto.NotificationResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.NotificationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val authContext: AuthContext,
) {

    @GetMapping
    fun getMyNotifications(@RequestParam userId: Long): List<NotificationResponse> {
        authContext.requireCurrentUserId(userId)
        return notificationService.getBuyerNotifications(userId)
    }
}
