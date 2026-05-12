package com.deuktemsiru.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "payment")
class Payment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val paymentId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Orders,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var method: PaymentMethod,

    @Column(length = 100)
    var externalTransactionId: String? = null,

    @Column(nullable = false)
    var amount: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.PENDING,

    var paidAt: LocalDateTime? = null,
)

enum class PaymentMethod { SIRU, CARD, CASH }
enum class PaymentStatus { PENDING, COMPLETED, FAILED, REFUNDED }
