package com.deuktemsiru.auth.controller

import com.deuktemsiru.auth.dto.DebugLoginRequest
import com.deuktemsiru.auth.dto.KakaoLoginRequest
import com.deuktemsiru.auth.dto.LoginResponse
import com.deuktemsiru.auth.dto.TokenRefreshRequest
import com.deuktemsiru.auth.dto.TokenResponse
import com.deuktemsiru.auth.service.AuthService
import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.security.AuthContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val authContext: AuthContext,
    @Value("\${app.security.dev-endpoints-enabled:true}")
    private val devEndpointsEnabled: Boolean,
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
     * POST /api/v1/auth/debug/login
     * 로컬 개발 전용 로그인. 카카오 SDK 없이 샘플 사용자 JWT를 발급합니다.
     */
    @PostMapping("/debug/login")
    fun debugLogin(
        @RequestBody req: DebugLoginRequest,
    ): ApiResponse<LoginResponse> {
        if (!devEndpointsEnabled) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "디버그 로그인은 비활성화되어 있습니다.",
            )
        }
        return ApiResponse.success(authService.debugLogin(req.role), "디버그 로그인 성공")
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
     * Authorization: Bearer {accessToken} 필요
     */
    @PostMapping("/logout")
    fun logout(): ApiResponse<Unit> {
        val memberId = authContext.getCurrentMemberId()
        authService.logout(memberId)
        return ApiResponse.success(Unit, "로그아웃 성공")
    }
}
