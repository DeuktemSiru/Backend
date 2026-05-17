package com.deuktemsiru.auth.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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

    // B9: raw Map 캐스팅 대신 데이터 클래스로 타입 안전하게 역직렬화
    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KakaoMeResponse(
        val id: Long,
        @JsonProperty("kakao_account") val kakaoAccount: KakaoAccount?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KakaoAccount(
        val email: String?,
        val profile: KakaoProfile?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KakaoProfile(
        val nickname: String?,
        @JsonProperty("profile_image_url") val profileImageUrl: String?,
    )

    private val objectMapper = ObjectMapper().registerKotlinModule()

    fun getUserInfo(kakaoAccessToken: String): KakaoUserInfo {
        val json: String = try {
            restClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer $kakaoAccessToken")
                .retrieve()
                .body(String::class.java)
                ?: throw IllegalArgumentException("카카오 사용자 정보를 가져올 수 없습니다.")
        } catch (e: RestClientResponseException) {
            throw IllegalArgumentException("유효하지 않은 카카오 액세스 토큰입니다.")
        }

        val response = runCatching { objectMapper.readValue(json, KakaoMeResponse::class.java) }
            .getOrElse { throw IllegalArgumentException("카카오 사용자 정보를 파싱할 수 없습니다.") }

        return KakaoUserInfo(
            id = response.id,
            email = response.kakaoAccount?.email,
            nickname = response.kakaoAccount?.profile?.nickname,
            profileImageUrl = response.kakaoAccount?.profile?.profileImageUrl,
        )
    }
}
