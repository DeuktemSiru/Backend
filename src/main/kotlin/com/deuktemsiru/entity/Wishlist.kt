package com.deuktemsiru.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "wishlists",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "store_id"])]
)
class Wishlist(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,
)
