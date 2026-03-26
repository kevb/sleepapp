package com.sleepwithme.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF003A6B),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFE0E0E0),
    secondaryContainer = Color(0xFF1E1E1E),
    onSecondaryContainer = Color(0xFFCCCCCC),
)

@Composable
fun SleepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
