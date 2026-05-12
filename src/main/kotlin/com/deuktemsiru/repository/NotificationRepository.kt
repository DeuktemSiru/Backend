package com.deuktemsiru.repository

import com.deuktemsiru.entity.Notification
import com.deuktemsiru.entity.Store
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByStoreOrderBySentAtDesc(store: Store): List<Notification>
    fun findByStoreInOrderBySentAtDesc(stores: Collection<Store>): List<Notification>
}
