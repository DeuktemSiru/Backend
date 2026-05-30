package com.deuktemsiru.dto

import com.deuktemsiru.entity.BusinessInfo
import com.deuktemsiru.entity.Store

data class VerificationRequest(
    /** true면 승인, false면 반려(승인 해제). */
    val approved: Boolean,
    /** 사업자 승인에서만 사용한다. null이면 시루 인증 상태를 바꾸지 않는다. */
    val siruApproved: Boolean? = null,
)

data class BusinessVerificationResponse(
    val businessInfoId: Long,
    val memberId: Long,
    val businessName: String,
    val businessNumber: String,
    val isVerified: Boolean,
    val isSiruVerified: Boolean,
    val verifiedAt: String?,
) {
    companion object {
        fun from(info: BusinessInfo) = BusinessVerificationResponse(
            businessInfoId = info.businessInfoId,
            memberId = info.member.memberId,
            businessName = info.businessName,
            businessNumber = info.businessNumber,
            isVerified = info.isVerified,
            isSiruVerified = info.isSiruVerified,
            verifiedAt = info.verifiedAt?.toString(),
        )
    }
}

data class StoreVerificationResponse(
    val storeId: Long,
    val name: String,
    val ownerId: Long,
    val isVerified: Boolean,
) {
    companion object {
        fun from(store: Store) = StoreVerificationResponse(
            storeId = store.storeId,
            name = store.name,
            ownerId = store.owner.memberId,
            isVerified = store.isVerified,
        )
    }
}

data class SettlementStatusRequest(
    /** COMPLETED(지급 완료) 또는 REJECTED(반려). */
    val status: String,
)
