package com.deuktemsiru.dto

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberRole
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원 응답")
data class MemberResponse(
    @field:Schema(description = "회원 ID", example = "1")
    val memberId: Long,
    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String,
    @field:Schema(description = "닉네임", example = "득템러")
    val nickname: String,
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "회원 역할", allowableValues = ["CONSUMER", "SELLER"], example = "CONSUMER")
    val role: MemberRole,
    @field:Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "전화번호", example = "010-1234-5678", nullable = true)
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

@Schema(description = "앱 사용자 내 정보 응답")
data class UserApiResponse(
    @field:Schema(description = "회원 ID", example = "1")
    val id: Long,
    @field:Schema(description = "닉네임", example = "득템러")
    val nickname: String,
    @field:Schema(description = "회원 역할", allowableValues = ["CONSUMER", "SELLER"], example = "CONSUMER")
    val role: String,
    @field:Schema(description = "회원 등급", example = "WELCOME")
    val grade: String,
    @field:Schema(description = "누적 절약 금액", example = "12000")
    val totalSavings: Int,
    @field:Schema(description = "보유 포인트", example = "300")
    val points: Int,
    @field:Schema(description = "보유 쿠폰 수", example = "2")
    val couponCount: Int,
    @field:Schema(description = "절감한 탄소량(kg)", example = "1.5")
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
