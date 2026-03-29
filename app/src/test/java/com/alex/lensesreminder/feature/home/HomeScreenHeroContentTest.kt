package com.alex.lensesreminder.feature.home

import com.alex.lensesreminder.core.model.SessionStatus
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenHeroContentTest {

    @Test
    fun `hero content shows completion summary when session has cleared after completion`() {
        val heroContent = resolveSessionHeroContent(
            displaySession = DisplaySessionUiState(),
            completionSummary = HomeCompletionSummaryUiState(
                wearDuration = Duration.ofHours(8),
                removedOnTime = true,
            ),
            maxWearMinutes = 600,
        )

        assertEquals(
            SessionHeroContentModel.CompletionSummary(
                summary = HomeCompletionSummaryUiState(
                    wearDuration = Duration.ofHours(8),
                    removedOnTime = true,
                )
            ),
            heroContent,
        )
    }

    @Test
    fun `hero content falls back to idle when there is no open session or summary`() {
        val heroContent = resolveSessionHeroContent(
            displaySession = DisplaySessionUiState(),
            completionSummary = null,
            maxWearMinutes = 600,
        )

        assertEquals(SessionHeroContentModel.Idle, heroContent)
    }

    @Test
    fun `hero content preserves active state without changing its content key`() {
        val heroContent = resolveSessionHeroContent(
            displaySession = DisplaySessionUiState(
                status = SessionStatus.ACTIVE,
                remaining = Duration.ofHours(2),
            ),
            completionSummary = null,
            maxWearMinutes = 720,
        )

        assertEquals(
            SessionHeroContentModel.Active(
                displaySession = DisplaySessionUiState(
                    status = SessionStatus.ACTIVE,
                    remaining = Duration.ofHours(2),
                ),
                maxWearMinutes = 720,
            ),
            heroContent,
        )
        assertEquals(SessionHeroContentKey.ACTIVE, heroContent.key)
    }
}
