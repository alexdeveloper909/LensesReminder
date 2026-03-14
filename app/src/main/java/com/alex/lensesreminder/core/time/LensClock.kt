package com.alex.lensesreminder.core.time

import java.time.Instant
import java.time.ZoneId

/**
 * Clock abstraction used to keep time-based logic testable.
 */
interface LensClock {
    fun now(): Instant
    fun zoneId(): ZoneId
}
