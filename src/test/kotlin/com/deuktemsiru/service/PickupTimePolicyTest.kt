package com.deuktemsiru.service

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalTime

class PickupTimePolicyTest {

    @Test
    fun `pickup time must be inside every product window`() {
        val windows = listOf(
            LocalTime.of(18, 0) to LocalTime.of(20, 30),
            LocalTime.of(18, 30) to LocalTime.of(21, 0),
        )

        assertDoesNotThrow { requirePickupTimeWithinWindows(LocalTime.of(19, 0), windows) }
        assertThrows(IllegalArgumentException::class.java) {
            requirePickupTimeWithinWindows(LocalTime.of(18, 0), windows)
        }
    }
}
