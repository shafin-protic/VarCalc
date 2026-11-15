package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OledDarkColorScheme = darkColorScheme(
  primary = AccentCyan,
  onPrimary = Color(0xFF002026),
  primaryContainer = Color(0xFF0C2B33),
  onPrimaryContainer = AccentCyan,
  secondary = AccentEmerald,
  onSecondary = Color(0xFF002116),
  secondaryContainer = Color(0xFF0B2B20),
  onSecondaryContainer = AccentEmerald,
  tertiary = AccentViolet,
  onTertiary = Color(0xFF1E1038),
  tertiaryContainer = Color(0xFF211738),
  onTertiaryContainer = AccentViolet,
  error = AccentRose,
  onError = Color(0xFF330611),
  errorContainer = Color(0xFF2E0B14),
  onErrorContainer = AccentRose,
  background = OledBlack,
  onBackground = OledTextPrimary,
  surface = OledSurface,
  onSurface = OledTextPrimary,
  surfaceVariant = OledSurfaceElevated,
  onSurfaceVariant = OledTextSecondary,
  surfaceContainerLowest = OledBlack,
  surfaceContainerLow = OledSurface,
  surfaceContainer = OledSurfaceElevated,
  surfaceContainerHigh = OledSurfaceHigh,
  surfaceContainerHighest = Color(0xFF212633),
  outline = Color(0xFF1F2430),
  outlineVariant = Color(0xFF151922),
)

@Composable
fun VarCalcTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = OledDarkColorScheme,
    typography = AppTypography,
    content = content,
  )
}
