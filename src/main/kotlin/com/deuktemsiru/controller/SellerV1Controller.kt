package com.deuktemsiru.controller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.*
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import com.deuktemsiru.service.SellerAppService
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/seller")
class SellerV1Controller(
    private val sellerAppService: SellerAppService,
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {

    @GetMapping("/products")
    fun getProducts(): ApiResponse<List<SellerSaleItemResponse>> =
        ApiResponse.success(sellerAppService.getProducts(authContext.getCurrentMemberId()))

    @PostMapping("/products")
    fun createProduct(@RequestBody req: SaleItemRequest): ApiResponse<SellerSaleItemResponse> =
        ApiResponse.created(sellerAppService.createProduct(authContext.getCurrentMemberId(), req))

    @PatchMapping("/products/{id}")
    fun updateProductStatus(
        @PathVariable id: Long,
        @RequestBody req: UpdateSaleStatusRequest,
    ): ApiResponse<SellerSaleItemResponse> =
        ApiResponse.success(sellerAppService.updateProductStatus(authContext.getCurrentMemberId(), id, req))

    @DeleteMapping("/products/{id}")
    fun deleteProduct(@PathVariable id: Long): ApiResponse<Unit> {
        sellerAppService.deleteProduct(authContext.getCurrentMemberId(), id)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/menus")
    fun getMenus(): ApiResponse<List<SellerMenuItemResponse>> =
        ApiResponse.success(sellerAppService.getMenus(authContext.getCurrentMemberId()))

    @PostMapping("/menus", consumes = ["application/json"])
    fun createMenu(@RequestBody req: SellerMenuItemRequest): ApiResponse<SellerMenuItemResponse> =
        ApiResponse.created(sellerAppService.createMenu(authContext.getCurrentMemberId(), req))

    @PostMapping("/menus", consumes = ["multipart/form-data"])
    fun createMenuMultipart(
        @RequestPart("name") name: String,
        @RequestPart("originalPrice") originalPrice: String,
        @RequestPart("discountRate", required = false) discountRate: String?,
        @RequestPart("quantity", required = false) quantity: String?,
        @RequestPart("pickupTimeSlot", required = false) pickupTimeSlot: String?,
        @RequestPart("allergyInfo", required = false) allergyInfo: String?,
        @RequestPart("image", required = false) image: MultipartFile?,
    ): ApiResponse<SellerMenuItemResponse> =
        ApiResponse.created(
            sellerAppService.createMenuWithImage(
                authContext.getCurrentMemberId(),
                name,
                originalPrice.toIntOrNull() ?: throw IllegalArgumentException("정상가가 올바르지 않습니다."),
                allergyInfo,
                discountRate?.toIntOrNull(),
                quantity?.toIntOrNull(),
                pickupTimeSlot,
                image,
            )
        )

    @PatchMapping("/menus/{menuItemId}")
    fun updateMenu(
        @PathVariable menuItemId: Long,
        @RequestBody req: SellerMenuItemUpdateRequest,
    ): ApiResponse<SellerMenuItemResponse> =
        ApiResponse.success(sellerAppService.updateMenu(authContext.getCurrentMemberId(), menuItemId, req))

    @DeleteMapping("/menus/{menuItemId}")
    fun deleteMenu(@PathVariable menuItemId: Long): ApiResponse<Unit> {
        sellerAppService.deleteMenu(authContext.getCurrentMemberId(), menuItemId)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/orders")
    fun getOrders(): ApiResponse<List<AppOrderResponse>> =
        ApiResponse.success(orderService.getStoreOrderEntities(authContext.getCurrentMemberId()).map { AppOrderResponse.from(it) })

    @PatchMapping("/orders/{orderId}")
    fun updateOrderStatus(
        @PathVariable orderId: Long,
        @RequestBody req: com.deuktemsiru.dto.UpdateOrderStatusRequest,
    ): ApiResponse<AppOrderResponse> =
        ApiResponse.success(AppOrderResponse.from(orderService.updateOrderStatusEntity(authContext.getCurrentMemberId(), orderId, req)))

    @GetMapping("/pickup/verify")
    fun verifyPickupCode(@RequestParam code: String): ApiResponse<AppOrderResponse> =
        ApiResponse.success(AppOrderResponse.from(orderService.verifyPickupCode(authContext.getCurrentMemberId(), code)))

    @GetMapping("/store")
    fun getStore(): ApiResponse<SellerStoreResponse> =
        ApiResponse.success(sellerAppService.getStore(authContext.getCurrentMemberId()))

    @PatchMapping("/store")
    fun updateStore(@RequestBody req: SellerUpdateStoreRequest): ApiResponse<SellerStoreResponse> =
        ApiResponse.success(sellerAppService.updateStore(authContext.getCurrentMemberId(), req))

    @PostMapping("/notifications")
    fun sendNotification(@RequestBody req: SendNotificationRequest): ApiResponse<SellerNotificationResponse> =
        ApiResponse.created(sellerAppService.sendNotification(authContext.getCurrentMemberId(), req))

    @GetMapping("/notifications")
    fun getNotifications(): ApiResponse<List<SellerNotificationResponse>> =
        ApiResponse.success(sellerAppService.getNotifications(authContext.getCurrentMemberId()))

    @GetMapping("/sales")
    fun getSales(
        @RequestParam(defaultValue = "weekly") period: String,
        @RequestParam(defaultValue = "0") offset: Int,
    ): ApiResponse<SellerSalesResponse> =
        ApiResponse.success(orderService.getSellerSalesStats(authContext.getCurrentMemberId(), period, offset))
}
