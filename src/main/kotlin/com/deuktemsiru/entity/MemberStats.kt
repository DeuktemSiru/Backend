package com.deuktemsiru.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "member_stats")
class MemberStats(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val statsId: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    var member: Member,

    @Column(nullable = false)
    var totalSavedAmount: Int = 0,

    @Column(nullable = false)
    var totalCarbonSavedKg: Double = 0.0,

    @Column(nullable = false)
    var totalOrders: Int = 0,

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
