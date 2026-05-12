package com.deuktemsiru.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "product")
class Product(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val productId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    var menuItem: MenuItem? = null,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(length = 500)
    var thumbnailUrl: String? = null,

    @Column(nullable = false)
    var originalPrice: Int,

    @Column(nullable = false)
    var discountPrice: Int,

    @Column(nullable = false)
    var quantityTotal: Int,

    @Column(nullable = false)
    var quantityRemaining: Int,

    @Column(length = 500)
    var allergenInfo: String? = null,

    @Column(length = 50)
    var madeAt: String? = null,

    @Column(nullable = false)
    var pickupStart: LocalTime,

    @Column(nullable = false)
    var pickupEnd: LocalTime,

    @Column(nullable = false)
    var availableDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ProductStatus = ProductStatus.AVAILABLE,

    @Column(nullable = false)
    var carbonSavedKg: Double = 0.0,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    val images: MutableList<ProductImage> = mutableListOf(),
)

enum class ProductStatus { AVAILABLE, SOLD_OUT, EXPIRED }
