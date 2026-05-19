package com.deuktemsiru.controller.seller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.created
import com.deuktemsiru.entity.BusinessInfo
import com.deuktemsiru.security.CurrentMemberId
import com.deuktemsiru.service.SellerAppService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class BusinessInfoRequest(
    val businessNumber: String,
    val businessName: String = "",
)

data class BusinessInfoResponse(
    val businessInfoId: Long,
    val businessNumber: String,
    val isVerified: Int,      // 0=미인증, 1=인증
    val isSiruVerified: Int,  // 0=미연동, 1=연동
    val siruStoreId: String?,
) {
    companion object {
        fun from(info: BusinessInfo) = BusinessInfoResponse(
            businessInfoId = info.businessInfoId,
            businessNumber = info.businessNumber,
            isVerified = if (info.isVerified) 1 else 0,
            isSiruVerified = if (info.isSiruVerified) 1 else 0,
            siruStoreId = info.siruStoreId,
        )
    }
}

@RestController
@RequestMapping("/api/v1/sellers")
class SellerAuthController(
    private val sellerAppService: SellerAppService,
) {
    /**
     * POST /api/v1/sellers/business-info
     * 사업자 정보 등록
     */
    @PostMapping("/business-info")
    fun registerBusinessInfo(
        @CurrentMemberId sellerId: Long,
        @RequestBody req: BusinessInfoRequest,
    ): ResponseEntity<ApiResponse<BusinessInfoResponse>> {
        val info = sellerAppService.registerBusinessInfo(sellerId, req.businessNumber, req.businessName)
        return created(BusinessInfoResponse.from(info), "사업자 정보 등록 성공")
    }
}
