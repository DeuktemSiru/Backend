package com.deuktemsiru.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "review",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_review_consumer_order", columnNames = ["consumer_id", "order_id"]),
    ],
)
class Review(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val reviewId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_id", nullable = false)
    var consumer: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Orders,

    @Column(nullable = false)
    var rating: Int,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @Column(nullable = false)
    var isDeleted: Boolean = false,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
