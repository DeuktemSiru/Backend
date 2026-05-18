package com.deuktemsiru.auth.service

import com.deuktemsiru.auth.dto.KakaoLoginRequest
import com.deuktemsiru.auth.dto.LoginResponse
import com.deuktemsiru.auth.dto.MemberSummary
import com.deuktemsiru.auth.dto.TokenRefreshRequest
import com.deuktemsiru.auth.dto.TokenResponse
import com.deuktemsiru.common.UnauthorizedException
import com.deuktemsiru.common.nowDateTime
import com.deuktemsiru.common.orNotFound
import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberProvider
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.RefreshToken
import com.deuktemsiru.repository.FcmTokenRepository
import com.deuktemsiru.repository.MemberRepository
import com.deuktemsiru.repository.RefreshTokenRepository
import com.deuktemsiru.security.JwtService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Base64

@Service
@Transactional(readOnly = true)
class AuthService(
    private val kakaoAuthClient: KakaoAuthClient,
    private val memberRepository: MemberRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val fcmTokenRepository: FcmTokenRepository,
    private val jwtService: JwtService,
    private val clock: Clock,
) {
    // 자기 참조: REQUIRES_NEW 트랜잭션 메서드를 프록시를 통해 호출하기 위해 필요
    @Lazy @Autowired private lateinit var self: AuthService

    /**
     * 카카오 소셜 로그인 / 자동 회원가입.
     * 반환값: (LoginResponse, isNewMember) — isNewMember=true면 201, false면 200
     */
    @Transactional
    fun kakaoLogin(req: KakaoLoginRequest): Pair<LoginResponse, Boolean> {
        val userInfo = kakaoAuthClient.getUserInfo(req.kakaoAccessToken)

        val providerId = userInfo.id.toString()
        val email = userInfo.email ?: "$providerId@kakao.local"
        val nickname = userInfo.nickname ?: "카카오사용자"

        // findByProviderAndProviderId is called once; result is reused for both isNewMember and member lookup
        val existing = memberRepository.findByProviderAndProviderId(MemberProvider.KAKAO, providerId)
        val isNewMember = !existing.isPresent
        val member = existing.orElseGet {
            memberRepository.save(
                Member(
                    provider = MemberProvider.KAKAO,
                    providerId = providerId,
                    email = email,
                    name = nickname,
                    nickname = nickname,
                    profileImageUrl = userInfo.profileImageUrl,
                    role = req.role,
                )
            )
        }

        // 프로필 정보 최신화 (닉네임·이미지는 카카오에서 변경될 수 있음)
        member.nickname = nickname
        member.profileImageUrl = userInfo.profileImageUrl

        return Pair(issueTokens(member), isNewMember)
    }

    /**
     * 로컬 개발용 로그인. 실제 카카오 토큰 검증 없이 샘플 사용자에게 JWT를 발급합니다.
     * 운영 환경에서는 SecurityConfig + dev-endpoints-enabled 플래그로 차단되어야 합니다.
     */
    @Transactional
    fun debugLogin(role: MemberRole): LoginResponse {
        val debugUser = when (role) {
            MemberRole.CONSUMER -> DebugUser(
                providerId = "kakao_buyer_1",
                email = "buyer@test.com",
                name = "홍길동",
                nickname = "득템러",
                role = MemberRole.CONSUMER,
            )
            MemberRole.SELLER -> DebugUser(
                providerId = "kakao_seller_1",
                email = "bakery@test.com",
                name = "영희",
                nickname = "영희네베이커리",
                role = MemberRole.SELLER,
            )
        }

        val member = findOrCreateDebugMember(debugUser)

        return issueTokens(member)
    }

    @Transactional
    fun debugLogin(role: MemberRole, email: String?): LoginResponse {
        val normalizedEmail = email?.trim()?.takeIf { it.isNotBlank() }
        if (role != MemberRole.SELLER || normalizedEmail == null) {
            return debugLogin(role)
        }

        val seller = memberRepository.findByEmail(normalizedEmail)
            .filter { it.role == MemberRole.SELLER }
            .orElseThrow { NoSuchElementException("판매자 디버그 계정을 찾을 수 없습니다.") }

        return issueTokens(seller)
    }

    /**
     * Refresh Token으로 Access Token 갱신.
     * DB 조회 + JWT 서명·만료·타입 이중 검증.
     * DB에는 토큰의 SHA-256 해시만 저장되므로 조회 전에 해싱합니다.
     */
    @Transactional
    fun refresh(req: TokenRefreshRequest): TokenResponse {
        // 1) JWT 서명 및 타입 검증
        jwtService.validateRefreshToken(req.refreshToken)
            ?: throw UnauthorizedException("유효하지 않은 리프레시 토큰입니다.")

        // 2) DB에서 미폐기 토큰 조회 (해시 기반)
        val storedToken = refreshTokenRepository.findByTokenAndIsRevokedFalse(hashToken(req.refreshToken))
            .orElseThrow { UnauthorizedException("이미 로그아웃된 리프레시 토큰입니다.") }

        // 3) DB 만료 시각 재확인
        if (storedToken.expiredAt.isBefore(now())) {
            // REQUIRES_NEW로 별도 커밋 — 외부 트랜잭션 롤백과 무관하게 폐기 상태를 저장
            self.revokeExpiredToken(storedToken.refreshTokenId)
            throw UnauthorizedException("만료된 리프레시 토큰입니다.")
        }

        val newAccessToken = jwtService.createAccessToken(storedToken.member)
        return TokenResponse(accessToken = newAccessToken)
    }

    /**
     * 로그아웃.
     * 해당 회원의 모든 Refresh Token 폐기 + FCM Token 비활성화.
     */
    @Transactional
    fun logout(memberId: Long) {
        val member = memberRepository.findById(memberId)
            .orNotFound("사용자를 찾을 수 없습니다.")
        refreshTokenRepository.revokeAllByMember(member)
        fcmTokenRepository.deactivateAllByMember(member)
    }

    // ──────────────────────── 내부 유틸 ────────────────────────

    /** 만료 토큰을 별도 트랜잭션으로 폐기. 외부 롤백과 독립적으로 커밋된다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun revokeExpiredToken(tokenId: Long) {
        refreshTokenRepository.revokeById(tokenId)
    }

    /**
     * 디버그 사용자를 DB에서 조회하거나, 없으면 새로 생성합니다.
     * kakaoLogin / debugLogin 양쪽에서 공유하는 member lookup/create 패턴을 하나로 통합합니다.
     */
    private fun findOrCreateDebugMember(debugUser: DebugUser): Member =
        memberRepository.findByProviderAndProviderId(MemberProvider.KAKAO, debugUser.providerId)
            .orElseGet {
                memberRepository.save(
                    Member(
                        provider = MemberProvider.KAKAO,
                        providerId = debugUser.providerId,
                        email = debugUser.email,
                        name = debugUser.name,
                        nickname = debugUser.nickname,
                        role = debugUser.role,
                    )
                )
            }

    private fun saveRefreshToken(member: Member, token: String) {
        val expiredAt = now()
            .plusSeconds(jwtService.refreshTokenExpirationSeconds)
        refreshTokenRepository.save(
            RefreshToken(member = member, token = hashToken(token), expiredAt = expiredAt)
        )
    }

    private fun issueTokens(member: Member): LoginResponse {
        val accessToken = jwtService.createAccessToken(member)
        val refreshToken = jwtService.createRefreshToken(member)
        saveRefreshToken(member, refreshToken)
        return buildLoginResponse(member, accessToken, refreshToken)
    }

    /** 리프레시 토큰을 SHA-256으로 해싱하여 DB에 평문이 저장되지 않도록 합니다. */
    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(token.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }

    private fun buildLoginResponse(member: Member, accessToken: String, refreshToken: String) =
        LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            member = MemberSummary(
                memberId = member.memberId,
                nickname = member.nickname,
                role = member.role.name,
            ),
        )

    private data class DebugUser(
        val providerId: String,
        val email: String,
        val name: String,
        val nickname: String,
        val role: MemberRole,
    )

    private fun now(): LocalDateTime = clock.nowDateTime()
}
