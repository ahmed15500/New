package com.ahmed.yawmeyaty.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF087A5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F6E8),
    onPrimaryContainer = Color(0xFF043D32),
    secondary = Color(0xFF0B5A58),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F2F1),
    onSecondaryContainer = Color(0xFF063B3A),
    tertiary = Color(0xFFC89A3D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEBC0),
    onTertiaryContainer = Color(0xFF4A3400),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF7FBF9),
    onBackground = Color(0xFF14201C),
    surface = Color(0xFFFCFFFD),
    onSurface = Color(0xFF14201C),
    surfaceVariant = Color(0xFFE0EAE5),
    onSurfaceVariant = Color(0xFF3F4945),
    outline = Color(0xFF71807A),
    outlineVariant = Color(0xFFC0CEC7),
    inverseSurface = Color(0xFF29322E),
    inverseOnSurface = Color(0xFFEDF5F0),
    inversePrimary = Color(0xFF6EDBAF)
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