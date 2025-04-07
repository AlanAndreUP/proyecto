package com.actividad1.rutasegura.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


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