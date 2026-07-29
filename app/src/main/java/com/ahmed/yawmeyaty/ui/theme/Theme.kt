package com.ahmed.yawmeyaty.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9F4E5),
    onPrimaryContainer = Color(0xFF00382F),
    secondary = Color(0xFF3D665D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC1EBE0),
    onSecondaryContainer = Color(0xFF163A33),
    tertiary = Color(0xFF486A2C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC9F0A4),
    onTertiaryContainer = Color(0xFF183800),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF6FBF8),
    onBackground = Color(0xFF17201D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17201D),
    surfaceVariant = Color(0xFFDCE8E3),
    onSurfaceVariant = Color(0xFF3E4945),
    outline = Color(0xFF6E7974),
    outlineVariant = Color(0xFFBEC9C4)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF72DBC1),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFF94F8DC),
    secondary = Color(0xFFA5CFC4),
    onSecondary = Color(0xFF0D3730),
    secondaryContainer = Color(0xFF254E47),
    onSecondaryContainer = Color(0xFFC1EBE0),
    tertiary = Color(0xFFADD28B),
    onTertiary = Color(0xFF213600),
    tertiaryContainer = Color(0xFF374F17),
    onTertiaryContainer = Color(0xFFC9F0A4),
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDDE5E1),
    surface = Color(0xFF111A17),
    onSurface = Color(0xFFDDE5E1),
    surfaceVariant = Color(0xFF3E4945),
    onSurfaceVariant = Color(0xFFBEC9C4),
    outline = Color(0xFF89938F)
)

@Composable
fun YawmeyatyTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
