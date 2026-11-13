package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CitySuggestion
import com.example.data.model.PresetLocations
import com.example.data.model.WeatherLocation
import com.example.ui.GpsStatus
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassDropdownSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SolarYellow

@Composable
fun WeatherHeader(
  gpsStatus: GpsStatus,
  searchQuery: String,
  suggestions: List<CitySuggestion>,
  isSearchingCities: Boolean,
  activeLocation: WeatherLocation,
  accentColor: Color,
  onSearchQueryChanged: (String) -> Unit,
  onClearSearch: () -> Unit,
  onSuggestionSelected: (CitySuggestion) -> Unit,
  onGpsClick: () -> Unit,
  onPresetClick: (WeatherLocation) -> Unit,
  onOpenSettingsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current

  WaterGlassSurface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(26.dp),
    cornerRadius = 26.dp,
    shadowElevation = 16.dp,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Brand + Live GPS Status + Settings Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Image(
            painter = painterResource(id = R.drawable.aether_logo),
            contentDescription = stringResource(id = R.string.cd_aether_logo),
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(28.dp)
              .clip(RoundedCornerShape(8.dp))
              .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
          )
          Text(
            text = stringResource(id = R.string.brand_title),
            style = MaterialTheme.typography.titleLarge,
            color = accentColor,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          GpsStatusPill(
            gpsStatus = gpsStatus,
            accentColor = accentColor,
            onClick = onGpsClick,
          )

          WaterGlassSurface(
            onClick = onOpenSettingsClick,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("settings_button"),
            shape = CircleShape,
            cornerRadius = 20.dp,
            shadowElevation = 4.dp,
          ) {
            Box(
              modifier = Modifier.padding(8.dp),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(id = R.string.cd_open_settings),
                tint = PureWhite,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      // Frosted Dark Translucent Search Input matching screenshot
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("city_search_input"),
        placeholder = {
          Text(
            text = stringResource(id = R.string.search_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.75f),
          )
        },
        trailingIcon = {
          if (isSearchingCities) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = accentColor,
            )
          } else if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = onClearSearch,
              modifier = Modifier.testTag("clear_search_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(id = R.string.cd_clear_search),
                tint = Color.White.copy(alpha = 0.85f),
              )
            }
          }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureWhite),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
          onSearch = {
            suggestions.firstOrNull()?.let {
              onSuggestionSelected(it)
              focusManager.clearFocus()
            }
          }
        ),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color.Black.copy(alpha = 0.36f),
          unfocusedContainerColor = Color.Black.copy(alpha = 0.30f),
          focusedBorderColor = accentColor,
          unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
          cursorColor = PureWhite,
        ),
      )

      // Autocomplete Suggestions Dropdown
      AnimatedVisibility(visible = suggestions.isNotEmpty()) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .testTag("suggestions_dropdown"),
          shape = RoundedCornerShape(14.dp),
          color = GlassDropdownSurface,
          border = BorderStroke(1.dp, GlassBorder),
        ) {
          Column {
            suggestions.forEachIndexed { index, suggestion ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .clickable {
                    onSuggestionSelected(suggestion)
                    focusManager.clearFocus()
                  }
                  .padding(horizontal = 14.dp, vertical = 12.dp)
                  .testTag("suggestion_item_$index"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.LocationOn,
                  contentDescription = null,
                  tint = accentColor,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = suggestion.displayName,
                  style = MaterialTheme.typography.bodyMedium,
                  color = PureWhite,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              if (index < suggestions.lastIndex) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
              }
            }
          }
        }
      }

      // Quick Location Chips matching screenshot
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
      ) {
        item {
          QuickCityChip(
            label = stringResource(id = R.string.chip_current_gps),
            isPrimary = true,
            isSelected = activeLocation.isGps,
            accentColor = accentColor,
            onClick = onGpsClick,
            testTag = "chip_current_gps",
          )
        }
        items(PresetLocations.quickList, key = { it.first }) { (label, loc) ->
          val isSelected =
            !activeLocation.isGps &&
              kotlin.math.abs(activeLocation.latitude - loc.latitude) < 0.02 &&
              kotlin.math.abs(activeLocation.longitude - loc.longitude) < 0.02
          QuickCityChip(
            label = label,
            isPrimary = false,
            isSelected = isSelected,
            accentColor = accentColor,
            onClick = { onPresetClick(loc) },
            testTag = "chip_${label.lowercase().replace(" ", "_")}",
          )
        }
      }
    }
  }
}

@Composable
private fun GpsStatusPill(
  gpsStatus: GpsStatus,
  accentColor: Color,
  onClick: () -> Unit,
) {
  val (label, tintColor) =
    when (gpsStatus) {
      GpsStatus.ACTIVE -> stringResource(id = R.string.gps_active) to accentColor
      GpsStatus.LOCATING -> stringResource(id = R.string.gps_locating) to Color(0xFF9AD8F5)
      GpsStatus.DEFAULT_FALLBACK -> stringResource(id = R.string.gps_default) to PureWhite
      GpsStatus.PERMISSION_NEEDED -> stringResource(id = R.string.gps_permission_needed) to SolarYellow
      GpsStatus.NETWORK_ERROR -> stringResource(id = R.string.network_error) to accentColor
    }

  val icon =
    if (gpsStatus == GpsStatus.NETWORK_ERROR) Icons.Rounded.WarningAmber
    else Icons.Rounded.MyLocation

  WaterGlassSurface(
    onClick = onClick,
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .testTag("gps_status_pill"),
    shape = CircleShape,
    cornerRadius = 20.dp,
    shadowElevation = 4.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tintColor,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = PureWhite,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun QuickCityChip(
  label: String,
  isPrimary: Boolean,
  isSelected: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  val labelColor = if (isPrimary) contrastingOnAccent(accentColor) else PureWhite
  WaterGlassSurface(
    onClick = onClick,
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .testTag(testTag),
    shape = CircleShape,
    cornerRadius = 20.dp,
    shadowElevation = 6.dp,
    isFilledAccent = isPrimary,
    accentColor = accentColor,
    edgeAccentColor = if (!isPrimary && isSelected) SolarYellow else null,
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = if (isPrimary || isSelected) FontWeight.Bold else FontWeight.SemiBold,
          shadow = if (isPrimary) null else Shadow(
            color = Color.Black.copy(alpha = 0.25f),
            offset = Offset(0f, 1f),
            blurRadius = 3f,
          ),
        ),
        color = labelColor,
        maxLines = 1,
      )
    }
  }
}
