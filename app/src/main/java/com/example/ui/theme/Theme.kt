package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = InfoAccentDark,
    onPrimary = Color(0xFF082F49),
    primaryContainer = InfoContainerDark,
    onPrimaryContainer = InfoAccentDark,
    secondary = InfoAccentLight,
    onSecondary = DarkOledBackground,
    secondaryContainer = DarkOledSurfaceVariant,
    onSecondaryContainer = TextPrimaryDark,
    background = DarkOledBackground,
    onBackground = TextPrimaryDark,
    surface = DarkOledSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkOledSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkOledBorder,
    outlineVariant = DarkOledBorder,
    error = StatusFueraServicio
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = LightContainer,
    onPrimaryContainer = CobaltDark,
    secondary = CobaltDark,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = TextPrimaryLight,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Color(0xFFDC2626)
)

@Composable
fun ReportesExpressTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

