package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.RenderedSquircleIcon
import com.example.data.SquircleIconRenderer
import com.example.ui.LaunchTransitionPhase
import com.example.ui.LaunchTransitionState
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay

@Composable
fun OrbitFirstLaunchLoadingOverlay(
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val badgeSizeDp = 96.dp
  val badgeSizePx = with(density) { badgeSizeDp.roundToPx() }
  val selfRenderer = remember(context) { SquircleIconRenderer(context.applicationContext) }
  val renderedSelfIcon by produceState<RenderedSquircleIcon?>(
    initialValue = null,
    key1 = selfRenderer,
    key2 = badgeSizePx,
  ) {
    value = selfRenderer.renderAppSelfIcon(badgeSizePx)
  }

  val infiniteTransition = rememberInfiniteTransition(label = "orbit_loading_pulse")
  val pulse by infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "orbit_icon_pulse",
  )
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.55f,
    targetValue = 0.92f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "orbit_ring_alpha",
  )
  val sweepRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "orbit_rainbow_rotation",
  )

  val rainbowAuraColors = remember {
    listOf(
      Color(0xFF22D3EE), // Cyan
      Color(0xFF3B82F6), // Blue
      Color(0xFF8B5CF6), // Violet
      Color(0xFFEC4899), // Magenta
      Color(0xFFF97316), // Coral Orange
      Color(0xFFFACC15), // Gold
      Color(0xFF10B981), // Emerald
      Color(0xFF22D3EE), // Seamless wrap
    )
  }

  val squirclePainter = remember(renderedSelfIcon?.bitmap) {
    renderedSelfIcon?.bitmap?.let { BitmapPainter(it, filterQuality = FilterQuality.High) }
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF080E1F))
      .testTag("orbit_loading_overlay"),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        contentAlignment = Alignment.Center,
      ) {
        // Rotating gradient rainbow aura + multi-ring orbital glow
        Canvas(
          modifier = Modifier
            .size(240.dp)
            .graphicsLayer { rotationZ = sweepRotation },
        ) {
          val baseRadius = (size.minDimension * 0.25f) * pulse
          val rainbowBrush = Brush.sweepGradient(colors = rainbowAuraColors, center = center)

          // Soft outer layered rainbow halo rings for smooth atmospheric bloom
          for (i in 1..6) {
            val stepFraction = i / 6f
            val ringAlpha = ((1f - stepFraction) * 0.24f * glowAlpha).coerceIn(0f, 1f)
            drawCircle(
              brush = rainbowBrush,
              radius = baseRadius * (1.02f + stepFraction * 0.78f),
              center = center,
              alpha = ringAlpha,
              style = Stroke(width = (18.dp.toPx() * (0.65f + stepFraction * 0.6f))),
            )
          }

          // Crisp inner orbital rainbow gradient ring framing the Orbit squircle
          drawCircle(
            brush = rainbowBrush,
            radius = baseRadius * 1.06f,
            center = center,
            alpha = glowAlpha * 0.85f,
            style = Stroke(width = 3.dp.toPx()),
          )
        }

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(badgeSizeDp)
            .graphicsLayer {
              scaleX = pulse
              scaleY = pulse
            }
            .shadow(
              elevation = 16.dp,
              shape = DefaultAppleSquircleShape,
              clip = false,
              ambientColor = Color(0xFFEC4899).copy(alpha = 0.65f),
              spotColor = Color(0xFF22D3EE).copy(alpha = 0.85f),
            )
            .clip(DefaultAppleSquircleShape),
        ) {
          Image(
            painter = squirclePainter ?: painterResource(id = R.drawable.img_orbit_splash_badge),
            contentDescription = stringResource(id = R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(id = R.string.app_name),
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.SemiBold,
          shadow = Shadow(
            color = Color.Black.copy(alpha = 0.45f),
            offset = Offset(0f, 2f),
            blurRadius = 8f,
          ),
        ),
        color = Color.White.copy(alpha = 0.94f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
fun MaterialSplashTransitionOverlay(
  transitionState: LaunchTransitionState,
  containerWidthPx: Float,
  containerHeightPx: Float,
  onLaunchAnimationReady: (Int) -> Unit,
  onCloseCompleted: () -> Unit,
) {
  val originRect = transitionState.originRect
  if (transitionState.phase == LaunchTransitionPhase.IDLE || originRect == null) return

  val currentOnLaunchReady by rememberUpdatedState(onLaunchAnimationReady)
  val currentOnCloseCompleted by rememberUpdatedState(onCloseCompleted)

  val progress = remember(transitionState.eventId) {
    val initial = when (transitionState.phase) {
      LaunchTransitionPhase.LAUNCHING -> 0f
      LaunchTransitionPhase.LAUNCHED_WAITING_RETURN,
      LaunchTransitionPhase.CLOSING -> 1f
      LaunchTransitionPhase.IDLE -> 0f
    }
    Animatable(initial)
  }

  LaunchedEffect(transitionState.phase, transitionState.eventId) {
    when (transitionState.phase) {
      LaunchTransitionPhase.LAUNCHING -> {
        progress.snapTo(0f)
        progress.animateTo(
          targetValue = 1f,
          animationSpec = tween(
            durationMillis = 420,
            easing = FastOutSlowInEasing,
          ),
        )
        delay(60L)
        currentOnLaunchReady(transitionState.eventId)
      }
      LaunchTransitionPhase.LAUNCHED_WAITING_RETURN -> {
        progress.snapTo(1f)
      }
      LaunchTransitionPhase.CLOSING -> {
        progress.snapTo(1f)
        delay(75L)
        progress.animateTo(
          targetValue = 0f,
          animationSpec = tween(
            durationMillis = 420,
            easing = FastOutSlowInEasing,
          ),
        )
        currentOnCloseCompleted()
      }
      LaunchTransitionPhase.IDLE -> Unit
    }
  }

  val originCenter = originRect.center
  val startRadius = min(originRect.width, originRect.height) * 0.45f
  val maxCornerRadius = hypot(
    max(originCenter.x, containerWidthPx - originCenter.x),
    max(originCenter.y, containerHeightPx - originCenter.y),
  ) * 1.06f

  val baseColor = transitionState.splashColor
  val secondaryColor = transitionState.secondarySplashColor
  val topGlowColor = remember(baseColor) { lerpColor(baseColor, Color.White, 0.18f) }
  val surfaceSplashColor = remember(baseColor) { lerpColor(baseColor, Color(0xFF101624), 0.18f) }
  val deepEdgeColor = remember(secondaryColor) { lerpColor(secondaryColor, Color(0xFF080D18), 0.34f) }
  val rippleHighlightColor = remember(baseColor) { lerpColor(baseColor, Color.White, 0.42f) }
  val labelTextColor = remember(surfaceSplashColor) {
    if (surfaceSplashColor.luminance() > 0.62f) Color(0xFF0F172A) else Color.White
  }

  val iconBitmap = transitionState.iconAsset?.bitmap
  val splashPainter = remember(iconBitmap) {
    iconBitmap?.let { BitmapPainter(it, filterQuality = FilterQuality.High) }
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val t = progress.value.coerceIn(0f, 1f)
      val currentRadius = startRadius + (maxCornerRadius - startRadius) * t
      val revealAlpha = (0.38f + 0.62f * t).coerceIn(0f, 1f)

      val splashGradient = Brush.radialGradient(
        0.0f to topGlowColor.copy(alpha = revealAlpha),
        0.48f to surfaceSplashColor.copy(alpha = revealAlpha),
        1.0f to deepEdgeColor.copy(alpha = revealAlpha),
        center = originCenter,
        radius = currentRadius.coerceAtLeast(1f),
      )

      drawCircle(
        brush = splashGradient,
        radius = currentRadius,
        center = originCenter,
      )

      val rippleAlpha = ((1f - t) * 0.65f).coerceIn(0f, 0.65f)
      if (rippleAlpha > 0.01f) {
        drawCircle(
          brush = Brush.linearGradient(
            colors = listOf(
              rippleHighlightColor.copy(alpha = rippleAlpha),
              secondaryColor.copy(alpha = rippleAlpha * 0.85f),
            )
          ),
          radius = currentRadius * 1.03f,
          center = originCenter,
          style = Stroke(width = 3.5.dp.toPx()),
        )
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .padding(horizontal = 32.dp)
        .graphicsLayer {
          val t = progress.value.coerceIn(0f, 1f)
          val emblemVis = ((t - 0.14f) / 0.64f).coerceIn(0f, 1f)
          val s = 0.68f + (0.32f * emblemVis)
          scaleX = s
          scaleY = s
          alpha = emblemVis
        },
    ) {
      if (splashPainter != null) {
        Image(
          painter = splashPainter,
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(88.dp),
        )
      }
      if (transitionState.label.isNotBlank()) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = transitionState.label,
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            shadow = Shadow(
              color = Color.Black.copy(alpha = 0.28f),
              offset = Offset(0f, 2f),
              blurRadius = 6f,
            ),
          ),
          color = labelTextColor,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
