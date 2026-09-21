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
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442927),
    secondaryContainer = Color(0xFF32343D),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = TimestampFlagColor,
    background = CharcoalBackground,
    onBackground = CharcoalOnSurface,
    surface = CharcoalSurface,
    onSurface = CharcoalOnSurface,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = CharcoalOnSurfaceVariant
)

// Sun Theme (Warm Amber / Sepia Paper)
val WarmColorScheme = lightColorScheme(
    primary = WarmPrimary,
    onPrimary = WarmOnPrimary,
    primaryContainer = WarmPrimaryContainer,
    onPrimaryContainer = WarmOnPrimaryContainer,
    secondary = Color(0xFF8D5B4C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF380D03),
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

