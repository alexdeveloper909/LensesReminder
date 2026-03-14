package com.alex.lensesreminder.data.local.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalTime

/**
 * Type converters for date and time types persisted by Room.
 */
class RoomTypeConverters {

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localTimeToText(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun textToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)
}
