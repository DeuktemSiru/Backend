package com.deuktemsiru.auth.dto

import com.deuktemsiru.entity.MemberRole
import io.swagger.v3.oas.annotations.media.Schema

// ──────────────────── 요청 ────────────────────

@Schema(description = "카카오 로그인 요청")
data class KakaoLoginRequest(
    @field:Schema(description = "카카오 SDK에서 발급받은 액세스 토큰", example = "kakao-access-token")
    val kakaoAccessToken: String,
    @field:Schema(description = "가입 유형", example = "CONSUMER", allowableValues = ["CONSUMER", "SELLER"])
    val role: MemberRole,
)

/**
 * 개발 환경에서 카카오 SDK 없이 디버그 사용자 JWT를 발급받기 위한 요청.
 * app.security.dev-endpoints-enabled=true 일 때만 동작합니다.
 */
@Schema(description = "개발용 로그인 요청")
data class DebugLoginRequest(
    @field:Schema(description = "로그인 없이 진입할 디버그 사용자 유형", example = "CONSUMER")
    val role: MemberRole,
    @field:Schema(description = "판매자 디버그 로그인 시 특정 샘플 계정을 선택하기 위한 이메일", example = "bakery@test.com")
    val email: String? = null,
)

@Schema(description = "토큰 갱신 요청")
data class TokenRefreshRequest(
    @field:Schema(description = "리프레시 토큰", example = "refresh-token")
    val refreshToken: String,
)

// ──────────────────── 응답 ────────────────────

@Schema(description = "회원 요약")
data class MemberSummary(
    @field:Schema(description = "회원 ID", example = "1")
    val memberId: Long,
    @field:Schema(description = "닉네임", example = "시흥득템러")
    val nickname: String,
    @field:Schema(description = "회원 역할", example = "CONSUMER")
    val role: String,
)

@Schema(description = "로그인 응답")
data class LoginResponse(
    @field:Schema(description = "JWT 액세스 토큰")
    val accessToken: String,
    @field:Schema(description = "JWT 리프레시 토큰")
    val refreshToken: String,
    val member: MemberSummary,
)

@Schema(description = "토큰 갱신 응답")
data class TokenResponse(
    @field:Schema(description = "새 JWT 액세스 토큰")
    val accessToken: String,
)
