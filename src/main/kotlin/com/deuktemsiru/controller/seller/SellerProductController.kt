package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.created
import com.deuktemsiru.common.ok
import com.deuktemsiru.common.toLocalDateOrThrow
import com.deuktemsiru.dto.SaleItemRequest
import com.deuktemsiru.dto.SellerSaleItemResponse
import com.deuktemsiru.dto.UpdateSaleItemRequest
import com.deuktemsiru.dto.UpdateSaleStatusRequest
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.SellerAppService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

// ── Controller ────────────────────────────────────────────────────────────────

@Tag(name = "Seller Products", description = "판매자 판매 상품 관리 API")
@RestController
@RequestMapping("/api/v1/sellers/products")
class SellerProductController(
    private val sellerAppService: SellerAppService,
) {

    /**
     * GET /api/v1/sellers/products
     * 오늘의 판매 상품(Product) 목록 조회
     */
    @GetMapping
    fun getProducts(
        @CurrentMemberId sellerId: Long,
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false) status: String?,
    ): ApiResponse<List<SellerSaleItemResponse>> {
        val parsedDate = date?.toLocalDateOrThrow()
        return ok(sellerAppService.getProducts(sellerId, parsedDate, status))
    }

    /**
     * POST /api/v1/sellers/products
     * 판매 상품 등록 (메뉴 기반 오늘의 판매분 생성)
     */
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createProduct(
        @CurrentMemberId sellerId: Long,
        @RequestBody req: SaleItemRequest,
    ): ResponseEntity<ApiResponse<SellerSaleItemResponse>> {
        return createdProduct(sellerAppService.createProduct(sellerId, req))
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createProductWithImage(
        @CurrentMemberId sellerId: Long,
        @ModelAttribute req: SaleItemRequest,
        @RequestPart(required = false) images: List<MultipartFile>?,
    ): ResponseEntity<ApiResponse<SellerSaleItemResponse>> {
        return createdProduct(sellerAppService.createProduct(sellerId, req, images.orEmpty()))
    }

    /**
     * PATCH /api/v1/sellers/products/{productId}/status
     * 판매 상품 상태 변경 (AVAILABLE / PAUSED / EXPIRED)
     */
    @RequestMapping("/{productId}/status", method = [RequestMethod.PATCH, RequestMethod.POST])
    fun updateProduct(
        @CurrentMemberId sellerId: Long,
        @PathVariable productId: Long,
        @RequestBody req: UpdateSaleStatusRequest,
    ): ApiResponse<SellerSaleItemResponse> {
        return ok(sellerAppService.updateProductStatus(sellerId, productId, req))
    }

    @RequestMapping("/{productId}", method = [RequestMethod.PATCH, RequestMethod.POST])
    fun updateProductDetails(
        @CurrentMemberId sellerId: Long,
        @PathVariable productId: Long,
        @RequestBody req: UpdateSaleItemRequest,
    ): ApiResponse<SellerSaleItemResponse> {
        return ok(sellerAppService.updateProduct(sellerId, productId, req))
    }

    /**
     * DELETE /api/v1/sellers/products/{productId}
     * 판매 상품 삭제
     */
    @DeleteMapping("/{productId}")
    fun deleteProduct(
        @CurrentMemberId sellerId: Long,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> = deleteProductInternal(sellerId, productId)

    @PostMapping("/{productId}/delete")
    fun deleteProductViaPost(
        @CurrentMemberId sellerId: Long,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> = deleteProductInternal(sellerId, productId)

    private fun deleteProductInternal(
        sellerId: Long,
        productId: Long,
    ): ApiResponse<Unit> {
        sellerAppService.deleteProduct(sellerId, productId)
        return ok(Unit, "판매 상품이 삭제되었습니다.")
    }

    private fun createdProduct(product: SellerSaleItemResponse): ResponseEntity<ApiResponse<SellerSaleItemResponse>> =
        created(product, "판매 상품이 등록되었습니다.")
}
