package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.RenderedSquircleIcon
import com.example.display.DisplayProfile
import com.example.model.AppEntry
import kotlin.math.roundToInt

private val AmberWarningAccent = Color(0xFFFBBF24)
private val AmberWarningBgTop = Color(0x33F59E0B)
private val AmberWarningBgBottom = Color(0x1AF59E0B)
private val AmberWarningBorder = Color(0x66FBBF24)
private val DockGlassBase = Color(0x880C121E)
private val DockGlassSheenBottom = Color(0x660B101B)

@Composable
fun ThallyAceAppsPage(
  apps: List<AppEntry>,
  gradientColors: List<Color>,
  fallbackDockGradientColors: List<Color>,
  isFolderExpanded: Boolean,
  isDisbanded: Boolean,
  onToggleFolderExpanded: () -> Unit,
  onCollapseFolder: () -> Unit,
  onToggleDisbanded: () -> Unit,
  profile: DisplayProfile,
  launchingComponentKey: String?,
  returningComponentKey: String?,
  onLaunchApp: (AppEntry, Rect?, RenderedSquircleIcon?) -> Unit,
  onLongPressApp: (AppEntry) -> Unit,
  onLongPressBlankSpace: () -> Unit,
  getCachedIcon: (String, Int) -> RenderedSquircleIcon?,
  loadIcon: suspend (AppEntry, Int) -> RenderedSquircleIcon?,
  modifier: Modifier = Modifier,
  iconPackKey: String = "orbit_fusion",
) {
  val effectiveGradientColors = remember(gradientColors, fallbackDockGradientColors) {
    when {
      gradientColors.isNotEmpty() -> gradientColors
      fallbackDockGradientColors.isNotEmpty() -> fallbackDockGradientColors
      else -> listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
    }
  }

  val textShadow = remember {
    Shadow(
      color = Color.Black.copy(alpha = 0.58f),
      offset = Offset(0f, 1.5f),
      blurRadius = 6f,
    )
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .testTag("thally_ace_apps_page"),
  ) {
    val availableHeightDp = maxHeight
    val isCompactHeight = availableHeightDp < 470.dp
    val showCautionCard = isDisbanded || !isFolderExpanded || apps.isEmpty()

    val outerVerticalPadding = if (isCompactHeight) 4.dp else 8.dp
    val headerToNextGap = if (isCompactHeight) 8.dp else 12.dp
    val cautionToFolderGap = if (isCompactHeight) 10.dp else 16.dp
    val cautionVerticalPad = if (isCompactHeight) 9.dp else 12.dp

    // Reserve header + spacing so both collapsed and expanded states fit above the dock
    val headerEstimateDp = 64.dp
    val bottomBreathingDp = if (isCompactHeight) 6.dp else 12.dp
    val availableForCollapsedFolder = (availableHeightDp - 196.dp).coerceAtLeast(132.dp)
    val availableForExpandedFolder = (availableHeightDp - headerEstimateDp - headerToNextGap - outerVerticalPadding * 2 - bottomBreathingDp)
      .coerceAtLeast(180.dp)

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .pointerInput(isFolderExpanded) {
          detectTapGestures(
            onTap = {
              if (isFolderExpanded) {
                onCollapseFolder()
              }
            },
            onLongPress = { onLongPressBlankSpace() },
          )
        }
        .padding(horizontal = profile.horizontalPaddingDp, vertical = outerVerticalPadding),
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 640.dp)
          .fillMaxWidth(),
      ) {
        // Page header: Title on first line, ALPHA badge + Folder/Disband toggle below
        Column(
          horizontalAlignment = Alignment.Start,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(id = R.string.thally_ace_apps_title),
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              shadow = textShadow,
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Surface(
              color = AmberWarningAccent.copy(alpha = 0.16f),
              contentColor = AmberWarningAccent,
              shape = CircleShape,
              modifier = Modifier
                .border(
                  width = 0.75.dp,
                  color = AmberWarningAccent.copy(alpha = 0.45f),
                  shape = CircleShape,
                )
                .testTag("thally_ace_alpha_badge"),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Science,
                  contentDescription = null,
                  modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = stringResource(id = R.string.thally_ace_apps_badge),
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                  maxLines = 1,
                )
              }
            }

            Surface(
              color = Color.White.copy(alpha = if (isDisbanded) 0.20f else 0.12f),
              contentColor = Color.White,
              shape = CircleShape,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .border(
                  width = 0.75.dp,
                  color = Color.White.copy(alpha = if (isDisbanded) 0.44f else 0.24f),
                  shape = CircleShape,
                )
                .clickable(onClick = onToggleDisbanded)
                .testTag("thally_ace_disband_toggle"),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              ) {
                Icon(
                  imageVector = if (isDisbanded) Icons.Rounded.FolderSpecial else Icons.Rounded.GridView,
                  contentDescription = if (isDisbanded) {
                    stringResource(id = R.string.thally_ace_group_folder)
                  } else {
                    stringResource(id = R.string.thally_ace_disband_folder)
                  },
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = if (isDisbanded) {
                    stringResource(id = R.string.thally_ace_group_folder)
                  } else {
                    stringResource(id = R.string.thally_ace_disband_folder)
                  },
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  maxLines = 1,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(headerToNextGap))

        // Dismiss caution smoothly when folder expands so the folder expands upwards into the reclaimed space
        AnimatedVisibility(
          visible = showCautionCard,
          enter = expandVertically(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
          ) + fadeIn(tween(durationMillis = 200)),
          exit = shrinkVertically(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
          ) + fadeOut(tween(durationMillis = 150)),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            CautionDevelopmentBuildsCard(
              verticalPaddingDp = cautionVerticalPad,
              isCompactHeight = isCompactHeight,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("thally_ace_caution_card"),
            )
            Spacer(modifier = Modifier.height(cautionToFolderGap))
          }
        }

        AnimatedContent(
          targetState = isDisbanded,
          transitionSpec = {
            fadeIn(tween(220, easing = FastOutSlowInEasing)) togetherWith
              fadeOut(tween(180, easing = FastOutSlowInEasing))
          },
          label = "thally_folder_or_disbanded",
        ) { disbanded ->
          if (disbanded) {
            DisbandedThallyAceAppsGrid(
              apps = apps,
              profile = profile,
              launchingComponentKey = launchingComponentKey,
              returningComponentKey = returningComponentKey,
              onLaunchApp = onLaunchApp,
              onLongPressApp = onLongPressApp,
              getCachedIcon = getCachedIcon,
              loadIcon = loadIcon,
              iconPackKey = iconPackKey,
              modifier = Modifier.fillMaxWidth(),
            )
          } else {
            // Big dock-material folder card (click-to-reveal), rescaled to device width & height
            FrostedDockFolderCard(
              apps = apps,
              gradientColors = effectiveGradientColors,
              isExpanded = isFolderExpanded,
              onToggleExpanded = onToggleFolderExpanded,
              profile = profile,
              availableForCollapsedHeightDp = availableForCollapsedFolder,
              availableForExpandedHeightDp = availableForExpandedFolder,
              launchingComponentKey = launchingComponentKey,
              returningComponentKey = returningComponentKey,
              onLaunchApp = onLaunchApp,
              onLongPressApp = onLongPressApp,
              getCachedIcon = getCachedIcon,
              loadIcon = loadIcon,
              iconPackKey = iconPackKey,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        Spacer(modifier = Modifier.height(bottomBreathingDp))
      }
    }
  }
}

@Composable
private fun DisbandedThallyAceAppsGrid(
  apps: List<AppEntry>,
  profile: DisplayProfile,
  launchingComponentKey: String?,
  returningComponentKey: String?,
  onLaunchApp: (AppEntry, Rect?, RenderedSquircleIcon?) -> Unit,
  onLongPressApp: (AppEntry) -> Unit,
  getCachedIcon: (String, Int) -> RenderedSquircleIcon?,
  loadIcon: suspend (AppEntry, Int) -> RenderedSquircleIcon?,
  iconPackKey: String,
  modifier: Modifier = Modifier,
) {
  if (apps.isEmpty()) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = modifier
        .heightIn(min = 160.dp)
        .testTag("thally_ace_disbanded_empty"),
    ) {
      Text(
        text = stringResource(id = R.string.thally_ace_disbanded_empty),
        style = MaterialTheme.typography.titleMedium,
        color = Color.White.copy(alpha = 0.72f),
        textAlign = TextAlign.Center,
      )
    }
    return
  }

  val rows = remember(apps, profile.columns) {
    apps.chunked(profile.columns.coerceAtLeast(3))
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(profile.gridVerticalSpacingDp),
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 4.dp)
      .testTag("thally_ace_disbanded_grid"),
  ) {
    rows.forEach { rowApps ->
      Row(
        horizontalArrangement = Arrangement.spacedBy(profile.gridHorizontalSpacingDp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
      ) {
        val cols = profile.columns.coerceAtLeast(3)
        for (colIndex in 0 until cols) {
          val entry = rowApps.getOrNull(colIndex)
          if (entry != null) {
            key(entry.componentKey, iconPackKey) {
              SquircleAppItem(
                entry = entry,
                iconSizeDp = profile.iconSizeDp,
                iconRenderPx = profile.iconRenderPx,
                labelFontSp = profile.labelFontSp,
                labelLineHeightSp = profile.labelLineHeightSp,
                labelMaxLines = profile.labelMaxLines,
                showLabel = true,
                isLaunchingTarget = entry.componentKey == launchingComponentKey,
                isReturningTarget = entry.componentKey == returningComponentKey,
                getCachedIcon = getCachedIcon,
                loadIcon = loadIcon,
                onClick = { bounds, asset -> onLaunchApp(entry, bounds, asset) },
                onLongClick = { onLongPressApp(entry) },
                iconPackKey = iconPackKey,
                modifier = Modifier.weight(1f),
              )
            }
          } else {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun CautionDevelopmentBuildsCard(
  verticalPaddingDp: Dp,
  isCompactHeight: Boolean,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(20.dp)
  val bgBrush = remember {
    Brush.verticalGradient(colors = listOf(AmberWarningBgTop, AmberWarningBgBottom))
  }

  Row(
    verticalAlignment = Alignment.Top,
    modifier = modifier
      .clip(shape)
      .background(Color(0x66121622), shape)
      .background(bgBrush, shape)
      .border(0.9.dp, AmberWarningBorder, shape)
      .padding(horizontal = 14.dp, vertical = verticalPaddingDp),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(if (isCompactHeight) 30.dp else 34.dp)
        .clip(CircleShape)
        .background(AmberWarningAccent.copy(alpha = 0.20f)),
    ) {
      Icon(
        imageVector = Icons.Rounded.WarningAmber,
        contentDescription = stringResource(id = R.string.thally_ace_caution_title),
        tint = AmberWarningAccent,
        modifier = Modifier.size(if (isCompactHeight) 18.dp else 20.dp),
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(id = R.string.thally_ace_caution_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = AmberWarningAccent,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = stringResource(id = R.string.thally_ace_caution_message),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
        color = Color.White.copy(alpha = 0.88f),
        maxLines = if (isCompactHeight) 3 else 4,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun FrostedDockFolderCard(
  apps: List<AppEntry>,
  gradientColors: List<Color>,
  isExpanded: Boolean,
  onToggleExpanded: () -> Unit,
  profile: DisplayProfile,
  availableForCollapsedHeightDp: Dp,
  availableForExpandedHeightDp: Dp,
  launchingComponentKey: String?,
  returningComponentKey: String?,
  onLaunchApp: (AppEntry, Rect?, RenderedSquircleIcon?) -> Unit,
  onLongPressApp: (AppEntry) -> Unit,
  getCachedIcon: (String, Int) -> RenderedSquircleIcon?,
  loadIcon: suspend (AppEntry, Int) -> RenderedSquircleIcon?,
  iconPackKey: String,
  modifier: Modifier = Modifier,
) {
  val resolvedGradientColors = remember(gradientColors) {
    when {
      gradientColors.size >= 2 -> gradientColors.map { it.copy(alpha = 0.34f) }
      gradientColors.size == 1 -> {
        val c = gradientColors.first()
        listOf(c.copy(alpha = 0.36f), lerpColor(c, Color.White, 0.25f).copy(alpha = 0.28f))
      }
      else -> listOf(
        Color.White.copy(alpha = 0.20f),
        Color.White.copy(alpha = 0.14f),
      )
    }
  }

  val borderColors = remember(gradientColors) {
    when {
      gradientColors.size >= 2 -> gradientColors.map {
        lerpColor(it, Color.White, 0.45f).copy(alpha = 0.46f)
      }
      gradientColors.size == 1 -> {
        val c = lerpColor(gradientColors.first(), Color.White, 0.45f).copy(alpha = 0.46f)
        listOf(c, Color.White.copy(alpha = 0.24f))
      }
      else -> listOf(
        Color.White.copy(alpha = 0.34f),
        Color.White.copy(alpha = 0.16f),
      )
    }
  }

  val dockTintBrush = remember(resolvedGradientColors) {
    Brush.horizontalGradient(colors = resolvedGradientColors)
  }
  val dockGlassSheen = remember {
    Brush.verticalGradient(
      colors = listOf(
        Color.White.copy(alpha = 0.14f),
        DockGlassSheenBottom,
      )
    )
  }
  val dockBorderBrush = remember(borderColors) {
    Brush.horizontalGradient(colors = borderColors)
  }

  val folderShape = RoundedCornerShape(30.dp)
  val cardInteraction = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .clip(folderShape)
      .background(DockGlassBase, folderShape)
      .background(dockTintBrush, folderShape)
      .background(dockGlassSheen, folderShape)
      .border(0.85.dp, dockBorderBrush, folderShape)
      .clickable(
        interactionSource = cardInteraction,
        indication = null,
        enabled = isExpanded && apps.isNotEmpty(),
        onClick = {},
      )
      .animateContentSize(
        animationSpec = spring(
          dampingRatio = 0.85f,
          stiffness = Spring.StiffnessMediumLow,
        )
      )
      .testTag("thally_ace_folder_card"),
  ) {
    if (apps.isEmpty()) {
      EmptyThallyAceFolderContent(
        availableHeightDp = availableForCollapsedHeightDp,
        modifier = Modifier.fillMaxWidth(),
      )
    } else {
      AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
          fadeIn(tween(220, easing = FastOutSlowInEasing)) togetherWith
            fadeOut(tween(160, easing = FastOutSlowInEasing))
        },
        label = "thally_folder_reveal",
      ) { expanded ->
        if (!expanded) {
          CollapsedThallyAceFolderContent(
            apps = apps,
            profile = profile,
            availableHeightDp = availableForCollapsedHeightDp,
            getCachedIcon = getCachedIcon,
            loadIcon = loadIcon,
            iconPackKey = iconPackKey,
            onClick = onToggleExpanded,
          )
        } else {
          ExpandedThallyAceFolderContent(
            apps = apps,
            profile = profile,
            availableHeightDp = availableForExpandedHeightDp,
            launchingComponentKey = launchingComponentKey,
            returningComponentKey = returningComponentKey,
            onLaunchApp = onLaunchApp,
            onLongPressApp = onLongPressApp,
            onCollapse = onToggleExpanded,
            getCachedIcon = getCachedIcon,
            loadIcon = loadIcon,
            iconPackKey = iconPackKey,
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyThallyAceFolderContent(
  availableHeightDp: Dp,
  modifier: Modifier = Modifier,
) {
  val isTight = availableHeightDp < 190.dp
  val verticalPad = if (isTight) 18.dp else 28.dp
  val iconBoxSize = if (isTight) 54.dp else 66.dp

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
      .heightIn(min = availableHeightDp.coerceIn(132.dp, 210.dp))
      .padding(horizontal = 22.dp, vertical = verticalPad),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(iconBoxSize)
        .clip(DefaultAppleSquircleShape)
        .background(Color.White.copy(alpha = 0.12f))
        .border(0.8.dp, Color.White.copy(alpha = 0.24f), DefaultAppleSquircleShape),
    ) {
      Icon(
        imageVector = Icons.Rounded.FolderSpecial,
        contentDescription = stringResource(id = R.string.thally_ace_apps_title),
        tint = Color.White.copy(alpha = 0.86f),
        modifier = Modifier.size(if (isTight) 28.dp else 34.dp),
      )
    }

    Spacer(modifier = Modifier.height(if (isTight) 10.dp else 14.dp))

    Text(
      text = stringResource(id = R.string.thally_ace_empty_folder_message),
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      color = Color.White,
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag("thally_ace_folder_empty_message"),
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = stringResource(id = R.string.thally_ace_empty_folder_subtext),
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
      color = Color.White.copy(alpha = 0.70f),
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun CollapsedThallyAceFolderContent(
  apps: List<AppEntry>,
  profile: DisplayProfile,
  availableHeightDp: Dp,
  getCachedIcon: (String, Int) -> RenderedSquircleIcon?,
  loadIcon: suspend (AppEntry, Int) -> RenderedSquircleIcon?,
  iconPackKey: String,
  onClick: () -> Unit,
) {
  val previewApps = remember(apps) { apps.take(6) }
  val previewRows = remember(previewApps) { previewApps.chunked(3) }

  // Rescale preview box and spacing so collapsed folder never encroaches into the dock
  val isTight = availableHeightDp < 200.dp
  val verticalPadding = (availableHeightDp * 0.08f).coerceIn(12.dp, 22.dp)
  val previewRowCount = previewRows.size.coerceAtLeast(1)
  val miniBadgeSize = if (isTight) 30.dp else 36.dp
  val miniGap = if (isTight) 8.dp else 10.dp
  val previewInnerPad = if (isTight) 10.dp else 12.dp
  val previewHeight = (miniBadgeSize * previewRowCount) +
    (miniGap * (previewRowCount - 1)) +
    (previewInnerPad * 2)
  val previewWidth = (miniBadgeSize * 3) + (miniGap * 2) + (previewInnerPad * 2) + 8.dp

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .clickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = verticalPadding)
      .testTag("thally_ace_folder_collapsed"),
  ) {
    // Mini squircle folder preview well (shows up to 6 apps in a 3x2 grid)
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(width = previewWidth, height = previewHeight)
        .clip(RoundedCornerShape(24.dp))
        .background(Color.White.copy(alpha = 0.11f))
        .border(0.8.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
        .padding(previewInnerPad),
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(miniGap),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        previewRows.forEach { rowApps ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(miniGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            rowApps.forEach { entry ->
              MiniFolderIconBadge(
                entry = entry,
                badgeSizeDp = miniBadgeSize,
                iconRenderPx = profile.iconRenderPx,
                iconPackKey = iconPackKey,
                getCachedIcon = getCachedIcon,
                loadIcon = loadIcon,
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(if (isTight) 10.dp else 14.dp))

    Text(
      text = stringResource(id = R.string.thally_ace_apps_title),
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      color = Color.White,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(3.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Text(
        text = "${apps.size} ${if (apps.size == 1) "app" else "apps"} · ${stringResource(id = R.string.thally_ace_folder_tap_reveal)}",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.72f),
      )
      Spacer(modifier = Modifier.width(4.dp))
      Icon(
        imageVector = Icons.Rounded.ExpandMore,
        contentDescription = stringResource(id = R.string.thally_ace_folder_tap_reveal),
        tint = Color.White.copy(alpha = 0.72f),
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun ExpandedThallyAceFolderContent(
  apps: List<AppEntry>,
  profile: DisplayProfile,
  availableHeightDp: Dp,
  launchingComponentKey: String?,
  returningComponentKey: String?,
  onLaunchApp: (AppEntry, Rect?, RenderedSquircleIcon?) -> Unit,
  onLongPressApp: (AppEntry) -> Unit,
  onCollapse: () -> Unit,
  getCachedIcon: (String, Int) -> RenderedSquircleIcon?,
  loadIcon: suspend (AppEntry, Int) -> RenderedSquircleIcon?,
  iconPackKey: String,
) {
  val density = LocalDensity.current
  val rows = remember(apps) { apps.chunked(3) }
  val rowCount = rows.size.coerceAtLeast(1)

  val isTightHeight = availableHeightDp < 340.dp
  val cardVerticalPad = if (isTightHeight) 12.dp else 16.dp
  val headerToGridGap = if (isTightHeight) 10.dp else 14.dp
  val rowSpacing = if (isTightHeight) {
    profile.gridVerticalSpacingDp.coerceAtMost(10.dp)
  } else {
    profile.gridVerticalSpacingDp.coerceIn(10.dp, 20.dp)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = cardVerticalPad)
      .testTag("thally_ace_folder_expanded"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 6.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Icon(
          imageVector = Icons.Rounded.FolderSpecial,
          contentDescription = null,
          tint = Color.White.copy(alpha = 0.90f),
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(id = R.string.thally_ace_apps_title),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Surface(
        color = Color.White.copy(alpha = 0.14f),
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .clip(CircleShape)
          .clickable(onClick = onCollapse)
          .testTag("thally_ace_folder_collapse_button"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(id = R.string.thally_ace_folder_tap_collapse),
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = stringResource(id = R.string.thally_ace_folder_tap_collapse),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(headerToGridGap))

    // 3 apps on the first row, 2 on the second auto-centered; icon size scales to both width & height
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val horizontalSpacing = profile.gridHorizontalSpacingDp
      val cellWidthDp = ((maxWidth - (horizontalSpacing * 2f)) / 3f).coerceAtLeast(60.dp)

      val chromeOverheadDp = (cardVerticalPad * 2) + 48.dp + headerToGridGap
      val availableForRowsDp = (availableHeightDp - chromeOverheadDp).coerceAtLeast(110.dp)
      val labelAndPaddingPerRowDp = 42.dp
      val maxIconByHeightDp = (((availableForRowsDp - (rowSpacing * (rowCount - 1))) / rowCount) - labelAndPaddingPerRowDp)
        .coerceAtLeast(48.dp)

      val desiredBiggerIconDp = (profile.iconSizeDp * 1.24f).coerceIn(68.dp, 92.dp)
      val expandedIconSizeDp = minOf(
        desiredBiggerIconDp,
        cellWidthDp * 0.84f,
        maxIconByHeightDp,
      ).coerceAtLeast(48.dp)

      val expandedIconRenderPx = ((expandedIconSizeDp.value * density.density) * 1.25f)
        .roundToInt()
        .coerceIn(profile.iconRenderPx, 384)

      Column(
        verticalArrangement = Arrangement.spacedBy(rowSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
      ) {
        rows.forEach { rowApps ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(
              space = horizontalSpacing,
              alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
          ) {
            rowApps.forEach { entry ->
              key(entry.componentKey, iconPackKey) {
                SquircleAppItem(
                  entry = entry,
                  iconSizeDp = expandedIconSizeDp,
                  iconRenderPx = expandedIconRenderPx,
                  labelFontSp = profile.labelFontSp,
                  labelLineHeightSp = profile.labelLineHeightSp,
                  labelMaxLines = profile.labelMaxLines,
                  showLabel = true,
                  isLaunchingTarget = entry.componentKey == launchingComponentKey,
                  isReturningTarget = entry.componentKey == returningComponentKey,
                  getCachedIcon = getCachedIcon,
                  loadIcon = loadIcon,
                  onClick = { bounds, asset -> onLaunchApp(entry, bounds, asset) },
                  onLongClick = { onLongPressApp(entry) },
                  iconPackKey = iconPackKey,
                  modifier = Modifier.width(cellWidthDp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MiniFolderIconBadge(
  entry: AppEntry,
  badgeSizeDp: Dp,
  iconRenderPx: Int,
  iconPackKey: String,
  getCachedIcon: (String, Int) -> RenderedSquircleIcon?,
  loadIcon: suspend (AppEntry, Int) -> RenderedSquircleIcon?,
) {
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

  SquircleAppBadge(
    label = entry.label,
    bitmap = iconAsset?.bitmap,
    sizeDp = badgeSizeDp,
  )
}
