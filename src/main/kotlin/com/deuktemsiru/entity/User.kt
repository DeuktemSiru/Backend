package com.deuktemsiru.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    var email: String,

    @Column(nullable = false)
    var nickname: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.BUYER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var grade: UserGrade = UserGrade.SPROUT,

    var totalSavings: Int = 0,
    var points: Int = 0,
    var couponCount: Int = 0,
    var co2Saved: Float = 0f,
)

enum class UserRole { BUYER, SELLER }

enum class UserGrade { SPROUT, TREE, FOREST }
