package com.deuktemsiru.repository

import com.deuktemsiru.entity.CartItem
import com.deuktemsiru.entity.Member
import org.springframework.data.jpa.repository.JpaRepository

interface CartItemRepository : JpaRepository<CartItem, Long> {
    fun findByMemberOrderByCreatedAtAsc(member: Member): List<CartItem>
    // S5: memberId 로 직접 조회하여 불필요한 Member 엔티티 로딩 제거
    fun findByMemberMemberIdOrderByCreatedAtAsc(memberId: Long): List<CartItem>
    fun deleteByMember(member: Member)
}
