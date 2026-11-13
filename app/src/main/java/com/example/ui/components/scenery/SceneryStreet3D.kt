package com.example.ui.components.scenery

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SolarYellow

fun DrawScope.draw3dMiniTrain(
  x: Float,
  railY: Float,
  isDay: Boolean,
  accentColor: Color,
) {
  val trainW = 58.dp.toPx()
  val trainH = 8.dp.toPx()
  val roofDy = 3.dp.toPx()
  // 3D train roof parallelogram
  val roof = Path().apply {
    moveTo(x + 2.dp.toPx(), railY - trainH - 1.dp.toPx())
    lineTo(x + 6.dp.toPx(), railY - trainH - 1.dp.toPx() - roofDy)
    lineTo(x + trainW + 4.dp.toPx(), railY - trainH - 1.dp.toPx() - roofDy)
    lineTo(x + trainW - 1.dp.toPx(), railY - trainH - 1.dp.toPx())
    close()
  }
  drawPath(roof, Color(0xFFE2E8F0))
  drawRoundRect(
    brush = Brush.verticalGradient(listOf(PureWhite, Color(0xFFCBD5E1))),
    topLeft = Offset(x, railY - trainH - 1.dp.toPx()),
    size = Size(trainW, trainH),
    cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
  )
  drawLine(
    color = accentColor,
    start = Offset(x + 4.dp.toPx(), railY - trainH * 0.45f),
    end = Offset(x + trainW - 4.dp.toPx(), railY - trainH * 0.45f),
    strokeWidth = 1.7.dp.toPx(),
  )
  if (!isDay) {
    drawCircle(SolarYellow, 2.2.dp.toPx(), Offset(x + trainW, railY - 4.5.dp.toPx()))
  }
}

fun DrawScope.draw3dWalkingPerson(
  x: Float,
  footY: Float,
  facingRight: Boolean,
  strideSin: Float,
  ped: PedestrianSpec,
  isRainy: Boolean,
) {
  val headR = 2.1.dp.toPx()
  val hipY = footY - 5.dp.toPx()
  val shoulderY = hipY - 4.6.dp.toPx()
  val headY = shoulderY - headR - 0.8.dp.toPx()
  val swing = strideSin * 2.5.dp.toPx()

  // Soft 3D ground shadow
  drawOval(
    color = Color(0xFF0F172A).copy(alpha = 0.25f),
    topLeft = Offset(x - 3.dp.toPx(), footY - 1.dp.toPx()),
    size = Size(6.dp.toPx(), 2.2.dp.toPx()),
  )

  // Legs walking stride
  drawLine(ped.pantsColor, Offset(x, hipY), Offset(x + swing, footY), 1.6.dp.toPx(), StrokeCap.Round)
  drawLine(ped.pantsColor, Offset(x, hipY), Offset(x - swing, footY), 1.6.dp.toPx(), StrokeCap.Round)

  // Rounded 3D torso
  drawLine(ped.shirtColor, Offset(x, shoulderY), Offset(x, hipY), 3.0.dp.toPx(), StrokeCap.Round)
  drawLine(ped.shirtColor, Offset(x, shoulderY + 0.8.dp.toPx()), Offset(x - swing * 0.7f, hipY), 1.4.dp.toPx(), StrokeCap.Round)

  // Highlighted 3D head
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFFFEF3C7), Color(0xFFF59E0B)),
      center = Offset(x - 0.7.dp.toPx(), headY - 0.7.dp.toPx()),
      radius = headR * 1.4f,
    ),
    radius = headR,
    center = Offset(x, headY),
  )

  if (isRainy && ped.hasBagOrUmbrella) {
    val umbTop = headY - 4.5.dp.toPx()
    val dome = Path().apply {
      moveTo(x - 5.5.dp.toPx(), umbTop + 2.5.dp.toPx())
      quadraticTo(x, umbTop - 3.5.dp.toPx(), x + 5.5.dp.toPx(), umbTop + 2.5.dp.toPx())
      close()
    }
    drawPath(dome, ped.shirtColor)
  } else if (ped.hasBagOrUmbrella) {
    val bagX = if (facingRight) x - 2.dp.toPx() else x + 2.dp.toPx()
    drawRect(Color(0xFF78350F), Offset(bagX - 1.dp.toPx(), shoulderY + 2.dp.toPx()), Size(2.4.dp.toPx(), 3.dp.toPx()))
  }
}

fun DrawScope.draw3dMiniVehicle(
  kind: MiniVehicleKind,
  x: Float,
  groundY: Float,
  facingRight: Boolean,
  isDay: Boolean,
  accentColor: Color,
  showHornBeep: Boolean = false,
) {
  val wheelR = 2.3.dp.toPx()

  if (kind == MiniVehicleKind.CYCLE_RICKSHAW) {
    val canopyW = 12.5.dp.toPx()
    val seatH = 8.dp.toPx()
    val rearWheelX = if (facingRight) x + 3.dp.toPx() else x + 16.5.dp.toPx()
    val frontWheelX = if (facingRight) x + 16.5.dp.toPx() else x + 3.dp.toPx()
    val canopyX = if (facingRight) x else x + 7.5.dp.toPx()

    // Ground shadow
    drawOval(
      color = Color(0xFF0F172A).copy(alpha = 0.28f),
      topLeft = Offset(x, groundY - 1.5.dp.toPx()),
      size = Size(20.dp.toPx(), 3.5.dp.toPx()),
    )

    // 3D canopy roof hood extrusion
    val hoodTopY = groundY - wheelR - seatH
    val canopyRoof = Path().apply {
      moveTo(canopyX, hoodTopY)
      lineTo(canopyX + 3.dp.toPx(), hoodTopY - 2.2.dp.toPx())
      lineTo(canopyX + canopyW + 3.dp.toPx(), hoodTopY - 2.2.dp.toPx())
      lineTo(canopyX + canopyW, hoodTopY)
      close()
    }
    drawPath(canopyRoof, Color(0xFFFB7185))
    drawRoundRect(
      color = Color(0xFFE11D48),
      topLeft = Offset(canopyX, hoodTopY),
      size = Size(canopyW, seatH),
      cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
    )
    drawRect(
      color = SolarYellow,
      topLeft = Offset(canopyX + 1.5.dp.toPx(), hoodTopY + 1.6.dp.toPx()),
      size = Size(canopyW - 3.dp.toPx(), 2.dp.toPx()),
    )
    drawLine(Color(0xFF94A3B8), Offset(rearWheelX, groundY - wheelR), Offset(frontWheelX, groundY - wheelR), 1.5.dp.toPx())
    drawCircle(Color(0xFF38BDF8), 1.8.dp.toPx(), Offset(frontWheelX, groundY - wheelR - 6.5.dp.toPx()))
    draw3dWheel(rearWheelX, groundY - wheelR * 0.65f, wheelR)
    draw3dWheel(frontWheelX, groundY - wheelR * 0.65f, wheelR)
    return
  }

  val bodyColor: Color
  val width: Float
  val height: Float

  when (kind) {
    MiniVehicleKind.SEDAN -> {
      bodyColor = Color(0xFF38BDF8)
      width = 23.dp.toPx()
      height = 7.5.dp.toPx()
    }
    MiniVehicleKind.NYC_TAXI -> {
      bodyColor = SolarYellow
      width = 23.dp.toPx()
      height = 7.5.dp.toPx()
    }
    MiniVehicleKind.LONDON_CAB -> {
      bodyColor = Color(0xFF1E293B)
      width = 22.dp.toPx()
      height = 8.dp.toPx()
    }
    MiniVehicleKind.DHAKA_BUS -> {
      bodyColor = Color(0xFF16A34A)
      width = 35.dp.toPx()
      height = 11.dp.toPx()
    }
    MiniVehicleKind.CITY_BUS -> {
      bodyColor = accentColor
      width = 35.dp.toPx()
      height = 11.dp.toPx()
    }
    MiniVehicleKind.DOUBLE_DECKER_BUS -> {
      bodyColor = Color(0xFFDC2626)
      width = 33.dp.toPx()
      height = 13.5.dp.toPx()
    }
    MiniVehicleKind.CNG_RICKSHAW -> {
      bodyColor = Color(0xFF10B981)
      width = 18.dp.toPx()
      height = 8.5.dp.toPx()
    }
    MiniVehicleKind.DELIVERY_VAN -> {
      bodyColor = PureWhite
      width = 26.dp.toPx()
      height = 9.5.dp.toPx()
    }
    MiniVehicleKind.CYCLE_RICKSHAW -> return
  }

  val bodyBottom = groundY - wheelR
  val bodyTop = bodyBottom - height
  val d3x = 3.8.dp.toPx()
  val d3y = 2.4.dp.toPx()

  // Ground shadow directly beneath vehicle
  drawOval(
    color = Color(0xFF0F172A).copy(alpha = 0.30f),
    topLeft = Offset(x - 1.dp.toPx(), groundY - 1.6.dp.toPx()),
    size = Size(width + d3x + 2.dp.toPx(), 3.8.dp.toPx()),
  )

  // 3D Shaded Right End / Side Depth
  val rearWall = Path().apply {
    moveTo(x + width, bodyTop)
    lineTo(x + width + d3x, bodyTop - d3y)
    lineTo(x + width + d3x, bodyBottom - d3y)
    lineTo(x + width, bodyBottom)
    close()
  }
  drawPath(rearWall, lerp(bodyColor, Color.Black, 0.34f))

  // 3D Lit Top Roof Deck (for vans, buses, CNGs)
  if (kind.isBus || kind == MiniVehicleKind.DELIVERY_VAN || kind == MiniVehicleKind.CNG_RICKSHAW) {
    val roofDeck = Path().apply {
      moveTo(x + 1.dp.toPx(), bodyTop)
      lineTo(x + 1.dp.toPx() + d3x, bodyTop - d3y)
      lineTo(x + width + d3x, bodyTop - d3y)
      lineTo(x + width, bodyTop)
      close()
    }
    drawPath(roofDeck, lerp(bodyColor, PureWhite, 0.32f))
  }

  // Glossy 3D Front Body Shell
  drawRoundRect(
    brush = Brush.verticalGradient(
      colors = listOf(lerp(bodyColor, PureWhite, 0.14f), lerp(bodyColor, Color.Black, 0.14f)),
      startY = bodyTop,
      endY = bodyBottom,
    ),
    topLeft = Offset(x, bodyTop),
    size = Size(width, height),
    cornerRadius = CornerRadius(2.8.dp.toPx(), 2.8.dp.toPx()),
  )

  when (kind) {
    MiniVehicleKind.SEDAN, MiniVehicleKind.NYC_TAXI, MiniVehicleKind.LONDON_CAB -> {
      val cabX = x + width * 0.21f
      val cabW = width * 0.56f
      val cabTop = bodyTop - 3.3.dp.toPx()
      // 3D car cabin roof
      val cabRoof = Path().apply {
        moveTo(cabX + 1.5.dp.toPx(), cabTop)
        lineTo(cabX + 1.5.dp.toPx() + d3x * 0.7f, cabTop - d3y * 0.7f)
        lineTo(cabX + cabW + d3x * 0.7f, cabTop - d3y * 0.7f)
        lineTo(cabX + cabW, cabTop)
        close()
      }
      drawPath(cabRoof, lerp(bodyColor, PureWhite, 0.38f))
      drawRoundRect(
        brush = Brush.linearGradient(listOf(PureWhite.copy(alpha = 0.85f), Color(0xFF7DD3FC))),
        topLeft = Offset(cabX, cabTop),
        size = Size(cabW, 4.2.dp.toPx()),
        cornerRadius = CornerRadius(1.8.dp.toPx(), 1.8.dp.toPx()),
      )
      if (kind == MiniVehicleKind.NYC_TAXI) {
        drawRect(
          color = Color(0xFF0F172A),
          topLeft = Offset(x + width * 0.40f, cabTop - 1.8.dp.toPx()),
          size = Size(width * 0.20f, 1.8.dp.toPx()),
        )
      }
    }
    MiniVehicleKind.DHAKA_BUS, MiniVehicleKind.CITY_BUS, MiniVehicleKind.DOUBLE_DECKER_BUS -> {
      if (kind == MiniVehicleKind.DHAKA_BUS) {
        drawRect(
          color = Color(0xFFEF4444),
          topLeft = Offset(x + 1.dp.toPx(), bodyBottom - 3.4.dp.toPx()),
          size = Size(width - 2.dp.toPx(), 2.dp.toPx()),
        )
      }
      drawRect(
        brush = Brush.horizontalGradient(listOf(PureWhite.copy(alpha = 0.85f), Color(0xFF7DD3FC))),
        topLeft = Offset(x + 2.5.dp.toPx(), bodyTop + 2.dp.toPx()),
        size = Size(width - 5.dp.toPx(), 2.8.dp.toPx()),
      )
      if (kind == MiniVehicleKind.DOUBLE_DECKER_BUS) {
        drawRect(
          brush = Brush.horizontalGradient(listOf(PureWhite.copy(alpha = 0.85f), Color(0xFF7DD3FC))),
          topLeft = Offset(x + 2.5.dp.toPx(), bodyTop + 6.5.dp.toPx()),
          size = Size(width - 5.dp.toPx(), 2.8.dp.toPx()),
        )
      }
    }
    MiniVehicleKind.CNG_RICKSHAW -> {
      drawRoundRect(
        color = SolarYellow.copy(alpha = 0.94f),
        topLeft = Offset(x + 2.dp.toPx(), bodyTop + 1.4.dp.toPx()),
        size = Size(width * 0.56f, 3.dp.toPx()),
        cornerRadius = CornerRadius(1.4.dp.toPx(), 1.4.dp.toPx()),
      )
    }
    else -> Unit
  }

  // 3D Wheels with metallic rim hub
  draw3dWheel(x + width * 0.24f, groundY - wheelR * 0.65f, wheelR)
  draw3dWheel(x + width * 0.76f, groundY - wheelR * 0.65f, wheelR)

  // Headlights & Taillights at night
  if (!isDay) {
    val headX = if (facingRight) x + width else x
    val tailX = if (facingRight) x else x + width
    drawCircle(SolarYellow, 1.9.dp.toPx(), Offset(headX, bodyBottom - height * 0.35f))
    drawCircle(Color(0xFFEF4444), 1.6.dp.toPx(), Offset(tailX, bodyBottom - height * 0.35f))
  }

  // Horn-beep acoustic arcs in front of honking cars/buses
  if (showHornBeep) {
    val frontX = if (facingRight) x + width + 2.dp.toPx() else x - 2.dp.toPx()
    val hornY = bodyTop + height * 0.35f
    val arcColor = if (kind.isBus) Color(0xFFF59E0B) else SolarYellow
    val startAngle = if (facingRight) -42f else 138f
    for (ring in 0..1) {
      val r = (3.8f + ring * 3.2f).dp.toPx()
      drawArc(
        color = arcColor.copy(alpha = 0.88f - ring * 0.26f),
        startAngle = startAngle,
        sweepAngle = 84f,
        useCenter = false,
        topLeft = Offset(frontX - r, hornY - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round),
      )
    }
  }
}

private fun DrawScope.draw3dWheel(cx: Float, cy: Float, r: Float) {
  drawCircle(Color(0xFF0F172A), r, Offset(cx, cy))
  drawCircle(Color(0xFF94A3B8), r * 0.45f, Offset(cx, cy))
}

fun DrawScope.drawDustOverlay(
  canvasWidth: Float,
  canvasHeight: Float,
  driftProgress: Float,
  isDay: Boolean,
) {
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color.Transparent,
        Color(0xFFD6A86E).copy(alpha = if (isDay) 0.14f else 0.08f),
        Color(0xFFC8965A).copy(alpha = if (isDay) 0.24f else 0.12f),
      ),
      startY = canvasHeight * 0.35f,
      endY = canvasHeight,
    )
  )
  val moteColor = if (isDay) Color(0xFFFDE68A).copy(alpha = 0.44f) else Color(0xFFD6B588).copy(alpha = 0.26f)
  for (i in 0..17) {
    val rx = ((i * 0.073f + driftProgress * 1.4f) % 1f) * canvasWidth
    val ry = canvasHeight * (0.45f + ((i * 0.19f) % 0.48f))
    val r = if (i % 3 == 0) 1.5.dp.toPx() else 0.9.dp.toPx()
    drawCircle(color = moteColor, radius = r, center = Offset(rx, ry))
  }
}
