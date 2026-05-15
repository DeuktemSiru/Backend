package com.deuktemsiru.controller.store

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.service.ReviewService
import com.deuktemsiru.service.StoreReviewListResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/stores/{storeId}/reviews")
class StoreReviewController(
    private val reviewService: ReviewService,
) {
    /**
     * GET /api/v1/stores/{storeId}/reviews
     * 가게 리뷰 목록 조회
     */
    @GetMapping
    fun getStoreReviews(
        @PathVariable storeId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<StoreReviewListResponse> {
        return ApiResponse.success(reviewService.getStoreReviews(storeId, page, size))
    }
}
