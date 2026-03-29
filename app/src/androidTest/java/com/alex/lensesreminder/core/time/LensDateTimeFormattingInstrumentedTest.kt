package com.alex.lensesreminder.core.time

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LensDateTimeFormattingInstrumentedTest {

    @Test
    fun nullDurationFallsBackToPlaceholder() {
        assertEquals("--", (null as Duration?).formatDuration())
    }

    @Test
    fun minuteOnlyDurationUsesLocaleAwareFormatter() {
        withLocale(Locale.US) {
            val expected = MeasureFormat.getInstance(
                Locale.US,
                MeasureFormat.FormatWidth.SHORT
            ).format(Measure(45, MeasureUnit.MINUTE))

            assertEquals(expected, 45.formatDuration())
            assertEquals(expected, Duration.ofMinutes(45).formatDuration())
        }
    }

    @Test
    fun hourAndMinuteDurationUsesLocaleAwareFormatter() {
        withLocale(Locale.US) {
            val expected = MeasureFormat.getInstance(
                Locale.US,
                MeasureFormat.FormatWidth.SHORT
            ).formatMeasures(
                Measure(2, MeasureUnit.HOUR),
                Measure(15, MeasureUnit.MINUTE),
            )

            assertEquals(expected, 135.formatDuration())
            assertEquals(expected, Duration.ofMinutes(135).formatDuration())
        }
    }

    private fun withLocale(
        locale: Locale,
        block: () -> Unit,
    ) {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(locale)
            block()
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
