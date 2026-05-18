package com.deuktemsiru.repository

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByMemberOrderByCreatedAtDesc(member: Member): List<Notification>
    fun findByMemberAndIsReadFalseOrderByCreatedAtDesc(member: Member): List<Notification>
    fun findByRelatedStoreIdAndTitleOrderByCreatedAtDesc(relatedStoreId: Long, title: String): List<Notification>

    @Modifying
    @Query("update Notification n set n.isRead = true where n.member = :member and n.isRead = false")
    fun markAllUnreadAsRead(member: Member): Int
}
