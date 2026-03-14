package com.alex.lensesreminder.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the single persisted lens profile.
 */
@Dao
interface LensProfileDao {

    @Query("SELECT * FROM lens_profiles WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<LensProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: LensProfileEntity)
}
