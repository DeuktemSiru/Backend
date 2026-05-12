package com.deuktemsiru.controller.store

import com.deuktemsiru.common.ApiResponse
import org.springframework.web.bind.annotation.*

// ── Response DTOs ─────────────────────────────────────────────────────────────

data class ReviewItem(
    val reviewId: Long,
    val authorNickname: String,
    val rating: Float,
    val comment: String,
    val createdAt: String,
)

data class StoreReviewListResponse(
    val reviews: List<ReviewItem>,
    val ratingAvg: Float,
    val totalCount: Int,
)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/stores/{storeId}/reviews")
class StoreReviewController {

    /**
     * GET /api/v1/stores/{storeId}/reviews
     * 가게 리뷰 목록 조회
     * TODO: ReviewService 및 Review 엔티티 구현 필요
     */
    @GetMapping
    fun getStoreReviews(
        @PathVariable storeId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<StoreReviewListResponse> {
        throw UnsupportedOperationException("가게 리뷰 조회: 미구현 — ReviewService 구현 필요")
    }
}
