package com.alex.lensesreminder.core.time

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun Instant?.format(
    zoneId: ZoneId,
    formatter: DateTimeFormatter,
): String = this?.atZone(zoneId)?.format(formatter).orEmpty()

internal fun Duration?.formatDuration(): String {
    if (this == null) {
        return "--"
    }

    return toMinutes().formatMinutesAsDuration()
}

internal fun Int.formatDuration(): String {
    return toLong().formatMinutesAsDuration()
}

private fun Long.formatMinutesAsDuration(): String {
    val hours = this / 60
    val minutes = this % 60
    val formatter = MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.SHORT)

    return if (hours > 0) {
        formatter.formatMeasures(
            Measure(hours, MeasureUnit.HOUR),
            Measure(minutes, MeasureUnit.MINUTE),
        )
    } else {
        formatter.format(Measure(minutes, MeasureUnit.MINUTE))
    }
}
