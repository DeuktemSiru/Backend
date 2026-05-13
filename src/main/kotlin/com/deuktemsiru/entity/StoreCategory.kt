package com.deuktemsiru.entity

import jakarta.persistence.*
import io.swagger.v3.oas.annotations.media.Schema

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

@Schema(description = "가게 카테고리", allowableValues = ["BAKERY", "RESTAURANT", "CAFE", "GROCERY", "OTHER"])
enum class CategoryType { BAKERY, RESTAURANT, CAFE, GROCERY, OTHER }
