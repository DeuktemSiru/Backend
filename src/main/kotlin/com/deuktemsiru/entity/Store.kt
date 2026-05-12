package com.deuktemsiru.entity

import jakarta.persistence.*

@Entity
@Table(name = "stores")
class Store(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: StoreCategory,

    var emoji: String = "",
    var rating: Float = 0f,

    @Column(nullable = false)
    var address: String,

    var phone: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,

    // HH:mm 형식 (예: "18:30")
    var closingTime: String = "21:00",

    @OneToMany(mappedBy = "store", cascade = [CascadeType.ALL], orphanRemoval = true)
    val menuItems: MutableList<MenuItem> = mutableListOf(),
)

enum class StoreCategory { BAKERY, LUNCHBOX, SALAD, CAFE }
