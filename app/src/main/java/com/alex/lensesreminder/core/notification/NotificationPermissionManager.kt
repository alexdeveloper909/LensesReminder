package com.alex.lensesreminder.core.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Helper for checking whether reminder notifications can currently be delivered.
 */
object NotificationPermissionManager {

    fun hasNotificationPermission(context: Context): Boolean {
        val notificationsEnabled = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        )?.areNotificationsEnabled() ?: true

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsEnabled && ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            notificationsEnabled
        }
    }
}
