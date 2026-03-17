package com.alex.lensesreminder.domain.session

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.time.LensClock
import java.time.Instant

internal fun computeFinalAlertTime(
    actualStartAt: Instant,
    profile: LensProfile,
    clock: LensClock,
): Instant? {
    if (!profile.remindersEnabled) {
        return null
    }

    val zoneId = clock.zoneId()
    val startDate = actualStartAt.atZone(zoneId).toLocalDate()
    val candidate = startDate.atTime(profile.finalAlertTime).atZone(zoneId).toInstant()
    return candidate.takeIf { it.isAfter(actualStartAt) }
}
