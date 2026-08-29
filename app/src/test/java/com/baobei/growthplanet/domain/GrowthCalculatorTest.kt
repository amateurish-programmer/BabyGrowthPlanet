package com.baobei.growthplanet.domain

import com.baobei.growthplanet.data.RecordSummaryRow
import org.junit.Assert.assertEquals
import org.junit.Test

class GrowthCalculatorTest {
    @Test fun `scores never become negative`() {
        assertEquals(0, GrowthCalculator.finalScore(3, 8))
        assertEquals(8, GrowthCalculator.finalScore(10, 2))
    }

    @Test fun `original 34 point thresholds remain compatible`() {
        assertEquals(listOf(20, 25, 30), GrowthCalculator.starThresholds(34))
        assertEquals(0, GrowthCalculator.stars(19, 34))
        assertEquals(1, GrowthCalculator.stars(20, 34))
        assertEquals(2, GrowthCalculator.stars(25, 34))
        assertEquals(3, GrowthCalculator.stars(30, 34))
    }

    @Test fun `thresholds scale with customized template`() {
        assertEquals(listOf(12, 15, 18), GrowthCalculator.starThresholds(20))
        assertEquals(3, GrowthCalculator.stars(18, 20))
    }

    @Test fun `empty report is safe`() {
        assertEquals(0, GrowthCalculator.totalScore(emptyList()))
        assertEquals(0, GrowthCalculator.totalMax(emptyList()))
        assertEquals(0f, GrowthCalculator.percent(0, 0))
    }

    @Test fun `report totals recompute from source rows`() {
        val rows = listOf(RecordSummaryRow(1, "2026-08-11", 30, 34, 2), RecordSummaryRow(2, "2026-08-12", 20, 34, 30))
        assertEquals(28, GrowthCalculator.totalScore(rows))
        assertEquals(68, GrowthCalculator.totalMax(rows))
    }
}
