package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.SaleItemRequest
import com.deuktemsiru.dto.SellerSaleItemResponse
import com.deuktemsiru.dto.UpdateSaleStatusRequest
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.SellerAppService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/products")
class SellerProductController(
    private val sellerAppService: SellerAppService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/sellers/products
     * 오늘의 판매 상품(Product) 목록 조회
     */
    @GetMapping
    fun getProducts(): ApiResponse<List<SellerSaleItemResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        return ApiResponse.success(sellerAppService.getProducts(sellerId))
    }

    /**
     * POST /api/v1/sellers/products
     * 판매 상품 등록 (메뉴 기반 오늘의 판매분 생성)
     */
    @PostMapping
    fun createProduct(
        @RequestBody req: SaleItemRequest,
    ): ResponseEntity<ApiResponse<SellerSaleItemResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val product = sellerAppService.createProduct(sellerId, req)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(product, "판매 상품이 등록되었습니다."))
    }

    /**
     * PATCH /api/v1/sellers/products/{productId}/status
     * 판매 상품 상태 변경 (SOLD_OUT / EXPIRED)
     */
    @PatchMapping("/{productId}/status")
    fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody req: UpdateSaleStatusRequest,
    ): ApiResponse<SellerSaleItemResponse> {
        val sellerId = authContext.getCurrentMemberId()
        return ApiResponse.success(sellerAppService.updateProductStatus(sellerId, productId, req))
    }

    /**
     * DELETE /api/v1/sellers/products/{productId}
     * 판매 상품 삭제
     */
    @DeleteMapping("/{productId}")
    fun deleteProduct(
        @PathVariable productId: Long,
    ): ResponseEntity<Void> {
        val sellerId = authContext.getCurrentMemberId()
        sellerAppService.deleteProduct(sellerId, productId)
        return ResponseEntity.noContent().build()
 