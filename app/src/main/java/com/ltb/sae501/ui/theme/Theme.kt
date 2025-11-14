package com.ltb.sae501.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme // Nécessaire si on utilise le défaut système

val IconGradientBrush = Brush.linearGradient(
    colors = listOf(IconGradientStart, IconGradientEnd)
)

val ButtonGradientBrush = Brush.horizontalGradient(
    colors = listOf(ButtonGradientStart, ButtonGradientEnd)
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    secondary = AccentBlue,
    tertiary = AccentPink,

    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFB0B0B0)
)

private val LightColorScheme = lightColorScheme(
    primary = AccentPurple,
    secondary = AccentBlue,

    // Couleurs de fond
    background = BackgroundLight,
    surface = SurfaceLight,

    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryGray
)

@Composable
fun SAE501Theme(
    darkTheme: Boolean,
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
        content = content
    )
}