package com.example.ui.components.scenery

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SolarYellow

/**
 * Draws the atmospheric sky backdrop, optional 3D mountain/pine-tree horizon (matching toy diorama
 * miniature style), and the raised beveled 3D island plinth that holds the neighborhood street.
 */
fun DrawScope.draw3dDioramaBackdropAndPlinth(
  spec: CityDioramaSpec,
  isDay: Boolean,
  ambientPulse: Float,
  plinthTopY: Float,
  plinthBottomY: Float,
) {
  val w = size.width
  val h = size.height

  // 1. Soft atmospheric backdrop + glossy ground plane under the island plinth
  drawRect(
    brush = Brush.verticalGradient(
      colors = when {
        spec.isDusty && isDay -> listOf(
          Color(0xFFE7CFA8).copy(alpha = 0.25f),
          Color(0xFFD2AE7E).copy(alpha = 0.36f),
          Color(0xFFB58E5D).copy(alpha = 0.42f),
        )
        spec.isDusty && !isDay -> listOf(
          Color(0xFF1F1C2C).copy(alpha = 0.42f),
          Color(0xFF3A2A22).copy(alpha = 0.56f),
        )
        isDay -> listOf(
          Color(0xFFDCECFB).copy(alpha = 0.14f),
          Color(0xFFBDD8F5).copy(alpha = 0.26f),
          Color(0xFFCDE2F6).copy(alpha = 0.38f),
        )
        else -> listOf(
          Color(0xFF040A1E).copy(alpha = 0.36f),
          Color(0xFF0B1736).copy(alpha = 0.56f),
        )
      }
    )
  )

  // 2. Sculpted 3D green hill/mountain + conical toon pines & fluffy clouds in the far back
  if (spec.hasMountainBackdrop) {
    val peakX = w * 0.78f
    val peakY = h * 0.10f
    val hillBaseY = plinthTopY + 8.dp.toPx()
    val hillPath = Path().apply {
      moveTo(w * 0.50f, hillBaseY)
      quadraticTo(w * 0.66f, h * 0.16f, peakX, peakY)
      quadraticTo(w * 0.92f, h * 0.14f, w * 1.02f, hillBaseY)
      close()
    }
    drawPath(
      path = hillPath,
      brush = Brush.horizontalGradient(
        colors = if (isDay) {
          listOf(Color(0xFF7CB342), Color(0xFF558B2F))
        } else {
          listOf(Color(0xFF1E3A2F), Color(0xFF142820))
        },
        startX = w * 0.50f,
        endX = w,
      ),
    )
    // Snow-capped / sunlit crest
    val capPath = Path().apply {
      moveTo(peakX - 24.dp.toPx(), peakY + 18.dp.toPx())
      quadraticTo(peakX, peakY - 2.dp.toPx(), peakX + 26.dp.toPx(), peakY + 18.dp.toPx())
      lineTo(peakX + 12.dp.toPx(), peakY + 23.dp.toPx())
      lineTo(peakX, peakY + 17.dp.toPx())
      lineTo(peakX - 11.dp.toPx(), peakY + 22.dp.toPx())
      close()
    }
    drawPath(
      path = capPath,
      color = (if (isDay) PureWhite else Color(0xFFCBD5E1)).copy(alpha = 0.88f),
    )

    // Tiny sculpted pines along hill base
    for (p in 0..4) {
      val tx = w * (0.52f + p * 0.085f)
      draw3dPineTree(x = tx, baseY = plinthTopY + 3.dp.toPx(), scale = 0.85f + (p % 2) * 0.25f, isDay = isDay)
    }
  }

  // Fluffy 3D cartoon cloud gently floating in background sky
  val cloudX = w * (0.45f + 0.04f * ambientPulse)
  val cloudY = h * 0.15f
  val cloudTint = if (isDay) PureWhite.copy(alpha = 0.85f) else Color(0xFF94A3B8).copy(alpha = 0.28f)
  drawCircle(cloudTint, radius = 9.dp.toPx(), center = Offset(cloudX - 9.dp.toPx(), cloudY + 2.dp.toPx()))
  drawCircle(cloudTint, radius = 12.dp.toPx(), center = Offset(cloudX, cloudY))
  drawCircle(cloudTint, radius = 8.5.dp.toPx(), center = Offset(cloudX + 10.dp.toPx(), cloudY + 2.dp.toPx()))

  // 3. Raised 3D Diorama Island Plinth (rounded platform + thick beveled side edge + soft drop shadow)
  val marginX = 6.dp.toPx()
  val plinthW = w - marginX * 2f
  val plinthH = (plinthBottomY - plinthTopY).coerceAtLeast(30.dp.toPx())
  val thickness = 11.dp.toPx()
  val radius = 18.dp.toPx()

  // Ambient occlusion drop shadow beneath the 3D plinth
  drawRoundRect(
    color = Color(0xFF0F172A).copy(alpha = if (isDay) 0.22f else 0.45f),
    topLeft = Offset(marginX + 2.dp.toPx(), plinthTopY + thickness + 3.dp.toPx()),
    size = Size(plinthW - 4.dp.toPx(), plinthH),
    cornerRadius = CornerRadius(radius, radius),
  )

  // Thick extruded 3D base skirt/bevel
  val skirtColor = lerp(spec.plinthGrassColor, Color(0xFF0F172A), 0.48f)
  drawRoundRect(
    brush = Brush.horizontalGradient(
      colors = listOf(
        lerp(skirtColor, Color.White, 0.08f),
        skirtColor,
        lerp(skirtColor, Color.Black, 0.22f),
      ),
      startX = marginX,
      endX = marginX + plinthW,
    ),
    topLeft = Offset(marginX, plinthTopY + thickness),
    size = Size(plinthW, plinthH),
    cornerRadius = CornerRadius(radius, radius),
  )

  // Top surface deck of the 3D miniature island
  val deckLight = lerp(spec.plinthGrassColor, PureWhite, if (isDay) 0.16f else -0.25f)
  val deckDark = lerp(spec.plinthGrassColor, Color(0xFF0F172A), if (isDay) 0.12f else 0.48f)
  drawRoundRect(
    brush = Brush.verticalGradient(
      colors = listOf(deckLight, deckDark),
      startY = plinthTopY,
      endY = plinthBottomY,
    ),
    topLeft = Offset(marginX, plinthTopY),
    size = Size(plinthW, plinthH),
    cornerRadius = CornerRadius(radius, radius),
  )

  // Specular rim highlight along the top-front bevel of the 3D island
  drawRoundRect(
    color = Color.White.copy(alpha = if (isDay) 0.48f else 0.22f),
    topLeft = Offset(marginX, plinthTopY),
    size = Size(plinthW, plinthH),
    cornerRadius = CornerRadius(radius, radius),
    style = Stroke(width = 1.1.dp.toPx()),
  )
}

fun DrawScope.draw3dSignatureLandmark(
  style: CityArchitectureStyle,
  baseY: Float,
  isDay: Boolean,
  accentColor: Color,
) {
  val w = size.width
  val h = size.height
  val frontTint = if (isDay) Color(0xFF475569).copy(alpha = 0.48f) else Color(0xFF1E293B).copy(alpha = 0.72f)
  val sideTint = lerp(frontTint, Color.Black, 0.35f)

  when (style) {
    CityArchitectureStyle.NEW_YORK_TIMES_SQUARE -> {
      // Iconic stepped 3D Midtown spire + glowing One Times Square beacon behind buildings
      val cx = w * 0.50f
      val bw = 30.dp.toPx()
      val depthX = 12.dp.toPx()
      val depthY = 8.dp.toPx()
      val topY = h * 0.08f
      // Front face
      drawRect(frontTint, Offset(cx - bw / 2f, topY), Size(bw, baseY - topY))
      // 3D Right side wall
      val side = Path().apply {
        moveTo(cx + bw / 2f, topY)
        lineTo(cx + bw / 2f + depthX, topY - depthY)
        lineTo(cx + bw / 2f + depthX, baseY - depthY)
        lineTo(cx + bw / 2f, baseY)
        close()
      }
      drawPath(side, sideTint)
      // Spire antenna
      drawLine(
        color = accentColor.copy(alpha = 0.9f),
        start = Offset(cx, h * 0.01f),
        end = Offset(cx, topY),
        strokeWidth = 2.4.dp.toPx(),
      )
      drawCircle(
        color = Color(0xFF38BDF8).copy(alpha = 0.85f),
        radius = 2.5.dp.toPx(),
        center = Offset(cx, h * 0.015f),
      )
    }
    CityArchitectureStyle.NEW_YORK_BROOKLYN -> {
      val cx = w * 0.85f
      val topY = h * 0.14f
      val tw = 22.dp.toPx()
      drawRect(Color(0xFF78716C).copy(alpha = 0.75f), Offset(cx - tw / 2f, topY), Size(tw, baseY - topY))
      val side = Path().apply {
        moveTo(cx + tw / 2f, topY)
        lineTo(cx + tw / 2f + 9.dp.toPx(), topY - 6.dp.toPx())
        lineTo(cx + tw / 2f + 9.dp.toPx(), baseY - 6.dp.toPx())
        lineTo(cx + tw / 2f, baseY)
        close()
      }
      drawPath(side, Color(0xFF57534E).copy(alpha = 0.80f))
      drawLine(Color(0xFF94A3B8), Offset(cx, topY + 3.dp.toPx()), Offset(w * 0.56f, baseY * 0.62f), 1.5.dp.toPx())
      drawLine(Color(0xFF94A3B8), Offset(cx, topY + 3.dp.toPx()), Offset(w * 0.99f, baseY * 0.62f), 1.5.dp.toPx())
    }
    CityArchitectureStyle.LONDON_WESTMINSTER -> {
      val cx = w * 0.88f
      val topY = h * 0.16f
      val tw = 20.dp.toPx()
      drawRect(Color(0xFFD6C3A5), Offset(cx - tw / 2f, topY), Size(tw, baseY - topY))
      val side = Path().apply {
        moveTo(cx + tw / 2f, topY)
        lineTo(cx + tw / 2f + 8.dp.toPx(), topY - 5.dp.toPx())
        lineTo(cx + tw / 2f + 8.dp.toPx(), baseY - 5.dp.toPx())
        lineTo(cx + tw / 2f, baseY)
        close()
      }
      drawPath(side, Color(0xFFAE997B))
      val spire = Path().apply {
        moveTo(cx - tw / 2f - 1.dp.toPx(), topY)
        lineTo(cx + 2.dp.toPx(), h * 0.03f)
        lineTo(cx + tw / 2f + 8.dp.toPx(), topY - 5.dp.toPx())
        lineTo(cx + tw / 2f, topY)
        close()
      }
      drawPath(spire, Color(0xFF334155))
      drawCircle(SolarYellow, radius = 5.dp.toPx(), center = Offset(cx, topY + 12.dp.toPx()))
    }
    CityArchitectureStyle.PARIS_HAUSSMANN -> {
      val cx = w * 0.88f
      val tower = Path().apply {
        moveTo(cx - 17.dp.toPx(), baseY)
        lineTo(cx - 3.dp.toPx(), h * 0.14f)
        lineTo(cx, h * 0.03f)
        lineTo(cx + 3.dp.toPx(), h * 0.14f)
        lineTo(cx + 17.dp.toPx(), baseY)
        close()
      }
      drawPath(tower, Color(0xFF64748B).copy(alpha = 0.72f))
    }
    CityArchitectureStyle.TOKYO_SHIBUYA -> {
      val cx = w * 0.88f
      val tower = Path().apply {
        moveTo(cx - 14.dp.toPx(), baseY)
        lineTo(cx, h * 0.05f)
        lineTo(cx + 14.dp.toPx(), baseY)
        close()
      }
      drawPath(tower, Color(0xFFEF4444).copy(alpha = 0.72f))
    }
    CityArchitectureStyle.CAIRO_SANDSTONE -> {
      // 3D sunlit Great Pyramids in golden desert backdrop
      val px = w * 0.78f
      val pyTop = h * 0.14f
      val pyBase = baseY - 4.dp.toPx()
      val leftFace = Path().apply {
        moveTo(px - 34.dp.toPx(), pyBase)
        lineTo(px, pyTop)
        lineTo(px + 6.dp.toPx(), pyBase)
        close()
      }
      val rightFace = Path().apply {
        moveTo(px + 6.dp.toPx(), pyBase)
        lineTo(px, pyTop)
        lineTo(px + 34.dp.toPx(), pyBase - 5.dp.toPx())
        close()
      }
      drawPath(leftFace, Color(0xFFE7C591))
      drawPath(rightFace, Color(0xFFB38B59))
    }
    CityArchitectureStyle.DUBAI_DESERT -> {
      val cx = w * 0.86f
      val spire = Path().apply {
        moveTo(cx - 13.dp.toPx(), baseY)
        lineTo(cx - 3.dp.toPx(), h * 0.24f)
        lineTo(cx, h * 0.02f)
        lineTo(cx + 3.dp.toPx(), h * 0.24f)
        lineTo(cx + 13.dp.toPx(), baseY)
        close()
      }
      drawPath(spire, Color(0xFF93C5FD).copy(alpha = 0.75f))
    }
    CityArchitectureStyle.SYDNEY_HARBOR -> {
      val cx = w * 0.77f
      for (i in 0..2) {
        val sx = cx + i * 12.dp.toPx()
        val sail = Path().apply {
          moveTo(sx - 9.dp.toPx(), baseY)
          quadraticTo(sx, h * 0.24f, sx + 11.dp.toPx(), baseY)
          close()
        }
        drawPath(sail, PureWhite.copy(alpha = 0.82f))
      }
    }
    CityArchitectureStyle.DHAKA_SHAMIBAGH, CityArchitectureStyle.SOUTH_ASIA_METRO -> {
      // 3D Old Dhaka heritage dome & minaret tower in background
      val cx = w * 0.89f
      val topY = h * 0.24f
      val tw = 28.dp.toPx()
      drawRect(Color(0xFFC28E6E).copy(alpha = 0.70f), Offset(cx - tw / 2f, topY), Size(tw, baseY - topY))
      val side = Path().apply {
        moveTo(cx + tw / 2f, topY)
        lineTo(cx + tw / 2f + 10.dp.toPx(), topY - 6.dp.toPx())
        lineTo(cx + tw / 2f + 10.dp.toPx(), baseY - 6.dp.toPx())
        lineTo(cx + tw / 2f, baseY)
        close()
      }
      drawPath(side, Color(0xFF96664A).copy(alpha = 0.74f))
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0xFFFFF7ED), Color(0xFFD6A888)),
          center = Offset(cx - 2.dp.toPx(), topY - 3.dp.toPx()),
          radius = 12.dp.toPx(),
        ),
        radius = 11.dp.toPx(),
        center = Offset(cx, topY),
      )
    }
    else -> Unit
  }
}

/**
 * Draws a chunky 3D cartoon-realistic miniature building with:
 * - Directional ground shadow
 * - Lit front facade (or reflective glass curtain wall with specular sweep)
 * - Darker shaded right 3D side wall with side windows / wrap-around Times Square ad
 * - Top 3D roof slab with parapet, pitched gables, domes, rooftop water tanks, or HVAC vents
 * - 3D balconies, awnings, and glossy backlit digital display billboards
 */
fun DrawScope.draw3dMiniBuilding(
  spec: MiniBuildingSpec,
  canvasWidth: Float,
  baseY: Float,
  isDay: Boolean,
  accentColor: Color,
  ambientPulse: Float,
  textMeasurer: TextMeasurer,
) {
  val bw = canvasWidth * spec.widthFraction
  val bh = size.height * spec.heightFraction
  val bx = canvasWidth * spec.xFraction
  val topY = baseY - bh

  // 3D oblique extrusion vector (back-right & up)
  val dx = (canvasWidth * spec.depthFraction).coerceIn(10.dp.toPx(), 26.dp.toPx())
  val dy = dx * 0.62f

  // 1. Directional Ground Cast Shadow
  val shadowPath = Path().apply {
    moveTo(bx + bw, baseY)
    lineTo(bx + bw + dx + 8.dp.toPx(), baseY - dy + 2.dp.toPx())
    lineTo(bx + bw + dx + 13.dp.toPx(), baseY + 3.dp.toPx())
    lineTo(bx + 4.dp.toPx(), baseY + 3.dp.toPx())
    close()
  }
  drawPath(shadowPath, Color(0xFF0F172A).copy(alpha = if (isDay) 0.16f else 0.30f))

  // 2. Shaded Right 3D Side Wall
  val sideBaseColor = lerp(spec.facadeColor, Color(0xFF0F172A), 0.34f)
  val sidePath = Path().apply {
    moveTo(bx + bw, topY)
    lineTo(bx + bw + dx, topY - dy)
    lineTo(bx + bw + dx, baseY - dy)
    lineTo(bx + bw, baseY)
    close()
  }
  drawPath(
    path = sidePath,
    brush = Brush.verticalGradient(
      colors = listOf(
        lerp(sideBaseColor, Color.White, 0.06f),
        lerp(sideBaseColor, Color.Black, 0.16f),
      ),
      startY = topY - dy,
      endY = baseY,
    ),
  )

  // Side wall 3D windows (skewed vertically along the side face)
  val sideWinColor = if (isDay) Color(0xFFBAE6FD).copy(alpha = 0.56f) else SolarYellow.copy(alpha = 0.66f)
  val sideRows = spec.windowRows.coerceAtMost(4)
  for (r in 0 until sideRows) {
    val sy = topY + bh * (0.18f + r * 0.16f)
    val winPath = Path().apply {
      moveTo(bx + bw + dx * 0.25f, sy - dy * 0.25f)
      lineTo(bx + bw + dx * 0.72f, sy - dy * 0.72f)
      lineTo(bx + bw + dx * 0.72f, sy - dy * 0.72f + 5.dp.toPx())
      lineTo(bx + bw + dx * 0.25f, sy - dy * 0.25f + 5.dp.toPx())
      close()
    }
    drawPath(winPath, sideWinColor)
  }

  // 3. Top 3D Roof Deck (or 3D Pitched Gable Roof)
  val roofLight = lerp(spec.facadeColor, PureWhite, 0.36f)
  if (spec.hasPitchedRoof) {
    val ridgeX = bx + bw * 0.5f + dx * 0.3f
    val ridgeY = topY - 12.dp.toPx()
    // Front pitched roof slope
    val frontSlope = Path().apply {
      moveTo(bx - 1.5.dp.toPx(), topY + 1.dp.toPx())
      lineTo(ridgeX, ridgeY)
      lineTo(bx + bw + 1.dp.toPx(), topY + 1.dp.toPx())
      close()
    }
    val roofColor = lerp(accentColor, Color(0xFFB45309), 0.45f)
    drawPath(frontSlope, roofColor)
    // Right shaded roof slope
    val rightSlope = Path().apply {
      moveTo(bx + bw, topY)
      lineTo(ridgeX, ridgeY)
      lineTo(ridgeX + dx * 0.55f, ridgeY - dy * 0.45f)
      lineTo(bx + bw + dx, topY - dy)
      close()
    }
    drawPath(rightSlope, lerp(roofColor, Color.Black, 0.30f))
  } else {
    val roofPath = Path().apply {
      moveTo(bx, topY)
      lineTo(bx + dx, topY - dy)
      lineTo(bx + bw + dx, topY - dy)
      lineTo(bx + bw, topY)
      close()
    }
    drawPath(roofPath, roofLight)
    // Recessed parapet rim on roof
    val inset = 2.5.dp.toPx()
    val innerRoof = Path().apply {
      moveTo(bx + inset, topY - 1.dp.toPx())
      lineTo(bx + dx, topY - dy + 1.5.dp.toPx())
      lineTo(bx + bw + dx - inset, topY - dy + 1.5.dp.toPx())
      lineTo(bx + bw - inset, topY - 1.dp.toPx())
      close()
    }
    drawPath(innerRoof, lerp(roofLight, Color(0xFF475569), 0.24f))
  }

  // Rooftop 3D details (Domes, Water Tanks on legs, HVAC units)
  if (spec.hasDome) {
    val domeCx = bx + bw * 0.5f + dx * 0.35f
    val domeCy = topY - dy * 0.35f
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(PureWhite, Color(0xFFCBD5E1)),
        center = Offset(domeCx - 3.dp.toPx(), domeCy - 3.dp.toPx()),
        radius = bw * 0.38f,
      ),
      radius = bw * 0.34f,
      center = Offset(domeCx, domeCy),
    )
  } else if (spec.hasWaterTank) {
    // 3D cylindrical rooftop water tank on steel legs (Dhaka / Brooklyn style)
    val tx = bx + bw * 0.38f + dx * 0.35f
    val ty = topY - dy * 0.35f - 7.dp.toPx()
    val tw = bw * 0.34f
    val th = 6.5.dp.toPx()
    // Legs
    drawLine(Color(0xFF334155), Offset(tx + 1.5.dp.toPx(), ty + th), Offset(tx + 1.5.dp.toPx(), ty + th + 2.5.dp.toPx()), 1.2.dp.toPx())
    drawLine(Color(0xFF334155), Offset(tx + tw - 1.5.dp.toPx(), ty + th), Offset(tx + tw - 1.5.dp.toPx(), ty + th + 2.5.dp.toPx()), 1.2.dp.toPx())
    drawRoundRect(
      brush = Brush.horizontalGradient(listOf(Color(0xFF475569), Color(0xFF0F172A))),
      topLeft = Offset(tx, ty),
      size = Size(tw, th),
      cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
    )
    drawOval(
      color = Color(0xFF94A3B8),
      topLeft = Offset(tx, ty - 1.5.dp.toPx()),
      size = Size(tw, 3.dp.toPx()),
    )
  } else if (spec.hasHvacUnits) {
    // 3D rooftop HVAC box matching reference diorama buildings
    val hx = bx + bw * 0.25f + dx * 0.35f
    val hy = topY - dy * 0.35f - 4.dp.toPx()
    val hw = bw * 0.38f
    val hh = 4.dp.toPx()
    drawRect(Color(0xFFCBD5E1), Offset(hx, hy), Size(hw, hh))
    drawRect(Color(0xFF64748B), Offset(hx + hw, hy - 1.5.dp.toPx()), Size(4.dp.toPx(), hh))
    drawRect(Color(0xFFF8FAFC), Offset(hx + 2.dp.toPx(), hy - 1.5.dp.toPx()), Size(hw, 1.8.dp.toPx()))
  }

  // 4. Front Lit 3D Facade
  val frontTopColor = lerp(spec.facadeColor, Color.White, if (isDay) 0.12f else 0.02f)
  val frontBotColor = lerp(spec.facadeColor, Color.Black, if (isDay) 0.08f else 0.28f)
  drawRoundRect(
    brush = Brush.verticalGradient(
      colors = listOf(frontTopColor, frontBotColor),
      startY = topY,
      endY = baseY,
    ),
    topLeft = Offset(bx, topY),
    size = Size(bw, bh),
    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
  )

  // Shiny glass curtain-wall specular highlight sweep for modern towers (NYC / Tokyo / Dubai)
  if (spec.isGlassTower) {
    val sweepX = bx + bw * ((ambientPulse * 0.7f) % 0.75f)
    val glassSweep = Path().apply {
      moveTo(sweepX, topY)
      lineTo((sweepX + bw * 0.32f).coerceAtMost(bx + bw), topY)
      lineTo((sweepX + bw * 0.12f).coerceAtMost(bx + bw), baseY)
      lineTo(sweepX.coerceAtLeast(bx), baseY)
      close()
    }
    drawPath(glassSweep, Color.White.copy(alpha = if (isDay) 0.20f else 0.10f))
  }

  // Soft 3D front bevel outline
  drawRoundRect(
    color = spec.trimColor,
    topLeft = Offset(bx, topY),
    size = Size(bw, bh),
    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
    style = Stroke(width = 0.8.dp.toPx()),
  )

  // 5. Recessed 3D Windows + Specular Sky Reflections + 3D Balconies
  val windowBase = if (isDay) {
    if (spec.isGlassTower) Color(0xFF38BDF8).copy(alpha = 0.78f) else Color(0xFFBAE6FD).copy(alpha = 0.85f)
  } else {
    SolarYellow.copy(alpha = 0.88f)
  }
  val padX = bw * 0.13f
  val topPadY = bh * 0.12f
  val bottomReserve = if (spec.hasShopAwning) 12.dp.toPx() else bh * 0.10f
  val usableW = (bw - padX * 2f).coerceAtLeast(3f)
  val usableH = (bh - topPadY - bottomReserve).coerceAtLeast(4f)
  val cellW = usableW / spec.windowCols
  val cellH = usableH / spec.windowRows

  for (r in 0 until spec.windowRows) {
    for (c in 0 until spec.windowCols) {
      if (!isDay && (r + c) % 3 == 0 && !spec.isGlassTower) continue
      val wx = bx + padX + c * cellW + cellW * 0.14f
      val wy = topY + topPadY + r * cellH + cellH * 0.16f
      val ww = (cellW * 0.68f).coerceAtLeast(2.2f)
      val wh = (cellH * 0.56f).coerceAtLeast(2.2f)

      // Dark 3D window frame recess
      drawRect(
        color = Color(0xFF0F172A).copy(alpha = 0.28f),
        topLeft = Offset(wx - 0.5.dp.toPx(), wy - 0.5.dp.toPx()),
        size = Size(ww + 1.dp.toPx(), wh + 1.dp.toPx()),
      )
      drawRect(
        brush = Brush.linearGradient(
          colors = listOf(PureWhite.copy(alpha = 0.65f), windowBase),
          start = Offset(wx, wy),
          end = Offset(wx + ww, wy + wh),
        ),
        topLeft = Offset(wx, wy),
        size = Size(ww, wh),
      )

      if (spec.hasBalconies && c == 0 && r < spec.windowRows - 1) {
        // 3D protruding balcony ledge with drop shadow
        val balY = wy + wh + 0.8.dp.toPx()
        drawRect(
          color = Color(0xFF0F172A).copy(alpha = 0.25f),
          topLeft = Offset(bx + padX * 0.35f, balY + 1.8.dp.toPx()),
          size = Size(bw - padX * 0.7f, 1.5.dp.toPx()),
        )
        drawRect(
          color = Color(0xFFF8FAFC).copy(alpha = 0.85f),
          topLeft = Offset(bx + padX * 0.35f, balY),
          size = Size(bw - padX * 0.7f, 2.dp.toPx()),
        )
      }
    }
  }

  // 6. 3D Sloped Shop Awning & Lit Storefront
  if (spec.hasShopAwning) {
    val awningPath = Path().apply {
      moveTo(bx + 1.dp.toPx(), baseY - 12.dp.toPx())
      lineTo(bx + bw - 1.dp.toPx(), baseY - 12.dp.toPx())
      lineTo(bx + bw + 2.dp.toPx(), baseY - 8.dp.toPx())
      lineTo(bx - 2.dp.toPx(), baseY - 8.dp.toPx())
      close()
    }
    drawPath(awningPath, spec.awningColor)
    drawRect(
      color = lerp(spec.awningColor, Color.Black, 0.32f),
      topLeft = Offset(bx - 2.dp.toPx(), baseY - 8.dp.toPx()),
      size = Size(bw + 4.dp.toPx(), 2.dp.toPx()),
    )
    drawRect(
      color = Color(0xFFFFF7ED).copy(alpha = if (isDay) 0.72f else 0.94f),
      topLeft = Offset(bx + 2.dp.toPx(), baseY - 6.dp.toPx()),
      size = Size((bw - 4.dp.toPx()).coerceAtLeast(2f), 6.dp.toPx()),
    )
  }

  // 7. Shiny Glossy Digital Display Billboards (Primary Front + Upper Stacked + 3D Wrap-around Side Ad)
  spec.billboard?.let { ad ->
    drawGlossyBillboard(
      ad = ad,
      x = bx + bw * 0.03f,
      y = topY + bh * 0.36f,
      w = (bw * 0.94f).coerceAtLeast(28.dp.toPx()),
      h = 15.dp.toPx(),
      isDay = isDay,
      ambientPulse = ambientPulse,
      textMeasurer = textMeasurer,
    )
  }
  spec.upperBillboard?.let { ad ->
    drawGlossyBillboard(
      ad = ad,
      x = bx + bw * 0.05f,
      y = topY + bh * 0.11f,
      w = (bw * 0.90f).coerceAtLeast(26.dp.toPx()),
      h = 12.5.dp.toPx(),
      isDay = isDay,
      ambientPulse = (ambientPulse + 0.45f) % 1f,
      textMeasurer = textMeasurer,
    )
  }
  spec.sideBillboard?.let { ad ->
    // Perspective-skewed shiny LED ad mounted on the 3D right wall of the skyscraper
    val adY = topY + bh * 0.24f
    val adH = 14.dp.toPx()
    val sideAdPath = Path().apply {
      moveTo(bx + bw + 1.dp.toPx(), adY)
      lineTo(bx + bw + dx - 1.5.dp.toPx(), adY - dy * 0.85f)
      lineTo(bx + bw + dx - 1.5.dp.toPx(), adY - dy * 0.85f + adH)
      lineTo(bx + bw + 1.dp.toPx(), adY + adH)
      close()
    }
    drawPath(
      path = sideAdPath,
      brush = Brush.linearGradient(listOf(ad.color, ad.subColor)),
    )
    drawPath(
      path = sideAdPath,
      color = Color.White.copy(alpha = 0.65f),
      style = Stroke(width = 0.8.dp.toPx()),
    )
  }
}

private fun DrawScope.drawGlossyBillboard(
  ad: MiniBillboardSpec,
  x: Float,
  y: Float,
  w: Float,
  h: Float,
  isDay: Boolean,
  ambientPulse: Float,
  textMeasurer: TextMeasurer,
) {
  val glowAlpha = 0.86f + 0.14f * ambientPulse
  // Luminous LED neon spill halo
  drawRoundRect(
    color = ad.color.copy(alpha = if (isDay) 0.30f else 0.56f * glowAlpha),
    topLeft = Offset(x - 2.5.dp.toPx(), y - 2.5.dp.toPx()),
    size = Size(w + 5.dp.toPx(), h + 5.dp.toPx()),
    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
  )
  // Metallic dark frame backing
  drawRoundRect(
    color = Color(0xFF0F172A),
    topLeft = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()),
    size = Size(w + 2.dp.toPx(), h + 2.dp.toPx()),
    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
  )
  // Vivid screen background
  drawRoundRect(
    brush = Brush.horizontalGradient(
      colors = listOf(ad.color.copy(alpha = glowAlpha), ad.subColor.copy(alpha = 0.96f))
    ),
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(2.2.dp.toPx(), 2.2.dp.toPx()),
  )
  // Animated diagonal glass sheen across the shiny LED billboard
  val sheenStart = x + w * (ambientPulse * 0.85f)
  val sheenPath = Path().apply {
    moveTo(sheenStart, y)
    lineTo((sheenStart + w * 0.24f).coerceAtMost(x + w), y)
    lineTo((sheenStart + w * 0.08f).coerceAtMost(x + w), y + h)
    lineTo(sheenStart.coerceAtLeast(x), y + h)
    close()
  }
  drawPath(sheenPath, Color.White.copy(alpha = 0.26f))

  // Beveled glass frame border
  drawRoundRect(
    color = Color.White.copy(alpha = 0.82f),
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(2.2.dp.toPx(), 2.2.dp.toPx()),
    style = Stroke(width = 0.9.dp.toPx()),
  )

  val measured = textMeasurer.measure(
    text = ad.text,
    style = TextStyle(
      color = ad.accentTextColor,
      fontSize = 6.2.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 0.35.sp,
    ),
    maxLines = 1,
  )
  val tx = x + ((w - measured.size.width) / 2f).coerceAtLeast(1.5.dp.toPx())
  val ty = y + ((h - measured.size.height) / 2f).coerceAtLeast(0f)
  if (measured.size.width <= w) {
    drawText(textLayoutResult = measured, topLeft = Offset(tx, ty))
  }
}

fun DrawScope.drawOverheadWires(canvasWidth: Float, baseY: Float) {
  val poles = listOf(0.14f, 0.47f, 0.80f)
  val topY = baseY - 40.dp.toPx()
  poles.forEach { rx ->
    val px = canvasWidth * rx
    drawLine(Color(0xFF334155), Offset(px, baseY), Offset(px, topY), 2.2.dp.toPx())
    drawLine(Color(0xFF475569), Offset(px - 5.dp.toPx(), topY + 4.dp.toPx()), Offset(px + 5.dp.toPx(), topY + 4.dp.toPx()), 1.7.dp.toPx())
  }
  for (i in 0 until poles.lastIndex) {
    val x1 = canvasWidth * poles[i]
    val x2 = canvasWidth * poles[i + 1]
    val midX = (x1 + x2) / 2f
    val wire1 = Path().apply {
      moveTo(x1, topY + 4.dp.toPx())
      quadraticTo(midX, topY + 15.dp.toPx(), x2, topY + 4.dp.toPx())
    }
    val wire2 = Path().apply {
      moveTo(x1, topY + 8.dp.toPx())
      quadraticTo(midX, topY + 20.dp.toPx(), x2, topY + 8.dp.toPx())
    }
    drawPath(wire1, Color(0xFF1E293B).copy(alpha = 0.65f), style = Stroke(width = 0.9.dp.toPx()))
    drawPath(wire2, Color(0xFF1E293B).copy(alpha = 0.50f), style = Stroke(width = 0.8.dp.toPx()))
  }
}

fun DrawScope.draw3dLondonPhoneBooth(x: Float, baseY: Float) {
  val w = 6.5.dp.toPx()
  val h = 14.dp.toPx()
  // Cast shadow
  drawOval(Color(0xFF0F172A).copy(alpha = 0.25f), Offset(x + 2.dp.toPx(), baseY - 1.5.dp.toPx()), Size(w + 4.dp.toPx(), 3.dp.toPx()))
  // Shaded 3D right wall
  drawRect(Color(0xFF991B1B), Offset(x + w - 1.dp.toPx(), baseY - h - 1.5.dp.toPx()), Size(3.dp.toPx(), h))
  // Front red booth
  drawRoundRect(
    color = Color(0xFFDC2626),
    topLeft = Offset(x, baseY - h),
    size = Size(w, h),
    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
  )
  drawRect(
    color = Color(0xFFBAE6FD).copy(alpha = 0.90f),
    topLeft = Offset(x + 1.2.dp.toPx(), baseY - h + 2.5.dp.toPx()),
    size = Size(w - 2.4.dp.toPx(), h * 0.55f),
  )
}

fun DrawScope.draw3dMiniTree(x: Float, baseY: Float, isDay: Boolean) {
  // Ground shadow
  drawOval(
    color = Color(0xFF0F172A).copy(alpha = if (isDay) 0.20f else 0.35f),
    topLeft = Offset(x - 6.dp.toPx(), baseY - 2.dp.toPx()),
    size = Size(14.dp.toPx(), 4.dp.toPx()),
  )
  // 3D shaded trunk
  drawLine(Color(0xFF78350F), Offset(x, baseY), Offset(x, baseY - 11.dp.toPx()), 2.6.dp.toPx())
  // Spherical 3D toon foliage with top-left specular highlight
  val center = Offset(x, baseY - 15.dp.toPx())
  val r = 7.5.dp.toPx()
  drawCircle(
    brush = Brush.radialGradient(
      colors = if (isDay) {
        listOf(Color(0xFFA3E635), Color(0xFF4D7C0F), Color(0xFF365314))
      } else {
        listOf(Color(0xFF4D7C0F), Color(0xFF1E3A1E), Color(0xFF0F1F12))
      },
      center = Offset(center.x - 2.5.dp.toPx(), center.y - 2.5.dp.toPx()),
      radius = r * 1.4f,
    ),
    radius = r,
    center = center,
  )
}

private fun DrawScope.draw3dPineTree(x: Float, baseY: Float, scale: Float, isDay: Boolean) {
  val h = 22.dp.toPx() * scale
  val w = 11.dp.toPx() * scale
  val left = Path().apply {
    moveTo(x, baseY - h)
    lineTo(x - w * 0.5f, baseY)
    lineTo(x, baseY)
    close()
  }
  val right = Path().apply {
    moveTo(x, baseY - h)
    lineTo(x, baseY)
    lineTo(x + w * 0.5f, baseY)
    close()
  }
  drawPath(left, if (isDay) Color(0xFF65A30D) else Color(0xFF284B2A))
  drawPath(right, if (isDay) Color(0xFF3F6212) else Color(0xFF1A331C))
}

fun DrawScope.draw3dMiniPalmTree(x: Float, baseY: Float, isDay: Boolean) {
  drawOval(
    color = Color(0xFF0F172A).copy(alpha = if (isDay) 0.18f else 0.30f),
    topLeft = Offset(x - 4.dp.toPx(), baseY - 2.dp.toPx()),
    size = Size(12.dp.toPx(), 3.5.dp.toPx()),
  )
  drawLine(Color(0xFF92400E), Offset(x, baseY), Offset(x + 2.dp.toPx(), baseY - 17.dp.toPx()), 2.3.dp.toPx())
  val top = Offset(x + 2.dp.toPx(), baseY - 17.dp.toPx())
  val frondLight = Color(0xFF4ADE80)
  val frondDark = Color(0xFF15803D)
  drawLine(frondLight, top, Offset(top.x - 8.dp.toPx(), top.y - 1.5.dp.toPx()), 2.dp.toPx())
  drawLine(frondDark, top, Offset(top.x + 8.dp.toPx(), top.y - 1.5.dp.toPx()), 2.dp.toPx())
  drawLine(frondLight, top, Offset(top.x - 5.dp.toPx(), top.y - 5.5.dp.toPx()), 1.8.dp.toPx())
  drawLine(frondDark, top, Offset(top.x + 5.dp.toPx(), top.y - 5.5.dp.toPx()), 1.8.dp.toPx())
}

fun DrawScope.draw3dMiniStreetLamp(x: Float, baseY: Float, isDay: Boolean) {
  val topY = baseY - 20.dp.toPx()
  drawLine(Color(0xFF94A3B8), Offset(x, baseY), Offset(x, topY), 1.6.dp.toPx())
  if (!isDay) {
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(SolarYellow.copy(alpha = 0.78f), Color.Transparent),
        center = Offset(x, topY),
        radius = 13.dp.toPx(),
      ),
      radius = 13.dp.toPx(),
      center = Offset(x, topY),
    )
  }
  drawCircle(if (isDay) Color(0xFFF1F5F9) else SolarYellow, 2.2.dp.toPx(), Offset(x, topY))
}
