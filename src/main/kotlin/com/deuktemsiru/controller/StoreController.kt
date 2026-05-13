package com.deuktemsiru.controller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.BuyerStoreResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses as SwaggerApiResponses

@Tag(name = "Stores", description = "구매자 가게 조회 API")
@RestController
@RequestMapping("/api/v1/stores")
class StoreController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    @Operation(summary = "가게 목록 조회", description = "구매자 앱에서 가게 목록을 조회합니다. category가 있으면 해당 카테고리로 필터링합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "가게 목록 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping
    fun getStores(
        @Parameter(description = "카테고리 필터", example = "BAKERY")
        @RequestParam(required = false) category: String?,
    ): ApiResponse<List<BuyerStoreResponse>> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(storeService.getStoresBuyer(category, memberId))
    }

    @Operation(summary = "가게 상세 조회", description = "구매자 앱에서 가게 상세 정보와 판매 중인 메뉴를 조회합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "가게 상세 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping("/{storeId}")
    fun getStore(
        @Parameter(description = "가게 ID", example = "1")
        @PathVariable storeId: Long,
    ): ApiResponse<BuyerStoreResponse> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(storeService.getStoreBuyer(storeId, memberId))
    }
}
