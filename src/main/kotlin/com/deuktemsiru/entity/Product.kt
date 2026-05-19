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

enum class ProductStatus { AVAILABLE, SOLD_OUT, EXPIRED, DELETED }

val Product.discountRate: Int
    get() = if (originalPrice > 0) ((originalPrice - discountPrice) * 100 / originalPrice).coerceAtLeast(0) else 0

fun Product.requirePurchasableOn(date: LocalDate, quantity: Int? = null) {
    require(status == ProductStatus.AVAILABLE) { "${name}은(는) 구매 불가 상태입니다." }
    require(availableDate == date) { "${name}은(는) 오늘 구매 가능한 상품이 아닙니다." }
    quantity?.let { require(quantityRemaining >= it) { "${name} 재고가 부족합니다." } }
}

fun Product.changeSaleStatus(nextStatus: ProductStatus) {
    require(status != ProductStatus.DELETED) { "삭제된 상품은 상태를 변경할 수 없습니다." }
    require(nextStatus != ProductStatus.DELETED) { "상품 삭제는 삭제 API를 사용해 주세요." }
    require(nextStatus != ProductStatus.AVAILABLE || quantityRemaining > 0) {
        "잔여 수량이 없는 상품은 먼저 수량을 수정해 주세요."
    }
    status = nextStatus
    if (nextStatus == ProductStatus.SOLD_OUT) quantityRemaining = 0
}
