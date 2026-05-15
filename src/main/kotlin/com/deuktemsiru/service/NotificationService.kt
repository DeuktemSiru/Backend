package com.deuktemsiru.service

import com.deuktemsiru.dto.NotificationResponse
import com.deuktemsiru.entity.Notification
import com.deuktemsiru.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class NotificationListResult(
    val notifications: List<NotificationResponse>,
    val unreadCount: Int,
)

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val memberService: MemberService,
) {

    fun getNotifications(memberId: Long): NotificationListResult {
        val member = memberService.findMember(memberId)
        val list = notificationRepository.findByMemberOrderByCreatedAtDesc(member)
        return NotificationListResult(
            notifications = list.map { NotificationResponse.from(it) },
            unreadCount = list.count { !it.isRead },
        )
    }

    fun getNotificationEntities(memberId: Long): List<Notification> {
        val member = memberService.findMember(memberId)
        return notificationRepository.findByMemberOrderByCreatedAtDesc(member)
    }

    @Transactional
    fun markAsRead(memberId: Long, notificationId: Long) {
        val member = memberService.findMember(memberId)
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { NoSuchElementException("알림을 찾을 수 없습니다.") }
        require(notification.member.memberId == member.memberId) { "접근 권한이 없습니다." }
        notification.isRead = true
    }

    @Transactional
    fun markAllAsRead(memberId: Long) {
        val member = memberService.findMember(memberId)
        notificationRepository.findByMemberOrderByCreatedAtDesc(member)
            .filter { !it.isRead }
            .forEach { it.isRead = true }
    }

    @Transactional
    fun deleteNotification(memberId: Long, notificationId: Long) {
        val member = memberService.findMember(memberId)
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { NoSuchElementException("알림을 찾을 수 없습니다.") }
        require(notification.member.memberId == member.memberId) { "접근 권한이 없습니다." }
        notificationRepository.delete(notification)
    }
}
