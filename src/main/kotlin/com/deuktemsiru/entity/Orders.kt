package com.deuktemsiru.entity

import jakarta.persistence.*
import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
class Orders(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val orderId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_id", nullable = false)
    var consumer: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(nullable = false)
    var totalPrice: Int = 0,

    @Column(length = 10)
    var pickupCode: String? = null,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<OrderItem> = mutableListOf(),
)

@Schema(description = "주문 상태", allowableValues = ["PENDING", "PREPARING", "READY", "COMPLETED", "CANCELLED"])
enum class OrderStatus { PENDING, PREPARING, READY, COMPLETED, CANCELLED }

@Entity
@Table(name = "order_item")
class OrderItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val orderItemId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Orders,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    var unitPrice: Int,
)
