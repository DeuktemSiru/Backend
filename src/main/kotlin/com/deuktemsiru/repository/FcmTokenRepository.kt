package com.deuktemsiru.repository

import com.deuktemsiru.entity.FcmToken
import com.deuktemsiru.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface FcmTokenRepository : JpaRepository<FcmToken, Long> {

    /** 특정 회원의 모든 FCM Token 비활성화 (로그아웃) */
    @Modifying
    @Query("UPDATE FcmToken ft SET ft.isActive = false WHERE ft.member = :member")
    fun deactivateAllByMember(member: Member)
}
