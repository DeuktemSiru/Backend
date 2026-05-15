package com.deuktemsiru.repository

import com.deuktemsiru.entity.Settlement
import com.deuktemsiru.entity.Store
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementRepository : JpaRepository<Settlement, Long> {
    fun findByStoreOrderByPeriodStartDesc(store: Store): List<Settlement>
}
