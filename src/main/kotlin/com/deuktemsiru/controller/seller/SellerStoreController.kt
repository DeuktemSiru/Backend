package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.CreateStoreRequest
import com.deuktemsiru.dto.SellerStoreResponse
import com.deuktemsiru.dto.SellerUpdateStoreRequest
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.SellerAppService
import com.deuktemsiru.service.StoreService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

data class CreateStoreResponse(val storeId: Long, val name: String)

@RestController
@RequestMapping("/api/v1/sellers/stores")
class SellerStoreController(
    private val sellerAppService: SellerAppService,
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {
    /**
     * GET /api/v1/sellers/stores/my
     * 내 가게 조회 (판매자 홈)
     */
    @GetMapping("/my")
    fun getMyStore(): ApiResponse<SellerStoreResponse> {
        val sellerId = authContext.getCurrentMemberId()
        return ApiResponse.success(sellerAppService.getStore(sellerId), "가게 조회 성공")
    }

    /**
     * PUT /api/v1/sellers/stores/my
     * 가게 정보 수정 (소개, 전화번호)
     */
    @PutMapping("/my")
    fun updateStore(
        @RequestBody req: SellerUpdateStoreRequest,
    ): ApiResponse<SellerStoreResponse> {
        val sellerId = authContext.getCurrentMemberId()
        return ApiResponse.success(sellerAppService.updateStore(sellerId, req), "수정 성공")
    }

    /**
     * POST /api/v1/sellers/stores
     * 가게 등록 (multipart/form-data)
     */
    @PostMapping(consumes = ["multipart/form-data"])
    fun createStore(
        @RequestParam name: String,
        @RequestParam(required = false) description: String?,
        @RequestParam address: String,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam(required = false) phone: String?,
        @RequestParam categories: List<String>,
        @RequestParam(required = false) thumbnail: MultipartFile?,
        @RequestParam(required = false) images: List<MultipartFile>?,
    ): ResponseEntity<ApiResponse<CreateStoreResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val req = CreateStoreRequest(
            name = name,
            description = description,
            address = address,
            latitude = latitude,
            longitude = longitude,
            phone = phone,
            categories = categories,
        )
        // 썸네일 저장 (MenuImageStorageService 재사용)
        val store = storeService.createStore(sellerId, req, thumbnailUrl = null)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(CreateStoreResponse(store.storeId, store.name), "가게 등록 성공"))
    }

    /**
     * POST /api/v1/sellers/stores (JSON 방식 — 이미지 없이 테스트용)
     */
    @PostMapping(consumes = ["application/json"])
    fun createStoreJson(
        @RequestBody req: CreateStoreRequest,
    ): ResponseEntity<ApiResponse<CreateStoreResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val store = storeService.createStore(sellerId, req)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(CreateStoreResponse(store.storeId, store.name), "가게 등록 성공"))
    }
}
