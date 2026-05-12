package com.deuktemsiru.controller

import com.deuktemsiru.dto.*
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import com.deuktemsiru.service.StoreService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/seller")
class SellerController(
    private val storeService: StoreService,
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {

    @GetMapping("/store")
    fun getMyStore(@RequestParam sellerId: Long): StoreResponse {
        authContext.requireCurrentMemberId(sellerId)
        return storeService.getSellerStore(sellerId)
    }

    @PatchMapping("/store")
    fun updateStore(
        @RequestParam sellerId: Long,
        @RequestBody req: UpdateStoreRequest,
    ): StoreResponse {
        authContext.requireCurrentMemberId(sellerId)
        return storeService.updateStore(sellerId, req)
    }

    @GetMapping("/orders")
    fun getOrders(@RequestParam sellerId: Long): List<OrderResponse> {
        authContext.requireCurrentMemberId(sellerId)
        return orderService.getStoreOrders(sellerId)
    }

    @PatchMapping("/orders/{orderId}")
    fun updateOrderStatus(
        @RequestParam sellerId: Long,
        @PathVariable orderId: Long,
        @RequestBody req: UpdateOrderStatusRequest,
    ): OrderResponse {
        authContext.requireCurrentMemberId(sellerId)
        return orderService.updateOrderStatus(sellerId, orderId, req)
    }

    @GetMapping("/sales")
    fun getSales(
        @RequestParam sellerId: Long,
        @RequestParam(defaultValue = "weekly") period: String,
        @RequestParam(defaultValue = "0") offset: Int,
    ): SalesResponse {
        authContext.requireCurrentMemberId(sellerId)
        return orderService.getSalesStats(sellerId, period, offset)
    }
}
