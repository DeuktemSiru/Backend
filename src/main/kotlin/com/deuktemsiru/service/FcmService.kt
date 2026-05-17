package com.deuktemsiru.service

import com.deuktemsiru.entity.FcmToken
import com.deuktemsiru.repository.FcmTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FcmService(
    private val fcmTokenRepository: FcmTokenRepository,
    private val memberService: MemberService,
) {
    private val log = LoggerFactory.getLogger(FcmService::class.java)

    @Transactional
    fun registerToken(memberId: Long, token: String, deviceInfo: String?) {
        require(token.isNotBlank()) { "FCM 토큰을 입력해 주세요." }
        require(token.length <= 500) { "FCM 토큰은 최대 500자입니다." }
        deviceInfo?.let { require(it.length <= 100) { "기기 정보는 최대 100자입니다." } }

        val member = memberService.findMember(memberId)
        val existing = fcmTokenRepository.findByToken(token).orElse(null)
        val saved = if (existing == null) {
            FcmToken(member = member, token = token)
        } else {
            // B4: 토큰이 다른 회원에게 속해 있으면 기존 레코드를 삭제하여 orphan 방지
            if (existing.member.memberId != member.memberId) {
                fcmTokenRepository.delete(existing)
                FcmToken(member = member, token = token)
            } else {
                existing
            }
        }
        saved.member = member
        saved.deviceInfo = deviceInfo
        saved.isActive = true
        fcmTokenRepository.save(saved)
    }

    fun sendToMember(memberId: Long, title: String, body: String): Int {
        val member = memberService.findMember(memberId)
        val activeTokens = fcmTokenRepository.findByMemberAndIsActiveTrue(member)
        // Firebase Admin SDK 설정이 없는 로컬/과제 환경에서는 DB 알림을 실제 발송원으로 삼고,
        // 활성 토큰 개수를 반환해 운영 연동 지점을 명확히 남긴다.
        return activeTokens.size.also {
            if (it > 0) {
                log.info("FCM stub: {} token(s), title={}, body={}", it, title, body)
            }
        }
    }
}
