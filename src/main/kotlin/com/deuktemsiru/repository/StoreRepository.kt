package com.deuktemsiru.repository

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.Store
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface StoreRepository : JpaRepository<Store, Long> {
    fun findByOwner(owner: Member): Optional<Store>
}
