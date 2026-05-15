package com.deuktemsiru.repository

import com.deuktemsiru.entity.CartItem
import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CartItemRepository : JpaRepository<CartItem, Long> {
    fun findByMemberOrderByCreatedAtAsc(member: Member): List<CartItem>
    fun findByMemberAndProduct(member: Member, product: Product): Optional<CartItem>
    fun deleteByMember(member: Member)
}
