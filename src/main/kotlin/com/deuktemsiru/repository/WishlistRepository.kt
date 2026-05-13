package com.deuktemsiru.repository

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.Store
import com.deuktemsiru.entity.Wishlist
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface WishlistRepository : JpaRepository<Wishlist, Long> {
    fun findByMember(member: Member): List<Wishlist>
    fun findByMemberAndStore(member: Member, store: Store): Optional<Wishlist>
    fun existsByMemberAndStore(member: Member, store: Store): Boolean
    fun countByStore(store: Store): Long
}
