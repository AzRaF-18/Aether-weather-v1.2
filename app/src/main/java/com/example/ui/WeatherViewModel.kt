package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AmbientSoundType
import com.example.audio.AmbientWeatherSoundEngine
import com.example.audio.TrafficAudioConfig
import com.example.ui.components.resolveTrafficAudioConfig
import com.example.data.local.UserPreferencesRepository
import com.example.data.location.LocationClient
import com.example.data.model.CitySuggestion
import com.example.data.model.PresetLocations
import com.example.data.model.SevereWeatherAlert
import com.example.data.model.WeatherLocation
import com.example.data.model.WeatherReport
import com.example.data.repository.SevereWeatherRepository
import com.example.data.repository.WeatherRepository
import com.example.notifications.AlertWorkScheduler
import com.example.notifications.WeatherAlertNotifier
import com.example.widget.AetherWeatherWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WeatherContentState {
  data object Loading : WeatherContentState

  data class Success(val report: WeatherReport) : WeatherContentState

  data class Error(val message: String) : WeatherContentState
}

enum class GpsStatus {
  ACTIVE,
  LOCATING,
  DEFAULT_FALLBACK,
  PERMISSION_NEEDED,
  NETWORK_ERROR,
}

data class WeatherUiState(
  val contentState: WeatherContentState = WeatherContentState.Loading,
  val isRefreshing: Boolean = false,
  val isFahrenheit: Boolean = false,
  val gpsStatus: GpsStatus = GpsStatus.DEFAULT_FALLBACK,
  val activeLocation: WeatherLocation = PresetLocations.SWAMIBAGH,
  val searchQuery: String = "",
  val suggestions: List<CitySuggestion> = emptyList(),
  val isSearchingCities: Boolean = false,
  val severeAlerts: List<SevereWeatherAlert> = emptyList(),
  val severeAlertsEnabled: Boolean = true,
  val hasNotificationPermission: Boolean = false,
  val isCheckingAlerts: Boolean = false,
  val accentColorArgb: Long = 0xFFEF4444L,
  val ambientSoundEnabled: Boolean = true,
  val activeAmbientSoundLabel: String = "Standby",
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = WeatherRepository()
  private val severeRepository = SevereWeatherRepository()
  private val prefsRepository = UserPreferencesRepository(application.applicationContext)
  private val locationClient = LocationClient(application.applicationContext)
  private val alertNotifier = WeatherAlertNotifier(application.applicationContext)
  private val soundEngine = AmbientWeatherSoundEngine()
  private var isUiForeground: Boolean = true

  private val _uiState = MutableStateFlow(WeatherUiState())
  val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

  private var searchJob: Job? = null
  private var weatherJob: Job? = null
  private var alertJob: Job? = null

  init {
    alertNotifier.ensureChannelExists()
    bootstrap()
  }

  private fun bootstrap() {
    viewModelScope.launch {
      val saved = prefsRepository.preferencesFlow.firstOrNull()
      val initialLocation = saved?.lastLocation ?: PresetLocations.SWAMIBAGH
      val initialUnit = saved?.isFahrenheit ?: false
      val alertsEnabled = saved?.severeAlertsEnabled ?: true
      val notifPerm = alertNotifier.hasNotificationPermission()
      val initialAccent = saved?.accentColorArgb ?: 0xFFEF4444L
      val initialAmbientSound = saved?.ambientSoundEnabled ?: true

      _uiState.update {
        it.copy(
          isFahrenheit = initialUnit,
          activeLocation = initialLocation,
          severeAlertsEnabled = alertsEnabled,
          hasNotificationPermission = notifPerm,
          accentColorArgb = initialAccent,
          ambientSoundEnabled = initialAmbientSound,
          gpsStatus =
            if (initialLocation.isGps && locationClient.hasLocationPermission()) {
              GpsStatus.ACTIVE
            } else {
              GpsStatus.DEFAULT_FALLBACK
            },
        )
      }

      if (alertsEnabled && notifPerm) {
        AlertWorkScheduler.schedulePeriodicAlerts(getApplication())
      }

      // Instant non-blocking startup with saved or default Swamibagh location
      loadWeather(initialLocation, showLoadingIfEmpty = true)

      // Background live GPS sync if permission is already granted
      if (locationClient.hasLocationPermission()) {
        syncLiveGps(silentFallback = true)
      }
    }
  }

  fun hasLocationPermission(): Boolean = locationClient.hasLocationPermission()

  fun hasNotificationPermission(): Boolean = alertNotifier.hasNotificationPermission()

  fun onLocationPermissionResult(granted: Boolean) {
    if (granted) {
      syncLiveGps(silentFallback = false)
    } else {
      _uiState.update { it.copy(gpsStatus = GpsStatus.PERMISSION_NEEDED) }
    }
  }

  fun onNotificationPermissionResult(granted: Boolean) {
    _uiState.update {
      it.copy(
        hasNotificationPermission = granted,
        severeAlertsEnabled = if (granted) true else it.severeAlertsEnabled,
      )
    }
    viewModelScope.launch {
      if (granted) {
        prefsRepository.setSevereAlertsEnabled(true)
        AlertWorkScheduler.schedulePeriodicAlerts(getApplication())
        checkSevereAlertsForLocation(_uiState.value.activeLocation, forceNotify = false)
      }
    }
  }

  fun setAccentColor(argb: Long) {
    _uiState.update { it.copy(accentColorArgb = argb) }
    viewModelScope.launch { prefsRepository.setAccentColor(argb) }
  }

  fun setAmbientSoundEnabled(enabled: Boolean) {
    _uiState.update { it.copy(ambientSoundEnabled = enabled) }
    viewModelScope.launch { prefsRepository.setAmbientSoundEnabled(enabled) }
    syncAmbientSoundEngine()
  }

  fun onForegroundChanged(foreground: Boolean) {
    isUiForeground = foreground
    syncAmbientSoundEngine()
  }

  private fun syncAmbientSoundEngine() {
    val state = _uiState.value
    val report = (state.contentState as? WeatherContentState.Success)?.report
    val soundType = report?.let { AmbientSoundType.fromWeather(it) }
    val trafficConfig = report?.let { resolveTrafficAudioConfig(it.location.displayName) }
      ?: TrafficAudioConfig()
    val label = soundType?.let { AmbientSoundType.labelFor(it, trafficConfig) } ?: "Standby"
    _uiState.update {
      it.copy(activeAmbientSoundLabel = label)
    }
    soundEngine.updateState(
      enabled = state.ambientSoundEnabled,
      soundType = soundType,
      foreground = isUiForeground,
      trafficConfig = trafficConfig,
    )
  }

  fun setSevereAlertsEnabled(enabled: Boolean) {
    _uiState.update { it.copy(severeAlertsEnabled = enabled) }
    viewModelScope.launch {
      prefsRepository.setSevereAlertsEnabled(enabled)
      if (enabled && alertNotifier.hasNotificationPermission()) {
        AlertWorkScheduler.schedulePeriodicAlerts(getApplication())
        checkSevereAlertsForLocation(_uiState.value.activeLocation, forceNotify = false)
      } else {
        AlertWorkScheduler.cancelAlerts(getApplication())
      }
    }
  }

  fun checkSevereAlertsNow(onFinished: (Int) -> Unit = {}) {
    val loc = _uiState.value.activeLocation
    checkSevereAlertsForLocation(loc, forceNotify = true, onFinished = onFinished)
  }

  fun sendTestAlertNotification(): Boolean {
    val locName = _uiState.value.activeLocation.displayName.ifBlank { "Current Location" }
    val sent = alertNotifier.sendSampleTestNotification(locName)
    _uiState.update { it.copy(hasNotificationPermission = alertNotifier.hasNotificationPermission()) }
    return sent
  }

  fun syncLiveGps(silentFallback: Boolean = false) {
    viewModelScope.launch {
      if (!locationClient.hasLocationPermission()) {
        if (!silentFallback) {
          _uiState.update { it.copy(gpsStatus = GpsStatus.PERMISSION_NEEDED) }
        }
        return@launch
      }

      _uiState.update { it.copy(gpsStatus = GpsStatus.LOCATING) }
      val coords = locationClient.getCurrentCoordinates()
      if (coords != null) {
        val gpsLoc =
          WeatherLocation(
            latitude = coords.first,
            longitude = coords.second,
            displayName = "",
            isGps = true,
          )
        loadWeather(
          location = gpsLoc,
          customName = null,
          showLoadingIfEmpty = false,
          markGpsActive = true,
        )
      } else if (!silentFallback) {
        loadWeather(
          location = PresetLocations.SWAMIBAGH,
          customName = PresetLocations.SWAMIBAGH.displayName,
          showLoadingIfEmpty = false,
          markGpsActive = false,
        )
      } else {
        val currentIsGps = _uiState.value.activeLocation.isGps
        _uiState.update {
          it.copy(gpsStatus = if (currentIsGps) GpsStatus.ACTIVE else GpsStatus.DEFAULT_FALLBACK)
        }
      }
    }
  }

  fun selectPresetOrCity(location: WeatherLocation) {
    _uiState.update { it.copy(searchQuery = "", suggestions = emptyList()) }
    loadWeather(
      location = location,
      customName = location.displayName,
      showLoadingIfEmpty = false,
      markGpsActive = false,
    )
  }

  fun selectSuggestion(suggestion: CitySuggestion) {
    val location =
      WeatherLocation(
        latitude = suggestion.latitude,
        longitude = suggestion.longitude,
        displayName = suggestion.displayName,
        isGps = false,
      )
    _uiState.update { it.copy(searchQuery = "", suggestions = emptyList(), isSearchingCities = false) }
    loadWeather(
      location = location,
      customName = suggestion.displayName,
      showLoadingIfEmpty = false,
      markGpsActive = false,
    )
  }

  fun onSearchQueryChanged(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    searchJob?.cancel()
    val trimmed = query.trim()
    if (trimmed.length < 2) {
      _uiState.update { it.copy(suggestions = emptyList(), isSearchingCities = false) }
      return
    }
    searchJob =
      viewModelScope.launch {
        _uiState.update { it.copy(isSearchingCities = true) }
        delay(300L)
        val results = repository.searchCities(trimmed)
        _uiState.update { it.copy(suggestions = results, isSearchingCities = false) }
      }
  }

  fun clearSearch() {
    searchJob?.cancel()
    _uiState.update { it.copy(searchQuery = "", suggestions = emptyList(), isSearchingCities = false) }
  }

  fun toggleTemperatureUnit() {
    val nextUnit = !_uiState.value.isFahrenheit
    _uiState.update { it.copy(isFahrenheit = nextUnit) }
    viewModelScope.launch {
      prefsRepository.setFahrenheit(nextUnit)
      runCatching { AetherWeatherWidget().updateAll(getApplication()) }
    }
  }

  fun refreshWeather() {
    val current = _uiState.value.activeLocation
    if (current.isGps && locationClient.hasLocationPermission()) {
      _uiState.update { it.copy(isRefreshing = true) }
      syncLiveGps(silentFallback = false)
    } else {
      loadWeather(
        location = current,
        customName = current.displayName.takeIf { it.isNotBlank() && !current.isGps },
        showLoadingIfEmpty = false,
        markGpsActive = current.isGps,
        isPullRefresh = true,
      )
    }
  }

  private fun loadWeather(
    location: WeatherLocation,
    customName: String? = location.displayName.takeIf { it.isNotBlank() && !location.isGps },
    showLoadingIfEmpty: Boolean = false,
    markGpsActive: Boolean = location.isGps,
    isPullRefresh: Boolean = false,
  ) {
    weatherJob?.cancel()
    weatherJob =
      viewModelScope.launch {
        if (showLoadingIfEmpty && _uiState.value.contentState !is WeatherContentState.Success) {
          _uiState.update { it.copy(contentState = WeatherContentState.Loading) }
        }
        if (isPullRefresh) {
          _uiState.update { it.copy(isRefreshing = true) }
        }

        val result =
          repository.fetchWeather(
            latitude = location.latitude,
            longitude = location.longitude,
            customName = customName,
            isGps = markGpsActive,
          )

        result
          .onSuccess { report ->
            prefsRepository.saveLocation(report.location)
            prefsRepository.saveWidgetSnapshot(report)
            runCatching { AetherWeatherWidget().updateAll(getApplication()) }
            _uiState.update {
              it.copy(
                contentState = WeatherContentState.Success(report),
                activeLocation = report.location,
                isRefreshing = false,
                gpsStatus = if (markGpsActive) GpsStatus.ACTIVE else GpsStatus.DEFAULT_FALLBACK,
              )
            }
            syncAmbientSoundEngine()
            checkSevereAlertsForLocation(report.location, forceNotify = false)
          }
          .onFailure { throwable ->
            val currentState = _uiState.value.contentState
            _uiState.update {
              it.copy(
                isRefreshing = false,
                gpsStatus = GpsStatus.NETWORK_ERROR,
                contentState =
                  if (currentState is WeatherContentState.Success) {
                    currentState
                  } else {
                    WeatherContentState.Error(
                      throwable.localizedMessage ?: "Unable to load live weather"
                    )
                  },
              )
            }
          }
      }
  }

  private fun checkSevereAlertsForLocation(
    location: WeatherLocation,
    forceNotify: Boolean,
    onFinished: (Int) -> Unit = {},
  ) {
    alertJob?.cancel()
    alertJob =
      viewModelScope.launch {
        _uiState.update { it.copy(isCheckingAlerts = true) }
        val alerts =
          severeRepository.fetchSevereAlerts(
            latitude = location.latitude,
            longitude = location.longitude,
            locationName = location.displayName.ifBlank { "Current Location" },
          )

        _uiState.update {
          it.copy(
            severeAlerts = alerts,
            isCheckingAlerts = false,
            hasNotificationPermission = alertNotifier.hasNotificationPermission(),
          )
        }

        val state = _uiState.value
        if (state.severeAlertsEnabled && alertNotifier.hasNotificationPermission() && alerts.isNotEmpty()) {
          val savedPrefs = prefsRepository.preferencesFlow.firstOrNull()
          val alreadyNotified = savedPrefs?.notifiedAlertIds ?: emptySet()
          val toNotify = if (forceNotify) alerts else alerts.filter { it.id !in alreadyNotified }
          toNotify.forEach { alertNotifier.showAlertNotification(it) }
          if (toNotify.isNotEmpty()) {
            prefsRepository.markAlertsNotified(toNotify.map { it.id }.toSet())
          }
        }

        onFinished(alerts.size)
      }
  }

  override fun onCleared() {
    soundEngine.release()
    super.onCleared()
  }
}
