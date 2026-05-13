package com.deuktemsiru.controller.product

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.BuyerMenuResponse
import com.deuktemsiru.dto.BuyerStoreResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

// ── Response DTOs ─────────────────────────────────────────────────────────────

data class ProductListResponse(val products: List<BuyerMenuResponse>, val hasNext: Boolean)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/products
     * 상품(Product) 목록 조회 — 모든 가게의 오늘 판매 상품
     * TODO: 위치 기반 필터링, keyword, sort 구현 필요
     */
    @GetMapping
    fun getProducts(
        @RequestParam(required = false) latitude: Double?,
        @RequestParam(required = false) longitude: Double?,
        @RequestParam(required = false, defaultValue = "1000") radius: Int,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false, defaultValue = "distance") sort: String,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<ProductListResponse> {
        val memberId = authContext.getCurrentMemberId()
        val stores = storeService.getStoresBuyer(category, memberId)
        val products = stores.flatMap { it.menus }
        return ApiResponse.success(ProductListResponse(products = products, hasNext = false))
    }

    /**
     * GET /api/v1/products/{productId}
     * 상품 상세 조회
     * TODO: ProductRepository 직접 조회 서비스 메서드 추가 필요
     */
    @GetMapping("/{productId}")
    fun getProduct(
        @PathVariable productId: Long,
    ): ApiResponse<BuyerMenuResponse> {
        throw UnsupportedOperationException("상품 상세 조회: 미구현 — StoreService.getProduct(productId) 추가 필요")
    }
}
