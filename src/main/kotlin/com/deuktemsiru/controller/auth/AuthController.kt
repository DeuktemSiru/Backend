package com.deuktemsiru.controller.auth

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.security.JwtService
import com.deuktemsiru.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Request / Response DTOs ───────────────────────────────────────────────────

data class KakaoLoginRequest(
    val kakaoAccessToken: String,
    val role: String,   // "CONSUMER" | "SELLER"
)

data class MemberInfo(
    val memberId: Long,
    val nickname: String,
    val role: String,
)

data class LoginData(
    val accessToken: String,
    val refreshToken: String,
    val member: MemberInfo,
)

data class TokenRefreshRequest(val refreshToken: String)

data class TokenData(val accessToken: String)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val userService: UserService,
    private val jwtService: JwtService,
    private val authContext: AuthContext,
) {

    /**
     * POST /api/v1/auth/kakao/login
     * 카카오 소셜 로그인 / 자동 회원가입
     * TODO: 카카오 SDK 연동 필요 (feature/kakao_social_login_and_erd_modify 브랜치 참고)
     */
    @PostMapping("/kakao/login")
    fun kakaoLogin(
        @RequestBody req: KakaoLoginRequest,
    ): ResponseEntity<ApiResponse<LoginData>> {
        throw UnsupportedOperationException("카카오 로그인: 미구현 — feature/kakao_social_login_and_erd_modify 브랜치 병합 필요")
    }

    /**
     * POST /api/v1/auth/refresh
     * 액세스 토큰 갱신 (AT 30분 / RT 14일)
     * TODO: RefreshToken 저장소 구현 필요
     */
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody req: TokenRefreshRequest,
    ): ApiResponse<TokenData> {
        throw UnsupportedOperationException("토큰 갱신: 미구현 — RefreshToken 저장소 구현 필요")
    }

    /**
     * POST /api/v1/auth/logout
     * 로그아웃 — RefreshToken 무효화 + FCM Token 비활성화
     * TODO: RefreshToken / FCM Token 비활성화 로직 구현 필요
     */
    @PostMapping("/logout")
    fun logout(): ApiResponse<Unit> {
        // memberId는 사용 가능하나 token 무효화는 미구현
        authContext.getCurrentMemberId()
        return ApiResponse.success(Unit, "로그아웃 성공")
    }
}
