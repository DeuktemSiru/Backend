package com.deuktemsiru.repository

import com.deuktemsiru.entity.CategoryType
import com.deuktemsiru.entity.Store
import com.deuktemsiru.entity.StoreCategory
import org.springframework.data.jpa.repository.JpaRepository

interface StoreCategoryRepository : JpaRepository<StoreCategory, Long> {
    fun findByStore(store: Store): List<StoreCategory>
    fun findByCategory(category: CategoryType): List<StoreCategory>
}
