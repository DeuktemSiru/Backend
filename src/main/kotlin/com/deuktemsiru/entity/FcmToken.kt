package com.deuktemsiru.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "fcm_token")
class FcmToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val fcmTokenId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @Column(nullable = false, length = 500)
    var token: String,

    @Column(length = 100)
    var deviceInfo: String? = null,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
