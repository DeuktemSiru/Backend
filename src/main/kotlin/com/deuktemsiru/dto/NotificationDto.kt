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

data class AppNotificationResponse(
    val id: Long,
    val storeId: Long,
    val storeName: String,
    val message: String,
    val sentAt: String,
    val recipientCount: Int,
) {
    companion object {
        fun from(notification: Notification) = AppNotificationResponse(
            id = notification.notificationId,
            storeId = notification.relatedStoreId ?: 0,
            storeName = notification.title,
            message = notification.body,
            sentAt = notification.createdAt.toString(),
            recipientCount = 1,
        )
    }
}
