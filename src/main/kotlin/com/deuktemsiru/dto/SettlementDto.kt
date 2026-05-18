package com.deuktemsiru.dto

import com.deuktemsiru.entity.Settlement

data class SettlementItem(
    val settlementId: Long,
    val periodStart: String,
    val periodEnd: String,
    val totalSales: Int,
    val platformFee: Int,
    val settlementAmount: Int,
    val status: String,
    val settledAt: String?,
) {
    companion object {
        fun from(settlement: Settlement) = SettlementItem(
            settlementId = settlement.settlementId,
            periodStart = settlement.periodStart.toString(),
            periodEnd = settlement.periodEnd.toString(),
            totalSales = settlement.totalAmount,
            platformFee = settlement.feeAmount,
            settlementAmount = settlement.netAmount,
            status = settlement.status.name,
            settledAt = settlement.settledAt?.toString(),
        )
    }
}

data class SettlementListResponse(val settlements: List<SettlementItem>)
data class SettlementWithdrawRequest(val year: Int, val month: Int)

