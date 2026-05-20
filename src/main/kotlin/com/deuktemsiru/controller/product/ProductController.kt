package com.deuktemsiru.controller.product

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.ProductDetailResponse
import com.deuktemsiru.dto.ProductListItemResponse
import com.deuktemsiru.service.StoreService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

data class ProductListResponse(val products: List<ProductListItemResponse>, val hasNext: Boolean)

@Tag(name = "Buyer Products", description = "구매자 상품 목록 및 상세 조회 API")
@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val storeService: StoreService,
) {
    /**
     * GET /api/v1/products
     * 주변 마감할인 상품 목록
     */
    @Operation(summary = "상품 목록 조회", description = "현재 위치, 반경, 카테고리, 정렬 조건으로 구매 가능한 마감 할인 상품을 조회합니다.")
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
        val products = storeService.getProductsBuyer(
            category = category,
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            sort = sort,
            page = page,
            size = size,
        )
        return ok(ProductListResponse(products = products.items, hasNext = products.hasNext))
    }

    /**
     * GET /api/v1/products/{productId}
     * 상품 상세 조회
     */
    @Operation(summary = "상품 상세 조회", description = "상품 상세 정보와 해당 상품의 매장 정보를 조회합니다.")
    @GetMapping("/{productId}")
    fun getProduct(
        @PathVariable productId: Long,
    ): ApiResponse<ProductDetailResponse> {
        return ok(storeService.getProductDetailBuyer(productId))
    }
}
