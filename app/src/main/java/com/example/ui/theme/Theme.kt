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

private val DarkColorScheme =
  darkColorScheme(
    primary = HighContrastWhite,
    secondary = PremiumGrayLight,
    tertiary = SilverSilver,
    background = ObsidianBlack,
    surface = PremiumGrayDark,
    onPrimary = ObsidianBlack,
    onSecondary = ObsidianBlack,
    onBackground = HighContrastWhite,
    onSurface = HighContrastWhite
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ObsidianBlack,
    secondary = PremiumGrayDark,
    tertiary = PremiumGrayMedium,
    background = HighContrastWhite,
    surface = PremiumGrayLight,
    onPrimary = HighContrastWhite,
    onSecondary = HighContrastWhite,
    onBackground = ObsidianBlack,
    onSurface = ObsidianBlack
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color so that our custom black & white design brand identity is strictly used
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
