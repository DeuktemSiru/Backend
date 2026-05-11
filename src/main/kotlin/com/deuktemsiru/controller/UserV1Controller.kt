package com.deuktemsiru.controller

import com.deuktemsiru.common.ApiResponse
import com.deuktemsiru.dto.UserApiResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.MemberService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val memberService: MemberService,
    private val authContext: AuthContext,
) {

    @GetMapping("/me")
    fun getMe(): ApiResponse<UserApiResponse> {
        val memberId = authContext.getCurrentMemberId()
        return ApiResponse.success(UserApiResponse.from(memberService.findMember(memberId)))
    }
}
