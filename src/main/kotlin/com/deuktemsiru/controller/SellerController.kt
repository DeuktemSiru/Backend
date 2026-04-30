package com.deuktemsiru.controller

import com.deuktemsiru.dto.*
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.MenuImageStorageService
import com.deuktemsiru.service.NotificationService
import com.deuktemsiru.service.OrderService
import com.deuktemsiru.service.StoreService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/seller")
class SellerController(
    private val storeService: StoreService,
    private val orderService: OrderService,
    private val notificationService: NotificationService,
    private val menuImageStorageService: MenuImageStorageService,
    private val authContext: AuthContext,
) {

    // GET /api/seller/store?sellerId=2
    @GetMapping("/store")
    fun getMyStore(@RequestParam sellerId: Long): StoreResponse {
        authContext.requireCurrentUserId(sellerId)
        return storeService.getSellerStore(sellerId)
    }

    // PATCH /api/seller/store?sellerId=2
    @PatchMapping("/store")
    fun updateStore(
        @RequestParam sellerId: Long,
        @RequestBody req: UpdateStoreRequest,
    ): StoreResponse {
        authContext.requireCurrentUserId(sellerId)
        return storeService.updateStore(sellerId, req)
    }

    // POST /api/seller/menus?sellerId=2
    @PostMapping("/menus", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun addMenu(
        @RequestParam sellerId: Long,
        @RequestBody req: MenuItemRequest,
    ): MenuItemResponse {
        authContext.requireCurrentUserId(sellerId)
        return storeService.addMenuItem(sellerId, req)
    }

    // POST /api/seller/menus?sellerId=2 (multipart/form-data)
    @PostMapping("/menus", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun addMenuWithImage(
        @RequestParam sellerId: Long,
        @RequestPart name: String,
        @RequestPart(required = false) emoji: String?,
        @RequestPart originalPrice: String,
        @RequestPart discountRate: String,
        @RequestPart quantity: String,
        @RequestPart pickupTimeSlot: String,
        @RequestPart(required = false) image: MultipartFile?,
    ): MenuItemResponse {
        authContext.requireCurrentUserId(sellerId)
        val imageUrl = menuImageStorageService.save(image)
        return storeService.addMenuItem(
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
    }

    // PATCH /api/seller/menus/{menuItemId}?sellerId=2
    @PatchMapping("/menus/{menuItemId}")
    fun updateMenu(
        @RequestParam sellerId: Long,
        @PathVariable menuItemId: Long,
        @RequestBody req: MenuItemUpdateRequest,
    ): MenuItemResponse {
        authContext.requireCurrentUserId(sellerId)
        return storeService.updateMenuItem(sellerId, menuItemId, req)
    }

    // DELETE /api/seller/menus/{menuItemId}?sellerId=2
    @DeleteMapping("/menus/{menuItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMenu(
        @RequestParam sellerId: Long,
        @PathVariable menuItemId: Long,
    ) {
        authContext.requireCurrentUserId(sellerId)
        storeService.deleteMenuItem(sellerId, menuItemId)
    }

    // GET /api/seller/orders?sellerId=2
    @GetMapping("/orders")
    fun getOrders(@RequestParam sellerId: Long): List<OrderResponse> {
        authContext.requireCurrentUserId(sellerId)
        return orderService.getStoreOrders(sellerId)
    }

    // PATCH /api/seller/orders/{orderId}?sellerId=2
    @PatchMapping("/orders/{orderId}")
    fun updateOrderStatus(
        @RequestParam sellerId: Long,
        @PathVariable orderId: Long,
        @RequestBody req: UpdateOrderStatusRequest,
    ): OrderResponse {
        authContext.requireCurrentUserId(sellerId)
        return orderService.updateOrderStatus(sellerId, orderId, req)
    }

    // GET /api/seller/sales?sellerId=2&period=weekly&offset=0
    @GetMapping("/sales")
    fun getSales(
        @RequestParam sellerId: Long,
        @RequestParam(defaultValue = "weekly") period: String,
        @RequestParam(defaultValue = "0") offset: Int,
    ): SalesResponse {
        authContext.requireCurrentUserId(sellerId)
        return orderService.getSalesStats(sellerId, period, offset)
    }

    // POST /api/seller/notifications?sellerId=2
    @PostMapping("/notifications")
    @ResponseStatus(HttpStatus.CREATED)
    fun sendNotification(
        @RequestParam sellerId: Long,
        @RequestBody req: SendNotificationRequest,
    ): NotificationResponse {
        authContext.requireCurrentUserId(sellerId)
        return notificationService.send(sellerId, req)
    }

    // GET /api/seller/notifications?sellerId=2
    @GetMapping("/notifications")
    fun getNotifications(@RequestParam sellerId: Long): List<NotificationResponse> {
        authContext.requireCurrentUserId(sellerId)
        return notificationService.getHistory(sellerId)
    }
}
