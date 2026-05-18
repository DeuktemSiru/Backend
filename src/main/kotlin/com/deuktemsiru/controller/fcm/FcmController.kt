package com.deuktemsiru.controller.fcm

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.FcmService
import org.springframework.web.bind.annotation.*

// ── Request DTOs ──────────────────────────────────────────────────────────────

data class FcmTokenRequest(
    val token: String,
    val deviceInfo: String?,
)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/fcm")
class FcmController(
    private val fcmService: FcmService,
) {

    /**
     * POST /api/v1/fcm/token
     * FCM 디바이스 토큰 등록 / 갱신
     */
    @PostMapping("/token")
    fun registerFcmToken(
        @CurrentMemberId memberId: Long,
        @RequestBody req: FcmTokenRequest,
    ): ApiResponse<Unit> {
        fcmService.registerToken(memberId, req.token, req.deviceInfo)
        return ApiResponse.success(Unit, "FCM 토큰이 등록되었습니다.")
    }
}
