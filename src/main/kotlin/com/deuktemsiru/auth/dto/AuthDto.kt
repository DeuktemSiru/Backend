package com.deuktemsiru.auth.dto

import com.deuktemsiru.entity.MemberRole

// ──────────────────── 요청 ────────────────────

data class KakaoLoginRequest(
    /** 카카오 SDK에서 발급받은 액세스 토큰 */
    val kakaoAccessToken: String,
    /** 가입 유형: CONSUMER(구매자) / SELLER(판매자) */
    val role: MemberRole,
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
