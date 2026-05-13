package com.deuktemsiru.repository

import com.deuktemsiru.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Optional<Member>
    fun findByProviderAndProviderId(provider: com.deuktemsiru.entity.MemberProvider, providerId: String): Optional<Member>
    fun existsByEmail(email: String): Boolean
}
