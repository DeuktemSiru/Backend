package com.deuktemsiru.security

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.access-token-expiration-seconds:1800}") val accessTokenExpirationSeconds: Long,
    @Value("\${app.jwt.refresh-token-expiration-seconds:1209600}") val refreshTokenExpirationSeconds: Long,
    @Value("\${spring.profiles.active:}") private val activeProfiles: String,
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

    // ponytail: lazy - validateSecret()의 32자 검사가 먼저 돌아야 WeakKeyException 대신 친절한 메시지가 나온다.
    private val key by lazy { Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8)) }

    private enum class TokenType { ACCESS, REFRESH }

    // ──────────────────────── 토큰 생성 ────────────────────────

    fun createAccessToken(member: Member): String =
        buildToken(member, TokenType.ACCESS, accessTokenExpirationSeconds)

    fun createRefreshToken(member: Member): String =
        buildToken(member, TokenType.REFRESH, refreshTokenExpirationSeconds)

    private fun buildToken(member: Member, type: TokenType, expirationSeconds: Long): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(member.memberId.toString())
            .claim("role", member.role.name)
            .claim("type", type.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expirationSeconds)))
            .signWith(key)
            .compact()
    }

    // ──────────────────────── 토큰 검증 ────────────────────────

    /** Authorization 헤더의 Access Token 검증 → JwtUser 반환 */
    fun validate(token: String): JwtUser? {
        val claims = parse(token, TokenType.ACCESS) ?: return null
        val memberId = claims.subject?.toLongOrNull() ?: return null
        val role = claims["role", String::class.java]
            ?.let { runCatching { MemberRole.valueOf(it) }.getOrNull() } ?: return null

        return JwtUser(memberId = memberId, role = role)
    }

    /** Refresh Token 검증 → memberId 반환 */
    fun validateRefreshToken(token: String): Long? =
        parse(token, TokenType.REFRESH)?.subject?.toLongOrNull()

    private fun parse(token: String, expected: TokenType): Claims? {
        val claims = runCatching {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        }.getOrElse { if (it is JwtException || it is IllegalArgumentException) return null else throw it }

        return claims.takeIf { it["type", String::class.java] == expected.name }
    }
}
