package com.alex.lensesreminder.app.di

import android.app.AlarmManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.core.content.getSystemService
import com.alex.lensesreminder.core.notification.SystemReminderNotificationPublisher
import com.alex.lensesreminder.core.time.LensClock
import com.alex.lensesreminder.core.time.SystemLensClock
import com.alex.lensesreminder.data.local.db.LensProfileDao
import com.alex.lensesreminder.data.local.db.LensesReminderDatabase
import com.alex.lensesreminder.data.local.db.WearSessionDao
import com.alex.lensesreminder.domain.scheduler.AlarmManagerReminderScheduler
import com.alex.lensesreminder.domain.scheduler.ReminderAlarmScheduler
import com.alex.lensesreminder.domain.scheduler.ReminderNotificationPublisher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.datastore.preferences.core.PreferenceDataStoreFactory

private const val APP_PREFERENCES_FILE = "app_preferences.preferences_pb"
private const val DATABASE_NAME = "lenses-reminder.db"

/**
 * Application dependency graph for persistence and time abstractions.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLensClock(): LensClock = SystemLensClock

    @Provides
    @Singleton
    fun provideAlarmManager(
        @ApplicationContext context: Context,
    ): AlarmManager = requireNotNull(context.getSystemService<AlarmManager>()) {
        "AlarmManager is required for reminder scheduling."
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(APP_PREFERENCES_FILE) }
    )

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): LensesReminderDatabase = Room.databaseBuilder(
        context,
        LensesReminderDatabase::class.java,
        DATABASE_NAME
    ).build()

    @Provides
    fun provideLensProfileDao(
        database: LensesReminderDatabase,
    ): LensProfileDao = database.lensProfileDao()

    @Provides
    fun provideWearSessionDao(
        database: LensesReminderDatabase,
    ): WearSessionDao = database.wearSessionDao()

    @Provides
    @Singleton
    fun provideReminderAlarmScheduler(
        implementation: AlarmManagerReminderScheduler,
    ): ReminderAlarmScheduler = implementation

    @Provides
    @Singleton
    fun provideReminderNotificationPublisher(
        implementation: SystemReminderNotificationPublisher,
    ): ReminderNotificationPublisher = implementation
}
