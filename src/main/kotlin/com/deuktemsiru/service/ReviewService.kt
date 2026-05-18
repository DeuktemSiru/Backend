package com.deuktemsiru.service

import com.deuktemsiru.common.orNotFound
import com.deuktemsiru.common.safePage
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Review
import com.deuktemsiru.repository.OrderRepository
import com.deuktemsiru.repository.ReviewRepository
import com.deuktemsiru.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class ReviewCreateRequest(
    val storeId: Long,
    val orderId: Long,
    val rating: Int,
    val content: String?,
)

data class ReviewItem(
    val reviewId: Long,
    val nickname: String,
    val rating: Int,
    val content: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(review: Review) = ReviewItem(
            reviewId = review.reviewId,
            nickname = review.consumer.nickname,
            rating = review.rating,
            content = review.content,
            createdAt = review.createdAt,
        )
    }
}

data class StoreReviewListResponse(
    val reviews: List<ReviewItem>,
    val ratingAvg: Double,
    val reviewCount: Int,
)

@Service
@Transactional(readOnly = true)
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val storeRepository: StoreRepository,
    private val orderRepository: OrderRepository,
    private val memberService: MemberService,
) {

    @Transactional
    fun createReview(consumerId: Long, req: ReviewCreateRequest) {
        require(req.rating in 1..5) { "별점은 1~5 사이여야 합니다." }

        val consumer = memberService.findMember(consumerId)
        val order = orderRepository.findById(req.orderId)
            .orNotFound("주문을 찾을 수 없습니다.")

        require(order.consumer.memberId == consumerId) { "본인의 주문에만 리뷰를 작성할 수 있습니다." }
        require(order.store.storeId == req.storeId) { "주문한 가게에만 리뷰를 작성할 수 있습니다." }
        require(order.status == OrderStatus.PICKED_UP) { "픽업 완료된 주문에만 리뷰를 작성할 수 있습니다." }
        require(!reviewRepository.existsByConsumerAndOrder(consumer, order)) { "이미 작성한 리뷰가 있습니다." }

        val store = order.store
        reviewRepository.save(
            Review(
                consumer = consumer,
                store = store,
                order = order,
                rating = req.rating,
                content = req.content,
            )
        )

        // 가게 평점 업데이트
        updateStoreRating(store)
    }

    fun getStoreReviews(storeId: Long, page: Int, size: Int): StoreReviewListResponse {
        val store = storeRepository.findById(storeId)
            .orNotFound("가게를 찾을 수 없습니다.")
        val pageable = safePage(page, size)
        val reviews = reviewRepository.findByStoreAndIsDeletedFalseOrderByCreatedAtDesc(store, pageable)
        return StoreReviewListResponse(
            reviews = reviews.map { ReviewItem.from(it) },
            ratingAvg = store.ratingAvg,
            reviewCount = store.reviewCount,
        )
    }

    @Transactional
    fun deleteReview(reviewId: Long, consumerId: Long) {
        val review = reviewRepository.findById(reviewId)
            .orNotFound("리뷰를 찾을 수 없습니다.")
        require(review.consumer.memberId == consumerId) { "본인의 리뷰만 삭제할 수 있습니다." }
        require(!review.isDeleted) { "이미 삭제된 리뷰입니다." }
        review.isDeleted = true

        // 가게 평점 재계산
        updateStoreRating(review.store)
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────────────

    /** 가게의 리뷰 수와 평균 평점을 DB에서 직접 집계하여 Store 엔티티에 반영합니다.
     *  항상 @Transactional 컨텍스트를 가진 메서드(createReview/deleteReview)에서만 호출됩니다. */
    private fun updateStoreRating(store: com.deuktemsiru.entity.Store) {
        val summary = reviewRepository.summarizeRatingByStore(store)
        store.reviewCount = summary.reviewCount.toInt()
        store.ratingAvg = summary.ratingAvg ?: 0.0
    }
}
