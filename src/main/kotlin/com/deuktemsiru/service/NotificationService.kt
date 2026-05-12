package com.deuktemsiru.service

import com.deuktemsiru.dto.NotificationResponse
import com.deuktemsiru.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val memberService: MemberService,
) {

    fun getNotifications(memberId: Long): List<NotificationResponse> {
        val member = memberService.findMember(memberId)
        return notificationRepository.findByMemberOrderByCreatedAtDesc(member)
            .map { NotificationResponse.from(it) }
    }

    @Transactional
    fun markAsRead(memberId: Long, notificationId: Long) {
        val member = memberService.findMember(memberId)
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { NoSuchElementException("알림을 찾을 수 없습니다.") }
        require(notification.member.memberId == member.memberId) { "접근 권한이 없습니다." }
        notification.isRead = true
    }
}
