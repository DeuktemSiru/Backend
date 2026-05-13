package com.deuktemsiru.auth.service

import com.deuktemsiru.auth.dto.KakaoLoginRequest
import com.deuktemsiru.auth.dto.LoginResponse
import com.deuktemsiru.auth.dto.MemberSummary
import com.deuktemsiru.auth.dto.TokenRefreshRequest
import com.deuktemsiru.auth.dto.TokenResponse
import com.deuktemsiru.common.UnauthorizedException
import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberProvider
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.RefreshToken
import com.deuktemsiru.repository.FcmTokenRepository
import com.deuktemsiru.repository.MemberRepository
import com.deuktemsiru.repository.RefreshTokenRepository
import com.deuktemsiru.security.JwtService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AuthService(
    private val kakaoAuthClient: KakaoAuthClient,
    private val memberRepository: MemberRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val fcmTokenRepository: FcmTokenRepository,
    private val jwtService: JwtService,
) {

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

        val isNewMember = !memberRepository.findByProviderAndProviderId(MemberProvider.KAKAO, providerId).isPresent

        val member = memberRepository.findByProviderAndProviderId(MemberProvider.KAKAO, providerId)
            .orElseGet {
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

        val accessToken = jwtService.createAccessToken(member)
        val refreshToken = jwtService.createRefreshToken(member)

        saveRefreshToken(member, refreshToken)

        return Pair(buildLoginResponse(member, accessToken, refreshToken), isNewMember)
    }

    /**
     * 로컬 개발용 로그인. 실제 카카오 토큰 검증 없이 샘플 사용자에게 JWT를 발급합니다.
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

        val member = memberRepository.findByProviderAndProviderId(MemberProvider.KAKAO, debugUser.providerId)
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

        val accessToken = jwtService.createAccessToken(member)
        val refreshToken = jwtService.createRefreshToken(member)
        saveRefreshToken(member, refreshToken)

        return buildLoginResponse(member, accessToken, refreshToken)
    }

    /**
     * Refresh Token으로 Access Token 갱신.
     * DB 조회 + JWT 서명·만료·타입 이중 검증.
     */
    @Transactional
    fun refresh(req: TokenRefreshRequest): TokenResponse {
        // 1) JWT 서명 및 타입 검증
        jwtService.validateRefreshToken(req.refreshToken)
            ?: throw UnauthorizedException("유효하지 않은 리프레시 토큰입니다.")

        // 2) DB에서 미폐기 토큰 조회
        val storedToken = refreshTokenRepository.findByTokenAndIsRevokedFalse(req.refreshToken)
            .orElseThrow { UnauthorizedException("이미 로그아웃된 리프레시 토큰입니다.") }

        // 3) DB 만료 시각 재확인
        if (storedToken.expiredAt.isBefore(LocalDateTime.now())) {
            storedToken.isRevoked = true
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
            .orElseThrow { NoSuchElementException("사용자를 찾을 수 없습니다.") }
        refreshTokenRepository.revokeAllByMember(member)
        fcmTokenRepository.deactivateAllByMember(member)
    }

    // ──────────────────────── 내부 유틸 ────────────────────────

    private fun saveRefreshToken(member: Member, token: String) {
        val expiredAt = LocalDateTime.now()
            .plusSeconds(jwtService.refreshTokenExpirationSeconds)
        refreshTokenRepository.save(
            RefreshToken(member = member, token = token, expiredAt = expiredAt)
        )
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
}
