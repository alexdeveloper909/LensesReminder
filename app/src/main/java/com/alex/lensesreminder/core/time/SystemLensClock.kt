package com.alex.lensesreminder.core.time

import java.time.Instant
import java.time.ZoneId

/**
 * Production clock backed by the device wall clock.
 */
object SystemLensClock : LensClock {
    override fun now(): Instant = Instant.now()

    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}
