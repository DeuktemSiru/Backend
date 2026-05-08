package com.deuktemsiru.entity

import jakarta.persistence.*

@Entity
@Table(name = "product_image")
class ProductImage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val productImageId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(nullable = false, length = 500)
    var imageUrl: String,

    @Column(nullable = false)
    var displayOrder: Int,
)
