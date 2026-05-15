package com.deuktemsiru.controller.product

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.ProductDetailResponse
import com.deuktemsiru.dto.ProductListItemResponse
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

data class ProductListResponse(val products: List<ProductListItemResponse>, val hasNext: Boolean)

@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val storeService: StoreService,
) {
    /**
     * GET /api/v1/products
     * 주변 마감할인 상품 목록
     */
    @GetMapping
    fun getProducts(
        @RequestParam(required = true) latitude: Double,
        @RequestParam(required = true) longitude: Double,
        @RequestParam(required = false, defaultValue = "1000") radius: Int,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false, defaultValue = "distance") sort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<ProductListResponse> {
        val products = storeService.getProductsBuyer(category)
        return ApiResponse.success(ProductListResponse(products = products, hasNext = false))
    }

    /**
     * GET /api/v1/products/{productId}
     * 상품 상세 조회
     */
    @GetMapping("/{productId}")
    fun getProduct(
        @PathVariable productId: Long,
    ): ApiResponse<ProductDetailResponse> {
        return ApiResponse.success(storeService.getProductDetailBuyer(productId))
    }
}
