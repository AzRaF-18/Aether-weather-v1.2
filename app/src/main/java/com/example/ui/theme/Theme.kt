package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AetherDarkColorScheme = darkColorScheme(
  primary = CrimsonRed,
  onPrimary = PureWhite,
  primaryContainer = CrimsonRedDark,
  onPrimaryContainer = PureWhite,
  secondary = SolarYellow,
  onSecondary = DarkInk,
  tertiary = SkyBlueMid,
  onTertiary = PureWhite,
  background = OledBlack,
  onBackground = PureWhite,
  surface = GlassDarkSurface,
  onSurface = PureWhite,
  surfaceVariant = NightCloudyBg,
  onSurfaceVariant = SolarGoldSoft,
  outline = GlassBorder,
  error = CrimsonRedSoft,
  onError = PureWhite,
)

private val AetherShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AetherTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = AetherDarkColorScheme,
    typography = AetherTypography,
    shapes = AetherShapes,
    content = content,
  )
}
