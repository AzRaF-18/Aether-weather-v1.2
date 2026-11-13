package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.WidgetWeatherSnapshot
import com.example.data.location.LocationClient
import com.example.data.model.PresetLocations
import com.example.data.model.WeatherIconKind
import com.example.data.repository.WeatherRepository
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.firstOrNull

class AetherWeatherWidget : GlanceAppWidget() {

  companion object {
    private val SMALL_BOX = DpSize(110.dp, 110.dp)
    private val MEDIUM_BOX = DpSize(220.dp, 110.dp)
    private val LARGE_BOX = DpSize(220.dp, 200.dp)
  }

  override val sizeMode: SizeMode =
    SizeMode.Responsive(setOf(SMALL_BOX, MEDIUM_BOX, LARGE_BOX))

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val prefsRepo = UserPreferencesRepository(context)
    var prefs = prefsRepo.preferencesFlow.firstOrNull()

    if (prefs?.widgetSnapshot == null) {
      val loc = prefs?.lastLocation ?: PresetLocations.SWAMIBAGH
      val fetched =
        WeatherRepository().fetchWeather(
          latitude = loc.latitude,
          longitude = loc.longitude,
          customName = loc.displayName.takeIf { it.isNotBlank() && !loc.isGps },
          isGps = loc.isGps,
        ).getOrNull()
      if (fetched != null) {
        prefsRepo.saveWidgetSnapshot(fetched)
        prefs = prefsRepo.preferencesFlow.firstOrNull()
      }
    }

    val snapshot = prefs?.widgetSnapshot
    val isFahrenheit = prefs?.isFahrenheit ?: false

    provideContent {
      GlanceTheme {
        AetherWidgetContent(
          snapshot = snapshot,
          isFahrenheit = isFahrenheit,
        )
      }
    }
  }
}

@Composable
private fun AetherWidgetContent(
  snapshot: WidgetWeatherSnapshot?,
  isFahrenheit: Boolean,
) {
  val size = LocalSize.current
  val isWide = size.width >= 180.dp
  val isTall = size.height >= 170.dp

  val cityName = snapshot?.cityName ?: "Swamibagh, Dhaka"
  val conditionLabel = snapshot?.conditionLabel ?: "CLEAR DAY"
  val iconRes = iconResForKind(snapshot?.iconKind ?: WeatherIconKind.CLEAR_DAY)
  val unitLabel = if (isFahrenheit) "°F" else "°C"
  val tempDisplay =
    if (snapshot != null) {
      val value = if (isFahrenheit) (snapshot.tempC * 9.0 / 5.0) + 32.0 else snapshot.tempC
      "${value.roundToInt()}$unitLabel"
    } else {
      "--$unitLabel"
    }

  val whiteColor = ColorProvider(Color.White)
  val yellowColor = ColorProvider(Color(0xFFFACC15))
  val crimsonColor = ColorProvider(Color(0xFFEF4444))
  val softWhiteColor = ColorProvider(Color(0xD9FFFFFF))

  Box(
    modifier = GlanceModifier
      .fillMaxSize()
      .cornerRadius(android.R.dimen.system_app_widget_background_radius)
      .background(ImageProvider(R.drawable.widget_water_glass_bg))
      .clickable(actionStartActivity<MainActivity>())
      .padding(14.dp),
  ) {
    when {
      isTall -> {
        // Large / Tall Responsive Layout (2x3, 3x2+, 4x2+)
        Column(
          modifier = GlanceModifier.fillMaxSize(),
          verticalAlignment = Alignment.Top,
          horizontalAlignment = Alignment.Start,
        ) {
          Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "AETHER",
              style = TextStyle(
                color = crimsonColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
              ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
              text = snapshot?.updatedTimeLabel ?: "",
              style = TextStyle(color = softWhiteColor, fontSize = 10.sp),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Image(
              provider = ImageProvider(R.drawable.ic_widget_refresh),
              contentDescription = "Refresh weather",
              modifier = GlanceModifier
                .size(22.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
            )
          }

          Spacer(modifier = GlanceModifier.height(6.dp))

          Text(
            text = cityName,
            style = TextStyle(
              color = whiteColor,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
          )

          Spacer(modifier = GlanceModifier.defaultWeight())

          Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = tempDisplay,
              style = TextStyle(
                color = whiteColor,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
              ),
            )
            Spacer(modifier = GlanceModifier.width(10.dp))
            Image(
              provider = ImageProvider(iconRes),
              contentDescription = conditionLabel,
              modifier = GlanceModifier.size(30.dp),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
              text = conditionLabel,
              style = TextStyle(
                color = yellowColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
              ),
              maxLines = 2,
            )
          }

          Spacer(modifier = GlanceModifier.defaultWeight())

          if (snapshot != null) {
            val feelsVal =
              if (isFahrenheit) (snapshot.feelsLikeC * 9.0 / 5.0) + 32.0 else snapshot.feelsLikeC
            Row(
              modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color(0x26FFFFFF)))
                .cornerRadius(14.dp)
                .padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Feels ${feelsVal.roundToInt()}$unitLabel",
                style = TextStyle(color = softWhiteColor, fontSize = 11.sp),
              )
              Spacer(modifier = GlanceModifier.defaultWeight())
              Text(
                text = "${snapshot.humidityPercent}% RH",
                style = TextStyle(color = softWhiteColor, fontSize = 11.sp),
              )
              Spacer(modifier = GlanceModifier.defaultWeight())
              Text(
                text = String.format(Locale.US, "%.1f m/s", snapshot.windSpeedMs),
                style = TextStyle(color = softWhiteColor, fontSize = 11.sp),
              )
            }
          }
        }
      }

      isWide -> {
        // Horizontal Wide Layout (3x1, 4x1, 3x2)
        Row(
          modifier = GlanceModifier.fillMaxSize(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "AETHER",
              style = TextStyle(
                color = crimsonColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
              ),
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
              text = cityName,
              style = TextStyle(
                color = whiteColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
              ),
              maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Image(
                provider = ImageProvider(iconRes),
                contentDescription = conditionLabel,
                modifier = GlanceModifier.size(18.dp),
              )
              Spacer(modifier = GlanceModifier.width(6.dp))
              Text(
                text = conditionLabel,
                style = TextStyle(
                  color = yellowColor,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
              )
            }
          }

          Spacer(modifier = GlanceModifier.width(8.dp))

          Column(
            horizontalAlignment = Alignment.End,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Image(
              provider = ImageProvider(R.drawable.ic_widget_refresh),
              contentDescription = "Refresh weather",
              modifier = GlanceModifier
                .size(20.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
              text = tempDisplay,
              style = TextStyle(
                color = whiteColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
              ),
            )
          }
        }
      }

      else -> {
        // Compact 2x2 Layout
        Column(
          modifier = GlanceModifier.fillMaxSize(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalAlignment = Alignment.Start,
        ) {
          Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Image(
              provider = ImageProvider(iconRes),
              contentDescription = conditionLabel,
              modifier = GlanceModifier.size(22.dp),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Image(
              provider = ImageProvider(R.drawable.ic_widget_refresh),
              contentDescription = "Refresh weather",
              modifier = GlanceModifier
                .size(18.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
            )
          }

          Spacer(modifier = GlanceModifier.defaultWeight())

          Text(
            text = tempDisplay,
            style = TextStyle(
              color = whiteColor,
              fontSize = 28.sp,
              fontWeight = FontWeight.Bold,
            ),
          )
          Text(
            text = conditionLabel,
            style = TextStyle(
              color = yellowColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
          )
          Text(
            text = cityName,
            style = TextStyle(
              color = softWhiteColor,
              fontSize = 11.sp,
            ),
            maxLines = 1,
          )
        }
      }
    }
  }
}

private fun iconResForKind(kind: WeatherIconKind): Int =
  when (kind) {
    WeatherIconKind.CLEAR_DAY -> R.drawable.ic_widget_sun
    WeatherIconKind.CLEAR_NIGHT -> R.drawable.ic_widget_moon
    WeatherIconKind.PARTLY_CLOUDY_DAY,
    WeatherIconKind.PARTLY_CLOUDY_NIGHT,
    WeatherIconKind.CLOUDY,
    WeatherIconKind.FOG -> R.drawable.ic_widget_cloud
    WeatherIconKind.RAIN,
    WeatherIconKind.SNOW -> R.drawable.ic_widget_rain
    WeatherIconKind.THUNDERSTORM -> R.drawable.ic_widget_storm
  }

class RefreshWidgetAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    WidgetSyncHelper.refreshWidgetWeather(context)
  }
}

class AetherWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = AetherWeatherWidget()

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    WidgetWorkScheduler.schedulePeriodicSync(context)
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    WidgetWorkScheduler.cancelPeriodicSync(context)
  }
}

class WidgetSyncWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
  override suspend fun doWork(): Result {
    WidgetSyncHelper.refreshWidgetWeather(applicationContext)
    return Result.success()
  }
}

object WidgetSyncHelper {
  suspend fun refreshWidgetWeather(context: Context) {
    val prefsRepo = UserPreferencesRepository(context)
    val prefs = prefsRepo.preferencesFlow.firstOrNull()
    val savedLocation = prefs?.lastLocation ?: PresetLocations.SWAMIBAGH

    var lat = savedLocation.latitude
    var lon = savedLocation.longitude
    var customName = savedLocation.displayName.takeIf { it.isNotBlank() && !savedLocation.isGps }
    val isGps = savedLocation.isGps

    if (isGps) {
      val locationClient = LocationClient(context)
      if (locationClient.hasLocationPermission()) {
        val fresh = locationClient.getCurrentCoordinates()
        if (fresh != null) {
          lat = fresh.first
          lon = fresh.second
          customName = null
        }
      }
    }

    val result = WeatherRepository().fetchWeather(
      latitude = lat,
      longitude = lon,
      customName = customName,
      isGps = isGps,
    )

    result.onSuccess { report ->
      prefsRepo.saveLocation(report.location)
      prefsRepo.saveWidgetSnapshot(report)
    }

    AetherWeatherWidget().updateAll(context)
  }
}

object WidgetWorkScheduler {
  private const val WORK_NAME = "aether_widget_periodic_sync"

  fun schedulePeriodicSync(context: Context) {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val request = PeriodicWorkRequestBuilder<WidgetSyncWorker>(15, TimeUnit.MINUTES)
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      WORK_NAME,
      ExistingPeriodicWorkPolicy.KEEP,
      request,
    )
  }

  fun cancelPeriodicSync(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
  }
}
