package com.deuktemsiru.repository

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.Orders
import com.deuktemsiru.entity.Review
import com.deuktemsiru.entity.Store
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ReviewRepository : JpaRepository<Review, Long> {
    fun findByStoreAndIsDeletedFalseOrderByCreatedAtDesc(store: Store, pageable: Pageable): List<Review>
    fun existsByConsumerAndOrder(consumer: Member, order: Orders): Boolean
    fun findByReviewIdAndIsDeletedFalse(reviewId: Long): Review?

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.store = :store AND r.isDeleted = false")
    fun findAverageRatingByStore(store: Store): Double?

    @Query("SELECT COUNT(r) FROM Review r WHERE r.store = :store AND r.isDeleted = false")
    fun countByStoreAndIsDeletedFalse(store: Store): Long
}
