package com.deuktemsiru.controller.fcm

import com.deuktemsiru.common.ApiResponse
import org.springframework.web.bind.annotation.*

// ── Request DTOs ──────────────────────────────────────────────────────────────

data class FcmTokenRequest(
    val token: String,
    val deviceInfo: String?,
)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/fcm")
class FcmController {

    /**
     * POST /api/v1/fcm/token
     * FCM 디바이스 토큰 등록 / 갱신
     * TODO: FcmTokenRepository 및 저장 로직 구현 필요
     */
    @PostMapping("/token")
    fun registerFcmToken(
        @RequestBody req: FcmTokenRequest,
    ): ApiResponse<Unit> {
        throw UnsupportedOperationException("FCM 토큰 등록: 미구현 — FcmTokenRepository 저장 로직 구현 필요")
    }
}
