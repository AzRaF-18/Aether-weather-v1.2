package com.example.data.repository

import com.example.data.model.CitySuggestion
import com.example.data.model.DailyForecastItem
import com.example.data.model.HourlyForecastItem
import com.example.data.model.SkyPhase
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherLocation
import com.example.data.model.WeatherReport
import com.example.data.remote.NetworkModule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.SimpleTimeZone
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository {

  private val forecastApi = NetworkModule.forecastApi
  private val geocodingApi = NetworkModule.geocodingApi
  private val nominatimApi = NetworkModule.nominatimApi

  suspend fun fetchWeather(
    latitude: Double,
    longitude: Double,
    customName: String?,
    isGps: Boolean,
  ): Result<WeatherReport> =
    withContext(Dispatchers.IO) {
      runCatching {
        val resolvedName =
          customName ?: resolveNeighborhoodName(latitude, longitude)

        val response = forecastApi.getForecast(latitude = latitude, longitude = longitude)
        val current = response.current ?: error("Missing current weather data")

        val utcOffsetSec = response.utcOffsetSeconds ?: 0
        val offsetMillis = utcOffsetSec * 1000
        val locationTimeZone = SimpleTimeZone(offsetMillis, "AetherLocal")
        val now = Date()

        val localCalendar = Calendar.getInstance(locationTimeZone).apply { time = now }
        val localMinutesOfDay =
          localCalendar.get(Calendar.HOUR_OF_DAY) * 60 + localCalendar.get(Calendar.MINUTE)

        val dateFormatter =
          SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).apply {
            timeZone = locationTimeZone
          }
        val timeFormatter =
          SimpleDateFormat("h:mm a", Locale.US).apply {
            timeZone = locationTimeZone
          }
        val formattedDate = dateFormatter.format(now)
        val formattedTime = timeFormatter.format(now)

        val isDay = current.isDay == 1
        val condition = WeatherCondition.fromWmoCode(current.weatherCode, isDay)
        val skyPhase = SkyPhase.resolve(isDay, localMinutesOfDay)

        val hourlyItems = buildHourlyItems(
          times = response.hourly?.time.orEmpty(),
          temps = response.hourly?.temperature2m.orEmpty(),
          codes = response.hourly?.weatherCode.orEmpty(),
          currentIsoTime = current.time,
          fallbackTemp = current.temperature2m,
          fallbackCode = current.weatherCode,
          fallbackIsDay = isDay,
        )

        val dailyItems = buildDailyItems(
          dates = response.daily?.time.orEmpty(),
          maxTemps = response.daily?.temperature2mMax.orEmpty(),
          minTemps = response.daily?.temperature2mMin.orEmpty(),
          codes = response.daily?.weatherCode.orEmpty(),
        )

        val weekMin = dailyItems.minOfOrNull { it.minTempC } ?: current.temperature2m
        val weekMax = dailyItems.maxOfOrNull { it.maxTempC } ?: (current.temperature2m + 1.0)

        WeatherReport(
          location = WeatherLocation(latitude, longitude, resolvedName, isGps),
          formattedDate = formattedDate,
          formattedTime = formattedTime,
          utcOffsetSeconds = utcOffsetSec,
          localMinutesOfDay = localMinutesOfDay,
          currentTempC = current.temperature2m,
          feelsLikeC = current.apparentTemperature,
          humidityPercent = current.relativeHumidity2m,
          windSpeedMs = current.windSpeed10m,
          pressureHpa = current.surfacePressure,
          isDay = isDay,
          weatherCode = current.weatherCode,
          condition = condition,
          skyPhase = skyPhase,
          hourly = hourlyItems,
          daily = dailyItems,
          weekMinTempC = weekMin,
          weekMaxTempC = if (weekMax > weekMin) weekMax else weekMin + 1.0,
        )
      }
    }

  suspend fun searchCities(query: String): List<CitySuggestion> =
    withContext(Dispatchers.IO) {
      if (query.trim().length < 2) return@withContext emptyList()
      runCatching {
        val response = geocodingApi.searchLocations(query.trim())
        response.results.orEmpty().map { loc ->
          val parts = listOfNotNull(
            loc.name.takeIf { it.isNotBlank() },
            loc.admin1?.takeIf { it.isNotBlank() },
            loc.country?.takeIf { it.isNotBlank() },
          )
          CitySuggestion(
            id = loc.id,
            displayName = parts.joinToString(", "),
            latitude = loc.latitude,
            longitude = loc.longitude,
          )
        }
      }.getOrDefault(emptyList())
    }

  private suspend fun resolveNeighborhoodName(lat: Double, lon: Double): String {
    return runCatching {
      val geo = nominatimApi.reverseGeocode(lat = lat, lon = lon)
      val addr = geo.address ?: return@runCatching formatCoordFallback(lat, lon)
      val neighborhood =
        listOfNotNull(
            addr.suburb,
            addr.neighbourhood,
            addr.cityDistrict,
            addr.residential,
            addr.quarter,
          )
          .firstOrNull { it.isNotBlank() }
      val city =
        listOfNotNull(addr.city, addr.town, addr.village, addr.state)
          .firstOrNull { it.isNotBlank() } ?: "Local Area"
      val country = addr.country?.takeIf { it.isNotBlank() }

      buildString {
        if (neighborhood != null) {
          append(neighborhood)
          append(", ")
        }
        append(city)
        if (country != null && !contains(country, ignoreCase = true)) {
          append(", ")
          append(country)
        }
      }
    }.getOrElse { formatCoordFallback(lat, lon) }
  }

  private fun formatCoordFallback(lat: Double, lon: Double): String =
    String.format(Locale.US, "GPS Location (%.2f, %.2f)", lat, lon)

  private fun buildHourlyItems(
    times: List<String>,
    temps: List<Double>,
    codes: List<Int>,
    currentIsoTime: String,
    fallbackTemp: Double,
    fallbackCode: Int,
    fallbackIsDay: Boolean,
  ): List<HourlyForecastItem> {
    if (times.isEmpty() || temps.isEmpty()) return emptyList()
    val currentHourPrefix = currentIsoTime.take(13) // e.g. "2026-11-12T22"
    val startIdx = times.indexOfFirst { it.startsWith(currentHourPrefix) }.coerceAtLeast(0)
    val endIdx = (startIdx + 18).coerceAtMost(times.size)

    val result = mutableListOf<HourlyForecastItem>()
    for (idx in startIdx until endIdx) {
      val iso = times[idx]
      val hour24 = iso.substringAfter("T", "00").take(2).toIntOrNull() ?: 0
      val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
      val ampm = if (hour24 >= 12) "PM" else "AM"
      val isNow = idx == startIdx
      val isHourDay = hour24 in 6..18
      result += HourlyForecastItem(
        timeLabel = if (isNow) "Now" else "$h12 $ampm",
        tempC = temps.getOrElse(idx) { fallbackTemp },
        weatherCode = codes.getOrElse(idx) { fallbackCode },
        isDay = if (isNow) fallbackIsDay else isHourDay,
        isNow = isNow,
      )
    }
    return result
  }

  private fun buildDailyItems(
    dates: List<String>,
    maxTemps: List<Double>,
    minTemps: List<Double>,
    codes: List<Int>,
  ): List<DailyForecastItem> {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayFmt = SimpleDateFormat("EEE", Locale.US)
    val count = minOf(dates.size, maxTemps.size, minTemps.size, 7)

    return (0 until count).map { idx ->
      val dayLabel =
        if (idx == 0) {
          "Today"
        } else {
          runCatching {
            val d = parser.parse(dates[idx])
            if (d != null) dayFmt.format(d) else "Day ${idx + 1}"
          }.getOrDefault("Day ${idx + 1}")
        }
      DailyForecastItem(
        dayLabel = dayLabel,
        minTempC = minTemps[idx],
        maxTempC = maxTemps[idx],
        weatherCode = codes.getOrElse(idx) { 0 },
      )
    }
  }
}
