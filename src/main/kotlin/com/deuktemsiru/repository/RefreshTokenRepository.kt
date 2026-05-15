package com.deuktemsiru.repository

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    /** 유효한(미폐기) Refresh Token 조회 */
    fun findByTokenAndIsRevokedFalse(token: String): Optional<RefreshToken>

    /** 특정 회원의 모든 Refresh Token 폐기 (로그아웃) */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.member = :member")
    fun revokeAllByMember(member: Member)
}
