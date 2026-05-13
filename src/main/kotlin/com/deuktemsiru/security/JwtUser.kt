package com.deuktemsiru.security

import com.deuktemsiru.entity.MemberRole

data class JwtUser(
    val memberId: Long,
    val role: MemberRole,
)
