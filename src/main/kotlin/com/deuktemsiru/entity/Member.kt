package com.deuktemsiru.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "member")
class Member(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val memberId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var provider: MemberProvider = MemberProvider.KAKAO,

    @Column(nullable = false, length = 100)
    var providerId: String,

    @Column(nullable = false, length = 100)
    var email: String,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(nullable = false, length = 30)
    var nickname: String,

    @Column(length = 20)
    var phone: String? = null,

    @Column(length = 500)
    var profileImageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var role: MemberRole = MemberRole.CONSUMER,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    var gender: MemberGender? = null,

    var birth: LocalDate? = null,

    @Column(nullable = false)
    var status: Boolean = true,

    var deletedAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

enum class MemberProvider { KAKAO }
enum class MemberRole { CONSUMER, SELLER }
enum class MemberGender { MALE, FEMALE, NONE }
