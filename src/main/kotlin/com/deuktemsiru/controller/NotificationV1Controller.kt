package com.deuktemsiru.controller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.AppNotificationResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.NotificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses as SwaggerApiResponses

@Tag(name = "Notifications", description = "알림 조회 및 읽음 처리 API")
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationV1Controller(
    private val notificationService: NotificationService,
    private val authContext: AuthContext,
) {

    @Operation(summary = "내 알림 목록 조회", description = "인증된 사용자의 알림 목록을 조회합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping
    fun getNotifications(): ApiResponse<List<AppNotificationResponse>> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(notificationService.getNotificationEntities(memberId).map { AppNotificationResponse.from(it) })
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "알림을 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @Parameter(description = "알림 ID", example = "1")
        @PathVariable notificationId: Long,
    ): ApiResponse<Unit> {
        notificationService.markAsRead(authContext.getCurrentMemberId(), notificationId)
        return ApiResponse.success(Unit)
    }
}
