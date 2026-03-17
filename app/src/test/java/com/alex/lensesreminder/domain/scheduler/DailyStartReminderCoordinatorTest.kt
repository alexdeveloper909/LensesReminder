package com.alex.lensesreminder.domain.scheduler

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.testutil.FakeLensClock
import com.alex.lensesreminder.testutil.FakeLensProfileDao
import com.alex.lensesreminder.testutil.FakeReminderAlarmScheduler
import com.alex.lensesreminder.testutil.FakeWearSessionDao
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyStartReminderCoordinatorTest {

    @Test
    fun `sync schedules today's daily reminder when time is still ahead`() = runTest {
        val clock = FakeLensClock(
            currentInstant = Instant.parse("2026-03-14T06:00:00Z"),
            currentZoneId = ZoneId.of("Europe/Madrid")
        )
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        profileRepository.saveProfile(LensProfile(dailyStartReminderTime = LocalTime.of(8, 30)))
        val scheduler = FakeReminderAlarmScheduler()
        val coordinator = DailyStartReminderCoordinator(
            profileRepository,
            WearSessionRepository(FakeWearSessionDao()),
            scheduler,
            clock
        )

        coordinator.sync()

        assertEquals(1, scheduler.scheduledAlarms.size)
        assertEquals(ReminderAlarmType.DAILY_START, scheduler.scheduledAlarms.single().type)
        assertEquals(Instant.parse("2026-03-14T07:30:00Z"), scheduler.scheduledAlarms.single().triggerAt)
    }

    @Test
    fun `sync moves daily reminder to tomorrow when a session is already open`() = runTest {
        val clock = FakeLensClock(
            currentInstant = Instant.parse("2026-03-14T06:00:00Z"),
            currentZoneId = ZoneId.of("Europe/Madrid")
        )
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        profileRepository.saveProfile(LensProfile(dailyStartReminderTime = LocalTime.of(8, 30)))
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        sessionRepository.saveSession(
            WearSession(
                actualStartAt = clock.now().minus(Duration.ofHours(1)),
                expectedEndAt = clock.now().plus(Duration.ofHours(10)),
                status = SessionStatus.ACTIVE
            )
        )
        val scheduler = FakeReminderAlarmScheduler()
        val coordinator = DailyStartReminderCoordinator(
            profileRepository,
            sessionRepository,
            scheduler,
            clock
        )

        coordinator.sync()

        assertEquals(Instant.parse("2026-03-15T07:30:00Z"), scheduler.scheduledAlarms.single().triggerAt)
    }
}
