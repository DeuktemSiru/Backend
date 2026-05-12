package com.deuktemsiru.repository

import com.deuktemsiru.entity.Order
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Store
import com.deuktemsiru.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByBuyerOrderByCreatedAtDesc(buyer: User): List<Order>
    fun findByStoreOrderByCreatedAtDesc(store: Store): List<Order>
    fun findByStoreAndStatus(store: Store, status: OrderStatus): List<Order>
    fun findByStoreAndStatusIn(store: Store, statuses: List<OrderStatus>): List<Order>
    fun existsByPickupCode(pickupCode: String): Boolean
}
