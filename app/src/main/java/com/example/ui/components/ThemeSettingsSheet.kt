package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PureWhite
import java.util.Locale

private val PresetAccentColors = listOf(
  0xFFEF4444L, // Crimson Red (default)
  0xFF06B6D4L, // Glossy Cyan
  0xFF3B82F6L, // Cobalt Blue
  0xFF8B5CF6L, // Royal Violet
  0xFF10B981L, // Emerald Mint
  0xFFF59E0BL, // Solar Amber
  0xFFEC4899L, // Neon Rose
  0xFF84CC16L, // Electric Lime
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsSheet(
  accentColorArgb: Long,
  isFahrenheit: Boolean,
  alertsEnabled: Boolean,
  hasNotificationPermission: Boolean,
  ambientSoundEnabled: Boolean,
  activeAmbientSoundLabel: String,
  onSelectAccentColor: (Long) -> Unit,
  onToggleUnit: (Boolean) -> Unit,
  onToggleAlerts: (Boolean) -> Unit,
  onToggleAmbientSound: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val currentAccent = Color(accentColorArgb)

  val hsv = remember { FloatArray(3) }
  var hue by remember { mutableFloatStateOf(0f) }
  var saturation by remember { mutableFloatStateOf(0.8f) }
  var value by remember { mutableFloatStateOf(0.95f) }

  LaunchedEffect(accentColorArgb) {
    android.graphics.Color.colorToHSV(currentAccent.toArgb(), hsv)
    hue = hsv[0]
    saturation = hsv[1]
    value = hsv[2]
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xEA0C1220),
    contentColor = PureWhite,
    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 12.dp, bottom = 6.dp)
          .size(width = 42.dp, height = 4.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.35f))
      )
    },
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // One UI 9 + iOS 27 Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = currentAccent.copy(alpha = 0.22f),
            border = BorderStroke(1.dp, currentAccent.copy(alpha = 0.7f)),
          ) {
            Icon(
              imageVector = Icons.Rounded.Palette,
              contentDescription = null,
              tint = currentAccent,
              modifier = Modifier
                .padding(8.dp)
                .size(20.dp),
            )
          }
          Column {
            Text(
              text = "THEME & CONTROLS",
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
            )
            Text(
              text = "Customize accent color & switches",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.68f),
            )
          }
        }

        // Reset to Default Crimson Red
        WaterGlassSurface(
          onClick = { onSelectAccentColor(0xFFEF4444L) },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("reset_accent_button"),
          shape = CircleShape,
          shadowElevation = 4.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.RestartAlt,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = "Reset",
              style = MaterialTheme.typography.labelMedium,
              color = PureWhite,
            )
          }
        }
      }

      // Active Color Preview + Hex
      WaterGlassSurface(
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 6.dp,
        edgeAccentColor = currentAccent,
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(currentAccent)
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
            )
            Text(
              text = "Active Theme Color",
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
              color = PureWhite,
            )
          }
          Text(
            text = String.format(Locale.US, "#%06X", (accentColorArgb and 0xFFFFFFL)),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            color = currentAccent,
          )
        }
      }

      // Quick Swatches
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PresetAccentColors.forEachIndexed { idx, argb ->
          val swatch = Color(argb)
          val selected = (accentColorArgb and 0xFFFFFFL) == (argb and 0xFFFFFFL)
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(swatch)
              .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) PureWhite else Color.White.copy(alpha = 0.35f),
                shape = CircleShape,
              )
              .clickable { onSelectAccentColor(argb) }
              .testTag("color_swatch_$idx"),
            contentAlignment = Alignment.Center,
          ) {
            if (selected) {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = contrastingOnAccent(swatch),
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      // Full Color Palette: 2D Saturation/Brightness Pad + Hue Spectrum Bar
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "FULL COLOR PALETTE",
          style = MaterialTheme.typography.labelMedium,
          color = Color.White.copy(alpha = 0.72f),
        )

        SaturationBrightnessPad(
          hue = hue,
          saturation = saturation,
          value = value,
          onChanged = { newSat, newVal ->
            saturation = newSat
            value = newVal
            val argbInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, newSat, newVal))
            onSelectAccentColor(argbInt.toLong() and 0xFFFFFFFFL)
          },
        )

        HueSpectrumSlider(
          hue = hue,
          onHueChanged = { newHue ->
            hue = newHue
            val argbInt = android.graphics.Color.HSVToColor(floatArrayOf(newHue, saturation, value))
            onSelectAccentColor(argbInt.toLong() and 0xFFFFFFFFL)
          },
        )
      }

      HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

      // One UI–Style Toggle Switches using the selected accent color
      WaterGlassSurface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp,
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Fahrenheit (°F)",
                style = MaterialTheme.typography.bodyLarge,
                color = PureWhite,
              )
              Text(
                text = "Toggle temperature scale across app & widget",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
              )
            }
            Switch(
              checked = isFahrenheit,
              onCheckedChange = onToggleUnit,
              colors = SwitchDefaults.colors(
                checkedThumbColor = contrastingOnAccent(currentAccent),
                checkedTrackColor = currentAccent,
                uncheckedThumbColor = PureWhite,
                uncheckedTrackColor = Color.White.copy(alpha = 0.18f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.35f),
              ),
            )
          }

          HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Severe Weather Push Alerts",
                style = MaterialTheme.typography.bodyLarge,
                color = PureWhite,
              )
              Text(
                text = "Background hazard notifications every 15 min",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
              )
            }
            Switch(
              checked = alertsEnabled && hasNotificationPermission,
              onCheckedChange = onToggleAlerts,
              colors = SwitchDefaults.colors(
                checkedThumbColor = contrastingOnAccent(currentAccent),
                checkedTrackColor = currentAccent,
                uncheckedThumbColor = PureWhite,
                uncheckedTrackColor = Color.White.copy(alpha = 0.18f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.35f),
              ),
            )
          }

          HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Ambient Weather Sounds",
                style = MaterialTheme.typography.bodyLarge,
                color = PureWhite,
              )
              Text(
                text = if (ambientSoundEnabled) {
                  "Playing: $activeAmbientSoundLabel"
                } else {
                  "Subtle rain patter, wind rustling & breezes"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
              )
            }
            Switch(
              checked = ambientSoundEnabled,
              onCheckedChange = onToggleAmbientSound,
              modifier = Modifier.testTag("ambient_sound_switch"),
              colors = SwitchDefaults.colors(
                checkedThumbColor = contrastingOnAccent(currentAccent),
                checkedTrackColor = currentAccent,
                uncheckedThumbColor = PureWhite,
                uncheckedTrackColor = Color.White.copy(alpha = 0.18f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.35f),
              ),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
    }
  }
}

@Composable
private fun SaturationBrightnessPad(
  hue: Float,
  saturation: Float,
  value: Float,
  onChanged: (Float, Float) -> Unit,
) {
  val pureHueColor = remember(hue) {
    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(148.dp)
      .clip(RoundedCornerShape(18.dp))
      .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
      .pointerInput(hue) {
        detectTapGestures { offset ->
          val s = (offset.x / size.width).coerceIn(0f, 1f)
          val v = (1f - offset.y / size.height).coerceIn(0f, 1f)
          onChanged(s, v)
        }
      }
      .pointerInput(hue) {
        detectDragGestures { change, _ ->
          change.consume()
          val s = (change.position.x / size.width).coerceIn(0f, 1f)
          val v = (1f - change.position.y / size.height).coerceIn(0f, 1f)
          onChanged(s, v)
        }
      }
      .testTag("color_saturation_pad"),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(
        brush = Brush.horizontalGradient(
          colors = listOf(Color.White, pureHueColor)
        )
      )
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(Color.Transparent, Color.Black)
        )
      )
      val cx = saturation * size.width
      val cy = (1f - value) * size.height
      drawCircle(
        color = Color.Black.copy(alpha = 0.55f),
        radius = 10.dp.toPx(),
        center = Offset(cx, cy),
        style = Stroke(width = 3.dp.toPx()),
      )
      drawCircle(
        color = Color.White,
        radius = 9.dp.toPx(),
        center = Offset(cx, cy),
        style = Stroke(width = 2.dp.toPx()),
      )
    }
  }
}

@Composable
private fun HueSpectrumSlider(
  hue: Float,
  onHueChanged: (Float) -> Unit,
) {
  val spectrumColors = remember {
    listOf(
      Color(0xFFFF0000),
      Color(0xFFFFFF00),
      Color(0xFF00FF00),
      Color(0xFF00FFFF),
      Color(0xFF0000FF),
      Color(0xFFFF00FF),
      Color(0xFFFF0000),
    )
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(28.dp)
      .clip(CircleShape)
      .border(1.dp, Color.White.copy(alpha = 0.40f), CircleShape)
      .pointerInput(Unit) {
        detectTapGestures { offset ->
          val newHue = ((offset.x / size.width).coerceIn(0f, 1f)) * 360f
          onHueChanged(newHue)
        }
      }
      .pointerInput(Unit) {
        detectDragGestures { change, _ ->
          change.consume()
          val newHue = ((change.position.x / size.width).coerceIn(0f, 1f)) * 360f
          onHueChanged(newHue)
        }
      }
      .testTag("color_hue_slider"),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(brush = Brush.horizontalGradient(colors = spectrumColors))
      val thumbX = ((hue / 360f).coerceIn(0f, 1f)) * size.width
      drawCircle(
        color = Color.White,
        radius = 11.dp.toPx(),
        center = Offset(thumbX, size.height / 2f),
        style = Stroke(width = 3.dp.toPx()),
      )
    }
  }
}
