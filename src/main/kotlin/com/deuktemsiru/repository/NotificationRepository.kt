package com.deuktemsiru.repository

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByMemberOrderByCreatedAtDesc(member: Member): List<Notification>
    fun findByMemberAndIsReadFalseOrderByCreatedAtDesc(member: Member): List<Notification>
    fun findByRelatedStoreIdAndTitleOrderByCreatedAtDesc(relatedStoreId: Long, title: String): List<Notification>
}
