package com.deuktemsiru.service

import com.deuktemsiru.entity.Member
import com.deuktemsiru.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
) {
    fun findMember(memberId: Long): Member =
        memberRepository.findById(memberId).orElseThrow { NoSuchElementException("사용자를 찾을 수 없습니다.") }

    fun findByEmail(email: String): Member =
        memberRepository.findByEmail(email).orElseThrow { NoSuchElementException("사용자를 찾을 수 없습니다.") }
}
