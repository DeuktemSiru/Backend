package com.deuktemsiru.controller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.BuyerStoreResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.StoreService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses as SwaggerApiResponses

@Tag(name = "Wishlist", description = "구매자 찜 API")
@RestController
@RequestMapping("/api/v1/wishlist")
class WishlistController(
    private val storeService: StoreService,
    private val authContext: AuthContext,
) {

    @Operation(summary = "찜 토글", description = "가게 찜 상태를 토글하고 변경된 찜 여부를 반환합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "찜 상태 변경 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "가게를 찾을 수 없음"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @PostMapping("/{storeId}")
    fun toggleWishlist(
        @Parameter(description = "가게 ID", example = "1")
        @PathVariable storeId: Long,
    ): ApiResponse<WishlistToggleResponse> {
        val memberId = authContext.getCurrentMemberId()
        val isWishlisted = storeService.toggleWishlist(memberId, storeId)
        return ApiResponse.success(WishlistToggleResponse(isWishlisted))
    }

    @Operation(summary = "찜 목록 조회", description = "인증된 구매자의 찜한 가게 목록을 조회합니다.")
    @SwaggerApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "찜 목록 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "500", description = "서버 오류"),
        ],
    )
    @GetMapping
    fun getWishlist(): ApiResponse<List<BuyerStoreResponse>> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(storeService.getWishlistBuyer(memberId))
    }
}

@Schema(description = "찜 토글 응답")
data class WishlistToggleResponse(
    @field:Schema(description = "변경 후 찜 여부", example = "true")
    val isWishlisted: Boolean,
)
