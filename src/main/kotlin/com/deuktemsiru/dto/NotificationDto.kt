package com.deuktemsiru.dto

import com.deuktemsiru.entity.Notification
import com.deuktemsiru.entity.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "알림 응답")
data class NotificationResponse(
    @field:Schema(description = "알림 ID", example = "1")
    val notificationId: Long,
    @field:Schema(description = "알림 유형")
    val type: NotificationType,
    @field:Schema(description = "알림 제목", example = "시루 베이커리")
    val title: String,
    @field:Schema(description = "알림 내용", example = "오늘 마감 할인 상품이 추가되었습니다.")
    val body: String,
    @field:Schema(description = "읽음 여부", example = "false")
    val isRead: Boolean,
    @field:Schema(description = "관련 가게 ID", example = "1", nullable = true)
    val relatedStoreId: Long?,
    @field:Schema(description = "관련 주문 ID", example = "1", nullable = true)
    val relatedOrderId: Long?,
    @field:Schema(description = "관련 상품 ID", example = "1", nullable = true)
    val relatedProductId: Long?,
    @field:Schema(description = "알림 생성 시각")
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

@Schema(description = "앱 알림 응답")
data class AppNotificationResponse(
    @field:Schema(description = "알림 ID", example = "1")
    val id: Long,
    @field:Schema(description = "가게 ID", example = "1")
    val storeId: Long,
    @field:Schema(description = "가게명", example = "시루 베이커리")
    val storeName: String,
    @field:Schema(description = "알림 메시지", example = "오늘 마감 할인 상품이 추가되었습니다.")
    val message: String,
    @field:Schema(description = "발송 시각", example = "2026-05-13T18:30:00")
    val sentAt: String,
    @field:Schema(description = "수신자 수", example = "1")
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
