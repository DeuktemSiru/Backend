package com.deuktemsiru.entity

import jakarta.persistence.*

@Entity
@Table(name = "menu_items")
class MenuItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @Column(nullable = false)
    var name: String,

    var emoji: String = "",
    var imageUrl: String? = null,
    var originalPrice: Int = 0,
    var discountedPrice: Int = 0,
    var discountRate: Int = 0,
    var remainingItems: Int = 0,
    var isSoldOut: Boolean = false,

    // 픽업 가능 시간대 (예: "17:00-18:30")
    var pickupTimeSlot: String = "",
)
