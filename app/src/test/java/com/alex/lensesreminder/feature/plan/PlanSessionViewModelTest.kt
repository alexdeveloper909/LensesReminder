package com.alex.lensesreminder.feature.plan

import app.cash.turbine.test
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.domain.scheduler.DailyStartReminderCoordinator
import com.alex.lensesreminder.domain.scheduler.ReminderScheduleCoordinator
import com.alex.lensesreminder.domain.session.SessionLifecycleManager
import com.alex.lensesreminder.testutil.FakeLensClock
import com.alex.lensesreminder.testutil.FakeLensProfileDao
import com.alex.lensesreminder.testutil.FakeReminderAlarmScheduler
import com.alex.lensesreminder.testutil.FakeReminderNotificationPublisher
import com.alex.lensesreminder.testutil.FakeWearSessionDao
import com.alex.lensesreminder.testutil.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `plan saved event is buffered until collector attaches`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
        val viewModel = PlanSessionViewModel(
            lensProfileRepository = profileRepository,
            wearSessionRepository = sessionRepository,
            sessionLifecycleManager = SessionLifecycleManager(
                profileRepository,
                sessionRepository,
                ReminderScheduleCoordinator(
                    profileRepository,
                    FakeReminderAlarmScheduler(),
                    clock
                ),
                DailyStartReminderCoordinator(
                    profileRepository,
                    sessionRepository,
                    FakeReminderAlarmScheduler(),
                    clock
                ),
                FakeReminderNotificationPublisher(),
                clock
            ),
            clock = clock
        )

        advanceUntilIdle()
        viewModel.savePlan()
        advanceUntilIdle()

        viewModel.events.test {
            assertEquals(PlanSessionEvent.PlanSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
