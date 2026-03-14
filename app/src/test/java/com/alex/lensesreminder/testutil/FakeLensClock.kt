package com.alex.lensesreminder.testutil

import com.alex.lensesreminder.core.time.LensClock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Mutable clock used by time-sensitive unit tests.
 */
class FakeLensClock(
    private var currentInstant: Instant,
    private val currentZoneId: ZoneId = ZoneId.of("Europe/Madrid"),
) : LensClock {
    override fun now(): Instant = currentInstant

    override fun zoneId(): ZoneId = currentZoneId

    fun setNow(
        instant: Instant,
    ) {
        currentInstant = instant
    }

    fun advanceBy(
        duration: Duration,
    ) {
        currentInstant = currentInstant.plus(duration)
    }
}
