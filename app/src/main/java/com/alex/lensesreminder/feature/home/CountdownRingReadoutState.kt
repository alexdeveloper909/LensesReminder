package com.alex.lensesreminder.feature.home

import java.time.Duration

internal data class CountdownRingMetric(
    val value: Long,
    val unit: CountdownRingUnit,
)

internal enum class CountdownRingUnit {
    HOURS,
    MINUTES,
}

internal fun Duration?.toCountdownRingMetrics(): List<CountdownRingMetric> {
    val totalMinutes = this?.toMinutes()?.coerceAtLeast(0) ?: 0L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return buildList {
        if (hours > 0) {
            add(CountdownRingMetric(value = hours, unit = CountdownRingUnit.HOURS))
            add(CountdownRingMetric(value = minutes, unit = CountdownRingUnit.MINUTES))
        } else {
            add(CountdownRingMetric(value = totalMinutes, unit = CountdownRingUnit.MINUTES))
        }
    }
}
