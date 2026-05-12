package com.deuktemsiru.controller.product

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

// ── Response DTOs ─────────────────────────────────────────────────────────────

data class ProductListItem(
    val productId: Long,
    val storeId: Long,
    val storeName: String,
    val name: String,
    val originalPrice: Int,
    val discountPrice: Int,
    val discountRate: Int,
    val quantityRemaining: Int,
    val pickupEnd: String,
    val status: String,
    val imageUrl: String?,
)

data class ProductListResponse(val products: List<ProductListItem>, val hasNext: Boolean)

data class ProductDetailResponse(
    val productId: Long,
    val storeId: Long,
    val storeName: String,
    val storeAddress: String,
    val name: String,
    val originalPrice: Int,
    val discountPrice: Int,
    val discountRate: Int,
    val quantityRemaining: Int,
    val pickupStart: String,
    val pickupEnd: String,
    val status: String,
    val imageUrl: String?,
)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val storeService: StoreService,
) {

    /**
     * GET /api/v1/products
     * 상품(메뉴) 목록 조회 — 카테고리·위치 기반 필터 지원 예정
     * TODO: 위치 기반 필터링 구현 필요
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
        val stores = storeService.getStores(category, null)
        val products = stores.flatMap { store ->
            store.menus.map { menu ->
                ProductListItem(
                    productId = menu.id,
                    storeId = store.id,
                    storeName = store.name,
                    name = menu.name,
                    originalPrice = menu.originalPrice,
                    discountPrice = menu.discountedPrice,
                    discountRate = menu.discountRate,
                    quantityRemaining = menu.remainingItems,
                    pickupEnd = menu.pickupTimeSlot.substringAfter("-").trim(),
                    status = if (menu.isSoldOut) "SOLD_OUT" else "AVAILABLE",
                    imageUrl = menu.imageUrl,
                )
            }
        }
        return ApiResponse.success(ProductListResponse(products = products, hasNext = false))
    }

    /**
     * GET /api/v1/products/{productId}
     * 상품(메뉴) 상세 조회
     * TODO: MenuItemRepository를 직접 조회하는 서비스 메서드 추가 필요
     */
    @GetMapping("/{productId}")
    fun getProduct(
        @PathVariable productId: Long,
    ): ApiResponse<ProductDetailResponse> {
        throw UnsupportedOperationException("상품 상세 조회: 미구현 — MenuItemService 추가 필요")
    }
}
