package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.WeatherIconKind
import com.example.data.model.WeatherReport
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DeepBlueInk
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SolarGoldSoft
import com.example.ui.theme.SolarYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.SimpleTimeZone
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun HeroWeatherSection(
  report: WeatherReport,
  isFahrenheit: Boolean,
  accentColor: Color,
  onToggleUnit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isLightHero = report.skyPhase.isLightTop
  val cityTextColor = if (isLightHero) DarkInk else PureWhite
  val dateAndCoordsColor = if (isLightHero) DeepBlueInk else SolarGoldSoft
  val descTextColor =
    if (isLightHero) lerp(accentColor, Color.Black, 0.35f)
    else lerp(accentColor, PureWhite, 0.32f)

  val displayTemp = convertTemp(report.currentTempC, isFahrenheit).roundToInt()
  val displayFeelsLike = convertTemp(report.feelsLikeC, isFahrenheit).roundToInt()
  val unitLabel = if (isFahrenheit) "°F" else "°C"

  var liveExactTime by remember(report.utcOffsetSeconds, report.formattedTime) {
    mutableStateOf(
      formatExactLocalTime(report.utcOffsetSeconds).ifBlank { report.formattedTime }
    )
  }
  LaunchedEffect(report.utcOffsetSeconds) {
    while (true) {
      liveExactTime = formatExactLocalTime(report.utcOffsetSeconds)
      delay(15_000L)
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Hero Stage with Animated Celestial Backdrop (Sun / Half-Sun / Moon / Clouds)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp),
    ) {
      CelestialHeroStage(
        skyPhase = report.skyPhase,
        isDay = report.isDay,
        condition = report.condition,
        modifier = Modifier.matchParentSize(),
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = report.formattedDate,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = dateAndCoordsColor,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = report.location.displayName,
          style = MaterialTheme.typography.headlineMedium,
          color = cityTextColor,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("hero_city_name"),
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          WeatherConditionIcon(
            kind = report.condition.iconKind,
            size = 18.dp,
            tint = descTextColor,
          )
          Text(
            text = liveExactTime,
            style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 2.2.sp),
            color = descTextColor,
            modifier = Modifier.testTag("hero_local_time"),
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Giant Temperature Display
        Row(
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.Center,
        ) {
          Text(
            text = "$displayTemp",
            style = MaterialTheme.typography.displayLarge.copy(
              shadow = Shadow(
                color = accentColor.copy(alpha = 0.40f),
                blurRadius = 24f,
              )
            ),
            color = accentColor,
            modifier = Modifier.testTag("hero_temperature"),
          )
          Text(
            text = unitLabel,
            style = MaterialTheme.typography.displayMedium,
            color = accentColor,
            modifier = Modifier.padding(top = 12.dp, start = 2.dp),
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Glossy °C / °F Switch Pill using active accent color
        WaterGlassSurface(
          onClick = onToggleUnit,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("unit_toggle_button"),
          shape = CircleShape,
          cornerRadius = 20.dp,
          isFilledAccent = true,
          accentColor = accentColor,
        ) {
          Box(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = stringResource(
                id = if (isFahrenheit) R.string.switch_to_c else R.string.switch_to_f
              ),
              style = MaterialTheme.typography.labelLarge,
              color = contrastingOnAccent(accentColor),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = String.format(
            Locale.US,
            "COORDS: %.2f, %.2f",
            report.location.latitude,
            report.location.longitude,
          ),
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.6.sp,
          ),
          color = dateAndCoordsColor.copy(alpha = 0.90f),
        )
      }
    }

    // 2x2 Floating Frosted-Glass Metric Cards matching screenshot
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        WeatherMetricCard(
          icon = Icons.Rounded.WaterDrop,
          label = stringResource(id = R.string.humidity_label),
          value = "${report.humidityPercent}%",
          modifier = Modifier.weight(1f),
          testTag = "metric_humidity",
        )
        WeatherMetricCard(
          icon = Icons.Rounded.Air,
          label = stringResource(id = R.string.wind_label),
          value = String.format(Locale.US, "%.1f m/s", report.windSpeedMs),
          modifier = Modifier.weight(1f),
          testTag = "metric_wind",
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        WeatherMetricCard(
          icon = Icons.Rounded.DeviceThermostat,
          label = stringResource(id = R.string.feels_like_label),
          value = "$displayFeelsLike$unitLabel",
          modifier = Modifier.weight(1f),
          testTag = "metric_feels_like",
        )
        WeatherMetricCard(
          icon = Icons.Rounded.Compress,
          label = stringResource(id = R.string.pressure_label),
          value = "${report.pressureHpa.roundToInt()} hPa",
          modifier = Modifier.weight(1f),
          testTag = "metric_pressure",
        )
      }
    }
  }
}

@Composable
private fun WeatherMetricCard(
  icon: ImageVector,
  label: String,
  value: String,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val textShadow = Shadow(
    color = Color.Black.copy(alpha = 0.25f),
    offset = Offset(0f, 1f),
    blurRadius = 3f,
  )

  WaterGlassSurface(
    modifier = modifier.testTag(testTag),
    shape = RoundedCornerShape(22.dp),
    cornerRadius = 22.dp,
    shadowElevation = 14.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = SolarYellow,
          modifier = Modifier.size(15.dp),
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
          color = SolarYellow,
          maxLines = 1,
        )
      }
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
        color = PureWhite,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun WeatherConditionIcon(
  kind: WeatherIconKind,
  size: androidx.compose.ui.unit.Dp,
  tint: Color? = null,
  modifier: Modifier = Modifier,
) {
  val (icon, defaultTint) =
    when (kind) {
      WeatherIconKind.CLEAR_DAY -> Icons.Rounded.WbSunny to SolarYellow
      WeatherIconKind.CLEAR_NIGHT -> Icons.Rounded.DarkMode to Color(0xFFE2E8F0)
      WeatherIconKind.PARTLY_CLOUDY_DAY -> Icons.Rounded.WbCloudy to SolarGoldSoft
      WeatherIconKind.PARTLY_CLOUDY_NIGHT -> Icons.Rounded.NightsStay to Color(0xFFCBD5E1)
      WeatherIconKind.CLOUDY -> Icons.Rounded.Cloud to Color(0xFFCBD5E1)
      WeatherIconKind.FOG -> Icons.Rounded.Grain to Color(0xFF94A3B8)
      WeatherIconKind.RAIN -> Icons.Rounded.WaterDrop to Color(0xFF60A5FA)
      WeatherIconKind.SNOW -> Icons.Rounded.AcUnit to Color(0xFFBAE6FD)
      WeatherIconKind.THUNDERSTORM -> Icons.Rounded.Thunderstorm to SolarYellow
    }
  Icon(
    imageVector = icon,
    contentDescription = null,
    tint = tint ?: defaultTint,
    modifier = modifier.size(size),
  )
}

fun convertTemp(tempC: Double, isFahrenheit: Boolean): Double =
  if (isFahrenheit) (tempC * 9.0 / 5.0) + 32.0 else tempC

fun formatExactLocalTime(
  utcOffsetSeconds: Int,
  timeMillis: Long = System.currentTimeMillis(),
): String {
  val tz = SimpleTimeZone(utcOffsetSeconds * 1000, "AetherLocal")
  return SimpleDateFormat("h:mm a", Locale.US).apply { timeZone = tz }.format(Date(timeMillis))
}
