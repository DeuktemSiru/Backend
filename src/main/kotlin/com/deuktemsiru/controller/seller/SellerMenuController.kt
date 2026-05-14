package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.SellerMenuItemResponse
import com.deuktemsiru.dto.SellerMenuItemRequest
import com.deuktemsiru.dto.SellerMenuItemUpdateRequest
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.SellerAppService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/menu-items")
class SellerMenuController(
    private val sellerAppService: SellerAppService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/sellers/menu-items
     * 내 가게 메뉴 목록 조회
     */
    @GetMapping
    fun getMenuItems(): ApiResponse<List<SellerMenuItemResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        return ApiResponse.success(sellerAppService.getMenus(sellerId))
    }

    /**
     * POST /api/v1/sellers/menu-items (application/json)
     * 메뉴 등록 — JSON 방식
     */
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addMenuItem(
        @RequestBody req: SellerMenuItemRequest,
    ): ResponseEntity<ApiResponse<SellerMenuItemResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val menu = sellerAppService.createMenu(sellerId, req)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(menu, "메뉴가 등록되었습니다."))
    }

    /**
     * POST /api/v1/sellers/menu-items (multipart/form-data)
     * 메뉴 등록 — 이미지 포함 방식
     * Fields: name(필수), description, originalPrice(필수), allergenInfo, image(필수)
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun addMenuItemWithImage(
        @RequestPart name: String,
        @RequestPart originalPrice: String,
        @RequestPart(required = false) description: String?,
        @RequestPart(required = false) allergenInfo: String?,
        @RequestPart(required = false) image: MultipartFile?,
    ): ResponseEntity<ApiResponse<SellerMenuItemResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val menu = sellerAppService.createMenuWithImage(
            sellerId = sellerId,
            name = name,
            description = description,
            originalPrice = originalPrice.toInt(),
            allergenInfo = allergenInfo,
            image = image,
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(menu, "메뉴가 등록되었습니다."))
    }

    /**
     * PATCH /api/v1/sellers/menu-items/{menuItemId}
     * 메뉴 수정
     */
    @PatchMapping("/{menuItemId}")
    fun updateMenuItem(
        @PathVariable menuItemId: Long,
        @RequestBody req: SellerMenuItemUpdateRequest,
    ): ApiResponse<SellerMenuItemResponse> {
        val sellerId = authContext.getCurrentMemberId()
        return ApiResponse.success(sellerAppService.updateMenu(sellerId, menuItemId, req))
    }

    /**
     * DELETE /api/v1/sellers/menu-items/{menuItemId}
     * 메뉴 삭제
     */
    @DeleteMapping("/{menuItemId}")
    fun deleteMenuItem(
        @PathVariable menuItemId: Long,
    ): ResponseEntity<Void> {
        val sellerId = authContext.getCurrentMemberId()
        sellerAppService.deleteMenu(sellerId, menuItemId)
        return ResponseEntity.noContent().build()
    }
}