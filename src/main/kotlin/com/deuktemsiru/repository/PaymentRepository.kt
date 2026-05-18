package com.deuktemsiru.repository

import com.deuktemsiru.entity.Orders
import com.deuktemsiru.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findFirstByOrderOrderByPaymentIdDesc(order: Orders): Optional<Payment>

    @Query(
        """
        select p from Payment p
        where p.order.orderId in :orderIds
          and p.paymentId in (
            select max(p2.paymentId)
            from Payment p2
            where p2.order.orderId in :orderIds
            group by p2.order.orderId
          )
        """
    )
    fun findLatestByOrderIds(@Param("orderIds") orderIds: List<Long>): List<Payment>
}
