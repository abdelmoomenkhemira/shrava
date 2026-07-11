package com.example.shrava.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = TextOnAccent,
    primaryContainer = DarkSurface,
    onPrimaryContainer = TextPrimary,
    secondary = AccentBlue,
    onSecondary = TextOnAccent,
    secondaryContainer = DarkCard,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentOrange,
    onTertiary = TextOnAccent,
    tertiaryContainer = DarkCard,
    onTertiaryContainer = TextPrimary,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = TextOnAccent,
    outline = TextMuted
)

private val ShravaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ShravaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = ShravaTypography,
        shapes = ShravaShapes,
        content = content
    )
}
