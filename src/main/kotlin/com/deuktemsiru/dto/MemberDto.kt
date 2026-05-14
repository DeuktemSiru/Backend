package com.deuktemsiru.dto

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberGender
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.MemberStats
import java.time.LocalDate
import java.time.LocalDateTime

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
    val status: Boolean,
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
            status = member.status,
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
