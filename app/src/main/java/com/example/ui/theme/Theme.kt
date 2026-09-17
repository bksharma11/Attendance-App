package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AttendXPrimary,
    onPrimary = AttendXOnPrimary,
    primaryContainer = AttendXPrimaryContainer,
    onPrimaryContainer = AttendXOnPrimaryContainer,
    secondary = AttendXSecondary,
    onSecondary = AttendXOnSecondary,
    secondaryContainer = AttendXSecondaryContainer,
    onSecondaryContainer = AttendXOnSecondaryContainer,
    tertiary = AttendXTertiary,
    onTertiary = AttendXOnTertiary,
    tertiaryContainer = AttendXTertiaryContainer,
    onTertiaryContainer = AttendXOnTertiaryContainer,
    background = AttendXBackground,
    onBackground = Color(0xFF0F172A),
    surface = AttendXSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = AttendXSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = AttendXOutline,
    outlineVariant = AttendXOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF1E3A8A),
    primaryContainer = Color(0xFF1E40AF),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF134E4A),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun AttendXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = AttendXTheme(darkTheme = darkTheme, content = content)
