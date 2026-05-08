package com.deuktemsiru.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "notification")
class Notification(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val notificationId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @Column(name = "related_store_id")
    var relatedStoreId: Long? = null,

    @Column(name = "related_order_id")
    var relatedOrderId: Long? = null,

    @Column(name = "related_product_id")
    var relatedProductId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var type: NotificationType,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(nullable = false, length = 500)
    var body: String,

    @Column(nullable = false)
    var isRead: Boolean = false,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

enum class NotificationType {
    NEW_PRODUCT, PICKUP_REMINDER, ORDER_CONFIRMED, ORDER_CANCELLED, EVENT
}
