package com.dante.volguard

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD9A8),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF1B5E3A),
    onPrimaryContainer = Color(0xFFB7F2CB),
    secondaryContainer = Color(0xFF334B3B),
    onSecondaryContainer = Color(0xFFD6F0DD),
    background = Color(0xFF101411),
    onBackground = Color(0xFFE0E4DD),
    surface = Color(0xFF101411),
    onSurface = Color(0xFFE0E4DD),
    surfaceVariant = Color(0xFF2A302B),
    onSurfaceVariant = Color(0xFFC0C9C0),
    surfaceContainer = Color(0xFF1C211D),
    outline = Color(0xFF8A938A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6B45),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F2CB),
    onPrimaryContainer = Color(0xFF00210F),
    secondaryContainer = Color(0xFFD3E8D8),
    onSecondaryContainer = Color(0xFF0E1F14),
    background = Color(0xFFF6FBF4),
    onBackground = Color(0xFF181D19),
    surface = Color(0xFFF6FBF4),
    onSurface = Color(0xFF181D19),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF404941),
    surfaceContainer = Color(0xFFE9F0E8),
    outline = Color(0xFF717971),
)

@Composable
fun VolGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
