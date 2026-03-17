package com.alex.lensesreminder.feature.home

import com.alex.lensesreminder.core.model.SessionStatus
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenDisplayStateTest {

    @Test
    fun `display state uses final alert when it is earlier than wear end`() {
        val currentTime = Instant.parse("2026-03-17T17:00:00Z")
        val expectedEndAt = Instant.parse("2026-03-18T03:00:00Z")
        val finalAlertAt = Instant.parse("2026-03-17T22:30:00Z")

        val displayState = HomeSessionUiState(
            status = SessionStatus.ACTIVE,
            actualStartAt = Instant.parse("2026-03-17T16:00:00Z"),
            expectedEndAt = expectedEndAt,
            finalAlertScheduledFor = finalAlertAt
        ).toDisplayState(currentTime)

        assertEquals(SessionStatus.ACTIVE, displayState.status)
        assertEquals(finalAlertAt, displayState.expectedEndAt)
        assertEquals(finalAlertAt, displayState.effectiveDeadlineAt)
        assertEquals(Duration.ofHours(5).plusMinutes(30), displayState.remaining)
    }

    @Test
    fun `display state uses wear end when it is earlier than final alert`() {
        val currentTime = Instant.parse("2026-03-17T17:00:00Z")
        val expectedEndAt = Instant.parse("2026-03-17T20:00:00Z")
        val finalAlertAt = Instant.parse("2026-03-17T22:30:00Z")

        val displayState = HomeSessionUiState(
            status = SessionStatus.ACTIVE,
            actualStartAt = Instant.parse("2026-03-17T10:00:00Z"),
            expectedEndAt = expectedEndAt,
            finalAlertScheduledFor = finalAlertAt
        ).toDisplayState(currentTime)

        assertEquals(SessionStatus.ACTIVE, displayState.status)
        assertEquals(expectedEndAt, displayState.expectedEndAt)
        assertEquals(expectedEndAt, displayState.effectiveDeadlineAt)
        assertEquals(Duration.ofHours(3), displayState.remaining)
    }

    @Test
    fun `display state becomes overdue when effective deadline passes`() {
        val currentTime = Instant.parse("2026-03-17T23:35:00Z")
        val expectedEndAt = Instant.parse("2026-03-18T03:00:00Z")
        val finalAlertAt = Instant.parse("2026-03-17T23:30:00Z")

        val displayState = HomeSessionUiState(
            status = SessionStatus.ACTIVE,
            actualStartAt = Instant.parse("2026-03-17T18:00:00Z"),
            expectedEndAt = expectedEndAt,
            finalAlertScheduledFor = finalAlertAt
        ).toDisplayState(currentTime)

        assertEquals(SessionStatus.OVERDUE, displayState.status)
        assertEquals(finalAlertAt, displayState.expectedEndAt)
        assertEquals(Duration.ofMinutes(5), displayState.overdueBy)
    }
}
