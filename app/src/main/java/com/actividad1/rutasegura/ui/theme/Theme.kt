package com.actividad1.rutasegura.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Esquema de colores oscuro
private val DarkColorScheme = darkColorScheme(
    primary = Teal500,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Teal700,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Amber500,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Amber700,
    onSecondaryContainer = Color(0xFF000000),
    tertiary = NeonPink,
    onTertiary = Color(0xFFFFFFFF),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = MediumBackground,
    onSurface = TextPrimary,
    error = Error,
    onError = Color(0xFFFFFFFF)
)

// Esquema de colores claro
private val LightColorScheme = lightColorScheme(
    primary = Teal700,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Teal500,
    onPrimaryContainer = Color(0xFF000000),
    secondary = Amber700,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Amber500,
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Coral,
    onTertiary = Color(0xFFFFFFFF),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    error = Error,
    onError = Color(0xFFFFFFFF)
)

@Composable
fun AppTheme(
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

    // Opcional: Configurar la barra de estado y navegación
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

// Tema adicional con acento de color eléctrico
private val ElectricDarkScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = ElectricBlue,
    secondary = Coral,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF5C2800),
    onSecondaryContainer = Coral,
    tertiary = Purple80,
    onTertiary = Color(0xFF000000),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = MediumBackground,
    onSurface = TextPrimary
)

@Composable
fun ElectricTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = ElectricDarkScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}