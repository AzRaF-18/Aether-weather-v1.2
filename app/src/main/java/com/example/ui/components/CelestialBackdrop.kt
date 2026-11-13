package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.data.model.SkyPhase
import com.example.data.model.WeatherCondition
import com.example.ui.theme.AfterSunsetTop
import com.example.ui.theme.BeforeSunsetTop
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.NightCloudyBg
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SkyBlueMid
import com.example.ui.theme.SkyBlueTop
import com.example.ui.theme.SunCore
import com.example.ui.theme.SunRim
import com.example.ui.theme.SunsetOrange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DynamicSkyBackground(
  skyPhase: SkyPhase,
  modifier: Modifier = Modifier,
) {
  val (targetTop, targetMid, targetBottom) = skyGradientColors(skyPhase)

  val topColor by animateColorAsState(
    targetValue = targetTop,
    animationSpec = tween(durationMillis = 900),
    label = "skyTopColor",
  )
  val midColor by animateColorAsState(
    targetValue = targetMid,
    animationSpec = tween(durationMillis = 900),
    label = "skyMidColor",
  )
  val bottomColor by animateColorAsState(
    targetValue = targetBottom,
    animationSpec = tween(durationMillis = 900),
    label = "skyBottomColor",
  )

  val infiniteTransition = rememberInfiniteTransition(label = "starsAnim")
  val starTwinkle by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2600, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "starTwinkle",
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          0.0f to topColor,
          0.55f to midColor,
          1.0f to bottomColor,
        )
      )
  ) {
    if (skyPhase.hasStars) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val starCoords = listOf(
          0.08f to 0.08f, 0.22f to 0.14f, 0.38f to 0.06f, 0.55f to 0.12f,
          0.74f to 0.09f, 0.90f to 0.16f, 0.14f to 0.28f, 0.46f to 0.25f,
          0.66f to 0.31f, 0.84f to 0.27f, 0.29f to 0.37f, 0.93f to 0.41f,
        )
        starCoords.forEachIndexed { index, (rx, ry) ->
          val phaseAlpha = if (index % 2 == 0) starTwinkle else 1.25f - starTwinkle
          val radius = if (index % 3 == 0) 2.5f else 1.6f
          drawCircle(
            color = Color.White.copy(alpha = phaseAlpha.coerceIn(0.25f, 0.95f)),
            radius = radius,
            center = Offset(size.width * rx, size.height * ry),
          )
        }
      }
    }
  }
}

@Composable
fun CelestialHeroStage(
  skyPhase: SkyPhase,
  isDay: Boolean,
  condition: WeatherCondition,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "celestialMotion")

  val cloudProgress1 by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 24000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "cloud1",
  )
  val cloudProgress2 by infiniteTransition.animateFloat(
    initialValue = 0.38f,
    targetValue = 1.38f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 30000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "cloud2",
  )
  val cloudProgress3 by infiniteTransition.animateFloat(
    initialValue = 0.72f,
    targetValue = 1.72f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 27000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "cloud3",
  )
  val sunGlowPulse by infiniteTransition.animateFloat(
    initialValue = 0.90f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "sunPulse",
  )
  val sunBeamRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 26000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "sunBeamRotation",
  )
  val rainDropProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "rainDrop",
  )

  Canvas(modifier = modifier) {
    val celestialX = size.width - 48.dp.toPx()
    val celestialY = 42.dp.toPx()

    // 1. Celestial Body — shiny rotating beams ONLY in daytime (isDay && SKY_BLUE)
    when (skyPhase) {
      SkyPhase.SKY_BLUE -> {
        drawCartoonSun(
          center = Offset(celestialX, celestialY),
          radius = 23.dp.toPx(),
          pulse = sunGlowPulse,
          beamRotationDeg = sunBeamRotation,
          showShinyBeams = isDay,
        )
      }
      SkyPhase.BEFORE_SUNSET -> {
        drawCartoonSun(
          center = Offset(celestialX, celestialY),
          radius = 23.dp.toPx(),
          pulse = sunGlowPulse,
          beamRotationDeg = sunBeamRotation,
          showShinyBeams = false,
        )
      }
      SkyPhase.SUNSET -> {
        drawHalfSunSetting(
          center = Offset(celestialX, celestialY + 8.dp.toPx()),
          radius = 26.dp.toPx(),
          pulse = sunGlowPulse,
        )
      }
      SkyPhase.NIGHT_CLOUDY, SkyPhase.MIDNIGHT, SkyPhase.AFTER_SUNSET, SkyPhase.DEEP_NIGHT -> {
        val cutoutColor = skyGradientColors(skyPhase).first
        drawCrescentMoon(
          center = Offset(celestialX, celestialY),
          radius = 21.dp.toPx(),
          cutoutColor = cutoutColor,
        )
      }
    }

    // 2. Drifting Cartoon Clouds
    val cloudAlpha = if (condition.isCloudy || skyPhase == SkyPhase.NIGHT_CLOUDY) 0.90f else 0.78f
    val span = size.width + 180.dp.toPx()

    fun xFor(progress: Float): Float = ((progress % 1f) * span) - 90.dp.toPx()

    drawCartoonCloud(
      origin = Offset(xFor(cloudProgress1), 26.dp.toPx()),
      width = 68.dp.toPx(),
      height = 24.dp.toPx(),
      alpha = cloudAlpha,
    )
    drawCartoonCloud(
      origin = Offset(xFor(cloudProgress2), 62.dp.toPx()),
      width = 92.dp.toPx(),
      height = 30.dp.toPx(),
      alpha = cloudAlpha * 0.92f,
    )
    drawCartoonCloud(
      origin = Offset(xFor(cloudProgress3), 102.dp.toPx()),
      width = 58.dp.toPx(),
      height = 20.dp.toPx(),
      alpha = cloudAlpha * 0.85f,
    )
    if (skyPhase == SkyPhase.NIGHT_CLOUDY || condition.isCloudy) {
      drawCartoonCloud(
        origin = Offset(xFor(cloudProgress1 + 0.52f), 138.dp.toPx()),
        width = 76.dp.toPx(),
        height = 26.dp.toPx(),
        alpha = cloudAlpha * 0.75f,
      )
    }

    // 3. Subtle Rain Streaks when WMO code indicates precipitation
    if (condition.isRainy) {
      val dropColor = Color(0xFF9AD8F5).copy(alpha = 0.55f)
      val columns = 10
      for (i in 0 until columns) {
        val rx = (i + 0.5f) / columns.toFloat()
        val offsetPhase = (rainDropProgress + i * 0.17f) % 1f
        val startX = size.width * rx
        val startY = 30.dp.toPx() + offsetPhase * (size.height - 60.dp.toPx())
        drawLine(
          color = dropColor,
          start = Offset(startX, startY),
          end = Offset(startX - 4.dp.toPx(), startY + 14.dp.toPx()),
          strokeWidth = 2.dp.toPx(),
        )
      }
    }
  }
}

private fun skyGradientColors(phase: SkyPhase): Triple<Color, Color, Color> =
  when (phase) {
    SkyPhase.SKY_BLUE -> Triple(
      Color(0xFF7ED6FA),
      Color(0xFF2896F6),
      Color(0xFF1464D2),
    )
    SkyPhase.BEFORE_SUNSET -> Triple(
      Color(0xFFFDE047),
      Color(0xFFFB923C),
      Color(0xFFD9480F),
    )
    SkyPhase.SUNSET -> Triple(
      Color(0xFFEF4444),
      Color(0xFFF97316),
      Color(0xFF6D28D9),
    )
    SkyPhase.AFTER_SUNSET -> Triple(
      Color(0xFF1E296B),
      Color(0xFF16204A),
      Color(0xFF11193D),
    )
    SkyPhase.NIGHT_CLOUDY -> Triple(
      Color(0xFF334155),
      Color(0xFF243042),
      Color(0xFF1A2434),
    )
    SkyPhase.MIDNIGHT -> Triple(
      Color(0xFF0E1B4D),
      Color(0xFF0C163E),
      Color(0xFF0A1335),
    )
    SkyPhase.DEEP_NIGHT -> Triple(
      Color(0xFF0F1836),
      Color(0xFF0D1530),
      Color(0xFF0B1229),
    )
  }

private fun DrawScope.drawCartoonSun(
  center: Offset,
  radius: Float,
  pulse: Float,
  beamRotationDeg: Float,
  showShinyBeams: Boolean,
) {
  // Soft warm atmospheric halo
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        Color(0xFFFFF59D).copy(alpha = if (showShinyBeams) 0.72f else 0.55f),
        SunRim.copy(alpha = 0.22f),
        Color.Transparent,
      ),
      center = center,
      radius = radius * 2.55f * pulse,
    ),
    radius = radius * 2.55f * pulse,
    center = center,
  )

  // Shiny rotating cartoon beams ONLY in daytime
  if (showShinyBeams) {
    rotate(degrees = beamRotationDeg, pivot = center) {
      // Long sweeping shiny light cones
      val coneCount = 6
      for (i in 0 until coneCount) {
        val baseAngle = (i * (360f / coneCount)) * (PI / 180.0)
        val spread = 0.20
        val outerR = radius * 3.2f * pulse
        val conePath = Path().apply {
          moveTo(center.x, center.y)
          lineTo(
            center.x + (cos(baseAngle - spread) * outerR).toFloat(),
            center.y + (sin(baseAngle - spread) * outerR).toFloat(),
          )
          lineTo(
            center.x + (cos(baseAngle + spread) * outerR).toFloat(),
            center.y + (sin(baseAngle + spread) * outerR).toFloat(),
          )
          close()
        }
        drawPath(
          path = conePath,
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xFFFFF9C4).copy(alpha = 0.42f),
              Color(0xFFFFE600).copy(alpha = 0.14f),
              Color.Transparent,
            ),
            center = center,
            radius = outerR,
          ),
        )
      }

      // 12 crisp rounded cartoon sun rays around the perimeter
      val rayCount = 12
      for (i in 0 until rayCount) {
        val angleRad = (i * (360f / rayCount)) * (PI / 180.0)
        val isMajor = i % 2 == 0
        val innerDist = radius * 1.20f
        val outerDist = radius * (if (isMajor) 1.88f else 1.56f) * pulse
        val start = Offset(
          center.x + (cos(angleRad) * innerDist).toFloat(),
          center.y + (sin(angleRad) * innerDist).toFloat(),
        )
        val end = Offset(
          center.x + (cos(angleRad) * outerDist).toFloat(),
          center.y + (sin(angleRad) * outerDist).toFloat(),
        )
        // Warm cartoon outline behind each ray
        drawLine(
          color = Color(0xFFEA580C).copy(alpha = 0.85f),
          start = start,
          end = end,
          strokeWidth = if (isMajor) 6.5.dp.toPx() else 5.dp.toPx(),
          cap = StrokeCap.Round,
        )
        // Bright shiny yellow-white ray core
        drawLine(
          brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFF176), Color(0xFFFFB300)),
            start = start,
            end = end,
          ),
          start = start,
          end = end,
          strokeWidth = if (isMajor) 4.dp.toPx() else 2.8.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
    }
  }

  // Cartoony Sun Core with bold warm outline
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFFFFF55A), SunCore, Color(0xFFFF9100)),
      center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
      radius = radius * 1.35f,
    ),
    radius = radius,
    center = center,
  )
  drawCircle(
    color = Color(0xFFEA580C),
    radius = radius,
    center = center,
    style = Stroke(width = 2.2.dp.toPx()),
  )

  // Glossy cartoon specular highlight on top-left of sun
  drawArc(
    color = Color.White.copy(alpha = 0.72f),
    startAngle = 200f,
    sweepAngle = 72f,
    useCenter = false,
    topLeft = Offset(center.x - radius * 0.68f, center.y - radius * 0.68f),
    size = androidx.compose.ui.geometry.Size(radius * 1.36f, radius * 1.36f),
    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round),
  )
}

private fun DrawScope.drawHalfSunSetting(center: Offset, radius: Float, pulse: Float) {
  clipRect(
    left = center.x - radius * 2.2f,
    top = center.y - radius * 2.2f,
    right = center.x + radius * 2.2f,
    bottom = center.y,
  ) {
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(CrimsonRed.copy(alpha = 0.55f), Color.Transparent),
        center = center,
        radius = radius * 2.1f * pulse,
      ),
      radius = radius * 2.1f * pulse,
      center = center,
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFFFCC00), CrimsonRed),
        center = center,
        radius = radius,
      ),
      radius = radius,
      center = center,
    )
  }
}

private fun DrawScope.drawCrescentMoon(center: Offset, radius: Float, cutoutColor: Color) {
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
      center = center,
      radius = radius * 1.9f,
    ),
    radius = radius * 1.9f,
    center = center,
  )
  val crescentPath = Path().apply {
    addOval(
      androidx.compose.ui.geometry.Rect(
        left = center.x - radius,
        top = center.y - radius,
        right = center.x + radius,
        bottom = center.y + radius,
      )
    )
  }
  drawPath(path = crescentPath, color = Color(0xFFF4F4F4))
  drawCircle(
    color = cutoutColor,
    radius = radius * 0.92f,
    center = Offset(center.x + radius * 0.42f, center.y - radius * 0.24f),
  )
}

private fun DrawScope.drawCartoonCloud(
  origin: Offset,
  width: Float,
  height: Float,
  alpha: Float,
) {
  val cloudColor = Color.White.copy(alpha = alpha)
  drawRoundRect(
    color = cloudColor,
    topLeft = origin,
    size = androidx.compose.ui.geometry.Size(width, height),
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2f, height / 2f),
  )
  drawCircle(
    color = cloudColor,
    radius = height * 0.58f,
    center = Offset(origin.x + width * 0.30f, origin.y + height * 0.15f),
  )
  drawCircle(
    color = cloudColor,
    radius = height * 0.72f,
    center = Offset(origin.x + width * 0.62f, origin.y + height * 0.05f),
  )
}
