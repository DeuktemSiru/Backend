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
