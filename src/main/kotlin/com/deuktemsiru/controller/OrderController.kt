package com.deuktemsiru.controller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.AppOrderResponse
import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses as SwaggerApiResponses

@Tag(name = "Orders", description = "구매자 주문 API")
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {

    @Operation(summary = "주문 생성", description = "인증된 구매자가 가게 상품을 주문합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "201", description = "주문 생성 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 주문 요청"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "가게 또는 상품을 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "409", description = "주문 처리 불가"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrderV1(
        @RequestBody req: CreateOrderRequest,
    ): ApiResponse<AppOrderResponse> {
        val consumerId = authContext.getCurrentMemberId()
        return ApiResponse.created(AppOrderResponse.from(orderService.createOrderEntity(consumerId, req)))
    }

    @Operation(summary = "내 주문 목록 조회", description = "인증된 구매자의 주문 목록을 조회합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "주문 목록 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping
    fun getMyOrdersV1(): ApiResponse<List<AppOrderResponse>> {
        val consumerId = authContext.getCurrentMemberId()
        return ApiResponse.success(orderService.getMyOrderEntities(consumerId).map { AppOrderResponse.from(it) })
    }

    @Operation(summary = "내 주문 상세 조회", description = "인증된 구매자의 특정 주문 상세 정보를 조회합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "주문 상세 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping("/{orderId}")
    fun getOrderV1(
        @Parameter(description = "주문 ID", example = "1")
        @PathVariable orderId: Long,
    ): ApiResponse<AppOrderResponse> {
        val consumerId = authContext.getCurrentMemberId()
        return ApiResponse.success(AppOrderResponse.from(orderService.getConsumerOrderEntity(consumerId, orderId)))
    }
}
