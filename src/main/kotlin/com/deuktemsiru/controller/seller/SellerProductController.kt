package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Request / Response DTOs ───────────────────────────────────────────────────

data class SellerProductCreateRequest(
    val menuItemId: Long,
    val quantity: Int,
    val pickupStart: String,
    val pickupEnd: String,
    val discountRate: Int?,
)

data class SellerProductUpdateRequest(
    val quantity: Int?,
    val discountRate: Int?,
    val pickupStart: String?,
    val pickupEnd: String?,
    val isSoldOut: Boolean?,
)

data class SellerProductItem(
    val productId: Long,
    val menuItemId: Long,
    val name: String,
    val originalPrice: Int,
    val discountPrice: Int,
    val discountRate: Int,
    val quantity: Int,
    val quantitySold: Int,
    val pickupStart: String,
    val pickupEnd: String,
    val status: String,
    val imageUrl: String?,
)

data class SellerProductListResponse(val products: List<SellerProductItem>)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/products")
class SellerProductController {

    /**
     * GET /api/v1/sellers/products
     * 오늘의 판매 상품 목록 조회
     * TODO: Product(판매 배치) 엔티티 설계 및 구현 필요
     *       현재 MenuItem이 상품 역할을 하나, 날짜별 배치 관리가 필요한 경우 분리 필요
     */
    @GetMapping
    fun getProducts(): ApiResponse<SellerProductListResponse> {
        throw UnsupportedOperationException("판매 상품 목록 조회: 미구현 — Product 엔티티 설계 필요")
    }

    /**
     * POST /api/v1/sellers/products
     * 판매 상품 등록 (메뉴 기반 오늘의 판매분 생성)
     * TODO: Product 엔티티 구현 필요
     */
    @PostMapping
    fun createProduct(
        @RequestBody req: SellerProductCreateRequest,
    ): ResponseEntity<ApiResponse<SellerProductItem>> {
        throw UnsupportedOperationException("판매 상품 등록: 미구현 — Product 엔티티 구현 필요")
    }

    /**
     * PATCH /api/v1/sellers/products/{productId}
     * 판매 상품 수정 (수량, 할인율, 픽업 시간, 품절 여부)
     * TODO: Product 엔티티 구현 필요
     */
    @PatchMapping("/{productId}")
    fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody req: SellerProductUpdateRequest,
    ): ApiResponse<SellerProductItem> {
        throw UnsupportedOperationException("판매 상품 수정: 미구현 — Product 엔티티 구현 필요")
    }
}
