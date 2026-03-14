package com.alex.lensesreminder.domain.scheduler

import com.alex.lensesreminder.core.model.LensProfile
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderAlarmHandlerTest {

    @Test
    fun `wear end alarm marks session overdue and schedules repeat`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T18:00:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        profileRepository.saveProfile(LensProfile(repeatReminderMinutes = 15))
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val session = WearSession(
            actualStartAt = clock.now().minus(Duration.ofHours(10)),
            expectedEndAt = clock.now(),
            status = SessionStatus.ACTIVE,
            finalAlertScheduledFor = clock.now().plus(Duration.ofHours(3))
        )
        val sessionId = sessionRepository.saveSession(session)
        val scheduler = FakeReminderAlarmScheduler()
        val publisher = FakeReminderNotificationPublisher()
        val handler = ReminderAlarmHandler(
            profileRepository,
            sessionRepository,
            ReminderScheduleCoordinator(profileRepository, scheduler, clock),
            publisher,
            clock
        )

        handler.handle(sessionId, ReminderAlarmType.WEAR_END, clock.now())

        val updatedSession = sessionRepository.getCurrentSession()
        assertEquals(SessionStatus.OVERDUE, updatedSession?.status)
        assertEquals(1, updatedSession?.reminderCount)
        assertEquals(sessionId to ReminderAlarmType.WEAR_END, publisher.shownNotifications.single())
        assertTrue(scheduler.scheduledAlarms.any { it.type == ReminderAlarmType.OVERDUE_REPEAT })
    }

    @Test
    fun `overdue repeat promotes to final alert once final cutoff is reached`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T22:05:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        profileRepository.saveProfile(LensProfile(repeatReminderMinutes = 15))
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val session = WearSession(
            actualStartAt = clock.now().minus(Duration.ofHours(12)),
            expectedEndAt = clock.now().minus(Duration.ofHours(2)),
            status = SessionStatus.OVERDUE,
            finalAlertScheduledFor = clock.now().minus(Duration.ofMinutes(5)),
            lastReminderSentAt = clock.now().minus(Duration.ofMinutes(15)),
            reminderCount = 2
        )
        val sessionId = sessionRepository.saveSession(session)
        val scheduler = FakeReminderAlarmScheduler()
        val publisher = FakeReminderNotificationPublisher()
        val handler = ReminderAlarmHandler(
            profileRepository,
            sessionRepository,
            ReminderScheduleCoordinator(profileRepository, scheduler, clock),
            publisher,
            clock
        )

        handler.handle(
            sessionId,
            ReminderAlarmType.OVERDUE_REPEAT,
            clock.now()
        )

        val updatedSession = sessionRepository.getCurrentSession()
        assertEquals(clock.now(), updatedSession?.finalAlertSentAt)
        assertEquals(sessionId to ReminderAlarmType.FINAL_ALERT, publisher.shownNotifications.single())
        assertNull(scheduler.scheduledAlarms.singleOrNull { it.type == ReminderAlarmType.OVERDUE_REPEAT })
    }

    @Test
    fun `planned start alarm is idempotent`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T09:00:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val plannedSession = WearSession(
            plannedStartAt = clock.now(),
            status = SessionStatus.PLANNED
        )
        val sessionId = sessionRepository.saveSession(plannedSession)
        val scheduler = FakeReminderAlarmScheduler()
        val publisher = FakeReminderNotificationPublisher()
        val handler = ReminderAlarmHandler(
            profileRepository,
            sessionRepository,
            ReminderScheduleCoordinator(profileRepository, scheduler, clock),
            publisher,
            clock
        )

        handler.handle(sessionId, ReminderAlarmType.PLANNED_START, clock.now())
        handler.handle(sessionId, ReminderAlarmType.PLANNED_START, clock.now())

        assertEquals(1, publisher.shownNotifications.size)
        assertEquals(clock.now(), sessionRepository.getCurrentSession()?.lastReminderSentAt)
    }
}
