package com.deuktemsiru.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "store_image")
class StoreImage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val storeImageId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @Column(nullable = false, length = 500)
    var imageUrl: String,

    @Column(nullable = false)
    var displayOrder: Int,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
