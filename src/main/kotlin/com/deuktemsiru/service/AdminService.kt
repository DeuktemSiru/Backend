package com.deuktemsiru.service

import com.deuktemsiru.common.nowDateTime
import com.deuktemsiru.common.orNotFound
import com.deuktemsiru.dto.BusinessVerificationResponse
import com.deuktemsiru.dto.SettlementItem
import com.deuktemsiru.dto.StoreVerificationResponse
import com.deuktemsiru.entity.SettlementStatus
import com.deuktemsiru.repository.BusinessInfoRepository
import com.deuktemsiru.repository.SettlementRepository
import com.deuktemsiru.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/**
 * 운영자 전용 승인·정산 처리. (PLAN P1 승인·정산)
 * 관리자 콘솔은 범위 밖이라 운영자가 `AdminController`를 직접 호출한다.
 */
@Service
@Transactional
class AdminService(
    private val businessInfoRepository: BusinessInfoRepository,
    private val storeRepository: StoreRepository,
    private val settlementRepository: SettlementRepository,
    private val clock: Clock,
) {
    fun verifyBusiness(businessInfoId: Long, approved: Boolean, siruApproved: Boolean?): BusinessVerificationResponse {
        val info = businessInfoRepository.findById(businessInfoId).orNotFound("사업자 정보를 찾을 수 없습니다.")
        info.isVerified = approved
        info.verifiedAt = if (approved) clock.nowDateTime() else null
        siruApproved?.let { info.isSiruVerified = it }
        if (!approved) info.isSiruVerified = false
        return BusinessVerificationResponse.from(info)
    }

    fun verifyStore(storeId: Long, approved: Boolean): StoreVerificationResponse {
        val store = storeRepository.findById(storeId).orNotFound("가게를 찾을 수 없습니다.")
        store.isVerified = approved
        return StoreVerificationResponse.from(store)
    }

    /** 정산은 신청된 PENDING 건만 지급(COMPLETED)/반려(REJECTED)로 전이한다. 실제 송금은 수기로 처리한다. */
    fun updateSettlementStatus(settlementId: Long, status: SettlementStatus): SettlementItem {
        require(status != SettlementStatus.PENDING) { "정산을 신청 대기 상태로 되돌릴 수 없습니다." }
        val settlement = settlementRepository.findById(settlementId).orNotFound("정산 내역을 찾을 수 없습니다.")
        check(settlement.status == SettlementStatus.PENDING) {
            "이미 처리된 정산입니다. (현재 ${settlement.status})"
        }
        settlement.status = status
        settlement.settledAt = if (status == SettlementStatus.COMPLETED) clock.nowDateTime() else null
        return SettlementItem.from(settlement)
    }
}
