package com.deuktemsiru.dto

import com.deuktemsiru.entity.Notification
import com.deuktemsiru.entity.NotificationType
import java.time.LocalDateTime

data class NotificationResponse(
    val notificationId: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val relatedStoreId: Long?,
    val relatedOrderId: Long?,
    val relatedProductId: Long?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(notification: Notification) = NotificationResponse(
            notificationId = notification.notificationId,
            type = notification.type,
            title = notification.title,
            body = notification.body,
            isRead = notification.isRead,
            relatedStoreId = notification.relatedStoreId,
            relatedOrderId = notification.relatedOrderId,
            relatedProductId = notification.relatedProductId,
            createdAt = notification.createdAt,
        )
    }
}
