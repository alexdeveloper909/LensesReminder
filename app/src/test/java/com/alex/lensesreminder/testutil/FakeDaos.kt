package com.alex.lensesreminder.testutil

import com.alex.lensesreminder.data.local.db.LensProfileDao
import com.alex.lensesreminder.data.local.db.LensProfileEntity
import com.alex.lensesreminder.data.local.db.WearSessionDao
import com.alex.lensesreminder.data.local.db.WearSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory DAO implementation used by repository and ViewModel unit tests.
 */
class FakeLensProfileDao : LensProfileDao {
    private val profileState = MutableStateFlow<LensProfileEntity?>(null)

    override fun observeProfile(): Flow<LensProfileEntity?> = profileState

    override suspend fun upsert(profile: LensProfileEntity) {
        profileState.value = profile
    }
}

/**
 * In-memory DAO implementation used by repository and ViewModel unit tests.
 */
class FakeWearSessionDao : WearSessionDao {
    private val sessionState = MutableStateFlow<WearSessionEntity?>(null)
    private var nextId = 1L

    override fun observeCurrentSession(): Flow<WearSessionEntity?> = sessionState

    override suspend fun upsert(session: WearSessionEntity): Long {
        val resolvedId = if (session.id == 0L) nextId++ else session.id
        sessionState.value = session.copy(id = resolvedId)
        return resolvedId
    }
}
