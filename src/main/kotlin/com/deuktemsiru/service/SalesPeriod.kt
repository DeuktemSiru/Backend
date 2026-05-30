package com.deuktemsiru.service

import com.deuktemsiru.common.toEnumOrNull
import com.deuktemsiru.dto.DailySales
import com.deuktemsiru.entity.Orders
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal enum class SalesPeriod {
    DAY, WEEK, MONTH, YEAR;

    fun window(targetDate: LocalDate): Pair<LocalDate, LocalDate> =
        when (this) {
            YEAR -> targetDate.withDayOfYear(1).let { it to it.plusYears(1) }
            MONTH -> targetDate.withDayOfMonth(1).let { it to it.plusMonths(1) }
            WEEK -> targetDate.minusDays(targetDate.dayOfWeek.value.toLong() - 1).let { it to it.plusDays(7) }
            DAY -> targetDate to targetDate.plusDays(1)
        }

    fun chartData(targetDate: LocalDate, orders: List<Orders>): List<DailySales> {
        val labels = when (this) {
            DAY -> (0..23).map { "${it}시" }
            WEEK -> {
                val start = window(targetDate).first
                (0..6).map { start.plusDays(it.toLong()).format(WEEK_LABEL) }
            }
            MONTH -> (1..5).map { "${it}주" }
            YEAR -> (1..12).map { "${it}월" }
        }
        val amounts = orders.groupingBy { bucketIndex(it.createdAt) }
            .fold(0) { total, order -> total + order.totalPrice }
        return labels.mapIndexed { index, label -> DailySales(label, amounts[index] ?: 0) }
    }

    private fun bucketIndex(date: LocalDateTime): Int = when (this) {
        DAY -> date.hour
        WEEK -> date.dayOfWeek.value - 1
        MONTH -> ((date.dayOfMonth - 1) / 7).coerceAtMost(4)
        YEAR -> date.monthValue - 1
    }

    companion object {
        private val WEEK_LABEL = DateTimeFormatter.ofPattern("MM/dd")

        fun from(value: String) = value.toEnumOrNull<SalesPeriod>() ?: DAY
    }
}
