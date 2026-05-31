package com.deuktemsiru.service

import com.deuktemsiru.dto.DailySales
import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.Orders
import com.deuktemsiru.entity.Store
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SalesPeriodTest {
    private val member = Member(providerId = "test", email = "test@example.com", name = "test", nickname = "test")
    private val store = Store(owner = member, name = "test", address = "test", latitude = 0.0, longitude = 0.0)

    @Test
    fun `windows use exclusive ends across leap days and year boundaries`() {
        val leapDay = LocalDate.of(2024, 2, 29)
        assertEquals(leapDay to LocalDate.of(2024, 3, 1), SalesPeriod.DAY.window(leapDay))
        assertEquals(LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 3, 1), SalesPeriod.MONTH.window(leapDay))
        assertEquals(LocalDate.of(2024, 1, 1) to LocalDate.of(2025, 1, 1), SalesPeriod.YEAR.window(leapDay))
        assertEquals(
            LocalDate.of(2024, 12, 30) to LocalDate.of(2025, 1, 6),
            SalesPeriod.WEEK.window(LocalDate.of(2025, 1, 1)),
        )
        assertEquals(SalesPeriod.MONTH, SalesPeriod.from("month"))
        assertEquals(SalesPeriod.DAY, SalesPeriod.from("invalid"))
    }

    @Test
    fun `daily chart sums shared hours and retains empty buckets`() {
        val date = LocalDate.of(2026, 9, 8)
        val chart = SalesPeriod.DAY.chartData(date, listOf(
            order("2026-09-08T09:00", 100),
            order("2026-09-08T09:45", 200),
            order("2026-09-08T23:59", 500),
        ))
        assertEquals(24, chart.size)
        assertEquals(DailySales("0시", 0), chart.first())
        assertEquals(DailySales("9시", 300), chart[9])
        assertEquals(DailySales("23시", 500), chart.last())
        assertEquals(800, chart.sumOf { it.amount })
    }

    @Test
    fun `weekly chart keeps Monday to Sunday labels over the new year`() {
        val chart = SalesPeriod.WEEK.chartData(LocalDate.of(2025, 1, 1), listOf(
            order("2024-12-30T12:00", 100),
            order("2025-01-05T12:00", 200),
        ))
        assertEquals(listOf("12/30", "12/31", "01/01", "01/02", "01/03", "01/04", "01/05"), chart.map { it.date })
        assertEquals(listOf(100, 0, 0, 0, 0, 0, 200), chart.map { it.amount })
    }

    @Test
    fun `monthly and yearly charts preserve existing bucket boundaries`() {
        val date = LocalDate.of(2026, 1, 31)
        val orders = listOf(
            order("2026-01-07T12:00", 100),
            order("2026-01-08T12:00", 200),
            order("2026-01-28T12:00", 300),
            order("2026-01-29T12:00", 400),
            order("2026-01-31T12:00", 500),
        )
        val monthly = SalesPeriod.MONTH.chartData(date, orders)
        assertEquals(listOf("1주", "2주", "3주", "4주", "5주"), monthly.map { it.date })
        assertEquals(listOf(100, 200, 0, 300, 900), monthly.map { it.amount })
        val yearly = SalesPeriod.YEAR.chartData(date, orders + order("2026-12-31T12:00", 600))
        assertEquals(12, yearly.size)
        assertEquals(DailySales("1월", 1500), yearly.first())
        assertEquals(DailySales("12월", 600), yearly.last())
        assertEquals(2100, yearly.sumOf { it.amount })
        assertEquals(List(5) { DailySales("${it + 1}주", 0) }, SalesPeriod.MONTH.chartData(date, emptyList()))
    }

    private fun order(createdAt: String, amount: Int) = Orders(
        consumer = member,
        store = store,
        totalPrice = amount,
        createdAt = LocalDateTime.parse(createdAt),
    )
}
