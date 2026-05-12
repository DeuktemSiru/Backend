package com.deuktemsiru.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthContext {

    fun getCurrentMemberId(): Long {
        val currentUser = SecurityContextHolder.getContext().authentication?.principal as? JwtUser
            ?: throw IllegalArgumentException("인증 정보가 없습니다.")
        return currentUser.userId
    }

    fun requireCurrentUserId(expectedUserId: Long) {
        val currentUser = SecurityContextHolder.getContex