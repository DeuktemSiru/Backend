package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.created
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.SellerMenuItemForm
import com.deuktemsiru.dto.SellerMenuItemResponse
import com.deuktemsiru.dto.SellerMenuItemRequest
import com.deuktemsiru.dto.SellerMenuItemUpdateRequest
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.SellerAppService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

// ── Controller ────────────────────────────────────────────────────────────────

@Tag(name = "Seller Menu", description = "판매자 메뉴 관리 API")
@RestController
@RequestMapping("/api/v1/sellers/menu-items")
class SellerMenuController(
    private val sellerAppService: SellerAppService,
) {

    /**
     * GET /api/v1/sellers/menu-items
     * 내 가게 메뉴 목록 조회
     */
    @GetMapping
    fun getMenuItems(@CurrentMemberId sellerId: Long): ApiResponse<List<SellerMenuItemResponse>> {
        return ok(sellerAppService.getMenus(sellerId))
    }

    /**
     * POST /api/v1/sellers/menu-items (application/json)
     * 메뉴 등록 — JSON 방식
     */
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addMenuItem(
        @CurrentMemberId sellerId: Long,
        @RequestBody req: SellerMenuItemRequest,
    ): ResponseEntity<ApiResponse<SellerMenuItemResponse>> {
        return createdMenu(sellerAppService.createMenu(sellerId, req))
    }

    /**
     * POST /api/v1/sellers/menu-items (multipart/form-data)
     * 메뉴 등록 — 이미지 포함 방식
     * Fields: name(필수), description, originalPrice(필수), allergenInfo, image(필수)
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun addMenuItemWithImage(
        @CurrentMemberId sellerId: Long,
        @ModelAttribute form: SellerMenuItemForm,
        @RequestPart(required = false) image: MultipartFile?,
    ): ResponseEntity<ApiResponse<SellerMenuItemResponse>> {
        return createdMenu(sellerAppService.createMenuWithImage(sellerId, form.toRequest(), image))
    }

    /**
     * PATCH /api/v1/sellers/menu-items/{menuItemId}
     * 메뉴 수정
     */
    @PatchMapping("/{menuItemId}")
    fun updateMenuItem(
        @CurrentMemberId sellerId: Long,
        @PathVariable menuItemId: Long,
        @RequestBody req: SellerMenuItemUpdateRequest,
    ): ApiResponse<SellerMenuItemResponse> {
        return ok(sellerAppService.updateMenu(sellerId, menuItemId, req))
    }

    /**
     * DELETE /api/v1/sellers/menu-items/{menuItemId}
     * 메뉴 삭제
     */
    @DeleteMapping("/{menuItemId}")
    fun deleteMenuItem(
        @CurrentMemberId sellerId: Long,
        @PathVariable menuItemId: Long,
    ): ApiResponse<Unit> {
        sellerAppService.deleteMenu(sellerId, menuItemId)
        return ok(Unit, "메뉴가 삭제되었습니다.")
    }

    private fun createdMenu(menu: SellerMenuItemResponse): ResponseEntity<ApiResponse<SellerMenuItemResponse>> =
        created(menu, "메뉴가 등록되었습니다.")
}
