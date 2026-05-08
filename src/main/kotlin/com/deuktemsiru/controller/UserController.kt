package com.deuktemsiru.controller

import com.deuktemsiru.dto.MemberResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.MemberService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/members")
class UserController(
    private val memberService: MemberService,
    private val authContext: AuthContext,
) {

    @GetMapping("/me")
    fun getMe(@RequestParam memberId: Long): MemberResponse {
        authContext.requireCurrentMemberId(memberId)
        return MemberResponse.from(memberService.findMember(memberId))
    }
}
