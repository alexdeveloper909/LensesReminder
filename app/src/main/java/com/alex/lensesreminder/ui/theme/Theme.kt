package com.alex.lensesreminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = SlateBlue40,
    onSecondary = Color.White,
    secondaryContainer = SlateBlue90,
    onSecondaryContainer = SlateBlue10,
    tertiary = Amber40,
    onTertiary = Color.White,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    error = CoralRed40,
    onError = Color.White,
    errorContainer = CoralRed90,
    onErrorContainer = CoralRed10,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Color.White,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal10,
    primaryContainer = Teal20,
    onPrimaryContainer = Teal90,
    secondary = SlateBlue80,
    onSecondary = SlateBlue10,
    secondaryContainer = SlateBlue20,
    onSecondaryContainer = SlateBlue90,
    tertiary = Amber80,
    onTertiary = Amber10,
    tertiaryContainer = Amber20,
    onTertiaryContainer = Amber90,
    error = CoralRed80,
    onError = CoralRed20,
    errorContainer = CoralRed30,
    onErrorContainer = CoralRed90,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral12,
    onSurface = Neutral90,
    surfaceVariant = Neutral17,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
)

@Composable
fun LensesReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
