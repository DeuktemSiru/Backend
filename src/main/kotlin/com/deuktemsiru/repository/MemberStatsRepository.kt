package com.deuktemsiru.repository

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberStats
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MemberStatsRepository : JpaRepository<MemberStats, Long> {
    fun findByMember(member: Member): Optional<MemberStats>
}
