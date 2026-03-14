package com.alex.lensesreminder.data.local.db

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.LensType
import com.alex.lensesreminder.core.model.SessionSource
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {

    @Test
    fun `lens profile maps to entity and back`() {
        val profile = LensProfile(
            id = 7L,
            lensType = LensType.DAILY,
            maxWearMinutes = 840,
            remindersEnabled = false,
            finalAlertTime = LocalTime.of(23, 15),
            repeatReminderMinutes = 20
        )

        val mappedBack = profile.toEntity().toDomain()

        assertEquals(profile, mappedBack)
    }

    @Test
    fun `wear session maps to entity and back`() {
        val session = WearSession(
            id = 11L,
            plannedStartAt = Instant.parse("2026-03-14T07:30:00Z"),
            actualStartAt = Instant.parse("2026-03-14T07:45:00Z"),
            expectedEndAt = Instant.parse("2026-03-14T19:45:00Z"),
            completedAt = Instant.parse("2026-03-14T18:00:00Z"),
            status = SessionStatus.COMPLETED,
            source = SessionSource.CORRECTED,
            finalAlertScheduledFor = Instant.parse("2026-03-14T22:00:00Z"),
            finalAlertSentAt = Instant.parse("2026-03-14T22:00:00Z"),
            lastReminderSentAt = Instant.parse("2026-03-14T18:15:00Z"),
            reminderCount = 3
        )

        val mappedBack = session.toEntity().toDomain()

        assertEquals(session, mappedBack)
    }
}
