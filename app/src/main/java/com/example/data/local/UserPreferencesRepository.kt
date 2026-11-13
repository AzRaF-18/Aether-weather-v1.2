package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PresetLocations
import com.example.data.model.WeatherIconKind
import com.example.data.model.WeatherLocation
import com.example.data.model.WeatherReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aether_prefs")

data class WidgetWeatherSnapshot(
  val cityName: String,
  val tempC: Double,
  val feelsLikeC: Double,
  val humidityPercent: Int,
  val windSpeedMs: Double,
  val conditionLabel: String,
  val iconKind: WeatherIconKind,
  val updatedTimeLabel: String,
)

data class SavedPreferences(
  val isFahrenheit: Boolean = false,
  val lastLocation: WeatherLocation = PresetLocations.SWAMIBAGH,
  val severeAlertsEnabled: Boolean = true,
  val notifiedAlertIds: Set<String> = emptySet(),
  val widgetSnapshot: WidgetWeatherSnapshot? = null,
  val accentColorArgb: Long = 0xFFEF4444L,
  val ambientSoundEnabled: Boolean = true,
)

class UserPreferencesRepository(private val context: Context) {

  private object Keys {
    val IS_FAHRENHEIT = booleanPreferencesKey("is_fahrenheit")
    val LAST_LAT = doublePreferencesKey("last_lat")
    val LAST_LON = doublePreferencesKey("last_lon")
    val LAST_NAME = stringPreferencesKey("last_name")
    val LAST_IS_GPS = booleanPreferencesKey("last_is_gps")
    val SEVERE_ALERTS_ENABLED = booleanPreferencesKey("severe_alerts_enabled")
    val NOTIFIED_ALERT_IDS = stringSetPreferencesKey("notified_alert_ids")
    val ACCENT_COLOR_ARGB = androidx.datastore.preferences.core.longPreferencesKey("accent_color_argb")
    val AMBIENT_SOUND_ENABLED = booleanPreferencesKey("ambient_sound_enabled")

    val WIDGET_CITY = stringPreferencesKey("widget_city")
    val WIDGET_TEMP_C = doublePreferencesKey("widget_temp_c")
    val WIDGET_FEELS_C = doublePreferencesKey("widget_feels_c")
    val WIDGET_HUMIDITY = intPreferencesKey("widget_humidity")
    val WIDGET_WIND_MS = doublePreferencesKey("widget_wind_ms")
    val WIDGET_CONDITION = stringPreferencesKey("widget_condition")
    val WIDGET_ICON_KIND = stringPreferencesKey("widget_icon_kind")
    val WIDGET_UPDATED_AT = stringPreferencesKey("widget_updated_at")
  }

  val preferencesFlow: Flow<SavedPreferences> =
    context.dataStore.data.map { prefs ->
      val lat = prefs[Keys.LAST_LAT]
      val lon = prefs[Keys.LAST_LON]
      val name = prefs[Keys.LAST_NAME]
      val isGps = prefs[Keys.LAST_IS_GPS] ?: false
      val location =
        if (lat != null && lon != null && !name.isNullOrBlank()) {
          WeatherLocation(lat, lon, name, isGps)
        } else {
          PresetLocations.SWAMIBAGH
        }

      val wCity = prefs[Keys.WIDGET_CITY]
      val wTemp = prefs[Keys.WIDGET_TEMP_C]
      val snapshot =
        if (!wCity.isNullOrBlank() && wTemp != null) {
          val iconKind =
            runCatching {
              WeatherIconKind.valueOf(prefs[Keys.WIDGET_ICON_KIND] ?: WeatherIconKind.CLEAR_DAY.name)
            }.getOrDefault(WeatherIconKind.CLEAR_DAY)
          WidgetWeatherSnapshot(
            cityName = wCity,
            tempC = wTemp,
            feelsLikeC = prefs[Keys.WIDGET_FEELS_C] ?: wTemp,
            humidityPercent = prefs[Keys.WIDGET_HUMIDITY] ?: 0,
            windSpeedMs = prefs[Keys.WIDGET_WIND_MS] ?: 0.0,
            conditionLabel = prefs[Keys.WIDGET_CONDITION] ?: "CLEAR DAY",
            iconKind = iconKind,
            updatedTimeLabel = prefs[Keys.WIDGET_UPDATED_AT] ?: "--:--",
          )
        } else {
          null
        }

      SavedPreferences(
        isFahrenheit = prefs[Keys.IS_FAHRENHEIT] ?: false,
        lastLocation = location,
        severeAlertsEnabled = prefs[Keys.SEVERE_ALERTS_ENABLED] ?: true,
        notifiedAlertIds = prefs[Keys.NOTIFIED_ALERT_IDS] ?: emptySet(),
        widgetSnapshot = snapshot,
        accentColorArgb = prefs[Keys.ACCENT_COLOR_ARGB] ?: 0xFFEF4444L,
        ambientSoundEnabled = prefs[Keys.AMBIENT_SOUND_ENABLED] ?: true,
      )
    }

  suspend fun setAccentColor(argb: Long) {
    context.dataStore.edit { prefs -> prefs[Keys.ACCENT_COLOR_ARGB] = argb }
  }

  suspend fun setAmbientSoundEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.AMBIENT_SOUND_ENABLED] = enabled }
  }

  suspend fun setFahrenheit(isFahrenheit: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.IS_FAHRENHEIT] = isFahrenheit }
  }

  suspend fun saveLocation(location: WeatherLocation) {
    context.dataStore.edit { prefs ->
      prefs[Keys.LAST_LAT] = location.latitude
      prefs[Keys.LAST_LON] = location.longitude
      prefs[Keys.LAST_NAME] = location.displayName
      prefs[Keys.LAST_IS_GPS] = location.isGps
    }
  }

  suspend fun saveWidgetSnapshot(report: WeatherReport) {
    val timeFmt = SimpleDateFormat("h:mm a", Locale.US).format(Date())
    context.dataStore.edit { prefs ->
      prefs[Keys.WIDGET_CITY] = report.location.displayName
      prefs[Keys.WIDGET_TEMP_C] = report.currentTempC
      prefs[Keys.WIDGET_FEELS_C] = report.feelsLikeC
      prefs[Keys.WIDGET_HUMIDITY] = report.humidityPercent
      prefs[Keys.WIDGET_WIND_MS] = report.windSpeedMs
      prefs[Keys.WIDGET_CONDITION] = report.condition.label
      prefs[Keys.WIDGET_ICON_KIND] = report.condition.iconKind.name
      prefs[Keys.WIDGET_UPDATED_AT] = timeFmt
    }
  }

  suspend fun setSevereAlertsEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.SEVERE_ALERTS_ENABLED] = enabled }
  }

  suspend fun markAlertsNotified(alertIds: Set<String>) {
    if (alertIds.isEmpty()) return
    context.dataStore.edit { prefs ->
      val existing = prefs[Keys.NOTIFIED_ALERT_IDS] ?: emptySet()
      val combined = (existing + alertIds).toList().takeLast(50).toSet()
      prefs[Keys.NOTIFIED_ALERT_IDS] = combined
    }
  }
}
