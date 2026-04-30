package com.deuktemsiru.dto

import com.deuktemsiru.entity.User
import com.deuktemsiru.entity.UserGrade
import com.deuktemsiru.entity.UserRole

data class RegisterRequest(
    val email: String,
    val nickname: String,
    val password: String,
    val role: UserRole = UserRole.BUYER,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class LoginResponse(
    val userId: Long,
    val nickname: String,
    val role: UserRole,
    val token: String,
)

data class UserResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val role: UserRole,
    val grade: UserGrade,
    val totalSavings: Int,
    val points: Int,
    val couponCount: Int,
    val co2Saved: Float,
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
            role = user.role,
            grade = user.grade,
            totalSavings = user.totalSavings,
            points = user.points,
            couponCount = user.couponCount,
            co2Saved = user.co2Saved,
        )
    }
}
