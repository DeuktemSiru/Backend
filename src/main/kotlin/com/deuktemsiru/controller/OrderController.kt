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

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @RequestParam consumerId: Long,
        @RequestBody req: CreateOrderRequest,
    ): OrderResponse {
        authContext.requireCurrentMemberId(consumerId)
        return orderService.createOrder(consumerId, req)
    }

    @GetMapping("/orders")
    fun getMyOrders(@RequestParam consumerId: Long): List<OrderResponse> {
        authContext.requireCurrentMemberId(consumerId)
        return orderService.getMyOrders(consumerId)
    }

    @GetMapping("/orders/{orderId}")
    fun getOrder(@PathVariable orderId: Long): OrderResponse =
        orderService.getOrder(orderId)
}
