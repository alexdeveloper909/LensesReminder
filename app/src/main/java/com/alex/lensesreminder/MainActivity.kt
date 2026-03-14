package com.alex.lensesreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alex.lensesreminder.app.LensesReminderApp
import com.alex.lensesreminder.ui.theme.LensesReminderTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity hosting the Compose navigation shell for the MVP.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LensesReminderTheme {
                LensesReminderApp()
            }
        }
    }
}
