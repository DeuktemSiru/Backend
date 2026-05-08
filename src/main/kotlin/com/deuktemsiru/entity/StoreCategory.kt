package com.deuktemsiru.entity

import jakarta.persistence.*

@Entity
@Table(name = "store_category")
class StoreCategory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val storeCategoryId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var category: CategoryType,
)

enum class CategoryType { BAKERY, RESTAURANT, CAFE, GROCERY, OTHER }
