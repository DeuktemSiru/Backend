package com.deuktemsiru.repository

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Orders
import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.Store
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.Optional

interface OrderRepository : JpaRepository<Orders, Long>, JpaSpecificationExecutor<Orders> {
    fun findByConsumerOrderByCreatedAtDesc(consumer: Member): List<Orders>
    fun findByConsumerOrderByCreatedAtDesc(consumer: Member, pageable: Pageable): List<Orders>
    fun findByConsumerAndStatusOrderByCreatedAtDesc(consumer: Member, status: OrderStatus, pageable: Pageable): List<Orders>
    fun findByStoreOrderByCreatedAtDesc(store: Store): List<Orders>
    fun countByStoreAndStatus(store: Store, status: OrderStatus): Long
    fun existsByPickupCode(pickupCode: String): Boolean

    @Query(
        """
        select case when count(o) > 0 then true else false end
        from Orders o
        join o.items i
        where o.store = :store
          and o.status in :statuses
          and i.product = :product
        """
    )
    fun existsActiveOrderForProduct(
        @Param("store") store: Store,
        @Param("statuses") statuses: List<OrderStatus>,
        @Param("product") product: Product,
    ): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orders o where o.orderId = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Optional<Orders>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orders o where o.pickupCode = :pickupCode")
    fun findByPickupCodeForUpdate(@Param("pickupCode") pickupCode: String): Orders?

    @Query(
        """
        select coalesce(sum(o.totalPrice), 0)
        from Orders o
        where o.store = :store
          and o.status = :status
          and o.createdAt >= :start
          and o.createdAt < :end
        """
    )
    fun sumTotalPriceByStoreAndStatusBetween(
        @Param("store") store: Store,
        @Param("status") status: OrderStatus,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): Long
}
