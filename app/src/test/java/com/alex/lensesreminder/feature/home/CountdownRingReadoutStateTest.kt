package com.alex.lensesreminder.feature.home

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownRingReadoutStateTest {

    @Test
    fun `countdown metrics keep hours and minutes when duration is at least one hour`() {
        assertEquals(
            listOf(
                CountdownRingMetric(value = 11, unit = CountdownRingUnit.HOURS),
                CountdownRingMetric(value = 59, unit = CountdownRingUnit.MINUTES),
            ),
            Duration.ofHours(11).plusMinutes(59).toCountdownRingMetrics(),
        )
    }

    @Test
    fun `countdown metrics collapse to minutes when duration is under one hour`() {
        assertEquals(
            listOf(
                CountdownRingMetric(value = 42, unit = CountdownRingUnit.MINUTES),
            ),
            Duration.ofMinutes(42).toCountdownRingMetrics(),
        )
    }

    @Test
    fun `countdown metrics never expose negative time`() {
        assertEquals(
            listOf(
                CountdownRingMetric(value = 0, unit = CountdownRingUnit.MINUTES),
            ),
            Duration.ofMinutes(-5).toCountdownRingMetrics(),
        )
    }
}
