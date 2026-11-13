package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponseDto(
  @param:Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int? = null,
  @param:Json(name = "current") val current: CurrentWeatherDto? = null,
  @param:Json(name = "hourly") val hourly: HourlyWeatherDto? = null,
  @param:Json(name = "daily") val daily: DailyWeatherDto? = null,
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
  @param:Json(name = "time") val time: String = "",
  @param:Json(name = "temperature_2m") val temperature2m: Double = 0.0,
  @param:Json(name = "relative_humidity_2m") val relativeHumidity2m: Int = 0,
  @param:Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
  @param:Json(name = "is_day") val isDay: Int = 1,
  @param:Json(name = "weather_code") val weatherCode: Int = 0,
  @param:Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0,
  @param:Json(name = "wind_gusts_10m") val windGusts10m: Double = 0.0,
  @param:Json(name = "precipitation") val precipitation: Double = 0.0,
  @param:Json(name = "surface_pressure") val surfacePressure: Double = 1013.0,
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
  @param:Json(name = "time") val time: List<String> = emptyList(),
  @param:Json(name = "temperature_2m") val temperature2m: List<Double> = emptyList(),
  @param:Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
  @param:Json(name = "wind_gusts_10m") val windGusts10m: List<Double> = emptyList(),
  @param:Json(name = "precipitation") val precipitation: List<Double> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
  @param:Json(name = "time") val time: List<String> = emptyList(),
  @param:Json(name = "temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
  @param:Json(name = "temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
  @param:Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class NwsAlertsResponseDto(
  @param:Json(name = "features") val features: List<NwsAlertFeatureDto>? = null,
)

@JsonClass(generateAdapter = true)
data class NwsAlertFeatureDto(
  @param:Json(name = "id") val id: String? = null,
  @param:Json(name = "properties") val properties: NwsAlertPropertiesDto? = null,
)

@JsonClass(generateAdapter = true)
data class NwsAlertPropertiesDto(
  @param:Json(name = "id") val id: String? = null,
  @param:Json(name = "event") val event: String? = null,
  @param:Json(name = "severity") val severity: String? = null,
  @param:Json(name = "headline") val headline: String? = null,
  @param:Json(name = "description") val description: String? = null,
  @param:Json(name = "instruction") val instruction: String? = null,
  @param:Json(name = "expires") val expires: String? = null,
)

@JsonClass(generateAdapter = true)
data class GeocodingResponseDto(
  @param:Json(name = "results") val results: List<GeocodingResultDto>? = null,
)

@JsonClass(generateAdapter = true)
data class GeocodingResultDto(
  @param:Json(name = "id") val id: Long = 0L,
  @param:Json(name = "name") val name: String = "",
  @param:Json(name = "latitude") val latitude: Double = 0.0,
  @param:Json(name = "longitude") val longitude: Double = 0.0,
  @param:Json(name = "country") val country: String? = null,
  @param:Json(name = "admin1") val admin1: String? = null,
)

@JsonClass(generateAdapter = true)
data class NominatimResponseDto(
  @param:Json(name = "address") val address: NominatimAddressDto? = null,
)

@JsonClass(generateAdapter = true)
data class NominatimAddressDto(
  @param:Json(name = "suburb") val suburb: String? = null,
  @param:Json(name = "neighbourhood") val neighbourhood: String? = null,
  @param:Json(name = "city_district") val cityDistrict: String? = null,
  @param:Json(name = "residential") val residential: String? = null,
  @param:Json(name = "quarter") val quarter: String? = null,
  @param:Json(name = "city") val city: String? = null,
  @param:Json(name = "town") val town: String? = null,
  @param:Json(name = "village") val village: String? = null,
  @param:Json(name = "state") val state: String? = null,
  @param:Json(name = "country") val country: String? = null,
)
