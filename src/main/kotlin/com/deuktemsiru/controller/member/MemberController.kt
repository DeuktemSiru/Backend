package com.deuktemsiru.controller.member

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.common.ok
import com.deuktemsiru.dto.MemberResponse
import com.deuktemsiru.dto.MemberStatsResponse
import com.deuktemsiru.dto.NotificationSettingsResponse
import com.deuktemsiru.dto.UpdateNotificationSettingsRequest
import com.deuktemsiru.security.CurrentMemberId
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
) {
    /**
     * GET /api/v1/members/me
     * 마이페이지 조회
     */
    @GetMapping("/me")
    fun getMyProfile(@CurrentMemberId memberId: Long): ApiResponse<MemberResponse> {
        val member = memberService.findMember(memberId)
        return ok(MemberResponse.from(member), "마이페이지 조회 성공")
    }

    /**
     * PUT /api/v1/members/me
     * 내 정보 수정 (닉네임, 전화번호)
     */
    @PutMapping("/me")
    fun updateMyProfile(
        @CurrentMemberId memberId: Long,
        @RequestBody req: MemberUpdateRequest,
    ): ApiResponse<MemberResponse> {
        val member = memberService.updateProfile(memberId, req.nickname, req.phone)
        return ok(MemberResponse.from(member), "수정 성공")
    }

    /**
     * DELETE /api/v1/members/me
     * 회원 탈퇴 (소프트 딜리트 — deleted_at 기록)
     */
    @DeleteMapping("/me")
    fun deleteMyAccount(@CurrentMemberId memberId: Long): ApiResponse<Unit> {
        memberService.deleteAccount(memberId)
        return ok(Unit, "탈퇴 처리 완료")
    }

    /**
     * GET /api/v1/members/me/stats
     * ESG 대시보드 — 절약 금액·탄소 저감량 (MemberStats 기반)
     */
    @GetMapping("/me/stats")
    fun getMyStats(@CurrentMemberId memberId: Long): ApiResponse<MemberStatsResponse> {
        return ok(memberService.getStats(memberId), "통계 조회 성공")
    }

    /**
     * GET /api/v1/members/me/notification-settings
     * 알림 설정 조회 — 소비자/판매자 역할에 따라 다른 필드 반환
     */
    @GetMapping("/me/notification-settings")
    fun getNotificationSettings(@CurrentMemberId memberId: Long): ApiResponse<NotificationSettingsResponse> {
        return ok(memberService.getNotificationSettings(memberId), "알림 설정 조회 성공")
    }

    /**
     * PUT /api/v1/members/me/notification-settings
     * 알림 설정 변경 — 소비자/판매자 역할에 맞는 필드만 적용됨
     */
    @PutMapping("/me/notification-settings")
    fun updateNotificationSettings(
        @CurrentMemberId memberId: Long,
        @RequestBody req: UpdateNotificationSettingsRequest,
    ): ApiResponse<NotificationSettingsResponse> {
        return ok(memberService.updateNotificationSettings(memberId, req), "설정 변경 성공")
    }
}
