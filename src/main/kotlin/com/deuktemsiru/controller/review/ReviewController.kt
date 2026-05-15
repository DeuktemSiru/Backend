package com.deuktemsiru.controller.review

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.ReviewCreateRequest
import com.deuktemsiru.service.ReviewService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/reviews")
class ReviewController(
    private val reviewService: ReviewService,
    private val authContext: AuthContext,
) {
    /**
     * POST /api/v1/reviews
     * 리뷰 작성 (PICKED_UP 상태 주문만 가능, 주문당 1개)
     */
    @PostMapping
    fun createReview(
        @RequestBody req: ReviewCreateRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        val memberId = authContext.getCurrentMemberId()
        reviewService.createReview(memberId, req)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(Unit, "리뷰가 작성되었습니다."))
    }

    /**
     * DELETE /api/v1/reviews/{reviewId}
     * 리뷰 삭제 (소프트 딜리트)
     */
    @DeleteMapping("/{reviewId}")
    fun deleteReview(
        @PathVariable reviewId: Long,
    ): ApiResponse<Unit> {
        val memberId = authContext.getCurrentMemberId()
        reviewService.deleteReview(reviewId, memberId)
        return ApiResponse.success(Unit, "리뷰가 삭제되었습니다.")
    }
}
