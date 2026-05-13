package com.deuktemsiru.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "business_info")
class BusinessInfo(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val businessInfoId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @Column(nullable = false, length = 100)
    var businessName: String,

    @Column(nullable = false, length = 20, unique = true)
    var businessNumber: String,

    @Column(nullable = false)
    var isVerified: Boolean = false,

    @Column(length = 100)
    var siruStoreId: String? = null,

    @Column(nullable = false)
    var isSiruVerified: Boolean = false,

    var verifiedAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
