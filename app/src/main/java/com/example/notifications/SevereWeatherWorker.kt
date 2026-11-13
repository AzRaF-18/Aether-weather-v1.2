package com.example.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.UserPreferencesRepository
import com.example.data.location.LocationClient
import com.example.data.repository.SevereWeatherRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.firstOrNull

class SevereWeatherWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val prefsRepo = UserPreferencesRepository(applicationContext)
    val prefs = prefsRepo.preferencesFlow.firstOrNull() ?: return Result.success()
    if (!prefs.severeAlertsEnabled) return Result.success()

    val locationClient = LocationClient(applicationContext)
    var lat = prefs.lastLocation.latitude
    var lon = prefs.lastLocation.longitude
    var locationName = prefs.lastLocation.displayName

    if (prefs.lastLocation.isGps && locationClient.hasLocationPermission()) {
      val freshCoords = locationClient.getCurrentCoordinates()
      if (freshCoords != null) {
        lat = freshCoords.first
        lon = freshCoords.second
      }
    }

    val severeRepo = SevereWeatherRepository()
    val alerts = severeRepo.fetchSevereAlerts(
      latitude = lat,
      longitude = lon,
      locationName = locationName,
    )

    val notifier = WeatherAlertNotifier(applicationContext)
    val newlyNotified = mutableSetOf<String>()

    alerts.forEach { alert ->
      if (alert.id !in prefs.notifiedAlertIds) {
        notifier.showAlertNotification(alert)
        newlyNotified += alert.id
      }
    }

    if (newlyNotified.isNotEmpty()) {
      prefsRepo.markAlertsNotified(newlyNotified)
    }

    return Result.success()
  }
}

object AlertWorkScheduler {
  private const val PERIODIC_WORK_NAME = "aether_severe_weather_periodic"
  private const val IMMEDIATE_WORK_NAME = "aether_severe_weather_immediate"

  fun schedulePeriodicAlerts(context: Context) {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val request = PeriodicWorkRequestBuilder<SevereWeatherWorker>(15, TimeUnit.MINUTES)
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      PERIODIC_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      request,
    )
  }

  fun triggerImmediateCheck(context: Context) {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val request = OneTimeWorkRequestBuilder<SevereWeatherWorker>()
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
      IMMEDIATE_WORK_NAME,
      ExistingWorkPolicy.REPLACE,
      request,
    )
  }

  fun cancelAlerts(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
  }
}
