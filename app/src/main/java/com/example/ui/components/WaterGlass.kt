package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Realistic modern frosted/refractive glass surface (no heavy drop shadows,
 * no blocky Windows 7 horizontal gloss bar):
 * - Delicate diagonal optical frosting with subtle cool depth
 * - Chamfered diagonal specular rim catching light on top-left and bottom-right edges
 * - Ultra-fine inner refraction bevel stroke
 */
@Composable
fun WaterGlassSurface(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(26.dp),
  cornerRadius: Dp = 26.dp,
  edgeAccentColor: Color? = null,
  shadowElevation: Dp = 6.dp,
  isFilledAccent: Boolean = false,
  accentColor: Color = Color(0xFFEF4444),
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val softShadowElevation = (shadowElevation * 0.42f).coerceAtMost(6.dp)

  if (isFilledAccent) {
    val topTint = lerp(accentColor, Color.White, 0.08f)
    val bottomTint = lerp(accentColor, Color.Black, 0.10f)
    Surface(
      modifier = modifier
        .shadow(
          elevation = softShadowElevation,
          shape = shape,
          ambientColor = accentColor.copy(alpha = 0.18f),
          spotColor = Color.Black.copy(alpha = 0.14f),
        )
        .clip(shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
      shape = shape,
      color = Color.Transparent,
      border = BorderStroke(
        width = 0.85.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            Color.White.copy(alpha = 0.56f),
            Color.White.copy(alpha = 0.14f),
            Color.White.copy(alpha = 0.30f),
          ),
        ),
      ),
    ) {
      Box(
        modifier = Modifier
          .background(
            Brush.linearGradient(
              colors = listOf(topTint, accentColor, bottomTint),
            )
          )
          .drawInnerGlassBevel(cornerRadius = cornerRadius, alpha = 0.18f),
        contentAlignment = Alignment.Center,
        content = content,
      )
    }
    return
  }

  val bodyBrush = Brush.linearGradient(
    colors = listOf(
      Color.White.copy(alpha = 0.15f),
      Color(0xFFDCEEFF).copy(alpha = 0.08f),
      Color.White.copy(alpha = 0.10f),
    ),
  )

  val borderBrush =
    if (edgeAccentColor != null) {
      Brush.linearGradient(
        colors = listOf(
          edgeAccentColor.copy(alpha = 0.85f),
          edgeAccentColor.copy(alpha = 0.35f),
          edgeAccentColor.copy(alpha = 0.65f),
        )
      )
    } else {
      Brush.linearGradient(
        colors = listOf(
          Color.White.copy(alpha = 0.52f),
          Color.White.copy(alpha = 0.12f),
          Color.White.copy(alpha = 0.28f),
        )
      )
    }

  Surface(
    modifier = modifier
      .shadow(
        elevation = softShadowElevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.08f),
        spotColor = Color.Black.copy(alpha = 0.12f),
      )
      .clip(shape)
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    shape = shape,
    color = Color.Transparent,
    border = BorderStroke(0.85.dp, borderBrush),
  ) {
    Box(
      modifier = Modifier
        .background(bodyBrush)
        .drawInnerGlassBevel(cornerRadius = cornerRadius, alpha = 0.14f),
      contentAlignment = Alignment.Center,
      content = content,
    )
  }
}

private fun Modifier.drawInnerGlassBevel(
  cornerRadius: Dp,
  alpha: Float,
): Modifier = this.drawBehind {
  val inset = 1.2.dp.toPx()
  val w = (size.width - inset * 2f).coerceAtLeast(0f)
  val h = (size.height - inset * 2f).coerceAtLeast(0f)
  if (w > 2f && h > 2f) {
    val r = (cornerRadius.toPx() - inset).coerceIn(0f, minOf(w, h) / 2f)
    drawRoundRect(
      brush = Brush.linearGradient(
        colors = listOf(
          Color.White.copy(alpha = alpha),
          Color.Transparent,
          Color.White.copy(alpha = alpha * 0.45f),
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
      ),
      topLeft = Offset(inset, inset),
      size = Size(w, h),
      cornerRadius = CornerRadius(r, r),
      style = Stroke(width = 0.7.dp.toPx()),
    )
  }
}

fun contrastingOnAccent(accentColor: Color): Color =
  if (accentColor.luminance() > 0.62f) Color(0xFF090D16) else Color.White
