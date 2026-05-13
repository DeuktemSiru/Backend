package com.deuktemsiru.dto

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberGender
import com.deuktemsiru.entity.MemberRole
import java.time.LocalDate
import java.time.LocalDateTime

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

data class UserApiResponse(
    val id: Long,
    val nickname: String,
    val role: String,
    val grade: String,
    val totalSavings: Int,
    val points: Int,
    val couponCount: Int,
    val co2Saved: Float,
) {
  