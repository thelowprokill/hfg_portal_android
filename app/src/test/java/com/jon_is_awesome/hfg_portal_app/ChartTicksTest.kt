package com.jon_is_awesome.hfg_portal_app

import com.jon_is_awesome.hfg_portal_app.ui.budget.niceTicks
import com.jon_is_awesome.hfg_portal_app.ui.budget.tickLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartTicksTest {
    @Test
    fun ticksCoverRangeWithRoundSteps() {
        assertEquals(listOf(0.0, 5000.0, 10000.0, 15000.0), niceTicks(1234.0, 13800.0))
        assertEquals(listOf(-2000.0, 0.0, 2000.0, 4000.0, 6000.0), niceTicks(-1500.0, 5100.0))
        // Flat data still yields at least two distinct lines.
        val flat = niceTicks(100.0, 100.0)
        assertTrue(flat.size >= 2 && flat.first() < 100.0 && flat.last() > 100.0)
    }

    @Test
    fun labelsUseStepUnit() {
        assertEquals("$15k", tickLabel(15000.0, 5000.0))
        assertEquals("-$2k", tickLabel(-2000.0, 2000.0))
        assertEquals("$1,500k", tickLabel(1_500_000.0, 500_000.0))
        assertEquals("$3M", tickLabel(3_000_000.0, 1_000_000.0))
        assertEquals("$250", tickLabel(250.0, 50.0))
    }
}
