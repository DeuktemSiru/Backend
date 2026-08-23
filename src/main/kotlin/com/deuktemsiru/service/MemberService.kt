package com.deuktemsiru.service

import com.deuktemsiru.dto.MemberStatsResponse
import com.deuktemsiru.dto.NotificationSettingsResponse
import com.deuktemsiru.dto.UpdateNotificationSettingsRequest
import com.deuktemsiru.common.nowDateTime
import com.deuktemsiru.common.orNotFound
import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.repository.MemberRepository
import com.deuktemsiru.repository.MemberStatsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val memberStatsRepository: MemberStatsRepository,
    private val clock: Clock,
    ) {
    fun findMember(memberId: Long): Member =
        memberRepository.findById(memberId).orNotFound("사용자를 찾을 수 없습니다.")

    fun findMemberForUpdate(memberId: Long): Member =
        memberRepository.findByIdForUpdate(memberId).orNotFound("사용자를 찾을 수 없습니다.")

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
        member.deletedAt = clock.nowDateTime()
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
        when (member.role) {
            MemberRole.CONSUMER -> member.applyConsumerNotificationSettings(req)
            MemberRole.SELLER -> member.applySellerNotificationSettings(req)
        }
        req.event?.let { member.notifEvent = it }
        return NotificationSettingsResponse.from(member)
    }

    @Transactional
    fun linkSiru(memberId: Long, siruAccessToken: String): Member {
        require(siruAccessToken.isNotBlank()) { "시루 액세스 토큰을 입력해 주세요." }
        val member = findMember(memberId)
        member.isSiruLinked = true
        if (member.siruBalance == 0) member.siruBalance = 50_000
        return member
    }

    @Transactional
    fun unlinkSiru(memberId: Long): Member {
        val member = findMember(memberId)
        member.isSiruLinked = false
        member.siruBalance = 0
        return member
    }

    private fun Member.applyConsumerNotificationSettings(req: UpdateNotificationSettingsRequest) {
        req.newProduct?.let { notifNewProduct = it }
        req.pickupReminder?.let { notifPickupReminder = it }
        req.orderConfirmed?.let { notifOrderConfirmed = it }
    }

    private fun Member.applySellerNotificationSettings(req: UpdateNotificationSettingsRequest) {
        req.newOrder?.let { notifNewOrder = it }
        req.pickupComplete?.let { notifPickupComplete = it }
        req.soldOut?.let { notifSoldOut = it }
    }
}
