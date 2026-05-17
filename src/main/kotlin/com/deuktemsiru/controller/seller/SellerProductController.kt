package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.SaleItemRequest
import com.deuktemsiru.dto.SellerSaleItemResponse
import com.deuktemsiru.dto.UpdateSaleItemRequest
import com.deuktemsiru.dto.UpdateSaleStatusRequest
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.SellerAppService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

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
    fun getProducts(
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false) status: String?,
    ): ApiResponse<List<SellerSaleItemResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val parsedDate = date?.let { LocalDate.parse(it) }
        return ApiResponse.success(sellerAppService.getProducts(sellerId, parsedDate, status))
    }

    /**
     * POST /api/v1/sellers/products
     * 판매 상품 등록 (메뉴 기반 오늘의 판매분 생성)
     */
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createProduct(
        @RequestBody req: SaleItemRequest,
    ): ResponseEntity<ApiResponse<SellerSaleItemResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val product = sellerAppService.createProduct(sellerId, req)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(product, "판매 상품이 등록되었습니다."))
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createProductWithImage(
        @RequestPart name: String,
        @RequestPart discountPrice: String,
        @RequestPart originalPrice: String,
        @RequestPart quantityTotal: String,
        @RequestPart pickupStart: String,
        @RequestPart pickupEnd: String,
        @RequestPart availableDate: String,
        @RequestPart(required = false) menuItemId: String?,
        @RequestPart(required = false) madeAt: String?,
        @RequestPart(required = false) allergenInfo: String?,
        @RequestPart(required = false) images: List<MultipartFile>?,
    ): ResponseEntity<ApiResponse<SellerSaleItemResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val product = sellerAppService.createProductWithImages(
            sellerId = sellerId,
            req = SaleItemRequest(
                menuItemId = menuItemId?.toLongOrNull(),
                name = name,
                discountPrice = discountPrice.toInt(),
                originalPrice = originalPrice.toInt(),
                quantityTotal = quantityTotal.toInt(),
                madeAt = madeAt,
                pickupStart = pickupStart,
                pickupEnd = pickupEnd,
                availableDate = availableDate,
                allergenInfo = allergenInfo,
            ),
            images = images.orEmpty(),
        )
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

    @PatchMapping("/{productId}")
    fun updateProductDetails(
        @PathVariable productId: Long,
        @RequestBody req: UpdateSaleItemRequest,
    ): ApiResponse<SellerSaleItemResponse> {
        val sellerId = authContext.getCurrentMemberId()
        return ApiResponse.success(sellerAppService.updateProduct(sellerId, productId, req))
    }

    /**
     * DELETE /api/v1/sellers/products/{productId}
     * 판매 상품 삭제
     */
    @DeleteMapping("/{productId}")
    fun deleteProduct(
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        val sellerId = authContext.getCurrentMemberId()
        sellerAppService.deleteProduct(sellerId, productId)
        return ApiResponse.success(Unit, "판매 상품이 삭제되었습니다.")
    }
}
