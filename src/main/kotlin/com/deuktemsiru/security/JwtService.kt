package com.deuktemsiru.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberRole
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.access-token-expiration-seconds:1800}") val accessTokenExpirationSeconds: Long,
    @Value("\${app.jwt.refresh-token-expiration-seconds:1209600}") val refreshTokenExpirationSeconds: Long,
    @Value("\${spring.profiles.active:}") private val activeProfiles: String,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(JwtService::class.java)

    @PostConstruct
    fun validateSecret() {
        val devDefault = "deuktemsiru-dev-secret-key-must-be-changed-in-production"
        if (secret == devDefault) {
            require(!activeProfiles.split(",").map { it.trim() }.contains("prod")) {
                "운영 환경에서는 APP_JWT_SECRET 환경변수를 반드시 교체해야 합니다."
            }
            log.warn("⚠️  JWT secret이 기본 개발용 값입니다. 운영 환경에서는 APP_JWT_SECRET 환경변수를 반드시 교체하세요!")
        }
        require(secret.length >= 32) { "JWT secret은 최소 32자 이상이어야 합니다. (현재: ${secret.length}자)" }
    }
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    private enum class TokenType { ACCESS, REFRESH }
    private val payloadType = object : TypeReference<Map<String, Any?>>() {}

    // ──────────────────────── 토큰 생성 ────────────────────────

    fun createAccessToken(member: Member): String =
        buildToken(member, TokenType.ACCESS, accessTokenExpirationSeconds)

    fun createRefreshToken(member: Member): String =
        buildToken(member, TokenType.REFRESH, refreshTokenExpirationSeconds)

    private fun buildToken(member: Member, type: TokenType, expirationSeconds: Long): String {
        val now = Instant.now().epochSecond
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val payload =
            """{"sub":"${member.memberId}","role":"${member.role}","type":"$type","iat":$now,"exp":${now + expirationSeconds}}"""
        val unsigned = "${encode(header)}.${encode(payload)}"
        return "$unsigned.${sign(unsigned)}"
    }

    // ──────────────────────── 토큰 검증 ────────────────────────

    /** Authorization 헤더의 Access Token 검증 → JwtUser 반환 */
    fun validate(token: String): JwtUser? {
        val payload = verifySignatureAndExpiry(token) ?: return null
        if (payload["type"] as? String != TokenType.ACCESS.name) return null

        val memberId = (payload["sub"] as? String)?.toLongOrNull() ?: return null
        val role = (payload["role"] as? String)
            ?.let { runCatching { MemberRole.valueOf(it) }.getOrNull() } ?: return null

        return JwtUser(memberId = memberId, role = role)
    }

    /** Refresh Token 검증 → memberId 반환 */
    fun validateRefreshToken(token: String): Long? {
        val payload = verifySignatureAndExpiry(token) ?: return null
        if (payload["type"] as? String != TokenType.REFRESH.name) return null
        return (payload["sub"] as? String)?.toLongOrNull()
    }

    // ──────────────────────── 내부 유틸 ────────────────────────

    private fun verifySignatureAndExpiry(token: String): Map<String, Any?>? {
        val parts = token.split(".")
        if (parts.size != 3) return null

        val unsigned = "${parts[0]}.${parts[1]}"
        if (!MessageDigest.isEqual(
                sign(unsigned).toByteArray(StandardCharsets.UTF_8),
                parts[2].toByteArray(StandardCharsets.UTF_8),
            )
        ) return null

        val payload = runCatching {
            objectMapper.readValue(
                String(decoder.decode(parts[1]), StandardCharsets.UTF_8),
                payloadType,
            )
        }.getOrNull() ?: return null
        val expiresAt = (payload["exp"] as? Number)?.toLong() ?: return null
        if (Instant.now().epochSecond >= expiresAt) return null

        return payload
    }

    private fun encode(value: String): String =
        encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun sign(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return encoder.encodeToString(mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
    }
}
