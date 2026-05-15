package com.deuktemsiru.auth.service

/** 카카오 API /v2/user/me 응답에서 추출한 사용자 정보 */
data class KakaoUserInfo(
    val id: Long,
    val email: String?,
    val nickname: String?,
    val profileImageUrl: String?,
)
