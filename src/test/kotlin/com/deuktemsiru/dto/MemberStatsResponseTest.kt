package com.deuktemsiru.dto

import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberStats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemberStatsResponseTest {

    @Test
    fun `derives buyer rewards from saved amount and completed orders`() {
        val member = Member(
            providerId = "test",
            email = "buyer@test.com",
            name = "buyer",
            nickname = "buyer",
        )
        val response = MemberStatsResponse.from(
            MemberStats(
                member = member,
                totalSavedAmount = 12_340,
                totalCarbonSavedKg = 3.5,
                totalOrders = 15,
            )
        )

        assertEquals("TREE", response.grade)
        assertEquals(1_234, response.points)
        assertEquals(0, response.couponCount)
    }
}
