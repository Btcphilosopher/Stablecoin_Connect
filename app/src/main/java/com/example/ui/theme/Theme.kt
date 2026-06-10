package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = TealPrimary,
    secondary = TealSecondary,
    tertiary = AccentYellowGold,
    background = ObsidianBg,
    surface = CardSurface,
    onPrimary = Color.White,
    onSecondary = ObsidianBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = AccentRed
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TealPrimary,
    secondary = CardSurface,
    tertiary = AccentYellowGold,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF0F172A),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    error = AccentRed
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to premium dark theme matching system terminal
  dynamicColor: Boolean = false, // Disable dynamic colors to maintain platform branding integrity
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
