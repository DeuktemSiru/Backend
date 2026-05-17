package com.deuktemsiru.repository

import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.Optional

interface StoreProductCount {
    val storeId: Long
    val productCount: Long
}

interface ProductRepository : JpaRepository<Product, Long> {
    fun findByStore(store: Store): List<Product>
    fun findByStoreAndAvailableDateOrderByCreatedAtDesc(store: Store, date: LocalDate): List<Product>
    fun findByStoreAndStatus(store: Store, status: ProductStatus): List<Product>
    fun findByStoreInAndAvailableDateAndStatus(stores: List<Store>, date: LocalDate, status: ProductStatus): List<Product>
    fun findByStoreAndAvailableDateAndStatus(store: Store, date: LocalDate, status: ProductStatus): List<Product>
    fun countByStoreAndAvailableDateAndStatus(store: Store, date: LocalDate, status: ProductStatus): Int

    @Query(
        """
        select p.store.storeId as storeId, coalesce(sum(p.quantityRemaining), 0) as productCount
        from Product p
        where p.store.storeId in :storeIds
          and p.availableDate = :date
          and p.status = :status
        group by p.store.storeId
        """
    )
    fun countAvailableProductsByStoreIds(
        @Param("storeIds") storeIds: List<Long>,
        @Param("date") date: LocalDate,
        @Param("status") status: ProductStatus = ProductStatus.AVAILABLE,
    ): List<StoreProductCount>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Optional<Product>
}
