package com.alex.lensesreminder.data.repository

import com.alex.lensesreminder.core.model.SessionSource
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.testutil.FakeWearSessionDao
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearSessionRepositoryTest {

    @Test
    fun `current session is null when storage is empty`() = runTest {
        val repository = WearSessionRepository(FakeWearSessionDao())

        assertNull(repository.currentSession.first())
    }

    @Test
    fun `save session stores and returns assigned identifier`() = runTest {
        val repository = WearSessionRepository(FakeWearSessionDao())
        val session = WearSession(
            status = SessionStatus.ACTIVE,
            source = SessionSource.MANUAL_START
        )

        val id = repository.saveSession(session)

        assertEquals(1L, id)
        assertEquals(session.copy(id = id), repository.currentSession.first())
    }

    @Test
    fun `get current session returns the open session snapshot`() = runTest {
        val repository = WearSessionRepository(FakeWearSessionDao())
        val session = WearSession(
            status = SessionStatus.PLANNED,
            source = SessionSource.PLANNED
        )

        val id = repository.saveSession(session)

        assertEquals(session.copy(id = id), repository.getCurrentSession())
    }

    @Test
    fun `stale wear end transition cannot overwrite completed session`() = runTest {
        val repository = WearSessionRepository(FakeWearSessionDao())
        val expectedEndAt = Instant.parse("2026-03-14T18:00:00Z")
        val id = repository.saveSession(
            WearSession(
                expectedEndAt = expectedEndAt,
                status = SessionStatus.ACTIVE,
                source = SessionSource.MANUAL_START
            )
        )

        assertTrue(
            repository.completeSession(
                sessionId = id,
                completedAt = Instant.parse("2026-03-14T18:01:00Z")
            )
        )

        assertFalse(
            repository.markWearEndTriggered(
                sessionId = id,
                expectedEndAt = expectedEndAt,
                reminderSentAt = Instant.parse("2026-03-14T18:02:00Z")
            )
        )
        assertEquals(SessionStatus.COMPLETED, repository.getSession(id)?.status)
        assertEquals(Instant.parse("2026-03-14T18:01:00Z"), repository.getSession(id)?.completedAt)
    }
}
