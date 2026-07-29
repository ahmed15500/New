package com.ahmed.yawmeyaty.ui.theme

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

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EDBAF),
    onPrimary = Color(0xFF003829),
    primaryContainer = Color(0xFF00513D),
    onPrimaryContainer = Color(0xFFD8F6E8),
    secondary = Color(0xFF9FDAD7),
    onSecondary = Color(0xFF003736),
    secondaryContainer = Color(0xFF164E4D),
    onSecondaryContainer = Color(0xFFD9F2F1),
    tertiary = Color(0xFFE7C16D),
    onTertiary = Color(0xFF3E2E00),
    tertiaryContainer = Color(0xFF594400),
    onTertiaryContainer = Color(0xFFFFEBC0),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDFE7E2),
    surface = Color(0xFF111A16),
    onSurface = Color(0xFFDFE7E2),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFC0CEC7),
    outline = Color(0xFF8A9992),
    outlineVariant = Color(0xFF3F4945)
)

@Composable
fun YawmeyatyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}