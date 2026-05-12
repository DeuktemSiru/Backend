package com.deuktemsiru.controller.review

import com.deuktemsiru.common.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Request / Response DTOs ───────────────────────────────────────────────────

data class ReviewCreateRequest(
    val orderId: Long,
    val storeId: Long,
    val rating: Float,
    val comment: String,
)

data class ReviewResponse(
    val reviewId: Long,
    val storeId: Long,
    val storeName: String,
    val rating: Float,
    val comment: String,
    val createdAt: String,
)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/reviews")
class ReviewController {

    /**
     * POST /api/v1/reviews
     * 리뷰 작성
     * TODO: Review 엔티티 및 ReviewService 구현 필요
     */
    @PostMapping
    fun createReview(
        @RequestBody req: ReviewCreateRequest,
    ): ResponseEntity<ApiResponse<ReviewResponse>> {
        throw UnsupportedOperationException("리뷰 작성: 미구현 — Review 엔티티 및 ReviewService 구현 필요")
    }

    /**
     * DELETE /api/v1/reviews/{reviewId}
     * 리뷰 삭제
     * TODO: Review 엔티티 및 ReviewService 구현 필요
     */
    @DeleteMapping("/{reviewId}")
    fun deleteReview(
        @PathVariable reviewId: Long,
    ): ApiResponse<Unit> {
        throw UnsupportedOperationException("리뷰 삭제: 미구현 — Review 엔티티 및 ReviewService 구현 필요")
    }
}
