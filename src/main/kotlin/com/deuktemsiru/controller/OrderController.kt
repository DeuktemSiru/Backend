package com.deuktemsiru.controller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.AppOrderResponse
import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrderV1(
        @RequestBody req: CreateOrderRequest,
    ): ApiResponse<AppOrderResponse> {
        val consumerId = authContext.getCurrentMemberId()
        return ApiResponse.created(AppOrderResponse.from(orderService.createOrderEntity(consumerId, req)))
    }

    @GetMapping
    fun getMyOrdersV1(): ApiResponse<List<AppOrderResponse>> {
        val consumerId = authContext.getCurrentMemberId()
        return ApiResponse.success(orderService.getMyOrderEntities(consumerId).map { AppOrderResponse.from(it) })
    }

    @GetMapping("/{orderId}")
    fun getOrderV1(@PathVariable orderId: Long): ApiResponse<AppOrderResponse> {
        val consumerId = authContext.getCurrentMemberId()
        return ApiResponse.success(AppOrderResponse.from(orderService.getConsumerOrderEntity(consumerId, orderId)))
    }
}
