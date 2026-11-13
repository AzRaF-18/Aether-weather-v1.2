package com.example.ui.components.scenery

import androidx.compose.ui.graphics.Color
import com.example.audio.TrafficAudioConfig
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SolarYellow
import kotlin.math.abs

enum class CityArchitectureStyle {
  DHAKA_SHAMIBAGH,
  NEW_YORK_TIMES_SQUARE,
  NEW_YORK_BROOKLYN,
  LONDON_WESTMINSTER,
  PARIS_HAUSSMANN,
  TOKYO_SHIBUYA,
  DUBAI_DESERT,
  CAIRO_SANDSTONE,
  SYDNEY_HARBOR,
  SOUTH_ASIA_METRO,
  SUBURBAN_NEIGHBORHOOD,
  GLOBAL_METRO,
}

enum class MiniVehicleKind {
  SEDAN,
  NYC_TAXI,
  LONDON_CAB,
  DHAKA_BUS,
  CITY_BUS,
  DOUBLE_DECKER_BUS,
  CNG_RICKSHAW,
  CYCLE_RICKSHAW,
  DELIVERY_VAN,
}

data class MiniBillboardSpec(
  val color: Color,
  val text: String,
  val subColor: Color,
  val accentTextColor: Color = Color.White,
  val secondaryText: String? = null,
)

data class MiniBuildingSpec(
  val xFraction: Float,
  val widthFraction: Float,
  val heightFraction: Float,
  val depthFraction: Float,
  val facadeColor: Color,
  val trimColor: Color,
  val isGlassTower: Boolean,
  val hasPitchedRoof: Boolean,
  val hasDome: Boolean,
  val hasWaterTank: Boolean,
  val hasHvacUnits: Boolean,
  val hasBalconies: Boolean,
  val hasShopAwning: Boolean,
  val awningColor: Color,
  val windowCols: Int,
  val windowRows: Int,
  val billboard: MiniBillboardSpec? = null,
  val upperBillboard: MiniBillboardSpec? = null,
  val sideBillboard: MiniBillboardSpec? = null,
)

data class PedestrianSpec(
  val startPhase: Float,
  val speedMultiplier: Float,
  val walksRight: Boolean,
  val shirtColor: Color,
  val pantsColor: Color,
  val hasBagOrUmbrella: Boolean,
)

data class CityDioramaSpec(
  val style: CityArchitectureStyle,
  val hierarchyLabel: String,
  val buildings: List<MiniBuildingSpec>,
  val vehiclesRight: List<MiniVehicleKind>,
  val vehiclesLeft: List<MiniVehicleKind>,
  val pedestrians: List<PedestrianSpec>,
  val isDusty: Boolean,
  val hasOverheadWires: Boolean,
  val hasPalms: Boolean,
  val hasElevatedTrain: Boolean,
  val hasCrosswalk: Boolean,
  val hasPhoneBooth: Boolean,
  val hasMountainBackdrop: Boolean,
  val roadLineColor: Color,
  val plinthGrassColor: Color,
)

val MiniVehicleKind.isBus: Boolean
  get() = this == MiniVehicleKind.DHAKA_BUS ||
    this == MiniVehicleKind.CITY_BUS ||
    this == MiniVehicleKind.DOUBLE_DECKER_BUS

val MiniVehicleKind.isCarLike: Boolean
  get() = this == MiniVehicleKind.SEDAN ||
    this == MiniVehicleKind.NYC_TAXI ||
    this == MiniVehicleKind.LONDON_CAB ||
    this == MiniVehicleKind.CNG_RICKSHAW ||
    this == MiniVehicleKind.DELIVERY_VAN

fun computeTrafficAudioConfig(locationName: String): TrafficAudioConfig {
  val spec = resolveCityDioramaSpec(locationName)
  val allVehicles = spec.vehiclesRight + spec.vehiclesLeft
  return TrafficAudioConfig(
    carCount = allVehicles.count { it.isCarLike },
    busCount = allVehicles.count { it.isBus },
  )
}

fun resolveCityDioramaSpec(locationName: String): CityDioramaSpec {
  val lower = locationName.lowercase()
  val parts = locationName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
  val hierarchyLabel =
    when {
      parts.size >= 3 -> "${parts[0]} · ${parts[1]} · ${parts.last()}"
      parts.size == 2 -> "${parts[0]} · ${parts[1]}"
      parts.size == 1 -> parts[0]
      else -> "Local Neighborhood"
    }

  val style =
    when {
      lower.contains("swamibagh") ||
        lower.contains("shamibag") ||
        lower.contains("swamibag") ||
        lower.contains("dhaka") ||
        lower.contains("motijheel") ||
        lower.contains("gulshan") ||
        lower.contains("dhanmondi") ||
        lower.contains("mirpur") ||
        lower.contains("uttara") ||
        lower.contains("bangladesh") ||
        lower.contains("chittagong") ||
        lower.contains("sylhet") -> CityArchitectureStyle.DHAKA_SHAMIBAGH

      lower.contains("brooklyn") ||
        lower.contains("queens") ||
        lower.contains("bronx") ||
        lower.contains("harlem") -> CityArchitectureStyle.NEW_YORK_BROOKLYN

      lower.contains("new york") ||
        lower.contains("manhattan") ||
        lower.contains("times square") ||
        lower.contains("midtown") ||
        lower.contains("chicago") ||
        lower.contains("toronto") ||
        lower.contains("united states") -> CityArchitectureStyle.NEW_YORK_TIMES_SQUARE

      lower.contains("paris") ||
        lower.contains("montmartre") ||
        lower.contains("france") ||
        lower.contains("rome") ||
        lower.contains("milan") ||
        lower.contains("barcelona") ||
        lower.contains("lyon") -> CityArchitectureStyle.PARIS_HAUSSMANN

      lower.contains("london") ||
        lower.contains("westminster") ||
        lower.contains("soho") ||
        lower.contains("camden") ||
        lower.contains("united kingdom") ||
        lower.contains("manchester") -> CityArchitectureStyle.LONDON_WESTMINSTER

      lower.contains("tokyo") ||
        lower.contains("shibuya") ||
        lower.contains("shinjuku") ||
        lower.contains("akihabara") ||
        lower.contains("japan") ||
        lower.contains("osaka") ||
        lower.contains("seoul") ||
        lower.contains("singapore") ||
        lower.contains("hong kong") -> CityArchitectureStyle.TOKYO_SHIBUYA

      lower.contains("cairo") ||
        lower.contains("giza") ||
        lower.contains("egypt") -> CityArchitectureStyle.CAIRO_SANDSTONE

      lower.contains("dubai") ||
        lower.contains("abu dhabi") ||
        lower.contains("doha") ||
        lower.contains("riyadh") ||
        lower.contains("uae") -> CityArchitectureStyle.DUBAI_DESERT

      lower.contains("sydney") ||
        lower.contains("melbourne") ||
        lower.contains("australia") ||
        lower.contains("miami") ||
        lower.contains("san francisco") -> CityArchitectureStyle.SYDNEY_HARBOR

      lower.contains("mumbai") ||
        lower.contains("delhi") ||
        lower.contains("kolkata") ||
        lower.contains("india") ||
        lower.contains("karachi") ||
        lower.contains("lahore") -> CityArchitectureStyle.SOUTH_ASIA_METRO

      parts.size >= 3 ||
        lower.contains("village") ||
        lower.contains("suburb") ||
        lower.contains("heights") ||
        lower.contains("town") ||
        lower.contains("gardens") -> CityArchitectureStyle.SUBURBAN_NEIGHBORHOOD

      else -> CityArchitectureStyle.GLOBAL_METRO
    }

  val seed = abs(lower.hashCode())
  val buildings = buildSkylineList(style, seed)
  val pedestrians = buildPedestrians(style, seed)

  val (vehiclesRight, vehiclesLeft) = when (style) {
    CityArchitectureStyle.DHAKA_SHAMIBAGH -> listOf(
      MiniVehicleKind.CYCLE_RICKSHAW,
      MiniVehicleKind.DHAKA_BUS,
      MiniVehicleKind.CNG_RICKSHAW,
      MiniVehicleKind.SEDAN,
      MiniVehicleKind.CYCLE_RICKSHAW,
    ) to listOf(
      MiniVehicleKind.SEDAN,
      MiniVehicleKind.CNG_RICKSHAW,
      MiniVehicleKind.DHAKA_BUS,
    )
    CityArchitectureStyle.NEW_YORK_TIMES_SQUARE -> listOf(
      MiniVehicleKind.NYC_TAXI,
      MiniVehicleKind.CITY_BUS,
      MiniVehicleKind.NYC_TAXI,
      MiniVehicleKind.SEDAN,
    ) to listOf(
      MiniVehicleKind.NYC_TAXI,
      MiniVehicleKind.DELIVERY_VAN,
      MiniVehicleKind.NYC_TAXI,
    )
    CityArchitectureStyle.NEW_YORK_BROOKLYN -> listOf(
      MiniVehicleKind.NYC_TAXI,
      MiniVehicleKind.SEDAN,
      MiniVehicleKind.DELIVERY_VAN,
    ) to listOf(
      MiniVehicleKind.CITY_BUS,
      MiniVehicleKind.NYC_TAXI,
    )
    CityArchitectureStyle.LONDON_WESTMINSTER -> listOf(
      MiniVehicleKind.DOUBLE_DECKER_BUS,
      MiniVehicleKind.LONDON_CAB,
      MiniVehicleKind.SEDAN,
    ) to listOf(
      MiniVehicleKind.LONDON_CAB,
      MiniVehicleKind.DOUBLE_DECKER_BUS,
    )
    CityArchitectureStyle.PARIS_HAUSSMANN -> listOf(
      MiniVehicleKind.SEDAN,
      MiniVehicleKind.CITY_BUS,
    ) to listOf(
      MiniVehicleKind.DELIVERY_VAN,
      MiniVehicleKind.SEDAN,
    )
    CityArchitectureStyle.TOKYO_SHIBUYA -> listOf(
      MiniVehicleKind.NYC_TAXI,
      MiniVehicleKind.DELIVERY_VAN,
      MiniVehicleKind.SEDAN,
    ) to listOf(
      MiniVehicleKind.SEDAN,
      MiniVehicleKind.CITY_BUS,
    )
    CityArchitectureStyle.SOUTH_ASIA_METRO -> listOf(
      MiniVehicleKind.CNG_RICKSHAW,
      MiniVehicleKind.DHAKA_BUS,
      MiniVehicleKind.SEDAN,
    ) to listOf(
      MiniVehicleKind.NYC_TAXI,
      MiniVehicleKind.CNG_RICKSHAW,
      MiniVehicleKind.SEDAN,
    )
    CityArchitectureStyle.DUBAI_DESERT, CityArchitectureStyle.CAIRO_SANDSTONE -> listOf(
      MiniVehicleKind.SEDAN,
      MiniVehicleKind.CITY_BUS,
    ) to listOf(
      MiniVehicleKind.NYC_TAXI,
      MiniVehicleKind.SEDAN,
    )
    CityArchitectureStyle.SYDNEY_HARBOR -> listOf(
      MiniVehicleKind.SEDAN,
      MiniVehicleKind.CITY_BUS,
      MiniVehicleKind.DELIVERY_VAN,
    ) to listOf(
      MiniVehicleKind.SEDAN,
      MiniVehicleKind.DELIVERY_VAN,
    )
    CityArchitectureStyle.SUBURBAN_NEIGHBORHOOD -> listOf(
      MiniVehicleKind.SEDAN,
      MiniVehicleKind.DELIVERY_VAN,
    ) to listOf(
      MiniVehicleKind.SEDAN,
    )
    CityArchitectureStyle.GLOBAL_METRO -> {
      val v1 = MiniVehicleKind.entries[seed % MiniVehicleKind.entries.size]
      listOf(v1, MiniVehicleKind.CITY_BUS, MiniVehicleKind.SEDAN) to listOf(
        MiniVehicleKind.DELIVERY_VAN,
        MiniVehicleKind.SEDAN,
      )
    }
  }

  val plinthGrass = when (style) {
    CityArchitectureStyle.DHAKA_SHAMIBAGH, CityArchitectureStyle.SOUTH_ASIA_METRO -> Color(0xFF7CB342)
    CityArchitectureStyle.DUBAI_DESERT, CityArchitectureStyle.CAIRO_SANDSTONE -> Color(0xFFD6B588)
    CityArchitectureStyle.NEW_YORK_TIMES_SQUARE, CityArchitectureStyle.TOKYO_SHIBUYA -> Color(0xFF64748B)
    else -> Color(0xFF81B64C)
  }

  return CityDioramaSpec(
    style = style,
    hierarchyLabel = hierarchyLabel,
    buildings = buildings,
    vehiclesRight = vehiclesRight,
    vehiclesLeft = vehiclesLeft,
    pedestrians = pedestrians,
    isDusty = style == CityArchitectureStyle.DHAKA_SHAMIBAGH ||
      style == CityArchitectureStyle.CAIRO_SANDSTONE ||
      style == CityArchitectureStyle.SOUTH_ASIA_METRO ||
      style == CityArchitectureStyle.DUBAI_DESERT,
    hasOverheadWires = style == CityArchitectureStyle.DHAKA_SHAMIBAGH ||
      style == CityArchitectureStyle.SOUTH_ASIA_METRO,
    hasPalms = style == CityArchitectureStyle.DHAKA_SHAMIBAGH ||
      style == CityArchitectureStyle.DUBAI_DESERT ||
      style == CityArchitectureStyle.CAIRO_SANDSTONE ||
      style == CityArchitectureStyle.SYDNEY_HARBOR,
    hasElevatedTrain = style == CityArchitectureStyle.TOKYO_SHIBUYA ||
      style == CityArchitectureStyle.NEW_YORK_BROOKLYN,
    hasCrosswalk = style == CityArchitectureStyle.NEW_YORK_TIMES_SQUARE ||
      style == CityArchitectureStyle.TOKYO_SHIBUYA ||
      style == CityArchitectureStyle.LONDON_WESTMINSTER,
    hasPhoneBooth = style == CityArchitectureStyle.LONDON_WESTMINSTER,
    hasMountainBackdrop = style == CityArchitectureStyle.SUBURBAN_NEIGHBORHOOD ||
      style == CityArchitectureStyle.TOKYO_SHIBUYA ||
      style == CityArchitectureStyle.SYDNEY_HARBOR ||
      style == CityArchitectureStyle.GLOBAL_METRO,
    roadLineColor = if (style == CityArchitectureStyle.NEW_YORK_TIMES_SQUARE ||
      style == CityArchitectureStyle.NEW_YORK_BROOKLYN ||
      style == CityArchitectureStyle.SUBURBAN_NEIGHBORHOOD
    ) SolarYellow else PureWhite,
    plinthGrassColor = plinthGrass,
  )
}

private fun buildPedestrians(style: CityArchitectureStyle, seed: Int): List<PedestrianSpec> {
  val count = when (style) {
    CityArchitectureStyle.NEW_YORK_TIMES_SQUARE,
    CityArchitectureStyle.TOKYO_SHIBUYA,
    CityArchitectureStyle.DHAKA_SHAMIBAGH,
    CityArchitectureStyle.SOUTH_ASIA_METRO -> 7
    CityArchitectureStyle.LONDON_WESTMINSTER,
    CityArchitectureStyle.PARIS_HAUSSMANN -> 5
    else -> 4
  }
  val outfits = listOf(
    Color(0xFFEF4444) to Color(0xFF1E293B),
    Color(0xFF38BDF8) to Color(0xFF0F172A),
    Color(0xFFFACC15) to Color(0xFF334155),
    Color(0xFF34D399) to Color(0xFF1E293B),
    Color(0xFFF472B6) to Color(0xFF312E81),
    Color(0xFFFFFFFF) to Color(0xFF334155),
    Color(0xFFFB923C) to Color(0xFF1F2937),
  )
  return List(count) { idx ->
    val outfit = outfits[(idx + seed) % outfits.size]
    PedestrianSpec(
      startPhase = ((idx * 0.17f + (seed % 10) * 0.03f) % 1f),
      speedMultiplier = 0.85f + ((idx + seed) % 3) * 0.18f,
      walksRight = idx % 2 == 0,
      shirtColor = outfit.first,
      pantsColor = outfit.second,
      hasBagOrUmbrella = idx % 3 == 0,
    )
  }
}

private fun buildSkylineList(style: CityArchitectureStyle, seed: Int): List<MiniBuildingSpec> {
  val nycMainAds = listOf(
    MiniBillboardSpec(Color(0xFFDC2626), "COCA-COLA", Color(0xFF991B1B), PureWhite),
    MiniBillboardSpec(Color(0xFF1E1B4B), "BROADWAY", Color(0xFF7C3AED), Color(0xFFFDE047)),
    MiniBillboardSpec(Color(0xFF0F172A), "APPLE", Color(0xFF475569), PureWhite),
    MiniBillboardSpec(Color(0xFF111827), "NIKE AIR", Color(0xFF84CC16), Color(0xFFECFCCB)),
    MiniBillboardSpec(Color(0xFF0284C7), "NASDAQ", Color(0xFF22D3EE), PureWhite),
    MiniBillboardSpec(Color(0xFFF8FAFC), "I ♥ NY", Color(0xFFE2E8F0), Color(0xFFDC2626)),
  )
  val nycUpperAds = listOf(
    MiniBillboardSpec(Color(0xFFD97706), "LION KING", Color(0xFFFEF08A), Color(0xFF451A03)),
    MiniBillboardSpec(Color(0xFFEC4899), "TIMES SQ", Color(0xFF9D174D), PureWhite),
    MiniBillboardSpec(Color(0xFF059669), "DISNEY+", Color(0xFF34D399), PureWhite),
    MiniBillboardSpec(Color(0xFF2563EB), "SAMSUNG", Color(0xFF60A5FA), PureWhite),
  )
  val tokyoAds = listOf(
    MiniBillboardSpec(Color(0xFFEC4899), "SHIBUYA", Color(0xFFBE185D), PureWhite),
    MiniBillboardSpec(Color(0xFF0284C7), "SONY 4K", Color(0xFF38BDF8), PureWhite),
    MiniBillboardSpec(Color(0xFFF59E0B), "RAMEN 24H", Color(0xFFD97706), Color(0xFFFEF9C3)),
  )
  val dhakaShopSigns = listOf(
    MiniBillboardSpec(Color(0xFF16A34A), "STORE", Color(0xFF15803D), PureWhite),
    MiniBillboardSpec(Color(0xFFDC2626), "TEA", Color(0xFFB91C1C), Color(0xFFFDE047)),
    MiniBillboardSpec(Color(0xFF0284C7), "PHARMA", Color(0xFF0369A1), PureWhite),
  )

  val palette: List<Color> = when (style) {
    CityArchitectureStyle.DHAKA_SHAMIBAGH, CityArchitectureStyle.SOUTH_ASIA_METRO -> listOf(
      Color(0xFFD17B5C),
      Color(0xFFEBD2B5),
      Color(0xFF89B8A6),
      Color(0xFFDC9D7E),
      Color(0xFFA7BCCB),
    )
    CityArchitectureStyle.NEW_YORK_TIMES_SQUARE -> listOf(
      Color(0xFF334155),
      Color(0xFF475569),
      Color(0xFF1E293B),
      Color(0xFF3B5170),
      Color(0xFF28374E),
    )
    CityArchitectureStyle.NEW_YORK_BROOKLYN -> listOf(
      Color(0xFFA85440),
      Color(0xFF8A4434),
      Color(0xFFBE6D50),
      Color(0xFF708090),
      Color(0xFF964F3E),
    )
    CityArchitectureStyle.LONDON_WESTMINSTER, CityArchitectureStyle.PARIS_HAUSSMANN -> listOf(
      Color(0xFFECE0CF),
      Color(0xFFC47458),
      Color(0xFFDFD0BC),
      Color(0xFF9FB0C4),
      Color(0xFFD5C5AE),
    )
    CityArchitectureStyle.TOKYO_SHIBUYA -> listOf(
      Color(0xFF2B3A52),
      Color(0xFF3B4C68),
      Color(0xFF4F46E5),
      Color(0xFF475569),
      Color(0xFF64748B),
    )
    CityArchitectureStyle.DUBAI_DESERT, CityArchitectureStyle.CAIRO_SANDSTONE -> listOf(
      Color(0xFFE0C1A3),
      Color(0xFFF0D9BD),
      Color(0xFF7DD3FC),
      Color(0xFFCAA373),
      Color(0xFFE2E8F0),
    )
    CityArchitectureStyle.SYDNEY_HARBOR -> listOf(
      Color(0xFFF8FAFC),
      Color(0xFF7DD3FC),
      Color(0xFFFED7AA),
      Color(0xFFCBD5E1),
      Color(0xFF93C5FD),
    )
    CityArchitectureStyle.SUBURBAN_NEIGHBORHOOD -> listOf(
      Color(0xFFFDE68A),
      Color(0xFFA7F3D0),
      Color(0xFFBFDBFE),
      Color(0xFFFECDD3),
      Color(0xFFE2E8F0),
    )
    CityArchitectureStyle.GLOBAL_METRO -> listOf(
      Color(0xFF64748B),
      Color(0xFF7C8EA3),
      Color(0xFF7DD3FC),
      Color(0xFFCBD5E1),
      Color(0xFFA5B4FC),
    )
  }

  val slots = listOf(
    0.05f to 0.125f,
    0.20f to 0.120f,
    0.34f to 0.130f,
    0.49f to 0.135f,
    0.65f to 0.125f,
    0.80f to 0.120f,
  )

  return slots.mapIndexed { idx, (xf, wf) ->
    val hBase = when (style) {
      CityArchitectureStyle.NEW_YORK_TIMES_SQUARE,
      CityArchitectureStyle.TOKYO_SHIBUYA,
      CityArchitectureStyle.DUBAI_DESERT -> 0.36f + ((idx * 17 + seed) % 3) * 0.065f
      CityArchitectureStyle.SUBURBAN_NEIGHBORHOOD -> 0.21f + ((idx * 13 + seed) % 3) * 0.045f
      else -> 0.28f + ((idx * 11 + seed) % 3) * 0.055f
    }

    val billboard = when (style) {
      CityArchitectureStyle.NEW_YORK_TIMES_SQUARE ->
        nycMainAds[(idx + seed) % nycMainAds.size]
      CityArchitectureStyle.TOKYO_SHIBUYA ->
        if (idx in listOf(1, 2, 4)) tokyoAds[(idx + seed) % tokyoAds.size] else null
      CityArchitectureStyle.DHAKA_SHAMIBAGH, CityArchitectureStyle.SOUTH_ASIA_METRO ->
        if (idx in listOf(1, 3, 4)) dhakaShopSigns[(idx + seed) % dhakaShopSigns.size] else null
      else -> null
    }

    val upperAd = if (style == CityArchitectureStyle.NEW_YORK_TIMES_SQUARE && idx in listOf(1, 2, 3, 4)) {
      nycUpperAds[(idx + seed) % nycUpperAds.size]
    } else null

    val sideAd = if (style == CityArchitectureStyle.NEW_YORK_TIMES_SQUARE && idx in listOf(0, 2, 4)) {
      nycMainAds[(idx + seed + 2) % nycMainAds.size]
    } else null

    MiniBuildingSpec(
      xFraction = xf,
      widthFraction = wf,
      heightFraction = hBase.coerceIn(0.20f, 0.50f),
      depthFraction = when (style) {
        CityArchitectureStyle.SUBURBAN_NEIGHBORHOOD -> 0.055f
        CityArchitectureStyle.NEW_YORK_TIMES_SQUARE -> 0.068f
        else -> 0.060f
      },
      facadeColor = palette[(idx + seed) % palette.size],
      trimColor = Color.White.copy(alpha = 0.36f),
      isGlassTower = style == CityArchitectureStyle.NEW_YORK_TIMES_SQUARE ||
        style == CityArchitectureStyle.TOKYO_SHIBUYA ||
        style == CityArchitectureStyle.DUBAI_DESERT ||
        (style == CityArchitectureStyle.GLOBAL_METRO && idx % 2 == 0),
      hasPitchedRoof = style == CityArchitectureStyle.SUBURBAN_NEIGHBORHOOD ||
        (style == CityArchitectureStyle.LONDON_WESTMINSTER && idx % 2 == 0) ||
        (style == CityArchitectureStyle.PARIS_HAUSSMANN),
      hasDome = (style == CityArchitectureStyle.DHAKA_SHAMIBAGH && idx == 2) ||
        (style == CityArchitectureStyle.CAIRO_SANDSTONE && idx == 3),
      hasWaterTank = (style == CityArchitectureStyle.DHAKA_SHAMIBAGH ||
        style == CityArchitectureStyle.SOUTH_ASIA_METRO ||
        style == CityArchitectureStyle.NEW_YORK_BROOKLYN) && idx % 2 == 1,
      hasHvacUnits = style == CityArchitectureStyle.NEW_YORK_TIMES_SQUARE ||
        style == CityArchitectureStyle.TOKYO_SHIBUYA ||
        style == CityArchitectureStyle.GLOBAL_METRO,
      hasBalconies = style == CityArchitectureStyle.DHAKA_SHAMIBAGH ||
        style == CityArchitectureStyle.PARIS_HAUSSMANN ||
        style == CityArchitectureStyle.NEW_YORK_BROOKLYN,
      hasShopAwning = style == CityArchitectureStyle.DHAKA_SHAMIBAGH ||
        style == CityArchitectureStyle.LONDON_WESTMINSTER ||
        style == CityArchitectureStyle.PARIS_HAUSSMANN ||
        style == CityArchitectureStyle.NEW_YORK_BROOKLYN,
      awningColor = when (idx % 3) {
        0 -> Color(0xFFDC2626)
        1 -> Color(0xFF0284C7)
        else -> Color(0xFF16A34A)
      },
      windowCols = if (wf >= 0.125f) 3 else 2,
      windowRows = if (hBase > 0.34f) 5 else 3,
      billboard = billboard,
      upperBillboard = upperAd,
      sideBillboard = sideAd,
    )
  }
}
