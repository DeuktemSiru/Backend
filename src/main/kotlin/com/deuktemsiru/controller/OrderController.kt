package com.deuktemsiru.controller

import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.OrderResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class OrderController(
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {

    // POST /api/orders?buyerId=1
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @RequestParam buyerId: Long,
        @RequestBody req: CreateOrderRequest,
    ): OrderResponse {
        authContext.requireCurrentUserId(buyerId)
        return orderService.createOrder(buyerId, req)
    }

    // GET /api/orders?buyerId=1
    @GetMapping("/orders")
    fun getMyOrders(@RequestParam buyerId: Long): List<OrderResponse> {
        authContext.requireCurrentUserId(buyerId)
        return orderService.getMyOrders(buyerId)
    }

    // GET /api/orders/{orderId}
    @GetMapping("/orders/{orderId}")
    fun getOrder(@PathVariable orderId: Long): OrderResponse =
        orderService.getOrder(orderId)
}
