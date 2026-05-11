package com.deuktemsiru.dto

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberRole

data class MemberResponse(
    val memberId: Long,
    val email: String,
    val nickname: String,
    val name: String,
    val role: MemberRole,
    val profileImageUrl: String?,
    val phone: String?,
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
    companion object {
        fun from(member: Member) = UserApiResponse(
            id = member.memberId,
            nickname = member.nickname,
            role = member.role.name,
            grade = "WELCOME",
            totalSavings = 0,
            points = 0,
            couponCount = 0,
            co2Saved = 0f,
        )
    }
}
