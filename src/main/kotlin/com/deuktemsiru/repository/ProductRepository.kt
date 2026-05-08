package com.deuktemsiru.repository

import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface ProductRepository : JpaRepository<Product, Long> {
    fun findByStore(store: Store): List<Product>
    fun findByStoreAndStatus(store: Store, status: ProductStatus): List<Product>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Optional<Product>
}
