package com.deuktemsiru.repository

import com.deuktemsiru.entity.Settlement
import com.deuktemsiru.entity.Store
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface SettlementRepository : JpaRepository<Settlement, Long> {
    fun findByStoreOrderByPeriodStartDesc(store: Store): List<Settlement>
    fun findByStoreAndPeriodEndGreaterThanEqualAndPeriodStartLessThanEqualOrderByPeriodStartDesc(
        store: Store,
        periodStart: LocalDate,
        periodEnd: LocalDate,
    ): List<Settlement>
    fun findFirstByStoreAndPeriodStartAndPeriodEndAndStatus(
        store: Store,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        status: com.deuktemsiru.entity.SettlementStatus,
    ): Settlement?
}
