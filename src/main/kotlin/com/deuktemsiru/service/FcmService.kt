package com.deuktemsiru.service

import com.deuktemsiru.entity.FcmToken
import com.deuktemsiru.repository.FcmTokenRepository
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FcmService(
    private val fcmTokenRepository: FcmTokenRepository,
    private val memberService: MemberService,
    private val firebaseMessaging: ObjectProvider<FirebaseMessaging>,
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
        val sender = firebaseMessaging.ifAvailable ?: return 0
        val member = memberService.findMember(memberId)
        val tokens = fcmTokenRepository.findByMemberAndIsActiveTrue(member).map { it.token }
        if (tokens.isEmpty()) return 0

        val message = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .build()
        return runCatching { sender.sendEachForMulticast(message).successCount }
            .onFailure { log.error("FCM 발송에 실패했습니다. memberId={}", memberId, it) }
            .getOrDefault(0)
    }
}
