package com.deuktemsiru.controller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.*
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.OrderService
import com.deuktemsiru.service.SellerAppService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses as SwaggerApiResponses

@Tag(name = "Seller", description = "판매자 상품, 메뉴, 주문, 가게, 알림, 매출 API")
@RestController
@RequestMapping("/api/v1/seller")
class SellerV1Controller(
    private val sellerAppService: SellerAppService,
    private val orderService: OrderService,
    private val authContext: AuthContext,
) {

    @Operation(summary = "판매 상품 목록 조회", description = "판매자가 등록한 마감 할인 상품 목록을 조회합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "판매 상품 목록 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "판매자 가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping("/products")
    fun getProducts(): ApiResponse<List<SellerSaleItemResponse>> =
        ApiResponse.success(sellerAppService.getProducts(authContext.getCurrentMemberId()))

    @Operation(summary = "판매 상품 등록", description = "기존 메뉴를 기반으로 오늘 판매할 마감 할인 상품을 등록합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "201", description = "판매 상품 등록 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 판매 상품 요청"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "판매자 가게 또는 메뉴를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PostMapping("/products")
    fun createProduct(@RequestBody req: SaleItemRequest): ApiResponse<SellerSaleItemResponse> =
        ApiResponse.created(sellerAppService.createProduct(authContext.getCurrentMemberId(), req))

    @Operation(summary = "판매 상품 상태 변경", description = "판매 상품의 상태를 변경합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "판매 상품 상태 변경 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 상태 값"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PatchMapping("/products/{id}")
    fun updateProductStatus(
        @Parameter(description = "상품 ID", example = "1")
        @PathVariable id: Long,
        @RequestBody req: UpdateSaleStatusRequest,
    ): ApiResponse<SellerSaleItemResponse> =
        ApiResponse.success(sellerAppService.updateProductStatus(authContext.getCurrentMemberId(), id, req))

    @Operation(summary = "판매 상품 삭제", description = "판매자가 등록한 마감 할인 상품을 삭제합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "판매 상품 삭제 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @DeleteMapping("/products/{id}")
    fun deleteProduct(
        @Parameter(description = "상품 ID", example = "1")
        @PathVariable id: Long,
    ): ApiResponse<Unit> {
        sellerAppService.deleteProduct(authContext.getCurrentMemberId(), id)
        return ApiResponse.success(Unit)
    }

    @Operation(summary = "메뉴 목록 조회", description = "판매자 가게에 등록된 기본 메뉴 목록을 조회합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "메뉴 목록 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "판매자 가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping("/menus")
    fun getMenus(): ApiResponse<List<SellerMenuItemResponse>> =
        ApiResponse.success(sellerAppService.getMenus(authContext.getCurrentMemberId()))

    @Operation(summary = "메뉴 등록(JSON)", description = "JSON 요청으로 판매자 가게의 기본 메뉴를 등록합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "201", description = "메뉴 등록 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 메뉴 요청"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "판매자 가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PostMapping("/menus", consumes = ["application/json"])
    fun createMenu(@RequestBody req: SellerMenuItemRequest): ApiResponse<SellerMenuItemResponse> =
        ApiResponse.created(sellerAppService.createMenu(authContext.getCurrentMemberId(), req))

    @Operation(summary = "메뉴 등록(이미지 포함)", description = "multipart/form-data 요청으로 판매자 메뉴와 선택 이미지 파일을 함께 등록합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "201", description = "메뉴 등록 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 메뉴 또는 이미지 요청"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "판매자 가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PostMapping("/menus", consumes = ["multipart/form-data"])
    fun createMenuMultipart(
        @Parameter(description = "메뉴명", example = "크루아상")
        @RequestPart("name") name: String,
        @Parameter(description = "정상가", example = "5000")
        @RequestPart("originalPrice") originalPrice: String,
        @Parameter(description = "할인율(%)", example = "30")
        @RequestPart("discountRate", required = false) discountRate: String?,
        @Parameter(description = "판매 수량", example = "10")
        @RequestPart("quantity", required = false) quantity: String?,
        @Parameter(description = "픽업 가능 시간대", example = "18:00-20:00")
        @RequestPart("pickupTimeSlot", required = false) pickupTimeSlot: String?,
        @Parameter(description = "알레르기 정보", example = "밀, 우유")
        @RequestPart("allergyInfo", required = false) allergyInfo: String?,
        @Parameter(description = "메뉴 이미지 파일")
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

    @Operation(summary = "메뉴 수정", description = "판매자 가게의 기본 메뉴 이름 또는 가격을 수정합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "메뉴 수정 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 메뉴 수정 요청"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "메뉴를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PatchMapping("/menus/{menuItemId}")
    fun updateMenu(
        @Parameter(description = "메뉴 ID", example = "1")
        @PathVariable menuItemId: Long,
        @RequestBody req: SellerMenuItemUpdateRequest,
    ): ApiResponse<SellerMenuItemResponse> =
        ApiResponse.success(sellerAppService.updateMenu(authContext.getCurrentMemberId(), menuItemId, req))

    @Operation(summary = "메뉴 삭제", description = "판매자 가게의 기본 메뉴를 삭제합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "메뉴 삭제 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "메뉴를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @DeleteMapping("/menus/{menuItemId}")
    fun deleteMenu(
        @Parameter(description = "메뉴 ID", example = "1")
        @PathVariable menuItemId: Long,
    ): ApiResponse<Unit> {
        sellerAppService.deleteMenu(authContext.getCurrentMemberId(), menuItemId)
        return ApiResponse.success(Unit)
    }

    @Operation(summary = "판매자 주문 목록 조회", description = "판매자 가게에 들어온 주문 목록을 조회합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "판매자 주문 목록 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "판매자 가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping("/orders")
    fun getOrders(): ApiResponse<List<AppOrderResponse>> =
        ApiResponse.success(orderService.getStoreOrderEntities(authContext.getCurrentMemberId()).map { AppOrderResponse.from(it) })

    @Operation(summary = "주문 상태 변경", description = "판매자 가게의 주문 상태를 변경합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "주문 상태 변경 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 주문 상태"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PatchMapping("/orders/{orderId}")
    fun updateOrderStatus(
        @Parameter(description = "주문 ID", example = "1")
        @PathVariable orderId: Long,
        @RequestBody req: com.deuktemsiru.dto.UpdateOrderStatusRequest,
    ): ApiResponse<AppOrderResponse> =
        ApiResponse.success(AppOrderResponse.from(orderService.updateOrderStatusEntity(authContext.getCurrentMemberId(), orderId, req)))

    @Operation(summary = "픽업 코드 검증", description = "판매자가 고객의 픽업 코드를 입력해 주문을 확인합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "픽업 코드 검증 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 픽업 코드"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping("/pickup/verify")
    fun verifyPickupCode(
        @Parameter(description = "픽업 코드", example = "1234")
        @RequestParam code: String,
    ): ApiResponse<AppOrderResponse> =
        ApiResponse.success(AppOrderResponse.from(orderService.verifyPickupCode(authContext.getCurrentMemberId(), code)))

    @Operation(summary = "판매자 가게 조회", description = "현재 판매자의 가게 정보를 조회합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "판매자 가게 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "판매자 가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping("/store")
    fun getStore(): ApiResponse<SellerStoreResponse> =
        ApiResponse.success(sellerAppService.getStore(authContext.getCurrentMemberId()))

    @Operation(summary = "판매자 가게 수정", description = "현재 판매자의 가게 주소, 전화번호, 종료 시간을 수정합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "판매자 가게 수정 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 가게 수정 요청"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "판매자 가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PatchMapping("/store")
    fun updateStore(@RequestBody req: SellerUpdateStoreRequest): ApiResponse<SellerStoreResponse> =
        ApiResponse.success(sellerAppService.updateStore(authContext.getCurrentMemberId(), req))

    @Operation(summary = "구독자 알림 발송", description = "판매자 가게를 찜한 사용자에게 알림을 발송합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "201", description = "알림 발송 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 알림 요청"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "판매자 가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PostMapping("/notifications")
    fun sendNotification(@RequestBody req: SendNotificationRequest): ApiResponse<SellerNotificationResponse> =
        ApiResponse.created(sellerAppService.sendNotification(authContext.getCurrentMemberId(), req))

    @Operation(summary = "판매자 알림 내역 조회", description = "판매자가 발송한 알림 내역을 조회합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "판매자 알림 내역 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "판매자 가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping("/notifications")
    fun getNotifications(): ApiResponse<List<SellerNotificationResponse>> =
        ApiResponse.success(sellerAppService.getNotifications(authContext.getCurrentMemberId()))

    @Operation(summary = "판매 매출 통계 조회", description = "판매자 가게의 기간별 매출과 인기 메뉴 통계를 조회합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "판매 매출 통계 조회 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 기간 요청"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "판매자 가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping("/sales")
    fun getSales(
        @Parameter(description = "통계 기간", example = "weekly")
        @RequestParam(defaultValue = "weekly") period: String,
        @Parameter(description = "현재 기간 기준 이동 오프셋", example = "0")
        @RequestParam(defaultValue = "0") offset: Int,
    ): ApiResponse<SellerSalesResponse> =
        ApiResponse.success(orderService.getSellerSalesStats(authContext.getCurrentMemberId(), period, offset))
}
