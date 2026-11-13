package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.SkyPhase
import com.example.ui.components.DynamicSkyBackground
import com.example.ui.components.HeroWeatherSection
import com.example.ui.components.HourlyForecastCard
import com.example.ui.components.LiveSceneryCard
import com.example.ui.components.SevereAlertsCard
import com.example.ui.components.ThemeSettingsSheet
import com.example.ui.components.WeatherHeader
import com.example.ui.components.WeeklyForecastCard
import com.example.ui.components.contrastingOnAccent
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SolarYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherScreen(
  viewModel: WeatherViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val accentColor = Color(uiState.accentColorArgb)
  var showSettingsSheet by remember { mutableStateOf(false) }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner, viewModel) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> viewModel.onForegroundChanged(true)
        Lifecycle.Event.ON_STOP -> viewModel.onForegroundChanged(false)
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      viewModel.onForegroundChanged(false)
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    viewModel.onLocationPermissionResult(granted)
    if (!granted) {
      scope.launch {
        snackbarHostState.showSnackbar("Location permission declined. Search any city or pick a preset.")
      }
    }
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    viewModel.onNotificationPermissionResult(granted)
    scope.launch {
      if (granted) {
        snackbarHostState.showSnackbar("Severe weather push notifications enabled.")
      } else {
        snackbarHostState.showSnackbar("Notification permission declined. In-app alerts remain active.")
      }
    }
  }

  LaunchedEffect(Unit) {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        uiState.severeAlertsEnabled &&
        !viewModel.hasNotificationPermission()
    ) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  val handleGpsRequest: () -> Unit = {
    if (viewModel.hasLocationPermission()) {
      viewModel.syncLiveGps(silentFallback = false)
    } else {
      locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
  }

  val handleToggleAlerts: (Boolean) -> Unit = { enabled ->
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.hasNotificationPermission()) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      viewModel.setSevereAlertsEnabled(enabled)
    }
  }

  val handleCheckNow: () -> Unit = {
    viewModel.checkSevereAlertsNow { count ->
      scope.launch {
        val locName = uiState.activeLocation.displayName.ifBlank { "your location" }
        val msg =
          if (count > 0) "$count active severe weather alert(s) found for $locName."
          else "All clear — no severe weather alerts for $locName."
        snackbarHostState.showSnackbar(msg)
      }
    }
  }

  val handleSendTestAlert: () -> Unit = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.hasNotificationPermission()) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      val sent = viewModel.sendTestAlertNotification()
      scope.launch {
        val msg =
          if (sent) "Test severe weather notification sent."
          else "Enable notifications in Android Settings to preview alerts."
        snackbarHostState.showSnackbar(msg)
      }
    }
  }

  val activeSkyPhase =
    when (val state = uiState.contentState) {
      is WeatherContentState.Success -> state.report.skyPhase
      else -> SkyPhase.DEEP_NIGHT
    }

  Box(modifier = Modifier.fillMaxSize()) {
    DynamicSkyBackground(skyPhase = activeSkyPhase)

    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
      PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refreshWeather() },
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        BoxWithConstraints(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.TopCenter,
        ) {
          val isExpandedLayout = maxWidth >= 840.dp
          val scrollState = rememberScrollState()

          if (isExpandedLayout) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1120.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(24.dp),
              verticalAlignment = Alignment.Top,
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                WeatherHeader(
                  gpsStatus = uiState.gpsStatus,
                  searchQuery = uiState.searchQuery,
                  suggestions = uiState.suggestions,
                  isSearchingCities = uiState.isSearchingCities,
                  activeLocation = uiState.activeLocation,
                  accentColor = accentColor,
                  onSearchQueryChanged = viewModel::onSearchQueryChanged,
                  onClearSearch = viewModel::clearSearch,
                  onSuggestionSelected = viewModel::selectSuggestion,
                  onGpsClick = handleGpsRequest,
                  onPresetClick = viewModel::selectPresetOrCity,
                  onOpenSettingsClick = { showSettingsSheet = true },
                )

                when (val content = uiState.contentState) {
                  is WeatherContentState.Loading -> LoadingCard(accentColor = accentColor)
                  is WeatherContentState.Error -> ErrorCard(
                    message = content.message,
                    accentColor = accentColor,
                    onRetry = viewModel::refreshWeather,
                  )
                  is WeatherContentState.Success -> {
                    HeroWeatherSection(
                      report = content.report,
                      isFahrenheit = uiState.isFahrenheit,
                      accentColor = accentColor,
                      onToggleUnit = viewModel::toggleTemperatureUnit,
                    )
                  }
                }
              }

              if (uiState.contentState is WeatherContentState.Success) {
                val report = (uiState.contentState as WeatherContentState.Success).report
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  LiveSceneryCard(
                    location = report.location,
                    isDay = report.isDay,
                    isRainy = report.condition.isRainy,
                    accentColor = accentColor,
                    ambientSoundEnabled = uiState.ambientSoundEnabled,
                    activeAmbientSoundLabel = uiState.activeAmbientSoundLabel,
                    onToggleAmbientSound = viewModel::setAmbientSoundEnabled,
                  )
                  SevereAlertsCard(
                    alerts = uiState.severeAlerts,
                    alertsEnabled = uiState.severeAlertsEnabled,
                    hasNotificationPermission = uiState.hasNotificationPermission,
                    isCheckingAlerts = uiState.isCheckingAlerts,
                    locationName = report.location.displayName,
                    accentColor = accentColor,
                    isDay = report.isDay,
                    isRainy = report.condition.isRainy,
                    onToggleAlerts = handleToggleAlerts,
                    onCheckNow = handleCheckNow,
                    onSendTestNotification = handleSendTestAlert,
                  )
                  HourlyForecastCard(
                    hourlyItems = report.hourly,
                    isFahrenheit = uiState.isFahrenheit,
                    accentColor = accentColor,
                  )
                  WeeklyForecastCard(
                    dailyItems = report.daily,
                    weekMinTempC = report.weekMinTempC,
                    weekMaxTempC = report.weekMaxTempC,
                    isFahrenheit = uiState.isFahrenheit,
                    accentColor = accentColor,
                  )
                }
              }
            }
          } else {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              WeatherHeader(
                gpsStatus = uiState.gpsStatus,
                searchQuery = uiState.searchQuery,
                suggestions = uiState.suggestions,
                isSearchingCities = uiState.isSearchingCities,
                activeLocation = uiState.activeLocation,
                accentColor = accentColor,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onClearSearch = viewModel::clearSearch,
                onSuggestionSelected = viewModel::selectSuggestion,
                onGpsClick = handleGpsRequest,
                onPresetClick = viewModel::selectPresetOrCity,
                onOpenSettingsClick = { showSettingsSheet = true },
              )

              when (val content = uiState.contentState) {
                is WeatherContentState.Loading -> LoadingCard(accentColor = accentColor)
                is WeatherContentState.Error -> ErrorCard(
                  message = content.message,
                  accentColor = accentColor,
                  onRetry = viewModel::refreshWeather,
                )
                is WeatherContentState.Success -> {
                  HeroWeatherSection(
                    report = content.report,
                    isFahrenheit = uiState.isFahrenheit,
                    accentColor = accentColor,
                    onToggleUnit = viewModel::toggleTemperatureUnit,
                  )
                  LiveSceneryCard(
                    location = content.report.location,
                    isDay = content.report.isDay,
                    isRainy = content.report.condition.isRainy,
                    accentColor = accentColor,
                    ambientSoundEnabled = uiState.ambientSoundEnabled,
                    activeAmbientSoundLabel = uiState.activeAmbientSoundLabel,
                    onToggleAmbientSound = viewModel::setAmbientSoundEnabled,
                  )
                  SevereAlertsCard(
                    alerts = uiState.severeAlerts,
                    alertsEnabled = uiState.severeAlertsEnabled,
                    hasNotificationPermission = uiState.hasNotificationPermission,
                    isCheckingAlerts = uiState.isCheckingAlerts,
                    locationName = content.report.location.displayName,
                    accentColor = accentColor,
                    isDay = content.report.isDay,
                    isRainy = content.report.condition.isRainy,
                    onToggleAlerts = handleToggleAlerts,
                    onCheckNow = handleCheckNow,
                    onSendTestNotification = handleSendTestAlert,
                  )
                  HourlyForecastCard(
                    hourlyItems = content.report.hourly,
                    isFahrenheit = uiState.isFahrenheit,
                    accentColor = accentColor,
                  )
                  WeeklyForecastCard(
                    dailyItems = content.report.daily,
                    weekMinTempC = content.report.weekMinTempC,
                    weekMaxTempC = content.report.weekMaxTempC,
                    isFahrenheit = uiState.isFahrenheit,
                    accentColor = accentColor,
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))
            }
          }
        }
      }
    }

    if (showSettingsSheet) {
      ThemeSettingsSheet(
        accentColorArgb = uiState.accentColorArgb,
        isFahrenheit = uiState.isFahrenheit,
        alertsEnabled = uiState.severeAlertsEnabled,
        hasNotificationPermission = uiState.hasNotificationPermission,
        ambientSoundEnabled = uiState.ambientSoundEnabled,
        activeAmbientSoundLabel = uiState.activeAmbientSoundLabel,
        onSelectAccentColor = viewModel::setAccentColor,
        onToggleUnit = { targetFahrenheit ->
          if (targetFahrenheit != uiState.isFahrenheit) {
            viewModel.toggleTemperatureUnit()
          }
        },
        onToggleAlerts = handleToggleAlerts,
        onToggleAmbientSound = viewModel::setAmbientSoundEnabled,
        onDismiss = { showSettingsSheet = false },
      )
    }
  }
}

@Composable
private fun LoadingCard(accentColor: Color) {
  com.example.ui.components.WaterGlassSurface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("loading_card"),
    shape = RoundedCornerShape(26.dp),
    cornerRadius = 26.dp,
  ) {
    Column(
      modifier = Modifier.padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      CircularProgressIndicator(color = accentColor)
      Text(
        text = "SYNCING ATMOSPHERE…",
        style = MaterialTheme.typography.titleSmall,
        color = SolarYellow,
      )
    }
  }
}

@Composable
private fun ErrorCard(
  message: String,
  accentColor: Color,
  onRetry: () -> Unit,
) {
  com.example.ui.components.WaterGlassSurface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("error_card"),
    shape = RoundedCornerShape(26.dp),
    cornerRadius = 26.dp,
    edgeAccentColor = accentColor,
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "WEATHER SYNC PAUSED",
        style = MaterialTheme.typography.titleSmall,
        color = accentColor,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite.copy(alpha = 0.92f),
        textAlign = TextAlign.Center,
      )
      com.example.ui.components.WaterGlassSurface(
        onClick = onRetry,
        shape = CircleShape,
        cornerRadius = 20.dp,
        isFilledAccent = true,
        accentColor = accentColor,
      ) {
        Text(
          text = "RETRY",
          style = MaterialTheme.typography.labelLarge,
          color = contrastingOnAccent(accentColor),
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
      }
    }
  }
}
