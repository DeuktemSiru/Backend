package com.deuktemsiru.controller.admin

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.UnauthorizedException
import com.deuktemsiru.common.ok
import com.deuktemsiru.common.toEnumOrThrow
import com.deuktemsiru.dto.BusinessVerificationResponse
import com.deuktemsiru.dto.SettlementItem
import com.deuktemsiru.dto.SettlementStatusRequest
import com.deuktemsiru.dto.StoreVerificationResponse
import com.deuktemsiru.dto.VerificationRequest
import com.deuktemsiru.entity.SettlementStatus
import com.deuktemsiru.service.AdminService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import java.security.MessageDigest

const val ADMIN_TOKEN_HEADER = "X-Admin-Token"

/** 설정된 운영자 토큰이 32자 미만이면 운영자 API 자체가 비활성이다. */
fun adminTokenMatches(configured: String, provided: String?): Boolean {
    if (configured.length < 32 || provided == null) return false
    return MessageDigest.isEqual(provided.toByteArray(), configured.toByteArray())
}

/**
 * 운영자 전용 승인·정산 API. (PLAN P1 승인·정산)
 * JWT가 아니라 `X-Admin-Token` 헤더로 인증한다. `app.admin.token`(APP_ADMIN_TOKEN)이 비어 있으면 모두 401이다.
 */
@Tag(name = "Admin", description = "운영자 전용 승인·정산 API (X-Admin-Token 헤더 필요)")
@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val adminService: AdminService,
    @Value("\${app.admin.token:}")
    private val adminToken: String,
) {

    /** POST /api/v1/admin/business-infos/{businessInfoId}/verification — 사업자 승인/반려 */
    @PostMapping("/business-infos/{businessInfoId}/verification")
    fun verifyBusiness(
        @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) token: String?,
        @PathVariable businessInfoId: Long,
        @RequestBody req: VerificationRequest,
    ): ApiResponse<BusinessVerificationResponse> {
        requireAdmin(token)
        return ok(
            adminService.verifyBusiness(businessInfoId, req.approved, req.siruApproved),
            if (req.approved) "사업자 정보를 승인했습니다." else "사업자 정보를 반려했습니다.",
        )
    }

    /** POST /api/v1/admin/stores/{storeId}/verification — 매장 승인/반려 */
    @PostMapping("/stores/{storeId}/verification")
    fun verifyStore(
        @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) token: String?,
        @PathVariable storeId: Long,
        @RequestBody req: VerificationRequest,
    ): ApiResponse<StoreVerificationResponse> {
        requireAdmin(token)
        return ok(
            adminService.verifyStore(storeId, req.approved),
            if (req.approved) "매장을 승인했습니다." else "매장 승인을 해제했습니다.",
        )
    }

    /** POST /api/v1/admin/settlements/{settlementId}/status — 정산 지급/반려 */
    @PostMapping("/settlements/{settlementId}/status")
    fun updateSettlementStatus(
        @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) token: String?,
        @PathVariable settlementId: Long,
        @RequestBody req: SettlementStatusRequest,
    ): ApiResponse<SettlementItem> {
        requireAdmin(token)
        val status = req.status.toEnumOrThrow<SettlementStatus>("정산 상태")
        return ok(
            adminService.updateSettlementStatus(settlementId, status),
            if (status == SettlementStatus.COMPLETED) "정산을 지급 완료 처리했습니다." else "정산을 반려했습니다.",
        )
    }

    private fun requireAdmin(token: String?) {
        if (!adminTokenMatches(adminToken, token)) {
            throw UnauthorizedException("운영자 토큰이 올바르지 않거나 운영자 API가 비활성화되어 있습니다.")
        }
    }
}
