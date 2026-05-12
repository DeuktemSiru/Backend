package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.MenuItemRequest
import com.deuktemsiru.dto.MenuItemResponse
import com.deuktemsiru.dto.MenuItemUpdateRequest
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.MenuImageStorageService
import com.deuktemsiru.service.StoreService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/sellers/menu-items")
class SellerMenuController(
    private val storeService: StoreService,
    private val menuImageStorageService: MenuImageStorageService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/sellers/menu-items
     * 내 가게 메뉴 목록 조회
     * TODO: 가게별 메뉴 목록 서비스 메서드 추가 필요 (현재 getSellerStore로 간접 접근 가능)
     */
    @GetMapping
    fun getMenuItems(): ApiResponse<List<MenuItemResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val store = storeService.getSellerStore(sellerId)
        return ApiResponse.success(store.menus)
    }

    /**
     * POST /api/v1/sellers/menu-items (application/json)
     * 메뉴 등록 — JSON 방식
     */
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addMenuItem(
        @RequestBody req: MenuItemRequest,
    ): ResponseEntity<ApiResponse<MenuItemResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val menu = storeService.addMenuItem(sellerId, req)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(menu, "메뉴가 등록되었습니다."))
    }

    /**
     * POST /api/v1/sellers/menu-items (multipart/form-data)
     * 메뉴 등록 — 이미지 포함 방식
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun addMenuItemWithImage(
        @RequestPart name: String,
        @RequestPart(required = false) emoji: String?,
        @RequestPart originalPrice: String,
        @RequestPart discountRate: String,
        @RequestPart quantity: String,
        @RequestPart pickupTimeSlot: String,
        @RequestPart(required = false) image: MultipartFile?,
    ): ResponseEntity<ApiResponse<MenuItemResponse>> {
        val sellerId = authContext.getCurrentMemberId()
        val imageUrl = menuImageStorageService.save(image)
        val menu = storeService.addMenuItem(
            sellerId,
            MenuItemRequest(
                name = name,
                emoji = emoji.orEmpty(),
                imageUrl = imageUrl,
                originalPrice = originalPrice.toInt(),
                discountRate = discountRate.toInt(),
                quantity = quantity.toInt(),
                pickupTimeSlot = pickupTimeSlot,
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(menu, "메뉴가 등록되었습니다."))
    }

    /**
     * PATCH /api/v1/sellers/menu-items/{menuItemId}
     * 메뉴 수정 (수량, 품절 여부, 할인율, 픽업 시간, 이미지)
     */
    @PatchMapping("/{menuItemId}")
    fun updateMenuItem(
        @PathVariable menuItemId: Long,
        @RequestBody req: MenuItemUpdateRequest,
    ): ApiResponse<MenuItemResponse> {
        val sellerId = authContext.getCurrentMemberId()
        val menu = storeService.updateMenuItem(sellerId, menuItemId, req)
        return ApiResponse.success(menu)
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
        storeService.deleteMenuItem(sellerId, menuItemId)
        return ResponseEntity.noContent().build()
    }
}
