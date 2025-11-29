package com.money.tracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Softer, easier on the eyes
val IncomeGreen = Color(0xFF66BB6A)
val ExpenseRed = Color(0xFFEF5350)
val WarningAmber = Color(0xFFFFB74D)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5C6BC0),          // Softer indigo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EAF6),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = Color(0xFF26A69A),         // Softer teal
    onSecondary = Color.White,
    tertiary = Color(0xFF7E57C2),          // Soft purple for gradients
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF212121),
    surface = Color.White,
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF546E7A)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7986CB),           // Softer indigo for dark
    onPrimary = Color(0xFF1A237E),
    primaryContainer = Color(0xFF3949AB),
    onPrimaryContainer = Color(0xFFC5CAE9),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color.Black,
    tertiary = Color(0xFF9575CD),          // Soft purple for gradients
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0BEC5)
)

@Composable
fun MoneyTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
