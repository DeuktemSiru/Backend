package com.deuktemsiru.repository

import com.deuktemsiru.entity.Orders
import com.deuktemsiru.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findFirstByOrderOrderByPaymentIdDesc(order: Orders): Optional<Payment>
}
