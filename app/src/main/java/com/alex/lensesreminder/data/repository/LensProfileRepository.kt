package com.alex.lensesreminder.data.repository

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.data.local.db.LensProfileDao
import com.alex.lensesreminder.data.local.db.toDomain
import com.alex.lensesreminder.data.local.db.toEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository exposing the single active lens profile.
 */
@Singleton
class LensProfileRepository @Inject constructor(
    private val lensProfileDao: LensProfileDao,
) {

    val profile: Flow<LensProfile> = lensProfileDao.observeProfile()
        .map { entity -> entity?.toDomain() ?: LensProfile() }

    suspend fun saveProfile(profile: LensProfile) {
        lensProfileDao.upsert(profile.toEntity())
    }
}
