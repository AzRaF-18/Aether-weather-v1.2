package com.example.data.remote

import com.example.data.model.GeocodingResponseDto
import com.example.data.model.NominatimResponseDto
import com.example.data.model.NwsAlertsResponseDto
import com.example.data.model.WeatherResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface OpenMeteoForecastApi {
  @GET("v1/forecast")
  suspend fun getForecast(
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("current")
    current: String =
      "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_gusts_10m,precipitation,surface_pressure",
    @Query("hourly") hourly: String = "temperature_2m,weather_code,wind_gusts_10m,precipitation",
    @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weather_code",
    @Query("wind_speed_unit") windSpeedUnit: String = "ms",
    @Query("timezone") timezone: String = "auto",
    @Query("forecast_days") forecastDays: Int = 7,
  ): WeatherResponseDto
}

interface NwsAlertsApi {
  @Headers("Accept: application/geo+json")
  @GET("alerts/active")
  suspend fun getActiveAlerts(
    @Query("point") point: String,
  ): NwsAlertsResponseDto
}

interface OpenMeteoGeocodingApi {
  @GET("v1/search")
  suspend fun searchLocations(
    @Query("name") name: String,
    @Query("count") count: Int = 6,
    @Query("language") language: String = "en",
    @Query("format") format: String = "json",
  ): GeocodingResponseDto
}

interface NominatimReverseApi {
  @GET("reverse")
  suspend fun reverseGeocode(
    @Query("format") format: String = "json",
    @Query("lat") lat: Double,
    @Query("lon") lon: Double,
    @Query("zoom") zoom: Int = 18,
    @Query("addressdetails") addressDetails: Int = 1,
    @Query("accept-language") acceptLanguage: String = "en",
  ): NominatimResponseDto
}

object NetworkModule {
  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private val okHttpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(12, TimeUnit.SECONDS)
      .addInterceptor { chain ->
        val req =
          chain.request()
            .newBuilder()
            .header("User-Agent", "AetherWeatherAndroid/1.0 (contact@aetherweather.app)")
            .build()
        chain.proceed(req)
      }
      .addInterceptor(
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
      )
      .build()

  val forecastApi: OpenMeteoForecastApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoForecastApi::class.java)
  }

  val nwsAlertsApi: NwsAlertsApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.weather.gov/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(NwsAlertsApi::class.java)
  }

  val geocodingApi: OpenMeteoGeocodingApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://geocoding-api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoGeocodingApi::class.java)
  }

  val nominatimApi: NominatimReverseApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://nominatim.openstreetmap.org/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(NominatimReverseApi::class.java)
  }
}
