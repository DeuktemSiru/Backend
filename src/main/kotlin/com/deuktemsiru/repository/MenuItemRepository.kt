package com.deuktemsiru.repository

import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Store
import org.springframework.data.jpa.repository.JpaRepository

interface MenuItemRepository : JpaRepository<MenuItem, Long> {
    fun findByStore(store: Store): List<MenuItem>
    fun findByStoreAndIsActiveTrue(store: Store): List<MenuItem>
}
