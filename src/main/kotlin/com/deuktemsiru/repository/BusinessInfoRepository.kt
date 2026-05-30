package com.deuktemsiru.repository

import com.deuktemsiru.entity.BusinessInfo
import com.deuktemsiru.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface BusinessInfoRepository : JpaRepository<BusinessInfo, Long> {
    fun findByMember(member: Member): Optional<BusinessInfo>
}
