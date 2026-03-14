package com.alex.lensesreminder.data.repository

import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.local.db.WearSessionDao
import com.alex.lensesreminder.data.local.db.toDomain
import com.alex.lensesreminder.data.local.db.toEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for the current session and session history.
 */
@Singleton
class WearSessionRepository @Inject constructor(
    private val wearSessionDao: WearSessionDao,
) {

    val currentSession: Flow<WearSession?> = wearSessionDao.observeCurrentSession()
        .map { entity -> entity?.toDomain() }

    suspend fun saveSession(session: WearSession): Long = wearSessionDao.upsert(session.toEntity())
}
