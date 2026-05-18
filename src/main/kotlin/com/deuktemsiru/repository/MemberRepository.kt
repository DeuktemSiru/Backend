package com.deuktemsiru.repository

import com.deuktemsiru.entity.Member
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Optional<Member>
    fun findByProviderAndProviderId(provider: com.deuktemsiru.entity.MemberProvider, providerId: String): Optional<Member>
    fun existsByEmail(email: String): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.memberId = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Optional<Member>
}
