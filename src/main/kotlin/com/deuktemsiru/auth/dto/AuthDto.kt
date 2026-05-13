package com.deuktemsiru.auth.dto

import com.deuktemsiru.entity.MemberRole
import io.swagger.v3.oas.annotations.media.Schema

// ──────────────────── 요청 ────────────────────

@Schema(description = "카카오 로그인 요청")
data class KakaoLoginRequest(
    @field:Schema(description = "카카오 SDK에서 발급받은 액세스 토큰", example = "kakao-access-token")
    val kakaoAccessToken: String,
    @field:Schema(description = "가입 유형", allowableValues = ["CONSUMER", "SELLER"], example = "CONSUMER")
    val role: MemberRole,
)

@Schema(description = "개발 환경 디버그 로그인 요청")
data class DebugLoginRequest(
    @field:Schema(description = "로그인 없이 진입할 디버그 사용자 유형", allowableValues = ["CONSUMER", "SELLER"], example = "CONSUMER")
    val role: MemberRole,
)

@Schema(description = "액세스 토큰 갱신 요청")
data class TokenRefreshRequest(
    @field:Schema(description = "리프레시 토큰", example = "refresh-token")
    val refreshToken: String,
)

// ──────────────────── 응답 ────────────────────

@Schema(description = "로그인 사용자 요약 정보")
data class MemberSummary(
    @field:Schema(description = "회원 ID", example = "1")
    val memberId: Long,
    @field:Schema(description = "닉네임", example = "득템러")
    val nickname: String,
    @field:Schema(description = "회원 역할", allowableValues = ["CONSUMER", "SELLER"], example = "CONSUMER")
    val role: String,
)

@Schema(description = "로그인 응답")
data class LoginResponse(
    @field:Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,
    @field:Schema(description = "리프레시 토큰", example = "refresh-token")
    val refreshToken: String,
    @field:Schema(description = "로그인한 회원 정보")
    val member: MemberSummary,
)

@Schema(description = "토큰 갱신 응답")
data class TokenResponse(
    @field:Schema(description = "새로 발급된 JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,
)
