package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AmbientSoundType
import com.example.audio.AmbientWeatherSoundEngine
import com.example.audio.TrafficAudioConfig
import com.example.data.model.AlertSeverity
import com.example.data.model.CurrentWeatherDto
import com.example.data.model.HourlyWeatherDto
import com.example.data.model.SkyPhase
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherIconKind
import com.example.data.model.WeatherLocation
import com.example.data.model.WeatherReport
import com.example.data.model.WeatherResponseDto
import com.example.data.repository.SevereWeatherRepository
import com.example.notifications.WeatherAlertNotifier
import com.example.ui.components.convertTemp
import com.example.ui.components.formatExactLocalTime
import com.example.ui.components.resolveTrafficAudioConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aether", appName)
  }

  @Test
  fun `sky phase resolves day afternoon late afternoon sunset and night windows`() {
    assertEquals(SkyPhase.SKY_BLUE, SkyPhase.resolve(isDay = true, timeInMinutes = 600))
    assertEquals(SkyPhase.SKY_BLUE, SkyPhase.resolve(isDay = true, timeInMinutes = 945))
    assertEquals(SkyPhase.BEFORE_SUNSET, SkyPhase.resolve(isDay = true, timeInMinutes = 1020))
    assertEquals(SkyPhase.BEFORE_SUNSET, SkyPhase.resolve(isDay = false, timeInMinutes = 1050))
    assertEquals(SkyPhase.SUNSET, SkyPhase.resolve(isDay = false, timeInMinutes = 1190))
    assertEquals(SkyPhase.MIDNIGHT, SkyPhase.resolve(isDay = false, timeInMinutes = 90))
  }

  @Test
  fun `exact local time formats utc offset accurately`() {
    // 2026-11-13T10:30:00Z = 1794565800000L; Dhaka UTC+6 (+21600s) -> 4:30 PM
    val epochMillis = 1794565800000L
    assertEquals("4:30 PM", formatExactLocalTime(utcOffsetSeconds = 21600, timeMillis = epochMillis))
    assertEquals("5:30 AM", formatExactLocalTime(utcOffsetSeconds = -18000, timeMillis = epochMillis))
  }

  @Test
  fun `wmo code maps clear and rain conditions`() {
    val clear = WeatherCondition.fromWmoCode(0, isDay = true)
    assertEquals("CLEAR DAY", clear.label)
    assertEquals(WeatherIconKind.CLEAR_DAY, clear.iconKind)

    val rain = WeatherCondition.fromWmoCode(63, isDay = false)
    assertTrue(rain.isRainy)
    assertEquals(WeatherIconKind.RAIN, rain.iconKind)
  }

  @Test
  fun `temperature converts celsius to fahrenheit accurately`() {
    assertEquals(32.0, convertTemp(0.0, isFahrenheit = true), 0.01)
    assertEquals(86.0, convertTemp(30.0, isFahrenheit = true), 0.01)
    assertEquals(25.0, convertTemp(25.0, isFahrenheit = false), 0.01)
  }

  @Test
  fun `severe hazard detector flags storm wind gusts and calm skies`() {
    val calmForecast = WeatherResponseDto(
      current = CurrentWeatherDto(
        time = "2026-11-12T14:00",
        apparentTemperature = 26.0,
        weatherCode = 0,
        windGusts10m = 5.0,
        precipitation = 0.0,
      ),
      hourly = HourlyWeatherDto(
        time = listOf("2026-11-12T14:00", "2026-11-12T15:00"),
        temperature2m = listOf(26.0, 27.0),
        weatherCode = listOf(0, 1),
        windGusts10m = listOf(5.0, 6.0),
        precipitation = listOf(0.0, 0.0),
      ),
    )
    assertTrue(
      SevereWeatherRepository.evaluateGlobalSevereHazards(calmForecast, "Dhaka", 23.71, 90.42).isEmpty()
    )

    val stormForecast = WeatherResponseDto(
      current = CurrentWeatherDto(
        time = "2026-11-12T14:00",
        apparentTemperature = 31.0,
        weatherCode = 96,
        windGusts10m = 23.0,
        precipitation = 24.0,
      ),
      hourly = HourlyWeatherDto(
        time = listOf("2026-11-12T14:00"),
        temperature2m = listOf(31.0),
        weatherCode = listOf(96),
        windGusts10m = listOf(23.0),
        precipitation = listOf(24.0),
      ),
    )
    val alerts = SevereWeatherRepository.evaluateGlobalSevereHazards(stormForecast, "Dhaka", 23.71, 90.42)
    assertTrue(alerts.any { it.severity == AlertSeverity.EXTREME })
    assertTrue(alerts.any { it.eventTitle.contains("Thunderstorm") })
    assertTrue(alerts.any { it.eventTitle.contains("Wind Gusts") })
  }

  @Test
  fun `notifier creates severe weather notification channel`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val notifier = WeatherAlertNotifier(context)
    notifier.ensureChannelExists()
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertNotNull(nm.getNotificationChannel(WeatherAlertNotifier.CHANNEL_ID))
  }

  @Test
  fun `ambient sound type maps street cars chatter midnight crickets and rain accurately`() {
    val baseReport = WeatherReport(
      location = WeatherLocation(23.71, 90.42, "Swamibagh, Dhaka, Bangladesh"),
      formattedDate = "Friday, November 13, 2026",
      localMinutesOfDay = 720,
      currentTempC = 28.0,
      feelsLikeC = 29.0,
      humidityPercent = 60,
      windSpeedMs = 2.0,
      pressureHpa = 1012.0,
      isDay = true,
      weatherCode = 0,
      condition = WeatherCondition.fromWmoCode(0, isDay = true),
      skyPhase = SkyPhase.SKY_BLUE,
      hourly = emptyList(),
      daily = emptyList(),
      weekMinTempC = 22.0,
      weekMaxTempC = 30.0,
    )

    assertEquals(
      AmbientSoundType.STREET_TRAFFIC_AND_VOICES,
      AmbientSoundType.fromWeather(baseReport),
    )
    assertEquals(
      AmbientSoundType.MIDNIGHT_CRICKETS,
      AmbientSoundType.fromWeather(
        baseReport.copy(
          isDay = false,
          localMinutesOfDay = 45,
          skyPhase = SkyPhase.MIDNIGHT,
        )
      ),
    )
    assertEquals(
      AmbientSoundType.RAIN_SOUNDS,
      AmbientSoundType.fromWeather(
        baseReport.copy(weatherCode = 63, condition = WeatherCondition.fromWmoCode(63, isDay = true))
      ),
    )
    assertEquals(
      AmbientSoundType.THUNDER_RAIN,
      AmbientSoundType.fromWeather(
        baseReport.copy(weatherCode = 95, condition = WeatherCondition.fromWmoCode(95, isDay = true))
      ),
    )
  }

  @Test
  fun `ambient sound buffer generator produces non-silent bounded pcm loops`() {
    val rainPcm = AmbientWeatherSoundEngine.generateLoopBuffer(AmbientSoundType.RAIN_SOUNDS)
    val streetPcm = AmbientWeatherSoundEngine.generateLoopBuffer(AmbientSoundType.STREET_TRAFFIC_AND_VOICES)
    val cricketPcm = AmbientWeatherSoundEngine.generateLoopBuffer(AmbientSoundType.MIDNIGHT_CRICKETS)
    assertEquals(AmbientWeatherSoundEngine.LOOP_SAMPLES, rainPcm.size)
    assertEquals(AmbientWeatherSoundEngine.LOOP_SAMPLES, streetPcm.size)
    assertEquals(AmbientWeatherSoundEngine.LOOP_SAMPLES, cricketPcm.size)
    assertTrue(rainPcm.any { it != 0.toShort() })
    assertTrue(streetPcm.any { it != 0.toShort() })
    assertTrue(cricketPcm.any { it != 0.toShort() })
  }

  @Test
  fun `traffic audio config detects many cars and buses by city and differentiates horn buffers`() {
    val dhakaTraffic = resolveTrafficAudioConfig("Swamibagh, Dhaka, Bangladesh")
    assertTrue(dhakaTraffic.hasManyCars)
    assertTrue(dhakaTraffic.hasBus)

    val nycTraffic = resolveTrafficAudioConfig("New York, United States")
    assertTrue(nycTraffic.hasManyCars)
    assertTrue(nycTraffic.hasBus)

    val parisTraffic = resolveTrafficAudioConfig("Paris, France")
    assertFalse(parisTraffic.hasManyCars)
    assertTrue(parisTraffic.hasBus)

    val suburbTraffic = resolveTrafficAudioConfig("Maple Gardens Village")
    assertFalse(suburbTraffic.hasManyCars)
    assertFalse(suburbTraffic.hasBus)

    val quietCarsNoBus = AmbientWeatherSoundEngine.generateLoopBuffer(
      AmbientSoundType.STREET_TRAFFIC_AND_VOICES,
      TrafficAudioConfig(carCount = 2, busCount = 0),
    )
    val quietCarsWithBus = AmbientWeatherSoundEngine.generateLoopBuffer(
      AmbientSoundType.STREET_TRAFFIC_AND_VOICES,
      TrafficAudioConfig(carCount = 2, busCount = 1),
    )
    val manyCarsWithBus = AmbientWeatherSoundEngine.generateLoopBuffer(
      AmbientSoundType.STREET_TRAFFIC_AND_VOICES,
      TrafficAudioConfig(carCount = 6, busCount = 1),
    )

    // Bus horn window (2.75s..3.34s) differs when busCount > 0
    val busWindowStart = (2.80f * AmbientWeatherSoundEngine.SAMPLE_RATE).toInt()
    val busWindowEnd = (3.30f * AmbientWeatherSoundEngine.SAMPLE_RATE).toInt()
    val busWindowDiff = (busWindowStart until busWindowEnd).any { idx ->
      quietCarsNoBus[idx] != quietCarsWithBus[idx]
    }
    assertTrue(busWindowDiff)

    // Car horn window (1.35s..1.67s) differs when hasManyCars (carCount >= 4)
    val carWindowStart = (1.36f * AmbientWeatherSoundEngine.SAMPLE_RATE).toInt()
    val carWindowEnd = (1.66f * AmbientWeatherSoundEngine.SAMPLE_RATE).toInt()
    val carHornDiff = (carWindowStart until carWindowEnd).any { idx ->
      quietCarsWithBus[idx] != manyCarsWithBus[idx]
    }
    assertTrue(carHornDiff)
  }

  @Test
  fun `3d neighborhood diorama resolves shamibagh dhaka features and real new york display ads`() {
    val dhaka = com.example.ui.components.scenery.resolveCityDioramaSpec("Swamibagh, Dhaka, Bangladesh")
    assertEquals(com.example.ui.components.scenery.CityArchitectureStyle.DHAKA_SHAMIBAGH, dhaka.style)
    assertTrue(dhaka.isDusty)
    assertTrue(dhaka.hasOverheadWires)
    assertTrue(dhaka.hasPalms)
    assertTrue(dhaka.buildings.any { it.hasWaterTank })
    assertTrue(dhaka.buildings.any { it.hasDome })

    val nyc = com.example.ui.components.scenery.resolveCityDioramaSpec("New York, United States")
    assertEquals(com.example.ui.components.scenery.CityArchitectureStyle.NEW_YORK_TIMES_SQUARE, nyc.style)
    assertTrue(nyc.buildings.all { it.isGlassTower })
    assertTrue(nyc.buildings.any { it.billboard?.text == "COCA-COLA" })
    assertTrue(nyc.buildings.any { it.billboard?.text == "BROADWAY" })
    assertTrue(nyc.buildings.any { it.sideBillboard != null })

    val london = com.example.ui.components.scenery.resolveCityDioramaSpec("Westminster, London, United Kingdom")
    assertEquals(com.example.ui.components.scenery.CityArchitectureStyle.LONDON_WESTMINSTER, london.style)
    assertTrue(london.hasPhoneBooth)
  }
}
