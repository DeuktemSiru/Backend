package com.deuktemsiru.controller.member

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.MemberResponse
import com.deuktemsiru.dto.UserApiResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.MemberService
import org.springframework.web.bind.annotation.*

// ── Request DTOs ──────────────────────────────────────────────────────────────

data class MemberUpdateRequest(
    val nickname: String?,
    val profileImageUrl: String?,
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
     * 내 프로필 수정 (닉네임, 프로필 이미지)
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
     * 회원 탈퇴
     * TODO: 탈퇴 처리 로직 구현 필요
     */
    @DeleteMapping("/me")
    fun deleteMyAccount(): ApiResponse<Unit> {
        throw UnsupportedOperationException("회원 탈퇴: 미구현 — 탈퇴 정책 및 처리 로직 구현 필요")
    }

    /**
     * GET /api/v1/members/me/stats
     * 내 절약 통계 조회
     * TODO: MemberStats 엔티티 기반 집계 구현 필요
     */
    @GetMapping("/me/stats")
    fun getMyStats(): ApiResponse<UserApiResponse> {
        val memberId = authContext.getCurrentMemberId()
        val member = memberService.findMember(memberId)
        return ApiResponse.success(UserApiResponse.from(member))
    }
}
