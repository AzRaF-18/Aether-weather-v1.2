package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AlertSeverity
import com.example.data.model.SevereWeatherAlert

class WeatherAlertNotifier(private val context: Context) {

  companion object {
    const val CHANNEL_ID = "aether_severe_weather_alerts"
    private const val TEST_NOTIFICATION_ID = 99001
  }

  fun ensureChannelExists() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          context.getString(R.string.notification_channel_name),
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = context.getString(R.string.notification_channel_desc)
          enableVibration(true)
          enableLights(true)
          lightColor = 0xFFEF4444.toInt()
        }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
  }

  @SuppressLint("MissingPermission")
  fun showAlertNotification(alert: SevereWeatherAlert) {
    if (!hasNotificationPermission()) return
    ensureChannelExists()

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      alert.id.hashCode(),
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val priority = when (alert.severity) {
      AlertSeverity.EXTREME -> NotificationCompat.PRIORITY_MAX
      AlertSeverity.SEVERE -> NotificationCompat.PRIORITY_HIGH
      AlertSeverity.MODERATE -> NotificationCompat.PRIORITY_DEFAULT
    }

    val expandedBody = "${alert.locationName}\n${alert.summary}\n\nSafety: ${alert.instruction}"

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_stat_weather_alert)
      .setContentTitle("${alert.severity.badge}: ${alert.eventTitle}")
      .setContentText("${alert.locationName} • ${alert.summary}")
      .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
      .setColor(0xFFEF4444.toInt())
      .setPriority(priority)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    NotificationManagerCompat.from(context).notify(alert.id.hashCode(), notification)
  }

  @SuppressLint("MissingPermission")
  fun sendSampleTestNotification(locationName: String): Boolean {
    if (!hasNotificationPermission()) return false
    ensureChannelExists()

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      TEST_NOTIFICATION_ID,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_stat_weather_alert)
      .setContentTitle("SEVERE WEATHER TEST: $locationName")
      .setContentText("Aether alert channel is active and ready to notify you.")
      .setStyle(
        NotificationCompat.BigTextStyle().bigText(
          "Aether is monitoring $locationName every 15 minutes for thunderstorms, heavy rain, high wind gusts, and extreme temperatures."
        )
      )
      .setColor(0xFFEF4444.toInt())
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
    return true
  }
}
