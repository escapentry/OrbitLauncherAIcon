package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val OrbitDarkColorScheme = darkColorScheme(
  primary = OrbitCyan,
  onPrimary = ObsidianVoid,
  primaryContainer = OrbitCyanContainer,
  onPrimaryContainer = TextPrimary,
  secondary = OrbitIndigo,
  onSecondary = ObsidianVoid,
  tertiary = OrbitMint,
  background = ObsidianVoid,
  onBackground = TextPrimary,
  surface = ObsidianSurface,
  onSurface = TextPrimary,
  surfaceVariant = ObsidianSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = OutlineSubtle,
  outlineVariant = OutlineSubtle,
  error = DangerCoral,
)

@Composable
fun OrbitTheme(
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val dynamic = dynamicDarkColorScheme(LocalContext.current)
      OrbitDarkColorScheme.copy(
        primary = dynamic.primary,
        primaryContainer = dynamic.primaryContainer,
        secondary = dynamic.secondary,
      )
    } else {
      OrbitDarkColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = OrbitTypography,
    content = content,
  )
}
