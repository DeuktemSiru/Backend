package com.deuktemsiru.auth.dto

import com.deuktemsiru.entity.MemberRole

// ──────────────────── 요청 ────────────────────

data class KakaoLoginRequest(
    /** 카카오 SDK에서 발급받은 액세스 토큰 */
    val kakaoAccessToken: String,
    /** 가입 유형: CONSUMER(구매자) / SELLER(판매자) */
    val role: MemberRole,
)

/**
 * 개발 환경에서 카카오 SDK 없이 디버그 사용자 JWT를 발급받기 위한 요청.
 * app.security.dev-endpoints-enabled=true 일 때만 동작합니다.
 */
data class DebugLoginRequest(
    /** 로그인 없이 진입할 디버그 사용자 유형 */
    val role: MemberRole,
    /** 판매자 디버그 로그인 시 특정 샘플 계정을 선택하기 위한 이메일 */
    val email: String? = null,
)

data class TokenRefreshRequest(
    val refreshToken: String,
)

// ──────────────────── 응답 ────────────────────

data class MemberSummary(
    val memberId: Long,
    val nickname: String,
    val role: String,
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val member: MemberSummary,
)

data class TokenResponse(
    val accessToken: String,
)
