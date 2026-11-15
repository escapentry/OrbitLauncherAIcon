package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.data.RenderedSquircleIcon
import com.example.model.AppEntry

val DefaultAppleSquircleShape = AppleSquircleShape(exponent = 4.5f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SquircleAppItem(
  entry: AppEntry,
  iconSizeDp: Dp,
  iconRenderPx: Int,
  labelFontSp: TextUnit,
  labelLineHeightSp: TextUnit,
  labelMaxLines: Int,
  showLabel: Boolean,
  isLaunchingTarget: Boolean,
  isReturningTarget: Boolean,
  getCachedIcon: (String, Int) -> RenderedSquircleIcon?,
  loadIcon: suspend (AppEntry, Int) -> RenderedSquircleIcon?,
  onClick: (Rect?, RenderedSquircleIcon?) -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
  iconPackKey: String = "orbit_fusion",
) {
  val haptics = LocalHapticFeedback.current
  val interactionSource = remember(entry.componentKey) { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  var iconBoundsInRoot by remember(entry.componentKey) { mutableStateOf<Rect?>(null) }

  val pressScale by animateFloatAsState(
    targetValue = when {
      isLaunchingTarget -> 0.84f
      isPressed -> 0.90f
      isReturningTarget -> 1.07f
      else -> 1.0f
    },
    animationSpec = spring(
      dampingRatio = if (isReturningTarget) 0.52f else Spring.DampingRatioNoBouncy,
      stiffness = if (isReturningTarget) Spring.StiffnessMedium else Spring.StiffnessHigh,
    ),
    label = "squircle_press_scale",
  )

  val initialAsset = remember(entry.componentKey, iconRenderPx, iconPackKey) {
    getCachedIcon(entry.componentKey, iconRenderPx)
  }

  val iconAsset by produceState(
    initialValue = initialAsset,
    key1 = entry.componentKey,
    key2 = iconRenderPx,
    key3 = iconPackKey,
  ) {
    val cached = getCachedIcon(entry.componentKey, iconRenderPx)
    if (cached != null) {
      value = cached
    } else {
      value = null
      value = loadIcon(entry, iconRenderPx)
    }
  }

  val currentOnClick by androidx.compose.runtime.rememberUpdatedState(onClick)
  val currentOnLongClick by androidx.compose.runtime.rememberUpdatedState(onLongClick)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Top,
    modifier = modifier
      .graphicsLayer {
        scaleX = pressScale
        scaleY = pressScale
      }
      .clip(RoundedCornerShape(18.dp))
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = Color.White),
        onClick = { currentOnClick(iconBoundsInRoot, iconAsset) },
        onLongClick = {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          currentOnLongClick()
        },
      )
      .padding(vertical = 4.dp, horizontal = 4.dp)
      .testTag("app_item_${entry.packageName}"),
  ) {
    SquircleAppBadge(
      label = entry.label,
      bitmap = iconAsset?.bitmap,
      sizeDp = iconSizeDp,
      modifier = Modifier.onGloballyPositioned { coords ->
        iconBoundsInRoot = coords.boundsInRoot()
      },
    )

    if (showLabel) {
      Spacer(modifier = Modifier.height(5.dp))
      Text(
        text = entry.label,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontSize = labelFontSp,
          lineHeight = labelLineHeightSp,
          shadow = Shadow(
            color = Color.Black.copy(alpha = 0.78f),
            offset = Offset(0f, 1.5f),
            blurRadius = 4f,
          ),
        ),
        color = Color.White.copy(alpha = 0.96f),
        maxLines = labelMaxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
fun SquircleAppBadge(
  label: String,
  bitmap: ImageBitmap?,
  sizeDp: Dp,
  modifier: Modifier = Modifier,
) {
  val highQualityPainter = remember(bitmap) {
    bitmap?.let { BitmapPainter(it, filterQuality = FilterQuality.High) }
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(sizeDp)
      .shadow(
        elevation = 12.dp,
        shape = DefaultAppleSquircleShape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.55f),
        spotColor = Color.Black.copy(alpha = 0.75f),
      )
      .then(
        if (highQualityPainter == null) {
          Modifier
            .clip(DefaultAppleSquircleShape)
            .background(Color(0xFFF3F6FA), DefaultAppleSquircleShape)
        } else {
          Modifier
        }
      ),
  ) {
    if (highQualityPainter != null) {
      Image(
        painter = highQualityPainter,
        contentDescription = label,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      val initial = label.trimStart().firstOrNull()?.uppercaseChar()?.toString() ?: "•"
      Text(
        text = initial,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color(0xFF0F172A),
      )
    }
  }
}
