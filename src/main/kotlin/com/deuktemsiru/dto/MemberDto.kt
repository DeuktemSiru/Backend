package com.deuktemsiru.dto

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberGender
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.MemberStats
import java.time.LocalDate
import java.time.LocalDateTime

// ── GET /members/me/notification-settings ─────────────────────────────────────
data class NotificationSettingsResponse(
    // 소비자 전용
    val newProduct: Boolean?,
    val pickupReminder: Boolean?,
    val orderConfirmed: Boolean?,
    // 판매자 전용
    val newOrder: Boolean?,
    val pickupComplete: Boolean?,
    val soldOut: Boolean?,
    // 공통
    val event: Boolean,
) {
    companion object {
        fun from(member: Member) = if (member.role == MemberRole.CONSUMER) {
            NotificationSettingsResponse(
                newProduct = member.notifNewProduct,
                pickupReminder = member.notifPickupReminder,
                orderConfirmed = member.notifOrderConfirmed,
                newOrder = null,
                pickupComplete = null,
                soldOut = null,
                event = member.notifEvent,
            )
        } else {
            NotificationSettingsResponse(
                newProduct = null,
                pickupReminder = null,
                orderConfirmed = null,
                newOrder = member.notifNewOrder,
                pickupComplete = member.notifPickupComplete,
                soldOut = member.notifSoldOut,
                event = member.notifEvent,
            )
        }
    }
}

// ── PUT /members/me/notification-settings ─────────────────────────────────────
data class UpdateNotificationSettingsRequest(
    val newProduct: Boolean? = null,
    val pickupReminder: Boolean? = null,
    val orderConfirmed: Boolean? = null,
    val newOrder: Boolean? = null,
    val pickupComplete: Boolean? = null,
    val soldOut: Boolean? = null,
    val event: Boolean? = null,
)

// ── GET /members/me ───────────────────────────────────────────────────────────
data class MemberResponse(
    val memberId: Long,
    val email: String,
    val nickname: String,
    val name: String,
    val role: MemberRole,
    val profileImageUrl: String?,
    val phone: String?,
    val gender: MemberGender?,
    val birth: LocalDate?,
    val status: Int,          // 1=활성, 0=비활성 (ERD tinyint 기준)
    val isSiruLinked: Boolean,
    val siruBalance: Int,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(member: Member) = MemberResponse(
            memberId = member.memberId,
            email = member.email,
            nickname = member.nickname,
            name = member.name,
            role = member.role,
            profileImageUrl = member.profileImageUrl,
            phone = member.phone,
            gender = member.gender,
            birth = member.birth,
            status = if (member.status) 1 else 0,
            isSiruLinked = member.isSiruLinked,
            siruBalance = member.siruBalance,
            createdAt = member.createdAt,
        )
    }
}

// ── GET /members/me/stats ─────────────────────────────────────────────────────
data class MemberStatsResponse(
    val totalSavedAmount: Int,
    val totalCarbonSavedKg: Double,
    val totalOrders: Int,
) {
    companion object {
        fun from(stats: MemberStats) = MemberStatsResponse(
            totalSavedAmount = stats.totalSavedAmount,
            totalCarbonSavedKg = stats.totalCarbonSavedKg,
            totalOrders = stats.totalOrders,
        )

        fun empty() = MemberStatsResponse(
            totalSavedAmount = 0,
            totalCarbonSavedKg = 0.0,
            totalOrders = 0,
        )
    }
}
