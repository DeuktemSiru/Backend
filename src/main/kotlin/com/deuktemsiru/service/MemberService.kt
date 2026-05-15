package com.deuktemsiru.service

import com.deuktemsiru.dto.MemberStatsResponse
import com.deuktemsiru.dto.NotificationSettingsResponse
import com.deuktemsiru.dto.UpdateNotificationSettingsRequest
import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.repository.MemberRepository
import com.deuktemsiru.repository.MemberStatsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val memberStatsRepository: MemberStatsRepository,
) {
    fun findMember(memberId: Long): Member =
        memberRepository.findById(memberId).orElseThrow { NoSuchElementException("사용자를 찾을 수 없습니다.") }

    fun findByEmail(email: String): Member =
        memberRepository.findByEmail(email).orElseThrow { NoSuchElementException("사용자를 찾을 수 없습니다.") }

    @Transactional
    fun updateProfile(memberId: Long, nickname: String?, phone: String?): Member {
        val member = findMember(memberId)
        nickname?.takeIf { it.isNotBlank() }?.let {
            require(it.length <= 30) { "닉네임은 최대 30자입니다." }
            member.nickname = it
        }
        phone?.let { member.phone = it }
        return member
    }

    @Transactional
    fun deleteAccount(memberId: Long) {
        val member = findMember(memberId)
        member.deletedAt = LocalDateTime.now()
        member.status = false
    }

    fun getStats(memberId: Long): MemberStatsResponse {
        val member = findMember(memberId)
        return memberStatsRepository.findByMember(member)
            .map { MemberStatsResponse.from(it) }
            .orElse(MemberStatsResponse.empty())
    }

    fun getNotificationSettings(memberId: Long): NotificationSettingsResponse {
        val member = findMember(memberId)
        return NotificationSettingsResponse.from(member)
    }

    @Transactional
    fun updateNotificationSettings(memberId: Long, req: UpdateNotificationSettingsRequest): NotificationSettingsResponse {
        val member = findMember(memberId)
        if (member.role == MemberRole.CONSUMER) {
            req.newProduct?.let { member.notifNewProduct = it }
            req.pickupReminder?.let { member.notifPickupReminder = it }
            req.orderConfirmed?.let { member.notifOrderConfirmed = it }
        } else {
            req.newOrder?.let { member.notifNewOrder = it }
            req.pickupComplete?.let { member.notifPickupComplete = it }
            req.soldOut?.let { member.notifSoldOut = it }
        }
        req.event?.let { member.notifEvent = it }
        return NotificationSettingsResponse.from(member)
    }
}
