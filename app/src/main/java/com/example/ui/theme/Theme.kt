package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Moon Theme (Charcoal Black / OLED)
val CharcoalColorScheme = darkColorScheme(
    primary = CharcoalPrimary,
    onPrimary = CharcoalOnPrimary,
    primaryContainer = CharcoalPrimaryContainer,
    onPrimaryContainer = CharcoalOnPrimaryContainer,
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = TimestampFlagColor,
    background = CharcoalBackground,
    onBackground = CharcoalOnSurface,
    surface = CharcoalSurface,
    onSurface = CharcoalOnSurface,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = CharcoalOnSurfaceVariant
)

// Sun Theme (Clean Light / Crisp Slate Blue)
val WarmColorScheme = lightColorScheme(
    primary = WarmPrimary,
    onPrimary = WarmOnPrimary,
    primaryContainer = WarmPrimaryContainer,
    onPrimaryContainer = WarmOnPrimaryContainer,
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = TimestampFlagColor,
    background = WarmBackground,
    onBackground = WarmOnSurface,
    surface = WarmSurface,
    onSurface = WarmOnSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = WarmOnSurfaceVariant
)

@Composable
fun VoiceNotesTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CharcoalColorScheme else WarmColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

