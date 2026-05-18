package com.deuktemsiru.repository

import com.deuktemsiru.entity.CategoryType
import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.Store
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface StoreRepository : JpaRepository<Store, Long> {
    fun findByOwner(owner: Member): Optional<Store>
    fun existsByOwner(owner: Member): Boolean

    @Query("SELECT DISTINCT s FROM Store s JOIN s.categories c WHERE c.category = :category")
    fun findByCategory(@Param("category") category: CategoryType): List<Store>
}
