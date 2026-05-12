package com.deuktemsiru.repository

import com.deuktemsiru.entity.Store
import com.deuktemsiru.entity.StoreCategory
import com.deuktemsiru.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface StoreRepository : JpaRepository<Store, Long> {
    fun findByCategory(category: StoreCategory): List<Store>
    fun findByOwner(owner: User): Optional<Store>
}
