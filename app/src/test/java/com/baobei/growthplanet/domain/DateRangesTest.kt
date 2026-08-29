package com.baobei.growthplanet.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateRangesTest {
    @Test fun `week is always monday through sunday across month`() {
        val range = DateRanges.week(LocalDate.of(2026, 8, 1))
        assertEquals(LocalDate.of(2026, 7, 27), range.start)
        assertEquals(LocalDate.of(2026, 8, 2), range.end)
    }

    @Test fun `leap february includes 29 days`() {
        val range = DateRanges.month(LocalDate.of(2028, 2, 14))
        assertEquals(LocalDate.of(2028, 2, 29), range.end)
    }

    @Test fun `year boundary is complete`() {
        val range = DateRanges.year(LocalDate.of(2026, 12, 31))
        assertEquals("2026-01-01", range.startText)
        assertEquals("2026-12-31", range.endText)
    }
}
