package com.deuktemsiru.repository

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Orders
import com.deuktemsiru.entity.Store
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Orders, Long> {
    fun findByConsumerOrderByCreatedAtDesc(consumer: Member): List<Orders>
    fun findByStoreOrderByCreatedAtDesc(store: Store): List<Orders>
    fun findByStoreAndStatus(store: Store, status: OrderStatus): List<Orders>
    fun findByStoreAndStatusOrderByCreatedAtDesc(store: Store, status: OrderStatus): List<Orders>
    fun findByStoreAndStatusIn(store: Store, statuses: List<OrderStatus>): List<Orders>
    fun countByStoreAndStatus(store: Store, status: OrderStatus): Long
    fun existsByPickupCode(pickupCode: String): Boolean
    fun findByPickupCode(pickupCode: String): Orders?
}
