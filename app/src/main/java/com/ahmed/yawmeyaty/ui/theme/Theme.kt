package com.ahmed.yawmeyaty.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D5A76),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE6EE),
    onPrimaryContainer = Color(0xFF23384C),
    secondary = Color(0xFF314B63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7EDF2),
    onSecondaryContainer = Color(0xFF23384C),
    tertiary = Color(0xFFCE8B5F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5E2D5),
    onTertiaryContainer = Color(0xFF5A3524),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF8F5EF),
    onBackground = Color(0xFF26323B),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF26323B),
    surfaceVariant = Color(0xFFE8E3DC),
    onSurfaceVariant = Color(0xFF5F6469),
    outline = Color(0xFF7A858E),
    outlineVariant = Color(0xFFD1CBC4),
    inverseSurface = Color(0xFF26323B),
    inverseOnSurface = Color(0xFFF7F2EC),
    inversePrimary = Color(0xFFB5CAE0)
)

@Composable
fun YawmeyatyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
