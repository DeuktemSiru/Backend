package com.deuktemsiru.repository

import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Store
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface MenuItemRepository : JpaRepository<MenuItem, Long> {
    fun findByStore(store: Store): List<MenuItem>
    fun findByStoreAndIsSoldOutFalse(store: Store): List<MenuItem>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MenuItem m where m.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Optional<MenuItem>
}
