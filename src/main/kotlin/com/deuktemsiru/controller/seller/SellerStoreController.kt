package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.created
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.CreateStoreRequest
import com.deuktemsiru.dto.CreateStoreResponse
import com.deuktemsiru.dto.SellerStoreResponse
import com.deuktemsiru.dto.SellerUpdateStoreRequest
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.MenuImageStorageService
import com.deuktemsiru.service.SellerAppService
import com.deuktemsiru.service.StoreService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Seller Stores", description = "판매자 매장 관리 API")
@RestController
@RequestMapping("/api/v1/sellers/stores")
class SellerStoreController(
    private val sellerAppService: SellerAppService,
    private val storeService: StoreService,
    private val menuImageStorageService: MenuImageStorageService,
) {
    /**
     * GET /api/v1/sellers/stores/my
     * 내 가게 조회 (판매자 홈)
     */
    @GetMapping("/my")
    fun getMyStore(@CurrentMemberId sellerId: Long): ApiResponse<SellerStoreResponse> {
        return ok(sellerAppService.getStore(sellerId), "가게 조회 성공")
    }

    /**
     * PUT /api/v1/sellers/stores/my
     * 가게 정보 수정 (소개, 전화번호)
     */
    @PutMapping("/my")
    fun updateStore(
        @CurrentMemberId sellerId: Long,
        @RequestBody req: SellerUpdateStoreRequest,
    ): ApiResponse<SellerStoreResponse> {
        return ok(sellerAppService.updateStore(sellerId, req), "수정 성공")
    }

    /**
     * POST /api/v1/sellers/stores
     * 가게 등록 (multipart/form-data)
     */
    @PostMapping(consumes = ["multipart/form-data"])
    fun createStore(
        @CurrentMemberId sellerId: Long,
        @ModelAttribute req: CreateStoreRequest,
        @RequestParam(required = false) thumbnail: MultipartFile?,
        @RequestParam(required = false) images: List<MultipartFile>?,
    ): ResponseEntity<ApiResponse<CreateStoreResponse>> {
        val thumbnailUrl = menuImageStorageService.save(thumbnail)
        val imageUrls = images.orEmpty().mapNotNull { menuImageStorageService.save(it) }
        return createdStore(CreateStoreResponse.from(storeService.createStore(sellerId, req, thumbnailUrl, imageUrls)))
    }

    /**
     * POST /api/v1/sellers/stores (JSON 방식 — 이미지 없이 테스트용)
     */
    @PostMapping(consumes = ["application/json"])
    fun createStoreJson(
        @CurrentMemberId sellerId: Long,
        @RequestBody req: CreateStoreRequest,
    ): ResponseEntity<ApiResponse<CreateStoreResponse>> {
        return createdStore(CreateStoreResponse.from(storeService.createStore(sellerId, req)))
    }

    private fun createdStore(store: CreateStoreResponse): ResponseEntity<ApiResponse<CreateStoreResponse>> =
        created(store, "가게 등록 성공")
}
