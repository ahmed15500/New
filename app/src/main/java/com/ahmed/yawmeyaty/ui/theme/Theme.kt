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
    primary = Color(0xFF176B45),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F2D1),
    onPrimaryContainer = Color(0xFF002113),
    secondary = Color(0xFF8A6717),
    secondaryContainer = Color(0xFFFFDEA1),
    background = Color(0xFFF7FBF7),
    surface = Color(0xFFF7FBF7),
    surfaceVariant = Color(0xFFDDE5DE)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD5B5),
    onPrimary = Color(0xFF003823),
    primaryContainer = Color(0xFF005234),
    secondary = Color(0xFFF4C96C),
    secondaryContainer = Color(0xFF684D00),
    background = Color(0xFF101512),
    surface = Color(0xFF101512),
    surfaceVariant = Color(0xFF404943)
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
