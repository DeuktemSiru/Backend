package com.deuktemsiru.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "store")
class Store(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val storeId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: Member,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, length = 255)
    var address: String,

    @Column(nullable = false)
    var latitude: Double,

    @Column(nullable = false)
    var longitude: Double,

    @Column(length = 20)
    var phone: String? = null,

    @Column(length = 5)
    var closingTime: String? = null,

    @Column(length = 500)
    var thumbnailUrl: String? = null,

    @Column(nullable = false)
    var ratingAvg: Double = 0.0,

    @Column(nullable = false)
    var reviewCount: Int = 0,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Column(nullable = false)
    var isVerified: Boolean = false,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "store", cascade = [CascadeType.ALL], orphanRemoval = true)
    val categories: MutableList<StoreCategory> = mutableListOf(),

    @OneToMany(mappedBy = "store", cascade = [CascadeType.ALL], orphanRemoval = true)
    val images: MutableList<StoreImage> = mutableListOf(),

    @OneToMany(mappedBy = "store", cascade = [CascadeType.ALL], orphanRemoval = true)
    val menuItems: MutableList<MenuItem> = mutableListOf(),

    @OneToMany(mappedBy = "store", cascade = [CascadeType.ALL], orphanRemoval = true)
    val products: MutableList<Product> = mutableListOf(),
)
