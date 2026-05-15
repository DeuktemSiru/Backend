package com.deuktemsiru.auth.service

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

/**
 * 카카오 REST API 클라이언트.
 * 앱에서 전달받은 카카오 액세스 토큰으로 사용자 정보를 조회합니다.
 */
@Component
class KakaoAuthClient {

    private val restClient = RestClient.create()

    fun getUserInfo(kakaoAccessToken: String): KakaoUserInfo {
        val response: Map<*, *> = try {
            @Suppress("UNCHECKED_CAST")
            restClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer $kakaoAccessToken")
                .retrieve()
                .body(Map::class.java) as? Map<*, *>
                ?: throw IllegalArgumentException("카카오 사용자 정보를 가져올 수 없습니다.")
        } catch (e: RestClientResponseException) {
            throw IllegalArgumentException("유효하지 않은 카카오 액세스 토큰입니다.")
        }

        // 카카오 응답은 snake_case → 직접 파싱
        val id = when (val raw = response["id"]) {
            is Long -> raw
            is Int -> raw.toLong()
            is Double -> raw.toLong()
            else -> throw IllegalArgumentException("카카오 사용자 ID를 파싱할 수 없습니다.")
        }

        val kakaoAccount = response["kakao_account"] as? Map<*, *>
        val profile = kakaoAccount?.get("profile") as? Map<*, *>

        return KakaoUserInfo(
            id = id,
            email = kakaoAccount?.get("email") as? String,
            nickname = profile?.get("nickname") as? String,
            profileImageUrl = profile?.get("profile_image_url") as? String,
        )
    }
}
