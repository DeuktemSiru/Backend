package com.deuktemsiru.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "settlement")
class Settlement(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val settlementId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var paymentMethod: PaymentMethod,

    @Column(nullable = false)
    var periodStart: LocalDate,

    @Column(nullable = false)
    var periodEnd: LocalDate,

    @Column(nullable = false)
    var totalAmount: Int,

    @Column(nullable = false)
    var feeAmount: Int,

    @Column(nullable = false)
    var netAmount: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SettlementStatus = SettlementStatus.PENDING,

    var settledAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

enum class SettlementStatus { PENDING, COMPLETED }
