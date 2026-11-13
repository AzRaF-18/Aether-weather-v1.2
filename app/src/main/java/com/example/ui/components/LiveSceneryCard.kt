package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherLocation
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SolarYellow
import java.util.Locale

/**
 * Dedicated large realistic-glass tile hosting:
 * 1) The oversized, neighborhood-accurate animated scenery diorama
 * 2) Embedded interactive Google Maps Satellite/Hybrid view for the active coordinates
 * 3) Direct launch actions for real 3D Google Earth and Google Maps + live street/weather audio toggle
 */
@Composable
fun LiveSceneryCard(
  location: WeatherLocation,
  isDay: Boolean,
  isRainy: Boolean,
  accentColor: Color,
  ambientSoundEnabled: Boolean,
  activeAmbientSoundLabel: String,
  onToggleAmbientSound: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  val textShadow = Shadow(
    color = Color.Black.copy(alpha = 0.25f),
    offset = Offset(0f, 1f),
    blurRadius = 3f,
  )

  val openGoogleEarth: () -> Unit = {
    val earthUrl = String.format(
      Locale.US,
      "https://earth.google.com/web/@%.5f,%.5f,400a,900d,35y,0h,45t,0r",
      location.latitude,
      location.longitude,
    )
    runCatching {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(earthUrl)))
    }
  }

  val openGoogleMaps: () -> Unit = {
    val encodedLabel = Uri.encode(location.displayName)
    val geoUri = Uri.parse(
      String.format(
        Locale.US,
        "geo:%.5f,%.5f?q=%.5f,%.5f(%s)",
        location.latitude,
        location.longitude,
        location.latitude,
        location.longitude,
        encodedLabel,
      )
    )
    val launchedGeo = runCatching {
      context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
    }.isSuccess
    if (!launchedGeo) {
      val webMapsUrl = String.format(
        Locale.US,
        "https://www.google.com/maps/search/?api=1&query=%.5f,%.5f",
        location.latitude,
        location.longitude,
      )
      runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webMapsUrl)))
      }
    }
  }

  WaterGlassSurface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("live_scenery_card"),
    shape = RoundedCornerShape(26.dp),
    cornerRadius = 26.dp,
    shadowElevation = 6.dp,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Header bar: Title + Active Location
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Explore,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "LIVE SCENERY",
              style = MaterialTheme.typography.titleSmall.copy(shadow = textShadow),
              color = SolarYellow,
              maxLines = 1,
            )
            Text(
              text = location.displayName,
              style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
              color = PureWhite.copy(alpha = 0.86f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      // Main Large 3D Miniature Neighborhood Scenery Viewport
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp)
          .height(288.dp)
          .clip(RoundedCornerShape(20.dp))
          .border(0.85.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(20.dp)),
      ) {
        MiniatureCityscapeBanner(
          locationName = location.displayName,
          isDay = isDay,
          isRainy = isRainy,
          accentColor = accentColor,
          bannerHeight = 288.dp,
        )

        // Floating Ambient Sound Status Pill in top-right of the viewport (tappable mute/unmute)
        Row(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 8.dp, end = 10.dp)
            .clip(CircleShape)
            .background(Color(0xFF091326).copy(alpha = 0.56f))
            .border(0.75.dp, Color.White.copy(alpha = 0.34f), CircleShape)
            .clickable { onToggleAmbientSound(!ambientSoundEnabled) }
            .padding(horizontal = 9.dp, vertical = 4.dp)
            .testTag("scenery_sound_pill"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = if (ambientSoundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
            contentDescription = if (ambientSoundEnabled) "Mute ambient sound" else "Enable ambient sound",
            tint = if (ambientSoundEnabled) accentColor else PureWhite.copy(alpha = 0.7f),
            modifier = Modifier.size(13.dp),
          )
          Text(
            text = if (ambientSoundEnabled) activeAmbientSoundLabel else "Audio Off",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.SemiBold,
            ),
            color = PureWhite.copy(alpha = 0.95f),
            maxLines = 1,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

      // Bottom Real Google Earth & Google Maps Action Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val onAccentColor = contrastingOnAccent(accentColor)
        WaterGlassSurface(
          onClick = openGoogleEarth,
          modifier = Modifier
            .weight(1f)
            .minimumInteractiveComponentSize()
            .testTag("open_google_earth_button"),
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
              imageVector = Icons.Rounded.Public,
              contentDescription = null,
              tint = onAccentColor,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Google Earth",
              style = MaterialTheme.typography.labelMedium,
              color = onAccentColor,
              maxLines = 1,
            )
          }
        }

        WaterGlassSurface(
          onClick = openGoogleMaps,
          modifier = Modifier
            .weight(1f)
            .minimumInteractiveComponentSize()
            .testTag("open_google_maps_button"),
          shape = CircleShape,
          cornerRadius = 20.dp,
          shadowElevation = 4.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Map,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Google Maps",
              style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow),
              color = PureWhite,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}
