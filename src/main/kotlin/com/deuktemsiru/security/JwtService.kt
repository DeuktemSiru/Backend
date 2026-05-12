package com.deuktemsiru.security

import com.deuktemsiru.entity.User
import com.deuktemsiru.entity.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class JwtUser(
    val userId: Long,
    val role: UserRole,
)

@Service
class JwtService(
    @Value("\${app.jwt.secret:deuktemsiru-dev-secret-change-me}") private val secret: String,
    @Value("\${app.jwt.expiration-seconds:86400}") private val expirationSeconds: Long,
) {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun createToken(user: User): String {
        val now = Instant.now().epochSecond
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val payload = """{"sub":"${user.id}","role":"${user.role}","iat":$now,"exp":${now + expirationSeconds}}"""
        val unsignedToken = "${encode(header)}.${encode(payload)}"
        return "$unsignedToken.${sign(unsignedToken)}"
    }

    fun validate(token: String): JwtUser? {
        val parts = token.split(".")
        if (parts.size != 3) return null

        val unsignedToken = "${parts[0]}.${parts[1]}"
        if (sign(unsignedToken) != parts[2]) return null

        val payload = String(decoder.decode(parts[1]), StandardCharsets.UTF_8)
        val userId = extractString(payload, "sub")?.toLongOrNull() ?: return null
        val role = extractString(payload, "role")?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: return null
        val expiresAt = extractNumber(payload, "exp") ?: return null
        if (Instant.now().epochSecond >= expiresAt) return null

        return JwtUser(userId = userId, role = role)
    }

    private fun encode(value: String): String =
        encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun sign(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return encoder.encodeToString(mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun extractString(json: String, key: String): String? {
        val match = Regex(""""$key"\s*:\s*"([^"]+)"""").find(json)
        return match?.groupValues?.get(1)
    }

    private fun extractNumber(json: String, key: String): Long? {
        val match = Regex(""""$key"\s*:\s*(\d+)""").find(json)
        return match?.groupValues?.get(1)?.toLongOrNull()
    }
}
