package com.deuktemsiru.dto

import com.deuktemsiru.entity.Notification
import java.time.LocalDateTime

data class SendNotificationRequest(
    val message: String,
)

data class NotificationResponse(
    val id: Long,
    val storeId: Long,
    val storeName: String,
    val message: String,
    val sentAt: LocalDateTime,
    val recipientCount: Int,
) {
    companion object {
        fun from(notification: Notification) = NotificationResponse(
            id = notification.id,
            storeId = notification.store.id,
            storeName = notification.store.name,
            message = notification.message,
            sentAt = notification.sentAt,
            recipientCount = notification.recipientCount,
        )
    }
}
