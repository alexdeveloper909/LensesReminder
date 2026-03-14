package com.alex.lensesreminder.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for reading and storing wear sessions.
 */
@Dao
interface WearSessionDao {

    @Query(
        """
        SELECT * FROM wear_sessions
        WHERE status IN ('PLANNED', 'ACTIVE', 'OVERDUE')
        ORDER BY COALESCE(actual_start_at_utc, planned_start_at_utc) DESC
        LIMIT 1
        """
    )
    fun observeCurrentSession(): Flow<WearSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: WearSessionEntity): Long
}
