package com.deuktemsiru.repository

import com.deuktemsiru.entity.Store
import com.deuktemsiru.entity.User
import com.deuktemsiru.entity.Wishlist
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface WishlistRepository : JpaRepository<Wishlist, Long> {
    fun findByUser(user: User): List<Wishlist>
    fun findByUserAndStore(user: User, store: Store): Optional<Wishlist>
    fun existsByUserAndStore(user: User, store: Store): Boolean
    fun countByStore(store: Store): Long
}
