package com.deuktemsiru.controller.member

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.MemberResponse
import com.deuktemsiru.dto.UserApiResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.MemberService
import org.springframework.web.bind.annotation.*

// ── Request / Response DTOs ───────────────────────────────────────────────────

data class MemberUpdateRequest(
    val nickname: String?,
    val phone: String?,
)

data class NotificationSettingsResponse(
    // 소비자용
    val newProduct: Boolean = true,
    val pickupReminder: Boolean = true,
    val orderConfirmed: Boolean = true,
    // 판매자용
    val newOrder: Boolean = true,
    val pickupComplete: Boolean = true,
    val soldOut: Boolean = true,
    // 공통
    val event: Boolean = false,
)

data class NotificationSettingsRequest(
    val newProduct: Boolean? = null,
    val pickupReminder: Boolean? = null,
    val orderConfirmed: Boolean? = null,
    val newOrder: Boolean? = null,
    val pickupComplete: Boolean? = null,
    val soldOut: Boolean? = null,
    val event: Boolean? = null,
)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberService: MemberService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/members/me
     * 내 프로필 조회
     */
    @GetMapping("/me")
    fun getMyProfile(): ApiResponse<MemberResponse> {
        val memberId = authContext.getCurrentMemberId()
        val member = memberService.findMember(memberId)
        return ApiResponse.success(MemberResponse.from(member))
    }

    /**
     * PUT /api/v1/members/me
     * 내 정보 수정 (닉네임, 전화번호)
     * TODO: MemberService.updateProfile() 구현 필요
     */
    @PutMapping("/me")
    fun updateMyProfile(
        @RequestBody req: MemberUpdateRequest,
    ): ApiResponse<MemberResponse> {
        throw UnsupportedOperationException("프로필 수정: 미구현 — MemberService.updateProfile() 구현 필요")
    }

    /**
     * DELETE /api/v1/members/me
     * 회원 탈퇴 (소프트 딜리트 — deleted_at 기록)
     * TODO: 