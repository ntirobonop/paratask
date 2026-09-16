package io.github.ntirobonop.paratask.feature.upcoming

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WeekTest {
    @Test
    fun `ISO week starts on Monday and ends on Sunday`() {
        val wednesday = LocalDate.parse("2026-09-16")
        val monday = wednesday.isoWeekStart()
        val dates = weekDates(monday)

        assertEquals(LocalDate.parse("2026-09-14"), monday)
        assertEquals(DayOfWeek.MONDAY, dates.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, dates.last().dayOfWeek)
        assertEquals(LocalDate.parse("2026-09-20"), dates.last())
    }

    @Test
    fun `week can cross month and year boundaries`() {
        val dates = weekDates(LocalDate.parse("2025-12-29"))

        assertEquals(LocalDate.parse("2025-12-29"), dates.first())
        assertEquals(LocalDate.parse("2026-01-04"), dates.last())
    }

    @Test
    fun `week dates reject a non-Monday start`() {
        assertThrows(IllegalArgumentException::class.java) {
            weekDates(LocalDate.parse("2026-09-15"))
        }
    }
}
