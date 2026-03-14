package com.alex.lensesreminder.data.repository

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.testutil.FakeLensProfileDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LensProfileRepositoryTest {

    @Test
    fun `profile emits default value when storage is empty`() = runTest {
        val repository = LensProfileRepository(FakeLensProfileDao())

        assertEquals(LensProfile(), repository.profile.first())
    }

    @Test
    fun `save profile persists new profile`() = runTest {
        val repository = LensProfileRepository(FakeLensProfileDao())
        val expected = LensProfile(maxWearMinutes = 900, remindersEnabled = false)

        repository.saveProfile(expected)

        assertEquals(expected, repository.profile.first())
    }
}
