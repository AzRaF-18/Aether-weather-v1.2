package com.example.data.model

data class WeatherLocation(
  val latitude: Double,
  val longitude: Double,
  val displayName: String,
  val isGps: Boolean = false,
)

data class CitySuggestion(
  val id: Long,
  val displayName: String,
  val latitude: Double,
  val longitude: Double,
)

enum class WeatherIconKind {
  CLEAR_DAY,
  CLEAR_NIGHT,
  PARTLY_CLOUDY_DAY,
  PARTLY_CLOUDY_NIGHT,
  CLOUDY,
  FOG,
  RAIN,
  SNOW,
  THUNDERSTORM,
}

data class WeatherCondition(
  val label: String,
  val iconKind: WeatherIconKind,
  val isRainy: Boolean,
  val isCloudy: Boolean,
) {
  companion object {
    fun fromWmoCode(code: Int, isDay: Boolean): WeatherCondition =
      when (code) {
        0 -> WeatherCondition(
          label = if (isDay) "CLEAR DAY" else "NIGHT SKY",
          iconKind = if (isDay) WeatherIconKind.CLEAR_DAY else WeatherIconKind.CLEAR_NIGHT,
          isRainy = false,
          isCloudy = false,
        )
        1, 2 -> WeatherCondition(
          label = "PARTLY CLOUDY",
          iconKind = if (isDay) WeatherIconKind.PARTLY_CLOUDY_DAY else WeatherIconKind.PARTLY_CLOUDY_NIGHT,
          isRainy = false,
          isCloudy = true,
        )
        3 -> WeatherCondition(
          label = "OVERCAST SKY",
          iconKind = WeatherIconKind.CLOUDY,
          isRainy = false,
          isCloudy = true,
        )
        45, 48 -> WeatherCondition(
          label = "MISTY FOG",
          iconKind = WeatherIconKind.FOG,
          isRainy = false,
          isCloudy = true,
        )
        51, 53, 55, 56, 57 -> WeatherCondition(
          label = "LIGHT DRIZZLE",
          iconKind = WeatherIconKind.RAIN,
          isRainy = true,
          isCloudy = true,
        )
        61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCondition(
          label = "RAIN SHOWERS",
          iconKind = WeatherIconKind.RAIN,
          isRainy = true,
          isCloudy = true,
        )
        71, 73, 75, 77, 85, 86 -> WeatherCondition(
          label = "SNOWFALL",
          iconKind = WeatherIconKind.SNOW,
          isRainy = false,
          isCloudy = true,
        )
        95, 96, 99 -> WeatherCondition(
          label = "THUNDERSTORM",
          iconKind = WeatherIconKind.THUNDERSTORM,
          isRainy = true,
          isCloudy = true,
        )
        else -> WeatherCondition(
          label = if (isDay) "CLEAR DAY" else "NIGHT SKY",
          iconKind = if (isDay) WeatherIconKind.CLEAR_DAY else WeatherIconKind.CLEAR_NIGHT,
          isRainy = false,
          isCloudy = false,
        )
      }
  }
}

enum class SkyPhase(val isLightTop: Boolean, val hasStars: Boolean) {
  SKY_BLUE(isLightTop = true, hasStars = false),
  BEFORE_SUNSET(isLightTop = true, hasStars = false),
  SUNSET(isLightTop = false, hasStars = false),
  AFTER_SUNSET(isLightTop = false, hasStars = true),
  NIGHT_CLOUDY(isLightTop = false, hasStars = false),
  MIDNIGHT(isLightTop = false, hasStars = true),
  DEEP_NIGHT(isLightTop = false, hasStars = true);

  companion object {
    fun resolve(isDay: Boolean, timeInMinutes: Int): SkyPhase {
      val normalized = ((timeInMinutes % 1440) + 1440) % 1440
      return when {
        normalized in 420..959 && (isDay || normalized >= 540) -> SKY_BLUE
        normalized in 960..1139 -> BEFORE_SUNSET
        normalized in 1140..1220 -> SUNSET
        normalized in 1221..1307 -> NIGHT_CLOUDY
        normalized in 1308..1439 -> AFTER_SUNSET
        normalized in 0..270 -> MIDNIGHT
        isDay -> SKY_BLUE
        else -> DEEP_NIGHT
      }
    }
  }
}

data class HourlyForecastItem(
  val timeLabel: String,
  val tempC: Double,
  val weatherCode: Int,
  val isDay: Boolean,
  val isNow: Boolean,
)

data class DailyForecastItem(
  val dayLabel: String,
  val minTempC: Double,
  val maxTempC: Double,
  val weatherCode: Int,
)

data class WeatherReport(
  val location: WeatherLocation,
  val formattedDate: String,
  val formattedTime: String = "",
  val utcOffsetSeconds: Int = 0,
  val localMinutesOfDay: Int,
  val currentTempC: Double,
  val feelsLikeC: Double,
  val humidityPercent: Int,
  val windSpeedMs: Double,
  val pressureHpa: Double,
  val isDay: Boolean,
  val weatherCode: Int,
  val condition: WeatherCondition,
  val skyPhase: SkyPhase,
  val hourly: List<HourlyForecastItem>,
  val daily: List<DailyForecastItem>,
  val weekMinTempC: Double,
  val weekMaxTempC: Double,
)

enum class AlertSeverity(val rank: Int, val badge: String) {
  EXTREME(rank = 3, badge = "EXTREME"),
  SEVERE(rank = 2, badge = "SEVERE"),
  MODERATE(rank = 1, badge = "ADVISORY");

  companion object {
    fun fromNwsString(raw: String?): AlertSeverity =
      when (raw?.lowercase()) {
        "extreme" -> EXTREME
        "severe" -> SEVERE
        else -> MODERATE
      }
  }
}

data class SevereWeatherAlert(
  val id: String,
  val eventTitle: String,
  val severity: AlertSeverity,
  val summary: String,
  val instruction: String,
  val source: String,
  val locationName: String,
)

object PresetLocations {
  val SWAMIBAGH = WeatherLocation(
    latitude = 23.7104,
    longitude = 90.4184,
    displayName = "Swamibagh, Dhaka, Bangladesh",
    isGps = false,
  )
  val NEW_YORK = WeatherLocation(
    latitude = 40.7128,
    longitude = -74.0060,
    displayName = "New York, United States",
    isGps = false,
  )
  val LONDON = WeatherLocation(
    latitude = 51.5074,
    longitude = -0.1278,
    displayName = "London, United Kingdom",
    isGps = false,
  )
  val TOKYO = WeatherLocation(
    latitude = 35.6762,
    longitude = 139.6503,
    displayName = "Tokyo, Japan",
    isGps = false,
  )

  val quickList = listOf(
    "Swamibagh" to SWAMIBAGH,
    "New York" to NEW_YORK,
    "London" to LONDON,
    "Tokyo" to TOKYO,
  )
}
