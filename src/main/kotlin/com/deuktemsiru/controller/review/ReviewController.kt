package com.deuktemsiru.controller.review

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.created
import com.deuktemsiru.common.ok
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.ReviewCreateRequest
import com.deuktemsiru.service.ReviewService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Reviews", description = "구매자 리뷰 API")
@RestController
@RequestMapping("/api/v1/reviews")
class ReviewController(
    private val reviewService: ReviewService,
) {
    /**
     * POST /api/v1/reviews
     * 리뷰 작성 (PICKED_UP 상태 주문만 가능, 주문당 1개)
     */
    @PostMapping
    fun createReview(
        @CurrentMemberId memberId: Long,
        @RequestBody req: ReviewCreateRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        reviewService.createReview(memberId, req)
        return created(Unit, "리뷰가 작성되었습니다.")
    }

    /**
     * DELETE /api/v1/reviews/{reviewId}
     * 리뷰 삭제 (소프트 딜리트)
     */
    @DeleteMapping("/{reviewId}")
    fun deleteReview(
        @CurrentMemberId memberId: Long,
        @PathVariable reviewId: Long,
    ): ApiResponse<Unit> {
        reviewService.deleteReview(reviewId, memberId)
        return ok(Unit, "리뷰가 삭제되었습니다.")
    }
}
