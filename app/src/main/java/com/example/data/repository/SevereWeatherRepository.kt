package com.example.data.repository

import com.example.data.model.AlertSeverity
import com.example.data.model.SevereWeatherAlert
import com.example.data.model.WeatherResponseDto
import com.example.data.remote.NetworkModule
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SevereWeatherRepository {

  private val forecastApi = NetworkModule.forecastApi
  private val nwsAlertsApi = NetworkModule.nwsAlertsApi

  suspend fun fetchSevereAlerts(
    latitude: Double,
    longitude: Double,
    locationName: String,
    cachedForecast: WeatherResponseDto? = null,
  ): List<SevereWeatherAlert> =
    withContext(Dispatchers.IO) {
      val alerts = mutableListOf<SevereWeatherAlert>()

      // 1. Official US National Weather Service Alerts (returns empty or 404 outside US)
      val pointParam = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
      val nwsResult = runCatching { nwsAlertsApi.getActiveAlerts(pointParam) }.getOrNull()
      nwsResult?.features.orEmpty().forEachIndexed { index, feature ->
        val props = feature.properties ?: return@forEachIndexed
        val event = props.event?.takeIf { it.isNotBlank() } ?: return@forEachIndexed
        val severity = AlertSeverity.fromNwsString(props.severity)
        val alertId = props.id ?: feature.id ?: "nws_${event.hashCode()}_$index"
        alerts += SevereWeatherAlert(
          id = alertId,
          eventTitle = event,
          severity = severity,
          summary = props.headline?.takeIf { it.isNotBlank() }
            ?: props.description?.take(180)
            ?: "$event active for $locationName",
          instruction = props.instruction?.takeIf { it.isNotBlank() }
            ?: "Monitor local advisories and take appropriate safety precautions.",
          source = "NWS Official",
          locationName = locationName,
        )
      }

      // 2. Global Open-Meteo Severe Hazard Detection (works worldwide)
      val forecast = cachedForecast
        ?: runCatching { forecastApi.getForecast(latitude = latitude, longitude = longitude) }.getOrNull()

      if (forecast != null) {
        alerts += evaluateGlobalSevereHazards(forecast, locationName, latitude, longitude)
      }

      alerts
        .distinctBy { it.eventTitle.lowercase(Locale.US) }
        .sortedByDescending { it.severity.rank }
    }

  companion object {
    fun evaluateGlobalSevereHazards(
      forecast: WeatherResponseDto,
      locationName: String,
      latitude: Double,
      longitude: Double,
    ): List<SevereWeatherAlert> {
      val current = forecast.current ?: return emptyList()
      val hourly = forecast.hourly
      val currentHourPrefix = current.time.take(13)
      val startIdx =
        hourly?.time?.indexOfFirst { it.startsWith(currentHourPrefix) }?.coerceAtLeast(0) ?: 0
      val endIdx = minOf(startIdx + 12, hourly?.weatherCode?.size ?: 0)
      val dateBucket = current.time.take(10).ifBlank { "today" }
      val locKey = String.format(Locale.US, "%.2f_%.2f", latitude, longitude)

      val detected = mutableListOf<SevereWeatherAlert>()

      val upcomingCodes =
        if (hourly != null && startIdx < endIdx) {
          hourly.weatherCode.subList(startIdx, endIdx)
        } else {
          emptyList()
        }
      val allCodes = listOf(current.weatherCode) + upcomingCodes

      val maxGustMs =
        maxOf(
          current.windGusts10m,
          hourly?.windGusts10m?.drop(startIdx)?.take(12)?.maxOrNull() ?: 0.0,
        )
      val maxPrecipMm =
        maxOf(
          current.precipitation,
          hourly?.precipitation?.drop(startIdx)?.take(12)?.maxOrNull() ?: 0.0,
        )
      val minTempC =
        minOf(
          current.apparentTemperature,
          hourly?.temperature2m?.drop(startIdx)?.take(12)?.minOrNull() ?: current.apparentTemperature,
        )
      val maxTempC =
        maxOf(
          current.apparentTemperature,
          hourly?.temperature2m?.drop(startIdx)?.take(12)?.maxOrNull() ?: current.apparentTemperature,
        )

      // Thunderstorms & Hail (WMO 95, 96, 99)
      val hasHailStorm = allCodes.any { it == 96 || it == 99 }
      val hasThunderstorm = allCodes.any { it == 95 }
      if (hasHailStorm || hasThunderstorm) {
        detected += SevereWeatherAlert(
          id = "om_storm_${locKey}_$dateBucket",
          eventTitle = if (hasHailStorm) "Severe Thunderstorm & Hail Warning" else "Thunderstorm Warning",
          severity = if (hasHailStorm) AlertSeverity.EXTREME else AlertSeverity.SEVERE,
          summary = "Convective storm activity detected near $locationName in the next 12 hours.",
          instruction = "Move indoors away from windows and avoid open areas or tall trees.",
          source = "Open-Meteo Hazard",
          locationName = locationName,
        )
      }

      // Heavy Rain / Flash Flood Risk (WMO 65, 67, 82 or >= 10mm/h)
      if (allCodes.any { it in listOf(65, 67, 82) } || maxPrecipMm >= 10.0) {
        detected += SevereWeatherAlert(
          id = "om_rain_${locKey}_$dateBucket",
          eventTitle = "Heavy Rain & Flood Advisory",
          severity = if (maxPrecipMm >= 20.0) AlertSeverity.SEVERE else AlertSeverity.MODERATE,
          summary = String.format(
            Locale.US,
            "Intense rainfall up to %.1f mm/h forecast around %s.",
            maxPrecipMm,
            locationName,
          ),
          instruction = "Avoid waterlogged roads and low-lying drainage areas.",
          source = "Open-Meteo Hazard",
          locationName = locationName,
        )
      }

      // Freezing Rain / Heavy Snow
      if (allCodes.any { it in listOf(56, 57, 66, 67, 75, 86) }) {
        detected += SevereWeatherAlert(
          id = "om_winter_${locKey}_$dateBucket",
          eventTitle = "Winter Ice & Heavy Snow Advisory",
          severity = AlertSeverity.SEVERE,
          summary = "Freezing precipitation or heavy snowfall expected near $locationName.",
          instruction = "Allow extra travel time and watch for slick roads and sidewalks.",
          source = "Open-Meteo Hazard",
          locationName = locationName,
        )
      }

      // High Wind Gusts (>= 15.3 m/s ~ 55 km/h)
      if (maxGustMs >= 15.3) {
        detected += SevereWeatherAlert(
          id = "om_wind_${locKey}_$dateBucket",
          eventTitle = "High Wind Gusts Warning",
          severity = if (maxGustMs >= 22.2) AlertSeverity.EXTREME else AlertSeverity.SEVERE,
          summary = String.format(
            Locale.US,
            "Damaging wind gusts reaching %.1f m/s forecast near %s.",
            maxGustMs,
            locationName,
          ),
          instruction = "Secure loose outdoor items and use caution when driving high-profile vehicles.",
          source = "Open-Meteo Hazard",
          locationName = locationName,
        )
      }

      // Extreme Heat (>= 40°C apparent temperature)
      if (maxTempC >= 40.0) {
        detected += SevereWeatherAlert(
          id = "om_heat_${locKey}_$dateBucket",
          eventTitle = "Extreme Heat Warning",
          severity = if (maxTempC >= 45.0) AlertSeverity.EXTREME else AlertSeverity.SEVERE,
          summary = "Dangerous heat index near ${maxTempC.roundToInt()}°C expected in $locationName.",
          instruction = "Stay hydrated, limit strenuous outdoor activity, and seek shade or A/C.",
          source = "Open-Meteo Hazard",
          locationName = locationName,
        )
      }

      // Extreme Cold (<= -15°C)
      if (minTempC <= -15.0) {
        detected += SevereWeatherAlert(
          id = "om_cold_${locKey}_$dateBucket",
          eventTitle = "Extreme Cold Advisory",
          severity = if (minTempC <= -25.0) AlertSeverity.SEVERE else AlertSeverity.MODERATE,
          summary = "Sub-freezing conditions down to ${minTempC.roundToInt()}°C forecast in $locationName.",
          instruction = "Dress in insulated layers and protect exposed skin against frostbite.",
          source = "Open-Meteo Hazard",
          locationName = locationName,
        )
      }

      return detected
    }
  }
}
