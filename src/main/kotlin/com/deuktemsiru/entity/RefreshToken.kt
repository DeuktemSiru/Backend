package com.deuktemsiru.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_token")
class RefreshToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val refreshTokenId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @Column(nullable = false, length = 500)
    var token: String,

    @Column(nullable = false)
    var expiredAt: LocalDateTime,

    @Column(nullable = false)
    var isRevoked: Boolean = false,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
