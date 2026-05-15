package com.deuktemsiru.controller.member

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.MemberResponse
import com.deuktemsiru.dto.MemberStatsResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.MemberService
import org.springframework.web.bind.annotation.*

data class MemberUpdateRequest(
    val nickname: String? = null,
    val phone: String? = null,
)

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberService: MemberService,
    private val authContext: AuthContext,
) {
    /**
     * GET /api/v1/members/me
     * 마이페이지 조회
     */
    @GetMapping("/me")
    fun getMyProfile(): ApiResponse<MemberResponse> {
        val memberId = authContext.getCurrentMemberId()
        val member = memberService.findMember(memberId)
        return ApiResponse.success(MemberResponse.from(member), "마이페이지 조회 성공")
    }

    /**
     * PUT /api/v1/members/me
     * 내 정보 수정 (닉네임, 전화번호)
     */
    @PutMapping("/me")
    fun updateMyProfile(
        @RequestBody req: MemberUpdateRequest,
    ): ApiResponse<MemberResponse> {
        val memberId = authContext.getCurrentMemberId()
        val member = memberService.updateProfile(memberId, req.nickname, req.phone)
        return ApiResponse.success(MemberResponse.from(member), "수정 성공")
    }

    /**
     * DELETE /api/v1/members/me
     * 회원 탈퇴 (소프트 딜리트 — deleted_at 기록)
     */
    @DeleteMapping("/me")
    fun deleteMyAccount(): ApiResponse<Unit> {
        val memberId = authContext.getCurrentMemberId()
        memberService.deleteAccount(memberId)
        return ApiResponse.success(Unit, "탈퇴 처리 완료")
    }

    /**
     * GET /api/v1/members/me/stats
     * ESG 대시보드 — 절약 금액·탄소 저감량 (MemberStats 기반)
     */
    @GetMapping("/me/stats")
    fun getMyStats(): ApiResponse<MemberStatsResponse> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(memberService.getStats(memberId), "통계 조회 성공")
    }
}
