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

// Muted, aesthetic accent colors
val IncomeGreen = Color(0xFF4CAF7C)        // Sage green
val ExpenseRed = Color(0xFFE57373)         // Soft coral red
val WarningAmber = Color(0xFFFFB966)       // Warm amber

// Soft gradient colors for cards
val GradientStart = Color(0xFF6B7FD7)      // Muted periwinkle blue
val GradientEnd = Color(0xFF8B7DC7)        // Soft lavender purple

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6B7FD7),           // Muted periwinkle
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF0F9),
    onPrimaryContainer = Color(0xFF3D4A7A),
    secondary = Color(0xFF5DA39B),         // Muted teal
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2F0),
    onSecondaryContainer = Color(0xFF2D5954),
    tertiary = Color(0xFF8B7DC7),          // Soft lavender
    background = Color(0xFFF9FAFB),        // Warm off-white
    onBackground = Color(0xFF2D3142),
    surface = Color.White,
    onSurface = Color(0xFF2D3142),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF6B7280)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9BA8E5),           // Lighter periwinkle for dark
    onPrimary = Color(0xFF2D3A6A),
    primaryContainer = Color(0xFF4A5899),
    onPrimaryContainer = Color(0xFFD9DDEF),
    secondary = Color(0xFF7EC4BB),         // Soft teal for dark
    onSecondary = Color(0xFF1A3D3A),
    secondaryContainer = Color(0xFF3D6B66),
    onSecondaryContainer = Color(0xFFD5EBE8),
    tertiary = Color(0xFFB5A8E0),          // Soft lavender for dark
    background = Color(0xFF1A1B23),        // Warm dark
    onBackground = Color(0xFFE8E9ED),
    surface = Color(0xFF22242E),
    onSurface = Color(0xFFE8E9ED),
    surfaceVariant = Color(0xFF2E3140),
    onSurfaceVariant = Color(0xFFA0A4B0)
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
