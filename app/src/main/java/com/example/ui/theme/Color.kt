package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val OledBlack = Color(0xFF000000)
val OledSurface = Color(0xFF0A0C10)
val OledSurfaceElevated = Color(0xFF11141B)
val OledSurfaceHigh = Color(0xFF181C26)

val OledKeyNumber = Color(0xFF11141B)
val OledKeyUtility = Color(0xFF161A23)
val OledKeyOperator = Color(0xFF182231)

val OledTextPrimary = Color(0xFFF8FAFC)
val OledTextSecondary = Color(0xFF94A3B8)
val OledTextMuted = Color(0xFF64748B)

val AccentCyan = Color(0xFF22D3EE)
val AccentEmerald = Color(0xFF34D399)
val AccentRose = Color(0xFFFB7185)
val AccentAmber = Color(0xFFFBBF24)
val AccentViolet = Color(0xFFA78BFA)
val AccentSky = Color(0xFF38BDF8)
val AccentLime = Color(0xFFA3E635)
val AccentCoral = Color(0xFFFB923C)

val VariablePalette: List<Color> = listOf(
  AccentCyan,
  AccentEmerald,
  AccentAmber,
  AccentViolet,
  AccentRose,
  AccentSky,
  AccentLime,
  AccentCoral,
)

fun variableColorAt(index: Int): Color {
  val safeIndex = ((index % VariablePalette.size) + VariablePalette.size) % VariablePalette.size
  return VariablePalette[safeIndex]
}
