package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyPrimaryContainer,
    onPrimaryContainer = NavyOnContainer,
    secondary = WeeklyGreen,
    onSecondary = Color.White,
    secondaryContainer = WeeklyGreenSoft,
    onSecondaryContainer = WeeklyGreenDark,
    tertiary = MonthlyPurple,
    onTertiary = Color.White,
    tertiaryContainer = MonthlyPurpleSoft,
    onTertiaryContainer = MonthlyPurpleDark,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFE8EEF7),
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    error = AbsentCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = NavyPrimaryDark,
    primaryContainer = NavyPrimary,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF6EE7B7),
    onSecondary = Color(0xFF064E3B),
    tertiary = Color(0xFFC4B5FD),
    onTertiary = Color(0xFF2E1065),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun ParivarEmsTheme(
    darkTheme: Boolean = false,
    isHindi: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildAppTypography(isHindi = isHindi),
        shapes = AppShapes,
        content = content
    )
}
