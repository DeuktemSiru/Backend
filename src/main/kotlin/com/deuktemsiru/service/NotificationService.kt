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
        val notification = findOwnedNotification(memberId, notificationId)
        notification.isRead = true
    }

    @Transactional
    fun markAllAsRead(memberId: Long) {
        val member = memberService.findMember(memberId)
        notificationRepository.markAllUnreadAsRead(member)
    }

    @Transactional
    fun deleteNotification(memberId: Long, notificationId: Long) {
        val notification = findOwnedNotification(memberId, notificationId)
        notificationRepository.delete(notification)
    }

    private fun findOwnedNotification(memberId: Long, notificationId: Long): Notification =
        notificationRepository.findById(notificationId)
            .orElseThrow { NoSuchElementException("알림을 찾을 수 없습니다.") }
            .also { require(it.member.memberId == memberId) { "접근 권한이 없습니다." } }
}
