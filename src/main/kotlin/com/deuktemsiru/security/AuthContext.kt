package com.deuktemsiru.security

import com.deuktemsiru.common.UnauthorizedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthContext {

    /** JWT에서 현재 로그인된 memberId 추출 */
    fun getCurrentMemberId(): Long = getCurrentUser().memberId

    /** JWT에서 현재 로그인된 JwtUser 추출 */
    fun getCurrentUser(): JwtUser =
        SecurityContextHolder.getContext().authentication?.principal as? JwtUser
            ?: throw UnauthorizedException("인증 정보가 없습니다.")

    /** 기존 컨트롤러 호환용 — 쿼리 파라미터의 memberId가 JWT와 일치하는지 검증 */
    fun requireCurrentMemberId(expectedMemberId: Long) {
        val currentUser = getCurrentUser()
        require(currentUser.memberId == expectedMemberId) { "인증된 사용자와 요청 사용자가 일치하지 않습니다." }
    }
}
