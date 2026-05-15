package com.deuktemsiru.service

import com.deuktemsiru.controller.seller.SettlementItem
import com.deuktemsiru.controller.seller.SettlementListResponse
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.repository.OrderRepository
import com.deuktemsiru.repository.SettlementRepository
import com.deuktemsiru.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
@Transactional(readOnly = true)
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val orderRepository: OrderRepository,
    private val storeRepository: StoreRepository,
    private val memberService: MemberService,
) {
    private val platformFeeRate = 0.03

    fun getSettlements(sellerId: Long, year: Int, month: Int): SettlementListResponse {
        val seller = memberService.findMember(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }

        val targetStart = LocalDate.of(year, month, 1)
        val targetEnd = targetStart.with(TemporalAdjusters.lastDayOfMonth())
        val now = LocalDate.now()

        // DB에 저장된 정산 내역 중 해당 월에 겹치는 항목만 조회
        val saved = settlementRepository.findByStoreOrderByPeriodStartDesc(store)
            .filter { !it.periodEnd.isBefore(targetStart) && !it.periodStart.isAfter(targetEnd) }
            .map {
                SettlementItem(
                    settlementId = it.settlementId,
                    periodStart = it.periodStart.toString(),
                    periodEnd = it.periodEnd.toString(),
                    totalSales = it.totalAmount,
                    platformFee = it.feeAmount,
                    settlementAmount = it.netAmount,
                    status = it.status.name,
                    settledAt = it.settledAt?.toString(),
                )
            }

        // 요청 월이 현재 월이면 실시간 집계 항목을 상단에 추가
        val isCurrentMonth = year == now.year && month == now.monthValue
        val computedCurrentMonth = if (isCurrentMonth) currentMonthSettlement(store.storeId, targetStart, targetEnd) else null

        return SettlementListResponse((listOfNotNull(computedCurrentMonth) + saved))
    }

    private fun currentMonthSettlement(storeId: Long, start: LocalDate, end: LocalDate): SettlementItem? {
        val totalSales = orderRepository.findAll()
            .filter { it.store.storeId == storeId }
            .filter { it.status == OrderStatus.PICKED_UP }
            .filter {
                val date = it.createdAt.toLocalDate()
                !date.isBefore(start) && !date.isAfter(end)
            }
            .sumOf { it.totalPrice }
        if (totalSales == 0) return null

        val fee = (totalSales * platformFeeRate).toInt()
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
