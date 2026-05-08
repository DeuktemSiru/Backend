package com.deuktemsiru.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthContext {
    fun requireCurrentMemberId(expectedMemberId: Long) {
        val currentUser = SecurityContextHolder.getContext().authentication?.principal as? JwtUser
            ?: throw IllegalArgumentException("인증 정보가 없습니다.")
        require(currentUser.memberId == expectedMemberId) { "인증된 사용자와 요청 사용자가 일치하지 않습니다." }
    }
}
