package com.deuktemsiru.security

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JwtServiceTest {

    private val jwt = JwtService(
        secret = "test-secret-key-for-jwt-service-unit-test",
        accessTokenExpirationSeconds = 1800,
        refreshTokenExpirationSeconds = 1209600,
        activeProfiles = "test",
    )

    private val member = Member(
        memberId = 42,
        providerId = "kakao-42",
        email = "a@b.c",
        name = "테스터",
        nickname = "테스터",
        role = MemberRole.SELLER,
    )

    @Test
    fun `access token 왕복`() {
        val user = jwt.validate(jwt.createAccessToken(member))
        assertEquals(42L, user?.memberId)
        assertEquals(MemberRole.SELLER, user?.role)
    }

    @Test
    fun `refresh token 왕복`() {
        assertEquals(42L, jwt.validateRefreshToken(jwt.createRefreshToken(member)))
    }

    @Test
    fun `access와 refresh는 서로 교차 사용할 수 없다`() {
        assertNull(jwt.validate(jwt.createRefreshToken(member)))
        assertNull(jwt.validateRefreshToken(jwt.createAccessToken(member)))
    }

    @Test
    fun `서명이 다르거나 변조된 토큰은 거절된다`() {
        val token = jwt.createAccessToken(member)
        assertNull(jwt.validate(token.dropLast(3) + "AAA"))
        assertNull(jwt.validate("garbage"))

        val other = JwtService("another-secret-key-that-is-long-enough!!", 1800, 1209600, "test")
        assertNull(jwt.validate(other.createAccessToken(member)))
    }

    @Test
    fun `만료된 토큰은 거절된다`() {
        val expired = JwtService("test-secret-key-for-jwt-service-unit-test", -60, -60, "test")
        assertNull(jwt.validate(expired.createAccessToken(member)))
    }
}
