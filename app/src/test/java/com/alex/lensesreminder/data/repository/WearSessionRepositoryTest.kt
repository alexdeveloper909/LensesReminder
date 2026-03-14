package com.alex.lensesreminder.data.repository

import com.alex.lensesreminder.core.model.SessionSource
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.testutil.FakeWearSessionDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
