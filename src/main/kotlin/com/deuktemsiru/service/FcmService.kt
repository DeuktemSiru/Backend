package com.deuktemsiru.service

import com.deuktemsiru.entity.FcmToken
import com.deuktemsiru.repository.FcmTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FcmService(
    private val fcmTokenRepository: FcmTokenRepository,
    private val memberService: MemberService,
) {

    @Transactional
    fun registerToken(memberId: Long, token: String, deviceInfo: String?) {
        require(token.isNotBlank()) { "FCM 토큰을 입력해 주세요." }
        require(token.length <= 500) { "FCM 토큰은 최대 500자입니다." }
        deviceInfo?.let { require(it.length <= 100) { "기기 정보는 최대 100자입니다." } }

        val member = memberService.findMember(memberId)
        val saved = fcmTokenRepository.findByToken(token).orElseGet {
            FcmToken(member = member, token = token)
        }
        saved.member = member
        saved.deviceInfo = deviceInfo
        saved.isActive = true
        fcmTokenRepository.save(saved)
    }
}
