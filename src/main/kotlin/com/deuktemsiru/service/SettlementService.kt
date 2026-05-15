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

    fun getSettlements(sellerId: Long, page: Int, size: Int): SettlementListResponse {
        val seller = memberService.findMember(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }

        val saved = settlementRepository.findByStoreOrderByPeriodStartDesc(store).map {
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

        val computedCurrentMonth = currentMonthSettlement(store.storeId)
        val merged = (listOfNotNull(computedCurrentMonth) + saved)
            .drop(page.coerceAtLeast(0) * size.coerceAtLeast(1))
            .take(size.coerceAtLeast(1))
        return SettlementListResponse(merged)
    }

    private fun currentMonthSettlement(storeId: Long): SettlementItem? {
        val start = LocalDate.now().withDayOfMonth(1)
        val end = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())
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
