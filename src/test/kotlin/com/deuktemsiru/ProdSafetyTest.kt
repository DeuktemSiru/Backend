package com.deuktemsiru

import com.deuktemsiru.controller.admin.adminTokenMatches
import com.deuktemsiru.security.JwtService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Profile
import java.util.Properties

/**
 * prod 프로파일 안전장치가 풀리면 실패하는 테스트. (PLAN P0-3)
 * 컨텍스트를 띄우지 않고 기동 시 검증 로직과 prod 설정값만 직접 확인한다.
 */
class ProdSafetyTest {

    private val devDefaultSecret = "deuktemsiru-dev-secret-key-must-be-changed-in-production"

    private fun jwtService(secret: String, activeProfiles: String) =
        JwtService(secret, 1800, 1209600, activeProfiles)

    @Test
    fun `prod는 기본 개발용 JWT 시크릿이면 기동에 실패한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            jwtService(devDefaultSecret, "prod").validateSecret()
        }
        assertThrows(IllegalArgumentException::class.java) {
            jwtService(devDefaultSecret, "prod,monitoring").validateSecret()
        }

        // dev에서는 경고만 남기고 기동한다.
        jwtService(devDefaultSecret, "dev").validateSecret()
    }

    @Test
    fun `32자 미만 JWT 시크릿은 프로파일과 무관하게 거절된다`() {
        assertThrows(IllegalArgumentException::class.java) {
            jwtService("short-secret", "prod").validateSecret()
        }
        assertThrows(IllegalArgumentException::class.java) {
            jwtService("short-secret", "dev").validateSecret()
        }
    }

    @Test
    fun `prod 설정은 debug 로그인을 막고 Flyway를 켠 채로 둔다`() {
        val prod = loadProperties("application-prod.properties")

        assertEquals("false", prod.getProperty("app.security.dev-endpoints-enabled"))
        assertEquals("\${SPRING_FLYWAY_ENABLED:true}", prod.getProperty("spring.flyway.enabled"))
        assertEquals("\${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}", prod.getProperty("spring.jpa.hibernate.ddl-auto"))
        assertEquals("false", prod.getProperty("spring.h2.console.enabled"))
    }

    @Test
    fun `샘플 데이터 생성기는 prod에서 동작하지 않는다`() {
        val profile = DataInitializer::class.java.getAnnotation(Profile::class.java)
        assertEquals(listOf("!prod"), profile.value.toList())
    }

    @Test
    fun `운영자 토큰이 없거나 짧으면 운영자 API는 열리지 않는다`() {
        val strong = "a".repeat(32)

        assertFalse(adminTokenMatches("", null))
        assertFalse(adminTokenMatches("", ""))
        assertFalse(adminTokenMatches("short-admin-token", "short-admin-token"))
        assertFalse(adminTokenMatches(strong, null))
        assertFalse(adminTokenMatches(strong, "a".repeat(31)))
        assertTrue(adminTokenMatches(strong, strong))
    }

    private fun loadProperties(name: String) = Properties().apply {
        checkNotNull(ProdSafetyTest::class.java.classLoader.getResourceAsStream(name)) { "$name 을(를) 찾을 수 없습니다." }
            .use { load(it) }
    }
}
