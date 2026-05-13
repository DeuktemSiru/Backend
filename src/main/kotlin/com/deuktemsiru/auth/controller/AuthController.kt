package com.deuktemsiru.auth.controller

import com.deuktemsiru.auth.dto.KakaoLoginRequest
import com.deuktemsiru.auth.dto.LoginResponse
import com.deuktemsiru.auth.dto.TokenRefreshRequest
import com.deuktemsiru.auth.dto.TokenResponse
import com.deuktemsiru.auth.service.AuthService
import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.security.AuthContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Request DTOs ──────────────────────────────────────────────────────────────

data class SiruLinkRequest(val siruAccessToken: String)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val authContext: AuthContext,
) {

    /**
     * POST /api/v1/auth/kakao/login
     * 카카오 소셜 로그인 / 자동 회원가입
     * - 신규 회원: 201 Created
     * - 기존 회원: 200 OK
     */
    @PostMapping("/kakao/login")
    fun kakaoLogin(
        @RequestBody req: KakaoLoginRequest,
    ): ResponseEntity<ApiResponse<LoginResponse>> {
        val (response, isNew) = authService.kakaoLogin(req)
        return if (isNew) {
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "회원가입 후 로그인 성공"))
        } else {
            ResponseEntity.ok(ApiResponse.success(response, "로그인 성공"))
        }
    }

    /**
     * POST /api/v1/auth/refresh
     * Access Token 갱신 (AT: 30분 / RT: 14일)
     */
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody req: TokenRefreshRequest,
    ): ApiResponse<TokenResponse> {
        val response = authService.refresh(req)
        return ApiResponse.success(response, "토큰 갱신 성공")
    }

    /**
     * POST /api/v1/auth/logout
     * 로그아웃 — Refresh Token 전체 폐기 + FCM Token 비활성화
     */
    @PostMapping("/logout")
    fun logout(): ApiResponse<Unit> {
        val memberId = authContext.getCurrentMemberId()
        authService.logout(memberId)
        return ApiResponse.success(Unit, "로그아웃 성공")
    }

    /**
     * POST /api/v1/auth/siru/link
     * 시루 계정 연동
     * TODO: 시루 API 확정 후 구현 필요 (TBD)
     */
    @PostMapping("/siru/link")
    fun linkSiru(
        @RequestBody req: SiruLinkRequest,
    ): ApiResponse<Unit> {
        throw UnsupportedOperationException("시루 연동: 미구현 — 시루 API 확정 후 구현 필요 (TBD)")
    }

    /**
     * DELETE /api/v1/auth/siru/link
     * 시루 계정 연동 해제
     * TODO: 시루 API 확정 후 구현 필요 (TBD)
     */
    @DeleteMapping("/siru/link")
    fun unlinkSiru(): ApiResponse<Unit> {
        throw UnsupportedOperationException("시루 연동 해제: 미구현 — 시루 API 확정 후 구현 필요 (TBD)")
    }
}
