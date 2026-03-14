package com.alex.lensesreminder.domain.session

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionSource
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.testutil.FakeLensClock
import com.alex.lensesreminder.testutil.FakeLensProfileDao
import com.alex.lensesreminder.testutil.FakeReminderAlarmScheduler
import com.alex.lensesreminder.testutil.FakeReminderNotificationPublisher
import com.alex.lensesreminder.testutil.FakeWearSessionDao
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionLifecycleManagerTest {

    @Test
    fun `start now creates active session with expected end and final alert`() = runTest {
        val clock = FakeLensClock(
            currentInstant = Instant.parse("2026-03-14T08:00:00Z"),
            currentZoneId = ZoneId.of("Europe/Madrid")
        )
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        profileRepository.saveProfile(
            LensProfile(
                maxWearMinutes = 600,
                remindersEnabled = true,
                finalAlertTime = LocalTime.of(23, 0)
            )
        )
        val repository = WearSessionRepository(FakeWearSessionDao())
        val reminderScheduler = FakeReminderAlarmScheduler()
        val reminderCoordinator = com.alex.lensesreminder.domain.scheduler.ReminderScheduleCoordinator(
            profileRepository,
            reminderScheduler,
            clock
        )
        val reminderPublisher = FakeReminderNotificationPublisher()
        val manager = SessionLifecycleManager(
            profileRepository,
            repository,
            reminderCoordinator,
            reminderPublisher,
            clock
        )

        val result = manager.startNow()

        assertTrue(result is SessionLifecycleResult.Success)
        val session = (result as SessionLifecycleResult.Success).value
        assertEquals(SessionStatus.ACTIVE, session.status)
        assertEquals(SessionSource.MANUAL_START, session.source)
        assertEquals(clock.now(), session.actualStartAt)
        assertEquals(clock.now().plus(Duration.ofHours(10)), session.expectedEndAt)
        assertEquals(
            Instant.parse("2026-03-14T22:00:00Z"),
            session.finalAlertScheduledFor
        )
        assertEquals(2, reminderScheduler.scheduledAlarms.size)
    }

    @Test
    fun `save planned session updates existing planned session`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val repository = WearSessionRepository(FakeWearSessionDao())
        val reminderScheduler = FakeReminderAlarmScheduler()
        val manager = SessionLifecycleManager(
            profileRepository,
            repository,
            com.alex.lensesreminder.domain.scheduler.ReminderScheduleCoordinator(
                profileRepository,
                reminderScheduler,
                clock
            ),
            FakeReminderNotificationPublisher(),
            clock
        )

        val firstPlan = (manager.savePlannedSession(clock.now().plus(Duration.ofHours(2)))
            as SessionLifecycleResult.Success).value

        val updatedPlan = manager.savePlannedSession(clock.now().plus(Duration.ofHours(4)))

        assertTrue(updatedPlan is SessionLifecycleResult.Success)
        val savedSession = (updatedPlan as SessionLifecycleResult.Success).value
        assertEquals(firstPlan.id, savedSession.id)
        assertEquals(SessionStatus.PLANNED, savedSession.status)
        assertEquals(clock.now().plus(Duration.ofHours(4)), savedSession.plannedStartAt)
        assertEquals(1, reminderScheduler.scheduledAlarms.size)
    }

    @Test
    fun `activate planned session turns it active`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        profileRepository.saveProfile(LensProfile(maxWearMinutes = 480))
        val repository = WearSessionRepository(FakeWearSessionDao())
        val manager = SessionLifecycleManager(
            profileRepository,
            repository,
            com.alex.lensesreminder.domain.scheduler.ReminderScheduleCoordinator(
                profileRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            FakeReminderNotificationPublisher(),
            clock
        )
        manager.savePlannedSession(clock.now().plus(Duration.ofHours(1)))

        val result = manager.activatePlannedSession()

        assertTrue(result is SessionLifecycleResult.Success)
        val session = (result as SessionLifecycleResult.Success).value
        assertEquals(SessionStatus.ACTIVE, session.status)
        assertEquals(clock.now().plus(Duration.ofHours(8)), session.expectedEndAt)
    }

    @Test
    fun `complete current session marks it completed`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val repository = WearSessionRepository(FakeWearSessionDao())
        val reminderPublisher = FakeReminderNotificationPublisher()
        val manager = SessionLifecycleManager(
            profileRepository,
            repository,
            com.alex.lensesreminder.domain.scheduler.ReminderScheduleCoordinator(
                profileRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            reminderPublisher,
            clock
        )
        repository.saveSession(
            WearSession(
                actualStartAt = clock.now().minus(Duration.ofHours(1)),
                expectedEndAt = clock.now().plus(Duration.ofHours(7)),
                status = SessionStatus.ACTIVE
            )
        )

        val result = manager.completeCurrentSession()

        assertTrue(result is SessionLifecycleResult.Success)
        assertNull(repository.getCurrentSession())
        assertEquals(listOf(1L), reminderPublisher.cancelledSessionIds)
    }

    @Test
    fun `refresh current session status marks expired active session overdue`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T12:00:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val repository = WearSessionRepository(FakeWearSessionDao())
        val reminderScheduler = FakeReminderAlarmScheduler()
        val manager = SessionLifecycleManager(
            profileRepository,
            repository,
            com.alex.lensesreminder.domain.scheduler.ReminderScheduleCoordinator(
                profileRepository,
                reminderScheduler,
                clock
            ),
            FakeReminderNotificationPublisher(),
            clock
        )
        repository.saveSession(
            WearSession(
                actualStartAt = clock.now().minus(Duration.ofHours(10)),
                expectedEndAt = clock.now().minus(Duration.ofMinutes(30)),
                status = SessionStatus.ACTIVE
            )
        )

        val refreshed = manager.refreshCurrentSessionStatus()

        assertEquals(SessionStatus.OVERDUE, refreshed?.status)
        assertEquals(SessionStatus.OVERDUE, repository.getCurrentSession()?.status)
        assertEquals(
            com.alex.lensesreminder.domain.scheduler.ReminderAlarmType.WEAR_END,
            reminderScheduler.scheduledAlarms.single().type
        )
    }

    @Test
    fun `snooze planned session moves reminder 15 minutes from now`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val repository = WearSessionRepository(FakeWearSessionDao())
        val reminderScheduler = FakeReminderAlarmScheduler()
        val manager = SessionLifecycleManager(
            profileRepository,
            repository,
            com.alex.lensesreminder.domain.scheduler.ReminderScheduleCoordinator(
                profileRepository,
                reminderScheduler,
                clock
            ),
            FakeReminderNotificationPublisher(),
            clock
        )
        val plannedSessionId = repository.saveSession(
            WearSession(
                plannedStartAt = clock.now(),
                status = SessionStatus.PLANNED
            )
        )

        val result = manager.snoozePlannedSession(plannedSessionId)

        assertTrue(result is SessionLifecycleResult.Success)
        val snoozedSession = (result as SessionLifecycleResult.Success).value
        assertEquals(clock.now().plus(Duration.ofMinutes(15)), snoozedSession.plannedStartAt)
        assertEquals(clock.now().plus(Duration.ofMinutes(15)), reminderScheduler.scheduledAlarms.single().triggerAt)
    }
}
