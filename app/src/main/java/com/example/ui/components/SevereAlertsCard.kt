package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AlertSeverity
import com.example.data.model.SevereWeatherAlert
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonRedSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SolarYellow
import com.example.ui.theme.SunsetOrange

@Composable
fun SevereAlertsCard(
  alerts: List<SevereWeatherAlert>,
  alertsEnabled: Boolean,
  hasNotificationPermission: Boolean,
  isCheckingAlerts: Boolean,
  locationName: String,
  accentColor: Color,
  onToggleAlerts: (Boolean) -> Unit,
  onCheckNow: () -> Unit,
  onSendTestNotification: () -> Unit,
  modifier: Modifier = Modifier,
  isDay: Boolean = true,
  isRainy: Boolean = false,
) {
  val hasActiveAlerts = alerts.isNotEmpty()
  val textShadow = Shadow(
    color = Color.Black.copy(alpha = 0.25f),
    offset = Offset(0f, 1f),
    blurRadius = 3f,
  )

  WaterGlassSurface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("severe_alerts_card"),
    shape = RoundedCornerShape(26.dp),
    cornerRadius = 26.dp,
    shadowElevation = 6.dp,
    edgeAccentColor = if (hasActiveAlerts) CrimsonRed else null,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header + Push Switch
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = if (hasActiveAlerts) Icons.Rounded.WarningAmber else Icons.Rounded.NotificationsActive,
            contentDescription = null,
            tint = if (hasActiveAlerts) CrimsonRed else SolarYellow,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = stringResource(id = R.string.severe_alerts_title),
            style = MaterialTheme.typography.titleSmall.copy(shadow = textShadow),
            color = if (hasActiveAlerts) CrimsonRedSoft else SolarYellow,
          )
        }

        Switch(
          checked = alertsEnabled && hasNotificationPermission,
          onCheckedChange = onToggleAlerts,
          modifier = Modifier.testTag("alerts_push_switch"),
          colors = SwitchDefaults.colors(
            checkedThumbColor = contrastingOnAccent(accentColor),
            checkedTrackColor = accentColor,
            uncheckedThumbColor = PureWhite,
            uncheckedTrackColor = Color.White.copy(alpha = 0.20f),
            uncheckedBorderColor = Color.White.copy(alpha = 0.45f),
          ),
        )
      }

      // Status subtitle
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = if (alertsEnabled && hasNotificationPermission) {
            Icons.Rounded.NotificationsActive
          } else {
            Icons.Rounded.NotificationsOff
          },
          contentDescription = null,
          tint = Color.White.copy(alpha = 0.85f),
          modifier = Modifier.size(14.dp),
        )
        Text(
          text = if (!hasNotificationPermission && alertsEnabled) {
            "Tap toggle to grant Android notification permission"
          } else if (alertsEnabled) {
            stringResource(id = R.string.alerts_push_subtitle)
          } else {
            "Push alerts paused • Enable switch for 15-min background monitoring"
          },
          style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
          color = PureWhite.copy(alpha = 0.92f),
        )
      }

      HorizontalDivider(color = Color.White.copy(alpha = 0.16f))

      // Active Alerts List or All Clear Banner
      if (isCheckingAlerts) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = accentColor,
          )
          Text(
            text = "Scanning NWS & global hazard feeds for $locationName…",
            style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
            color = PureWhite.copy(alpha = 0.92f),
          )
        }
      } else if (alerts.isEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("alerts_all_clear_banner"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Shield,
            contentDescription = null,
            tint = Color(0xFF4ADE80),
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = stringResource(id = R.string.alerts_all_clear),
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Medium,
              shadow = textShadow,
            ),
            color = Color(0xFFDCFCE7),
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          alerts.forEach { alert ->
            AlertItemBanner(alert = alert)
          }
        }
      }

      // Action Buttons: Frosted Glass Check Now + Crimson Pill Test Alert
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        WaterGlassSurface(
          onClick = onCheckNow,
          modifier = Modifier
            .weight(1f)
            .minimumInteractiveComponentSize()
            .testTag("alerts_check_now_button"),
          shape = CircleShape,
          shadowElevation = 6.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
              text = stringResource(id = R.string.alerts_check_now),
              style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow),
              color = PureWhite,
            )
          }
        }

        val onAccentColor = contrastingOnAccent(accentColor)
        WaterGlassSurface(
          onClick = onSendTestNotification,
          modifier = Modifier
            .weight(1f)
            .minimumInteractiveComponentSize()
            .testTag("alerts_test_push_button"),
          shape = CircleShape,
          cornerRadius = 20.dp,
          isFilledAccent = true,
          accentColor = accentColor,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.NotificationsActive,
              contentDescription = null,
              tint = onAccentColor,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
              text = stringResource(id = R.string.alerts_test_push),
              style = MaterialTheme.typography.labelMedium,
              color = onAccentColor,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AlertItemBanner(alert: SevereWeatherAlert) {
  val badgeColor = when (alert.severity) {
    AlertSeverity.EXTREME -> CrimsonRed
    AlertSeverity.SEVERE -> SunsetOrange
    AlertSeverity.MODERATE -> SolarYellow
  }
  val badgeTextColor = if (alert.severity == AlertSeverity.MODERATE) Color.Black else PureWhite

  WaterGlassSurface(
    shape = RoundedCornerShape(16.dp),
    shadowElevation = 6.dp,
    edgeAccentColor = badgeColor,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = CircleShape,
          color = badgeColor,
        ) {
          Text(
            text = alert.severity.badge,
            style = MaterialTheme.typography.labelSmall,
            color = badgeTextColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
        Text(
          text = alert.source,
          style = MaterialTheme.typography.labelSmall,
          color = PureWhite.copy(alpha = 0.80f),
        )
      }

      Text(
        text = alert.eventTitle,
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
      )
      Text(
        text = alert.summary,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite.copy(alpha = 0.94f),
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Safety: ${alert.instruction}",
        style = MaterialTheme.typography.labelMedium,
        color = SolarYellow,
      )
    }
  }
}
