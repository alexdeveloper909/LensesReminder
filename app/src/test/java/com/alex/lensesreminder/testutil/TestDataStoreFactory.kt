package com.alex.lensesreminder.testutil

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.nio.file.Files
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Creates isolated DataStore instances for host-side unit tests.
 */
fun createTestPreferencesDataStore(): DataStore<Preferences> {
    val file = Files.createTempFile(
        "test-prefs-${UUID.randomUUID()}",
        ".preferences_pb"
    ).toFile().apply {
        deleteOnExit()
    }

    return PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        produceFile = { file }
    )
}
