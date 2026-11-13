package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TrafficAudioConfig
import com.example.ui.components.scenery.CityArchitectureStyle
import com.example.ui.components.scenery.computeTrafficAudioConfig
import com.example.ui.components.scenery.draw3dDioramaBackdropAndPlinth
import com.example.ui.components.scenery.draw3dLondonPhoneBooth
import com.example.ui.components.scenery.draw3dMiniBuilding
import com.example.ui.components.scenery.draw3dMiniPalmTree
import com.example.ui.components.scenery.draw3dMiniStreetLamp
import com.example.ui.components.scenery.draw3dMiniTrain
import com.example.ui.components.scenery.draw3dMiniTree
import com.example.ui.components.scenery.draw3dMiniVehicle
import com.example.ui.components.scenery.draw3dSignatureLandmark
import com.example.ui.components.scenery.draw3dWalkingPerson
import com.example.ui.components.scenery.drawDustOverlay
import com.example.ui.components.scenery.drawOverheadWires
import com.example.ui.components.scenery.isBus
import com.example.ui.components.scenery.isCarLike
import com.example.ui.components.scenery.resolveCityDioramaSpec
import com.example.ui.theme.PureWhite
import kotlin.math.PI
import kotlin.math.sin

fun resolveTrafficAudioConfig(locationName: String): TrafficAudioConfig =
  computeTrafficAudioConfig(locationName)

/**
 * Large 3D cartoon-realistic miniature city & neighborhood diorama on a raised beveled island plinth:
 * - Shamibagh, Dhaka: 3D terracotta/pastel apartments, rooftop water tanks, heritage dome,
 *   tangled overhead wires, coconut palms, cycle-rickshaws, CNGs, Dhaka buses, walkers & dusty haze
 * - New York: Shiny 3D skyscrapers with glossy wrap-around & stacked Times Square display ads,
 *   Midtown spire, yellow taxis, city buses & bustling crosswalk pedestrians
 * - Adaptive 3D buildings, landmarks & vehicles tailored to any requested city or neighborhood
 */
@Composable
fun MiniatureCityscapeBanner(
  locationName: String,
  isDay: Boolean,
  isRainy: Boolean,
  accentColor: Color,
  modifier: Modifier = Modifier,
  bannerHeight: Dp = 280.dp,
) {
  val spec = remember(locationName) { resolveCityDioramaSpec(locationName) }
  val textMeasurer = rememberTextMeasurer()

  val infiniteTransition = rememberInfiniteTransition(label = "miniCityScene3D")
  val rightTraffic by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 16000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "rightTraffic",
  )
  val leftTraffic by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 20000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "leftTraffic",
  )
  val walkProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 28000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "walkProgress",
  )
  val trainTraffic by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 9000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "trainTraffic",
  )
  val ambientPulse by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "ambientPulse",
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(bannerHeight)
      .testTag("miniature_cityscape_banner"),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      val plinthTopY = h * 0.56f
      val plinthBottomY = h - 14.dp.toPx()
      val roadHeight = 38.dp.toPx()
      val sidewalkHeight = 12.dp.toPx()
      val roadTopY = plinthBottomY - roadHeight - 6.dp.toPx()
      val sidewalkTopY = roadTopY - sidewalkHeight
      val roadMarginX = 12.dp.toPx()
      val roadWidth = (w - roadMarginX * 2f).coerceAtLeast(20f)

      // 1. Atmospheric Sky, 3D Mountains/Clouds & Raised Beveled 3D Island Plinth
      draw3dDioramaBackdropAndPlinth(
        spec = spec,
        isDay = isDay,
        ambientPulse = ambientPulse,
        plinthTopY = plinthTopY,
        plinthBottomY = plinthBottomY,
      )

      // 2. Signature 3D Background Landmarks (Pyramids, Big Ben, Eiffel, Spire, Dhaka Dome)
      draw3dSignatureLandmark(
        style = spec.style,
        baseY = sidewalkTopY,
        isDay = isDay,
        accentColor = accentColor,
      )

      // 3. 3D Elevated Rail / Viaduct (Tokyo / Brooklyn)
      if (spec.hasElevatedTrain) {
        val railY = h * 0.36f
        drawLine(
          color = Color(0xFF94A3B8).copy(alpha = 0.72f),
          start = Offset(roadMarginX, railY),
          end = Offset(w - roadMarginX, railY),
          strokeWidth = 3.8.dp.toPx(),
        )
        for (px in 1..4) {
          val x = roadMarginX + roadWidth * (px / 5f)
          drawLine(
            color = Color(0xFF64748B).copy(alpha = 0.55f),
            start = Offset(x, railY),
            end = Offset(x, sidewalkTopY),
            strokeWidth = 2.8.dp.toPx(),
          )
        }
        val trainX = (trainTraffic * (w + 110.dp.toPx())) - 55.dp.toPx()
        draw3dMiniTrain(x = trainX, railY = railY, isDay = isDay, accentColor = accentColor)
      }

      // 4. Chunky 3D Buildings on the Island Deck (front, side wall, 3D roof, shiny NYC display ads)
      spec.buildings.forEachIndexed { idx, bldg ->
        draw3dMiniBuilding(
          spec = bldg,
          canvasWidth = w,
          baseY = sidewalkTopY,
          isDay = isDay,
          accentColor = accentColor,
          ambientPulse = (ambientPulse + idx * 0.22f) % 1f,
          textMeasurer = textMeasurer,
        )
      }

      // 5. Overhead Utility Poles & Tangled Neighborhood Wires (Shamibagh Dhaka / South Asia)
      if (spec.hasOverheadWires) {
        drawOverheadWires(canvasWidth = w, baseY = sidewalkTopY)
      }

      // 6. Sculpted 3D Foliage, Street Lamps & Phone Booth
      val treePositions = listOf(0.05f, 0.33f, 0.64f, 0.94f)
      treePositions.forEachIndexed { idx, rx ->
        if (spec.hasPalms) {
          draw3dMiniPalmTree(x = w * rx, baseY = sidewalkTopY + 2.dp.toPx(), isDay = isDay)
        } else if (idx % 2 == 0 || spec.style == CityArchitectureStyle.SUBURBAN_NEIGHBORHOOD) {
          draw3dMiniTree(x = w * rx, baseY = sidewalkTopY + 2.dp.toPx(), isDay = isDay)
        }
      }

      val lampPositions = listOf(0.17f, 0.49f, 0.80f)
      lampPositions.forEach { rx ->
        draw3dMiniStreetLamp(x = w * rx, baseY = sidewalkTopY + 2.dp.toPx(), isDay = isDay)
      }

      if (spec.hasPhoneBooth) {
        draw3dLondonPhoneBooth(x = w * 0.25f, baseY = sidewalkTopY + 2.dp.toPx())
      }

      // 7. 3D Sidewalk & Curb inside the Plinth
      drawRoundRect(
        color = if (spec.isDusty) {
          Color(0xFFD9C5A8).copy(alpha = if (isDay) 0.85f else 0.55f)
        } else {
          Color(0xFFD8E2EE).copy(alpha = if (isDay) 0.86f else 0.50f)
        },
        topLeft = Offset(roadMarginX, sidewalkTopY),
        size = Size(roadWidth, sidewalkHeight + 2.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
      )
      drawLine(
        color = Color(0xFF475569).copy(alpha = 0.62f),
        start = Offset(roadMarginX, roadTopY),
        end = Offset(w - roadMarginX, roadTopY),
        strokeWidth = 1.4.dp.toPx(),
      )

      // 8. 3D Walking Pedestrians Along the Sidewalk
      val walkSpan = w + 40.dp.toPx()
      spec.pedestrians.forEachIndexed { idx, ped ->
        val raw = (walkProgress * ped.speedMultiplier + ped.startPhase) % 1f
        val px = if (ped.walksRight) {
          (raw * walkSpan) - 20.dp.toPx()
        } else {
          (w + 20.dp.toPx()) - (raw * walkSpan)
        }
        val stridePhase = (walkProgress * ped.speedMultiplier * 28f + idx * 1.3f) * (2f * PI.toFloat())
        draw3dWalkingPerson(
          x = px.coerceIn(roadMarginX, w - roadMarginX),
          footY = roadTopY - 1.dp.toPx(),
          facingRight = ped.walksRight,
          strideSin = sin(stridePhase),
          ped = ped,
          isRainy = isRainy,
        )
      }

      // 9. Rounded Two-Lane Roadway Sitting Inside the 3D Island Base
      drawRoundRect(
        brush = Brush.verticalGradient(
          colors = if (spec.isDusty) {
            listOf(Color(0xFF544F4A), Color(0xFF393531))
          } else {
            listOf(Color(0xFF3B485C), Color(0xFF1E293B))
          }
        ),
        topLeft = Offset(roadMarginX, roadTopY),
        size = Size(roadWidth, roadHeight),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
      )

      if (spec.hasCrosswalk) {
        val cwX = w * 0.50f
        for (stripe in 0..4) {
          val sy = roadTopY + 4.dp.toPx() + stripe * 5.8.dp.toPx()
          drawRect(
            color = PureWhite.copy(alpha = 0.72f),
            topLeft = Offset(cwX - 11.dp.toPx(), sy),
            size = Size(22.dp.toPx(), 3.dp.toPx()),
          )
        }
      }

      if (isRainy) {
        for (i in 0..8) {
          val rx = roadMarginX + roadWidth * ((i * 0.11f + 0.04f) % 0.88f)
          val ry = roadTopY + roadHeight * (if (i % 2 == 0) 0.30f else 0.75f)
          drawLine(
            color = Color(0xFF93C5FD).copy(alpha = 0.32f),
            start = Offset(rx, ry),
            end = Offset(rx + 20.dp.toPx(), ry),
            strokeWidth = 1.5.dp.toPx(),
          )
        }
      }

      val roadMidY = roadTopY + roadHeight * 0.5f
      drawLine(
        color = spec.roadLineColor.copy(alpha = 0.86f),
        start = Offset(roadMarginX + 8.dp.toPx(), roadMidY),
        end = Offset(w - roadMarginX - 8.dp.toPx(), roadMidY),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(11.dp.toPx(), 7.dp.toPx())),
      )

      // Upper & Lower 3D Lane Vehicles
      val upperLaneGroundY = roadMidY - 1.5.dp.toPx()
      val carCount = spec.vehiclesRight.count { it.isCarLike } + spec.vehiclesLeft.count { it.isCarLike }
      val showCarHornPulse = carCount >= 4 && ambientPulse in 0.22f..0.42f
      val showBusHornPulse = ambientPulse in 0.62f..0.84f

      spec.vehiclesLeft.forEachIndexed { idx, kind ->
        val phase = (leftTraffic + idx / spec.vehiclesLeft.size.toFloat()) % 1f
        val span = w + 90.dp.toPx()
        val vx = (w + 45.dp.toPx()) - (phase * span)
        val shouldBeep = (kind.isBus && showBusHornPulse && idx == 0) ||
          (kind.isCarLike && showCarHornPulse && idx == 0)
        draw3dMiniVehicle(
          kind = kind,
          x = vx,
          groundY = upperLaneGroundY,
          facingRight = false,
          isDay = isDay,
          accentColor = accentColor,
          showHornBeep = shouldBeep,
        )
      }

      val lowerLaneGroundY = roadTopY + roadHeight - 3.dp.toPx()
      spec.vehiclesRight.forEachIndexed { idx, kind ->
        val phase = (rightTraffic + idx / spec.vehiclesRight.size.toFloat()) % 1f
        val span = w + 90.dp.toPx()
        val vx = (phase * span) - 45.dp.toPx()
        val shouldBeep = (kind.isBus && showBusHornPulse && idx == 1) ||
          (kind.isCarLike && showCarHornPulse && idx == 0)
        draw3dMiniVehicle(
          kind = kind,
          x = vx,
          groundY = lowerLaneGroundY,
          facingRight = true,
          isDay = isDay,
          accentColor = accentColor,
          showHornBeep = shouldBeep,
        )
      }

      // 10. Floating Dust Particles & Warm Haze Veil for Dusty Cities (Shamibagh Dhaka, Cairo)
      if (spec.isDusty) {
        drawDustOverlay(
          canvasWidth = w,
          canvasHeight = h,
          driftProgress = walkProgress,
          isDay = isDay,
        )
      }
    }

    // Location Hierarchy Badge in top-left corner
    Row(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = 10.dp, top = 8.dp)
        .clip(CircleShape)
        .background(Color(0xFF091326).copy(alpha = 0.56f))
        .border(0.75.dp, Color.White.copy(alpha = 0.36f), CircleShape)
        .padding(horizontal = 9.dp, vertical = 4.dp)
        .testTag("city_diorama_badge"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.LocationCity,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(12.dp),
      )
      Text(
        text = spec.hierarchyLabel,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.4.sp,
        ),
        color = PureWhite.copy(alpha = 0.96f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
