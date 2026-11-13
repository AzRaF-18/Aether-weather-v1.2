package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyForecastItem
import com.example.data.model.HourlyForecastItem
import com.example.data.model.WeatherCondition
import com.example.ui.theme.GlassSubtleTrack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SolarYellow
import kotlin.math.roundToInt

@Composable
fun HourlyForecastCard(
  hourlyItems: List<HourlyForecastItem>,
  isFahrenheit: Boolean,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  val textShadow = Shadow(
    color = Color.Black.copy(alpha = 0.25f),
    offset = Offset(0f, 1f),
    blurRadius = 3f,
  )

  WaterGlassSurface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("hourly_forecast_card"),
    shape = RoundedCornerShape(26.dp),
    cornerRadius = 26.dp,
    shadowElevation = 14.dp,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Schedule,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = stringResource(id = R.string.hourly_forecast_title),
          style = MaterialTheme.typography.titleSmall.copy(shadow = textShadow),
          color = SolarYellow,
        )
      }

      LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(hourlyItems) { item ->
          val temp = convertTemp(item.tempC, isFahrenheit).roundToInt()
          val cond = WeatherCondition.fromWmoCode(item.weatherCode, item.isDay)

          WaterGlassSurface(
            shape = RoundedCornerShape(18.dp),
            cornerRadius = 18.dp,
            shadowElevation = 8.dp,
            edgeAccentColor = if (item.isNow) Color.White.copy(alpha = 0.65f) else null,
          ) {
            Column(
              modifier = Modifier
                .width(74.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = item.timeLabel,
                style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
                color = SolarYellow,
                maxLines = 1,
              )
              WeatherConditionIcon(
                kind = cond.iconKind,
                size = 20.dp,
              )
              Text(
                text = "$temp°",
                style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
                color = PureWhite,
                maxLines = 1,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun WeeklyForecastCard(
  dailyItems: List<DailyForecastItem>,
  weekMinTempC: Double,
  weekMaxTempC: Double,
  isFahrenheit: Boolean,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  val textShadow = Shadow(
    color = Color.Black.copy(alpha = 0.25f),
    offset = Offset(0f, 1f),
    blurRadius = 3f,
  )

  WaterGlassSurface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("weekly_forecast_card"),
    shape = RoundedCornerShape(26.dp),
    cornerRadius = 26.dp,
    shadowElevation = 14.dp,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.CalendarMonth,
          contentDescription = null,
          tint = SolarYellow,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = stringResource(id = R.string.weekly_forecast_title),
          style = MaterialTheme.typography.titleSmall.copy(shadow = textShadow),
          color = SolarYellow,
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      val spanC = (weekMaxTempC - weekMinTempC).coerceAtLeast(1.0)

      dailyItems.forEachIndexed { index, day ->
        val minT = convertTemp(day.minTempC, isFahrenheit).roundToInt()
        val maxT = convertTemp(day.maxTempC, isFahrenheit).roundToInt()
        val cond = WeatherCondition.fromWmoCode(day.weatherCode, isDay = true)

        val startFraction = ((day.minTempC - weekMinTempC) / spanC).toFloat().coerceIn(0f, 0.85f)
        val widthFraction = (((day.maxTempC - day.minTempC) / spanC).toFloat()).coerceIn(0.15f, 1f - startFraction)

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = day.dayLabel,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Bold,
              shadow = textShadow,
            ),
            color = SolarYellow,
            modifier = Modifier.width(54.dp),
          )

          WeatherConditionIcon(
            kind = cond.iconKind,
            size = 18.dp,
          )

          Text(
            text = "$minT°",
            style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
            color = PureWhite.copy(alpha = 0.94f),
            modifier = Modifier.width(34.dp),
          )

          // Proportional Temperature Range Bar
          BoxWithConstraints(
            modifier = Modifier
              .weight(1f)
              .height(6.dp)
              .clip(CircleShape)
              .background(GlassSubtleTrack),
          ) {
            val barOffset = maxWidth * startFraction
            val barWidth = (maxWidth * widthFraction).coerceAtLeast(10.dp)
            Box(
              modifier = Modifier
                .offset(x = barOffset)
                .width(barWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(accentColor, SolarYellow),
                  )
                ),
            )
          }

          Text(
            text = "$maxT°",
            style = MaterialTheme.typography.labelLarge.copy(shadow = textShadow),
            color = lerp(accentColor, PureWhite, 0.25f),
            modifier = Modifier.width(38.dp),
          )
        }

        if (index < dailyItems.lastIndex) {
          HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        }
      }
    }
  }
}
