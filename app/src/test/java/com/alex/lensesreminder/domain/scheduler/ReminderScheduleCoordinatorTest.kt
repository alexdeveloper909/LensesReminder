package com.alex.lensesreminder.domain.scheduler

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.testutil.FakeLensClock
import com.alex.lensesreminder.testutil.FakeLensProfileDao
import com.alex.lensesreminder.testutil.FakeReminderAlarmScheduler
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderScheduleCoordinatorTest {

    @Test
    fun `active session schedules wear end and final alert`() = runTest {
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
        val scheduler = FakeReminderAlarmScheduler()
        val coordinator = ReminderScheduleCoordinator(profileRepository, scheduler, clock)

        coordinator.sync(
            WearSession(
                id = 7,
                actualStartAt = clock.now(),
                expectedEndAt = clock.now().plus(Duration.ofHours(10)),
                status = SessionStatus.ACTIVE,
                finalAlertScheduledFor = Instant.parse("2026-03-14T22:00:00Z")
            )
        )

        assertEquals(2, scheduler.scheduledAlarms.size)
        assertTrue(scheduler.scheduledAlarms.any { it.type == ReminderAlarmType.WEAR_END })
        assertTrue(scheduler.scheduledAlarms.any { it.type == ReminderAlarmType.FINAL_ALERT })
    }

    @Test
    fun `overdue session skips repeat when final alert comes first`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T19:00:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        profileRepository.saveProfile(LensProfile(repeatReminderMinutes = 15))
        val scheduler = FakeReminderAlarmScheduler()
        val coordinator = ReminderScheduleCoordinator(profileRepository, scheduler, clock)

        coordinator.sync(
            WearSession(
                id = 5,
                actualStartAt = clock.now().minus(Duration.ofHours(12)),
                expectedEndAt = clock.now().minus(Duration.ofMinutes(45)),
                status = SessionStatus.OVERDUE,
                finalAlertScheduledFor = clock.now().plus(Duration.ofMinutes(10)),
                lastReminderSentAt = clock.now(),
                reminderCount = 1
            )
        )

        assertEquals(1, scheduler.scheduledAlarms.size)
        assertEquals(ReminderAlarmType.FINAL_ALERT, scheduler.scheduledAlarms.single().type)
    }

    @Test
    fun `overdue session schedules final alert immediately when cutoff already passed`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T22:05:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        profileRepository.saveProfile(LensProfile(repeatReminderMinutes = 15))
        val scheduler = FakeReminderAlarmScheduler()
        val coordinator = ReminderScheduleCoordinator(profileRepository, scheduler, clock)

        coordinator.sync(
            WearSession(
                id = 6,
                actualStartAt = clock.now().minus(Duration.ofHours(12)),
                expectedEndAt = clock.now().minus(Duration.ofHours(2)),
                status = SessionStatus.OVERDUE,
                finalAlertScheduledFor = clock.now().minus(Duration.ofMinutes(5)),
                lastReminderSentAt = clock.now().minus(Duration.ofMinutes(15)),
                reminderCount = 2
            )
        )

        assertEquals(1, scheduler.scheduledAlarms.size)
        assertEquals(ReminderAlarmType.FINAL_ALERT, scheduler.scheduledAlarms.single().type)
        assertEquals(clock.now(), scheduler.scheduledAlarms.single().triggerAt)
    }
}
