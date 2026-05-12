package com.deuktemsiru.controller.member

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.UserResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.UserService
import org.springframework.web.bind.annotation.*

// ── Request / Response DTOs ───────────────────────────────────────────────────

data class MemberUpdateRequest(
    val nickname: String?,
    val profileImageUrl: String?,
)

data class MemberStatsResponse(
    val totalSavings: Int,
    val points: Int,
    val couponCount: Int,
    val co2Saved: Float,
    val grade: String,
    val orderCount: Int,
)

// ── Controller ────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val userService: UserService,
    private val authContext: AuthContext,
) {

    /**
     * GET /api/v1/members/me
     * 내 프로필 조회
     */
    @GetMapping("/me")
    fun getMyProfile(): ApiResponse<UserResponse> {
        val memberId = authContext.getCurrentMemberId()
        val user = userService.getUser(memberId)
        return ApiResponse.success(user)
    }

    /**
     * PUT /api/v1/members/me
     * 내 프로필 수정 (닉네임, 프로필 이미지)
     * TODO: 프로필 수정 서비스 메서드 구현 필요
     */
    @PutMapping("/me")
    fun updateMyProfile(
        @RequestBody req: MemberUpdateRequest,
    ): ApiResponse<UserResponse> {
        throw UnsupportedOperationException("프로필 수정: 미구현 — UserService.updateProfile() 구현 필요")
    }

    /**
     * DELETE /api/v1/members/me
     * 회원 탈퇴
     * TODO: 탈퇴 처리 로직 (데이터 익명화 or 삭제) 구현 필요
     */
    @DeleteMapping("/me")
    fun deleteMyAccount(): ApiResponse<Unit> {
        throw UnsupportedOperationException("회원 탈퇴: 미구현 — 탈퇴 정책 및 처리 로직 구현 필요")
    }

    /**
     * GET /api/v1/members/me/stats
     * 내 절약 통계 조회
     */
    @GetMapping("/me/stats")
    fun getMyStats(): ApiResponse<MemberStatsResponse> {
        val memberId = authContext.getCurrentMemberId()
        val user = userService.getUser(memberId)
        val stats = MemberStatsResponse(
            totalSavings = user.totalSavings,
            points = user.points,
            couponCount = user.couponCount,
            co2Saved = user.co2Saved,
            grade = user.grade.name,
            orderCount = 0,   // TODO: 주문 횟수 집계 구현 필요
        )
        return ApiResponse.success(stats)
    }
}
