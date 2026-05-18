package com.deuktemsiru.service

import com.deuktemsiru.dto.SettlementItem
import com.deuktemsiru.dto.SettlementListResponse
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.PaymentMethod
import com.deuktemsiru.entity.Settlement
import com.deuktemsiru.entity.SettlementStatus
import com.deuktemsiru.entity.Store
import com.deuktemsiru.repository.OrderRepository
import com.deuktemsiru.repository.SettlementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@Service
@Transactional(readOnly = true)
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val orderRepository: OrderRepository,
    private val storeOwnershipService: StoreOwnershipService,
    private val clock: Clock,
) {
    private val platformFeeRate = 0.03

    fun getSettlements(sellerId: Long, year: Int, month: Int): SettlementListResponse {
        val store = storeOwnershipService.findSellerStoreOrNull(sellerId)
            ?: return SettlementListResponse(emptyList())

        val targetStart = LocalDate.of(year, month, 1)
        val targetEnd = targetStart.with(TemporalAdjusters.lastDayOfMonth())
        val now = LocalDate.now(clock)

        // DB에 저장된 정산 내역 중 해당 월에 겹치는 항목만 조회
        val saved = settlementRepository
            .findByStoreAndPeriodEndGreaterThanEqualAndPeriodStartLessThanEqualOrderByPeriodStartDesc(
                store,
                targetStart,
                targetEnd,
            )
            .map { SettlementItem.from(it) }

        // 요청 월이 현재 월이면 실시간 집계 항목을 상단에 추가
        val isCurrentMonth = year == now.year && month == now.monthValue
        val computedCurrentMonth = if (isCurrentMonth) currentMonthSettlement(store, targetStart, targetEnd) else null

        return SettlementListResponse((listOfNotNull(computedCurrentMonth) + saved))
    }

    @Transactional
    fun requestWithdrawal(sellerId: Long, year: Int, month: Int): SettlementItem {
        val store = storeOwnershipService.findSellerStore(sellerId)
        val start = LocalDate.of(year, month, 1)
        val end = start.with(TemporalAdjusters.lastDayOfMonth())
        val existing = settlementRepository.findFirstByStoreAndPeriodStartAndPeriodEndAndStatus(
            store,
            start,
            end,
            SettlementStatus.PENDING,
        )
        val settlement = existing ?: run {
            val computed = currentMonthSettlement(store, start, end)
                ?: throw IllegalStateException("출금 신청 가능한 정산 금액이 없습니다.")
            settlementRepository.save(
                Settlement(
                    store = store,
                    paymentMethod = PaymentMethod.SIRU,
                    periodStart = start,
                    periodEnd = end,
                    totalAmount = computed.totalSales,
                    feeAmount = computed.platformFee,
                    netAmount = computed.settlementAmount,
                )
            )
        }
        return SettlementItem.from(settlement)
    }

    private fun currentMonthSettlement(store: Store, start: LocalDate, end: LocalDate): SettlementItem? {
        val totalSales = orderRepository.sumTotalPriceByStoreAndStatusBetween(
            store,
            OrderStatus.PICKED_UP,
            start.atStartOfDay(),
            end.plusDays(1).atStartOfDay(),
        ).toInt()
        if (totalSales == 0) return null

        val fee = (totalSales * platformFeeRate).roundToInt()
        return SettlementItem(
            settlementId = 0,
            periodStart = start.toString(),
            periodEnd = end.toString(),
            totalSales = totalSales,
            platformFee = fee,
            settlementAmount = totalSales - fee,
            status = "PENDING",
            settledAt = null,
        )
    }
}
