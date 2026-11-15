package com.example.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.RenderedSquircleIcon
import com.example.display.DisplayAutoScaler
import com.example.display.DisplayProfile
import com.example.icons.ExternalIconPackLoader
import com.example.icons.PackIconChoiceItem
import com.example.model.AppEntry
import com.example.ui.components.DefaultAppleSquircleShape
import com.example.ui.components.LauncherAtmosphericBackground
import com.example.ui.components.MaterialSplashTransitionOverlay
import com.example.ui.components.OrbitFirstLaunchLoadingOverlay
import com.example.ui.components.SquircleAppBadge
import com.example.ui.components.SquircleAppItem
import com.example.ui.components.ThallyAceAppsPage
import com.example.ui.components.WallpaperPickerSheetContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
  viewModel: LauncherViewModel,
  homePressSignal: Int,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val hostView = LocalView.current
  val haptics = LocalHapticFeedback.current
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val hardwareInfo = remember(context) { DisplayAutoScaler.resolveHardwareMetrics(context) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { pickedUri ->
    if (pickedUri != null) {
      viewModel.saveCustomWallpaper(pickedUri)
    }
  }

  val importIconPackLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { pickedUri ->
    if (pickedUri != null) {
      viewModel.importIconPack(pickedUri)
    }
  }

  val effectiveIconPackKey = "${uiState.activeIconPackId}#${uiState.iconRenderEpoch}"

  val isTransitionActive =
    uiState.transitionState.phase == LaunchTransitionPhase.LAUNCHING ||
      uiState.transitionState.phase == LaunchTransitionPhase.LAUNCHED_WAITING_RETURN

  val workspaceScale by animateFloatAsState(
    targetValue = if (isTransitionActive) 0.96f else 1.0f,
    animationSpec = spring(
      dampingRatio = 0.86f,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "workspace_gpu_scale",
  )

  var rootWidthPx by remember { mutableFloatStateOf(0f) }
  var rootHeightPx by remember { mutableFloatStateOf(0f) }

  val readyState = uiState.loadState as? AppsLoadState.Ready
  val appPages = readyState?.pages.orEmpty()
  val thallyAcePageIndex = 1 + appPages.size
  val totalPages = 1 + appPages.size + 1

  var isThallyFolderExpanded by remember { mutableStateOf(false) }

  val pagerState = rememberPagerState(
    initialPage = 0,
    pageCount = { totalPages },
  )
  var hasPositionedInitialPage by remember { mutableStateOf(false) }
  LaunchedEffect(readyState != null, appPages.size) {
    if (readyState != null && !hasPositionedInitialPage) {
      hasPositionedInitialPage = true
      if (appPages.isNotEmpty()) {
        pagerState.scrollToPage(1)
      }
    }
  }
  LaunchedEffect(pagerState.currentPage, thallyAcePageIndex, uiState.isThallyFolderDisbanded) {
    if ((pagerState.currentPage != thallyAcePageIndex || uiState.isThallyFolderDisbanded) && isThallyFolderExpanded) {
      isThallyFolderExpanded = false
    }
  }

  val isNightlyFolderExpandedActive =
    readyState != null &&
      pagerState.currentPage == thallyAcePageIndex &&
      !uiState.isThallyFolderDisbanded &&
      isThallyFolderExpanded &&
      uiState.searchQuery.isEmpty() &&
      readyState.thallyAceApps.isNotEmpty()

  val nightlyWarningTintProgress by animateFloatAsState(
    targetValue = if (isNightlyFolderExpandedActive) 1f else 0f,
    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
    label = "nightly_warning_tint",
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF080E1F))
      .onSizeChanged { size ->
        rootWidthPx = size.width.toFloat()
        rootHeightPx = size.height.toFloat()
      },
  ) {
    // Dual-layer wallpaper: crisp on far-left Time page (0), GPU-blurred on App pages (1+)
    LauncherAtmosphericBackground(
      wallpaperState = uiState.wallpaperState,
      blurMixProvider = {
        if (readyState == null) {
          0f
        } else {
          (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
        }
      },
      scaleProvider = { 1f + ((1f - workspaceScale) * 0.5f) },
    )

    if (nightlyWarningTintProgress > 0.001f) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color(0xFFDC2626).copy(alpha = 0.20f * nightlyWarningTintProgress),
                Color(0xFFB91C1C).copy(alpha = 0.14f * nightlyWarningTintProgress),
                Color(0xFF991B1B).copy(alpha = 0.22f * nightlyWarningTintProgress),
              )
            )
          )
          .testTag("nightly_folder_warning_tint"),
      )
    }

    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding(),
    ) {
      val profile = remember(
        maxWidth,
        maxHeight,
        density.density,
        density.fontScale,
        hardwareInfo,
      ) {
        DisplayAutoScaler.computeProfile(
          availableWidthDp = maxWidth.value,
          availableHeightDp = maxHeight.value,
          density = density.density,
          systemFontScale = density.fontScale,
          hardwareInfo = hardwareInfo,
        )
      }

      LaunchedEffect(profile.itemsPerPage, profile.dockColumns, profile.iconRenderPx) {
        viewModel.updateLayoutMetrics(
          itemsPerPage = profile.itemsPerPage,
          dockColumns = profile.dockColumns,
          targetIconRenderPx = profile.iconRenderPx,
        )
      }

      val currentTimeMillis = rememberLiveClockMillis()
      val launchingKey =
        if (isTransitionActive) {
          uiState.transitionState.componentKey
        } else {
          null
        }
      val returningKey =
        if (uiState.transitionState.phase == LaunchTransitionPhase.CLOSING) {
          uiState.transitionState.componentKey
        } else {
          null
        }

      if (readyState != null) {
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(homePressSignal) {
          if (homePressSignal > 0) {
            focusManager.clearFocus()
            keyboardController?.hide()
            isThallyFolderExpanded = false
            when (val action = viewModel.handleHomeButtonPressed(pagerState.currentPage, appPages.size)) {
              is HomeNavigationAction.ScrollToPage -> {
                pagerState.animateScrollToPage(
                  page = action.targetPage,
                  animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                )
              }
              HomeNavigationAction.None -> Unit
            }
          }
        }

        BackHandler(enabled = true) {
          when {
            uiState.iconPickerTargetApp != null -> viewModel.dismissIconPicker()
            uiState.isWallpaperMenuOpen -> viewModel.dismissWallpaperMenu()
            uiState.selectedAppForActions != null -> viewModel.dismissAppActions()
            uiState.searchQuery.isNotEmpty() -> {
              viewModel.clearSearch()
              focusManager.clearFocus()
              keyboardController?.hide()
            }
            pagerState.currentPage == thallyAcePageIndex && isThallyFolderExpanded -> {
              isThallyFolderExpanded = false
            }
            pagerState.currentPage != 1 && appPages.isNotEmpty() -> {
              coroutineScope.launch {
                pagerState.animateScrollToPage(
                  page = 1,
                  animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                )
              }
            }
            pagerState.currentPage != 0 && appPages.isEmpty() -> {
              coroutineScope.launch {
                pagerState.animateScrollToPage(
                  page = 0,
                  animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                )
              }
            }
          }
        }

        val openWallpaperWithHaptics = remember(viewModel, haptics) {
          {
            focusManager.clearFocus()
            keyboardController?.hide()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.openWallpaperMenu()
          }
        }

        Column(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleX = workspaceScale
              scaleY = workspaceScale
            },
        ) {
          HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            userScrollEnabled = uiState.searchQuery.isEmpty() && uiState.transitionState.phase == LaunchTransitionPhase.IDLE,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .testTag("horizontal_apps_pager"),
          ) { pageIndex ->
            if (pageIndex == 0) {
              // Far-left empty homescreen: hold blank area to open Wallpaper menu
              FarLeftTimeOnlyPage(
                profile = profile,
                currentTimeMillis = currentTimeMillis,
                onLongPressBlankSpace = openWallpaperWithHaptics,
                modifier = Modifier
                  .fillMaxSize()
                  .testTag("far_left_time_page"),
              )
            } else {
              val isThallyAcePage = pageIndex == thallyAcePageIndex
              val appPageIndex = pageIndex - 1
              val pageApps = if (isThallyAcePage) emptyList() else appPages.getOrElse(appPageIndex) { emptyList() }
              val singleMatch =
                if (uiState.searchQuery.isNotBlank() && readyState.searchResults.size == 1) {
                  readyState.searchResults.first()
                } else {
                  null
                }

              Column(modifier = Modifier.fillMaxSize()) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = profile.horizontalPaddingDp)
                    .padding(top = 10.dp, bottom = 6.dp),
                ) {
                  AppPagesSearchBar(
                    query = uiState.searchQuery,
                    profile = profile,
                    singleMatch = singleMatch,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onClearSearch = {
                      viewModel.clearSearch()
                      focusManager.clearFocus()
                      keyboardController?.hide()
                    },
                    onSubmitSearch = {
                      if (viewModel.launchTopMatchIfSingleOrExact()) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                      }
                    },
                    onOpenCustomize = {
                      focusManager.clearFocus()
                      keyboardController?.hide()
                      viewModel.openWallpaperMenu()
                    },
                    modifier = Modifier.fillMaxWidth(),
                  )

                  if (nightlyWarningTintProgress > 0.001f) {
                    Box(
                      modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626).copy(alpha = 0.18f * nightlyWarningTintProgress))
                        .then(
                          if (isNightlyFolderExpandedActive) {
                            Modifier.pointerInput(Unit) {
                              detectTapGestures(onTap = { isThallyFolderExpanded = false })
                            }
                          } else {
                            Modifier
                          }
                        ),
                    )
                  }
                }

                if (uiState.searchQuery.isNotBlank()) {
                  SearchResultsGrid(
                    results = readyState.searchResults,
                    profile = profile,
                    iconPackKey = effectiveIconPackKey,
                    launchingComponentKey = launchingKey,
                    returningComponentKey = returningKey,
                    onLaunchApp = { entry, bounds, asset ->
                      focusManager.clearFocus()
                      keyboardController?.hide()
                      viewModel.launchApp(entry, hostView, bounds, asset)
                    },
                    onLongPressApp = viewModel::showAppActions,
                    getCachedIcon = viewModel::getCachedIcon,
                    loadIcon = viewModel::loadIcon,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxWidth(),
                  )
                } else if (isThallyAcePage) {
                  ThallyAceAppsPage(
                    apps = readyState.thallyAceApps,
                    gradientColors = readyState.thallyAceGradientColors,
                    fallbackDockGradientColors = readyState.dockGradientColors,
                    isFolderExpanded = isThallyFolderExpanded,
                    isDisbanded = uiState.isThallyFolderDisbanded,
                    onToggleFolderExpanded = { isThallyFolderExpanded = !isThallyFolderExpanded },
                    onCollapseFolder = { isThallyFolderExpanded = false },
                    onToggleDisbanded = {
                      isThallyFolderExpanded = false
                      viewModel.toggleThallyFolderDisbanded()
                    },
                    profile = profile,
                    iconPackKey = effectiveIconPackKey,
                    launchingComponentKey = launchingKey,
                    returningComponentKey = returningKey,
                    onLaunchApp = { entry, bounds, asset ->
                      focusManager.clearFocus()
                      keyboardController?.hide()
                      viewModel.launchApp(entry, hostView, bounds, asset)
                    },
                    onLongPressApp = viewModel::showAppActions,
                    onLongPressBlankSpace = openWallpaperWithHaptics,
                    getCachedIcon = viewModel::getCachedIcon,
                    loadIcon = viewModel::loadIcon,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxWidth(),
                  )
                } else if (pageApps.isEmpty()) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxWidth()
                      .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { openWallpaperWithHaptics() })
                      },
                  ) {
                    Text(
                      text = stringResource(id = R.string.no_apps_found),
                      style = MaterialTheme.typography.titleMedium,
                      color = Color.White.copy(alpha = 0.72f),
                    )
                  }
                } else {
                  HorizontalAppPage(
                    apps = pageApps,
                    profile = profile,
                    iconPackKey = effectiveIconPackKey,
                    launchingComponentKey = launchingKey,
                    returningComponentKey = returningKey,
                    onLaunchApp = { entry, bounds, asset ->
                      focusManager.clearFocus()
                      keyboardController?.hide()
                      viewModel.launchApp(entry, hostView, bounds, asset)
                    },
                    onLongPressApp = viewModel::showAppActions,
                    onLongPressBlankSpace = openWallpaperWithHaptics,
                    getCachedIcon = viewModel::getCachedIcon,
                    loadIcon = viewModel::loadIcon,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxWidth(),
                  )
                }
              }
            }
          }

          if (totalPages > 1 && uiState.searchQuery.isEmpty()) {
            ApplePageIndicator(
              pagerState = pagerState,
              pageCount = totalPages,
              onSelectPage = { target ->
                coroutineScope.launch {
                  pagerState.animateScrollToPage(
                    page = target,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                  )
                }
              },
              modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp),
            )
          } else {
            Spacer(modifier = Modifier.height(8.dp))
          }

          if (readyState.dockApps.isNotEmpty() && uiState.searchQuery.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = profile.horizontalPaddingDp)
                .padding(bottom = 12.dp),
            ) {
              AppleFrostedDock(
                dockApps = readyState.dockApps,
                dockGradientColors = readyState.dockGradientColors,
                profile = profile,
                iconPackKey = effectiveIconPackKey,
                launchingComponentKey = launchingKey,
                returningComponentKey = returningKey,
                onLaunchApp = { entry, bounds, asset ->
                  if (!isNightlyFolderExpandedActive) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    viewModel.launchApp(entry, hostView, bounds, asset)
                  }
                },
                onLongPressApp = { entry ->
                  if (!isNightlyFolderExpandedActive) {
                    viewModel.showAppActions(entry)
                  }
                },
                getCachedIcon = viewModel::getCachedIcon,
                loadIcon = viewModel::loadIcon,
                modifier = Modifier.fillMaxWidth(),
              )

              if (nightlyWarningTintProgress > 0.001f) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFDC2626).copy(alpha = 0.24f * nightlyWarningTintProgress))
                    .then(
                      if (isNightlyFolderExpandedActive) {
                        Modifier.pointerInput(Unit) {
                          detectTapGestures(onTap = { isThallyFolderExpanded = false })
                        }
                      } else {
                        Modifier
                      }
                    )
                    .testTag("dock_nightly_folder_guard"),
                ) {
                  Surface(
                    color = Color(0xCC16080C).copy(alpha = 0.80f * nightlyWarningTintProgress),
                    contentColor = Color.White.copy(alpha = 0.96f * nightlyWarningTintProgress),
                    shape = CircleShape,
                    modifier = Modifier
                      .border(
                        width = 0.8.dp,
                        color = Color(0xFFFCA5A5).copy(alpha = 0.45f * nightlyWarningTintProgress),
                        shape = CircleShape,
                      )
                      .testTag("tap_outside_folder_close_hint"),
                  ) {
                    Text(
                      text = stringResource(id = R.string.thally_ace_tap_outside_to_close),
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                  }
                }
              }
            }
          } else if (nightlyWarningTintProgress > 0.001f && uiState.searchQuery.isEmpty()) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = profile.horizontalPaddingDp)
                .padding(bottom = 14.dp)
                .then(
                  if (isNightlyFolderExpandedActive) {
                    Modifier.pointerInput(Unit) {
                      detectTapGestures(onTap = { isThallyFolderExpanded = false })
                    }
                  } else {
                    Modifier
                  }
                ),
            ) {
              Surface(
                color = Color(0xCC16080C).copy(alpha = 0.80f * nightlyWarningTintProgress),
                contentColor = Color.White.copy(alpha = 0.96f * nightlyWarningTintProgress),
                shape = CircleShape,
                modifier = Modifier
                  .border(
                    width = 0.8.dp,
                    color = Color(0xFFFCA5A5).copy(alpha = 0.45f * nightlyWarningTintProgress),
                    shape = CircleShape,
                  )
                  .testTag("tap_outside_folder_close_hint"),
              ) {
                Text(
                  text = stringResource(id = R.string.thally_ace_tap_outside_to_close),
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
              }
            }
          }
        }
      }

      if (uiState.isWallpaperMenuOpen) {
        val wallpaperSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
          onDismissRequest = viewModel::dismissWallpaperMenu,
          sheetState = wallpaperSheetState,
          containerColor = Color(0xF0141C2B),
          contentColor = Color.White,
          shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
          WallpaperPickerSheetContent(
            wallpaperState = uiState.wallpaperState,
            availableIconPacks = uiState.availableIconPacks,
            activeIconPackId = uiState.activeIconPackId,
            isImportingIconPack = uiState.isImportingIconPack,
            iconImportStatusMessage = uiState.iconImportStatusMessage,
            isIconImportError = uiState.isIconImportError,
            isThallyFolderDisbanded = uiState.isThallyFolderDisbanded,
            onSelectIconPack = viewModel::selectIconPack,
            onImportIconPack = {
              importIconPackLauncher.launch(
                arrayOf(
                  "application/zip",
                  "application/vnd.android.package-archive",
                  "application/x-zip-compressed",
                  "*/*",
                )
              )
            },
            onDeleteImportedIconPack = viewModel::deleteImportedIconPack,
            onToggleThallyFolderDisbanded = {
              isThallyFolderExpanded = false
              viewModel.toggleThallyFolderDisbanded()
            },
            onSelectPreset = { preset ->
              viewModel.selectWallpaperPreset(preset)
            },
            onChooseCustomPhoto = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            onUseAsDefaultHome = viewModel::openDefaultHomeSettings,
          )
        }
      }

      val actionApp = uiState.selectedAppForActions
      if (actionApp != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isPinnedToDock = actionApp.componentKey in uiState.pinnedDockKeys
        val isSystemPack = uiState.activeIconPackId == ExternalIconPackLoader.PACK_ID_SYSTEM_DEFAULT
        val activePackTitle = viewModel.getActivePackTitle()
        val hasManualOverride = viewModel.hasManualOverrideInActivePack(actionApp.componentKey)
        val isSupportedInPack by produceState<Boolean?>(
          initialValue = null,
          key1 = actionApp.componentKey,
          key2 = effectiveIconPackKey,
        ) {
          value = viewModel.isAppSupportedInActivePack(actionApp)
        }
        val isUnsupportedInPack = isSupportedInPack == false

        ModalBottomSheet(
          onDismissRequest = viewModel::dismissAppActions,
          sheetState = sheetState,
          containerColor = Color(0xF0182030),
          contentColor = Color.White,
          shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
          AppQuickActionsSheet(
            entry = actionApp,
            isPinnedToDock = isPinnedToDock,
            iconRenderPx = profile.iconRenderPx,
            iconPackKey = effectiveIconPackKey,
            activePackTitle = activePackTitle,
            canPickPackIcon = !isSystemPack && (isUnsupportedInPack || hasManualOverride),
            isUnsupportedInPack = isUnsupportedInPack && !isSystemPack,
            hasManualOverride = hasManualOverride && !isSystemPack,
            getCachedIcon = viewModel::getCachedIcon,
            loadIcon = viewModel::loadIcon,
            onLaunch = { viewModel.launchApp(actionApp, hostView) },
            onTogglePinDock = { viewModel.togglePinToDock(actionApp) },
            onChoosePackIcon = { viewModel.openIconPickerForApp(actionApp) },
            onResetPackIcon = { viewModel.resetManualIconOverride(actionApp) },
            onAppInfo = { viewModel.openAppInfo(actionApp) },
          )
        }
      }

      val pickerApp = uiState.iconPickerTargetApp
      val pickerLockedPackId = uiState.iconPickerPackId
      if (pickerApp != null && pickerLockedPackId != null) {
        val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hasManualOverride = uiState.manualOverridesByPack[pickerLockedPackId]?.containsKey(pickerApp.componentKey) == true
        ModalBottomSheet(
          onDismissRequest = viewModel::dismissIconPicker,
          sheetState = pickerSheetState,
          containerColor = Color(0xF5111827),
          contentColor = Color.White,
          shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
          ActivePackIconPickerSheet(
            targetApp = pickerApp,
            lockedPackId = pickerLockedPackId,
            lockedPackTitle = uiState.iconPickerPackTitle,
            choices = uiState.iconPickerChoices,
            isLoading = uiState.isLoadingIconPicker,
            hasManualOverride = hasManualOverride,
            loadChoicePreview = viewModel::loadPickerChoicePreview,
            onSelectChoice = viewModel::applyManualIconFromActivePack,
            onResetOverride = { viewModel.resetManualIconOverride(pickerApp) },
            onClose = viewModel::dismissIconPicker,
          )
        }
      }
    }

    // Immediate Orbit icon loading screen on first launch instead of black freeze
    AnimatedVisibility(
      visible = readyState == null,
      enter = fadeIn(tween(120)),
      exit = fadeOut(tween(220, easing = FastOutSlowInEasing)),
    ) {
      OrbitFirstLaunchLoadingOverlay()
    }

    // GPU-accelerated sequenced splash launch & reverse close overlay
    if (rootWidthPx > 0f && rootHeightPx > 0f) {
      MaterialSplashTransitionOverlay(
        transitionState = uiState.transitionState,
        containerWidthPx = rootWidthPx,
        containerHeightPx = rootHeightPx,
        onLaunchAnimationReady = { eventId ->
          viewModel.commitPendingLaunch(eventId = eventId, hostView = hostView)
        },
        onCloseCompleted = viewModel::clearLaunchTransition,
      )
    }
  }
}

@Composable
private fun AppPagesSearchBar(
  query: String,
  profile: DisplayProfile,
  singleMatch: AppEntry?,
  onQueryChange: (String) -> Unit,
  onClearSearch: () -> Unit,
  onSubmitSearch: () -> Unit,
  onOpenCustomize: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color.White.copy(alpha = 0.14f),
    shape = CircleShape,
    modifier = modifier
      .heightIn(min = 48.dp)
      .border(
        width = 0.75.dp,
        color = if (query.isNotEmpty()) {
          Color.White.copy(alpha = 0.48f)
        } else {
          Color.White.copy(alpha = 0.22f)
        },
        shape = CircleShape,
      )
      .testTag("app_pages_search_bar"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.80f),
        modifier = Modifier.size(20.dp),
      )

      Spacer(modifier = Modifier.width(10.dp))

      Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.weight(1f),
      ) {
        if (query.isEmpty()) {
          Text(
            text = stringResource(id = R.string.search_apps_hint),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = profile.searchFontSp),
            color = Color.White.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = profile.searchFontSp,
            color = Color.White,
          ),
          cursorBrush = SolidColor(Color.White),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
          keyboardActions = KeyboardActions(onGo = { onSubmitSearch() }),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("search_input_field"),
        )
      }

      if (singleMatch != null) {
        Surface(
          color = Color.White.copy(alpha = 0.90f),
          contentColor = Color(0xFF0A0F1A),
          shape = CircleShape,
          modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onSubmitSearch)
            .testTag("instant_launch_chip"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Bolt,
              contentDescription = "Open ${singleMatch.label}",
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = "GO",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }

      if (query.isNotEmpty()) {
        IconButton(
          onClick = onClearSearch,
          modifier = Modifier.testTag("clear_search_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(id = R.string.clear_search),
            tint = Color.White.copy(alpha = 0.85f),
          )
        }
      } else {
        IconButton(
          onClick = onOpenCustomize,
          modifier = Modifier.testTag("customize_launcher_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Palette,
            contentDescription = stringResource(id = R.string.customize_launcher),
            tint = Color.White.copy(alpha = 0.85f),
          )
        }
      }
    }
  }
}

@Composable
private fun SearchResultsGrid(
  results: List<AppEntry>,
  profile: DisplayProfile,
  iconPackKey: String,
  launchingComponentKey: String?,
  returningComponentKey: String?,
  onLaunchApp: (AppEntry, Rect?, RenderedSquircleIcon?) -> Unit,
  onLongPressApp: (AppEntry) -> Unit,
  getCachedIcon: (String, Int) -> RenderedSquircleIcon?,
  loadIcon: suspend (AppEntry, Int) -> RenderedSquircleIcon?,
  modifier: Modifier = Modifier,
) {
  if (results.isEmpty()) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = modifier,
    ) {
      Text(
        text = stringResource(id = R.string.no_matching_apps),
        style = MaterialTheme.typography.titleMedium,
        color = Color.White.copy(alpha = 0.75f),
      )
    }
  } else {
    LazyVerticalGrid(
      columns = GridCells.Fixed(profile.columns),
      contentPadding = PaddingValues(
        horizontal = profile.horizontalPaddingDp,
        vertical = 10.dp,
      ),
      horizontalArrangement = Arrangement.spacedBy(profile.gridHorizontalSpacingDp),
      verticalArrangement = Arrangement.spacedBy(profile.gridVerticalSpacingDp),
      modifier = modifier.testTag("search_results_grid"),
    ) {
      items(
        items = results,
        key = { "${it.componentKey}_$iconPackKey" },
      ) { entry ->
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
        )
      }
    }
  }
}

@Composable
private fun FarLeftTimeOnlyPage(
  profile: DisplayProfile,
  currentTimeMillis: Long,
  onLongPressBlankSpace: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }

  val timeText = remember(currentTimeMillis, is24Hour) {
    val pattern = if (is24Hour) "HH:mm" else "h:mm"
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(currentTimeMillis))
  }
  val amPmText = remember(currentTimeMillis, is24Hour) {
    if (is24Hour) "" else SimpleDateFormat("a", Locale.getDefault()).format(Date(currentTimeMillis))
  }
  val fullDateText = remember(currentTimeMillis) {
    SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(currentTimeMillis))
  }

  val timeSp = (profile.clockFontSp.value * 1.75f).coerceIn(48f, 84f)
  val dateSp = (profile.dateFontSp.value * 1.18f).coerceIn(15f, 21f)
  val textShadow = remember {
    Shadow(
      color = Color.Black.copy(alpha = 0.55f),
      offset = Offset(0f, 2f),
      blurRadius = 8f,
    )
  }

  // Empty home page: hold anywhere on the blank wallpaper to bring up the Wallpaper menu
  Box(
    contentAlignment = Alignment.TopStart,
    modifier = modifier
      .pointerInput(Unit) {
        detectTapGestures(onLongPress = { onLongPressBlankSpace() })
      }
      .padding(horizontal = profile.horizontalPaddingDp)
      .padding(top = 20.dp),
  ) {
    Column(horizontalAlignment = Alignment.Start) {
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = timeText,
          style = MaterialTheme.typography.displayLarge.copy(
            fontSize = TextUnit(timeSp, TextUnitType.Sp),
            lineHeight = TextUnit(timeSp * 1.02f, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            shadow = textShadow,
          ),
          color = Color.White,
          modifier = Modifier.testTag("far_left_clock"),
        )
        if (amPmText.isNotEmpty()) {
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = amPmText,
            style = MaterialTheme.typography.titleMedium.copy(
              fontSize = TextUnit(dateSp, TextUnitType.Sp),
              fontWeight = FontWeight.SemiBold,
              shadow = textShadow,
            ),
            color = Color.White.copy(alpha = 0.80f),
            modifier = Modifier.padding(bottom = 10.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = fullDateText,
        style = MaterialTheme.typography.titleMedium.copy(
          fontSize = TextUnit(dateSp, TextUnitType.Sp),
          fontWeight = FontWeight.Medium,
          shadow = textShadow,
        ),
        color = Color.White.copy(alpha = 0.88f),
      )
    }
  }
}

@Composable
private fun HorizontalAppPage(
  apps: List<AppEntry>,
  profile: DisplayProfile,
  iconPackKey: String,
  launchingComponentKey: String?,
  returningComponentKey: String?,
  onLaunchApp: (AppEntry, Rect?, RenderedSquircleIcon?) -> Unit,
  onLongPressApp: (AppEntry) -> Unit,
  onLongPressBlankSpace: () -> Unit,
  getCachedIcon: (String, Int) -> RenderedSquircleIcon?,
  loadIcon: suspend (AppEntry, Int) -> RenderedSquircleIcon?,
  modifier: Modifier = Modifier,
) {
  val rows = remember(apps, profile.columns) {
    apps.chunked(profile.columns)
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(
      space = profile.gridVerticalSpacingDp,
      alignment = Alignment.CenterVertically,
    ),
    modifier = modifier
      .fillMaxSize()
      .pointerInput(Unit) {
        detectTapGestures(onLongPress = { onLongPressBlankSpace() })
      }
      .padding(horizontal = profile.horizontalPaddingDp),
  ) {
    for (rowApps in rows) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(profile.gridHorizontalSpacingDp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
      ) {
        for (colIndex in 0 until profile.columns) {
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
private fun ApplePageIndicator(
  pagerState: PagerState,
  pageCount: Int,
  onSelectPage: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color.White.copy(alpha = 0.14f),
    shape = CircleShape,
    modifier = modifier.testTag("page_indicator"),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
      repeat(pageCount) { index ->
        val selected = pagerState.currentPage == index
        val isDedicatedLastPage = index == pageCount - 1
        val dotColor = when {
          isDedicatedLastPage && selected -> Color(0xFFFBBF24)
          isDedicatedLastPage -> Color(0xFFFBBF24).copy(alpha = 0.55f)
          selected -> Color.White
          else -> Color.White.copy(alpha = 0.38f)
        }
        Box(
          modifier = Modifier
            .size(if (selected) 8.dp else 6.dp)
            .clip(CircleShape)
            .background(dotColor)
            .clickable { onSelectPage(index) },
        )
      }
    }
  }
}

@Composable
private fun AppleFrostedDock(
  dockApps: List<AppEntry>,
  dockGradientColors: List<Color>,
  profile: DisplayProfile,
  iconPackKey: String,
  launchingComponentKey: String?,
  returningComponentKey: String?,
  onLaunchApp: (AppEntry, Rect?, RenderedSquircleIcon?) -> Unit,
  onLongPressApp: (AppEntry) -> Unit,
  getCachedIcon: (String, Int) -> RenderedSquircleIcon?,
  loadIcon: suspend (AppEntry, Int) -> RenderedSquircleIcon?,
  modifier: Modifier = Modifier,
) {
  val resolvedGradientColors = remember(dockGradientColors) {
    when {
      dockGradientColors.size >= 2 -> dockGradientColors.map { it.copy(alpha = 0.34f) }
      dockGradientColors.size == 1 -> {
        val c = dockGradientColors.first()
        listOf(c.copy(alpha = 0.36f), lerpColor(c, Color.White, 0.25f).copy(alpha = 0.28f))
      }
      else -> listOf(
        Color.White.copy(alpha = 0.20f),
        Color.White.copy(alpha = 0.14f),
      )
    }
  }

  val borderColors = remember(dockGradientColors) {
    when {
      dockGradientColors.size >= 2 -> dockGradientColors.map { lerpColor(it, Color.White, 0.45f).copy(alpha = 0.46f) }
      dockGradientColors.size == 1 -> {
        val c = lerpColor(dockGradientColors.first(), Color.White, 0.45f).copy(alpha = 0.46f)
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
        Color(0x660B101B),
      )
    )
  }
  val dockBorderBrush = remember(borderColors) {
    Brush.horizontalGradient(colors = borderColors)
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(30.dp))
      .background(Color(0x880C121E), RoundedCornerShape(30.dp))
      .background(dockTintBrush, RoundedCornerShape(30.dp))
      .background(dockGlassSheen, RoundedCornerShape(30.dp))
      .border(0.85.dp, dockBorderBrush, RoundedCornerShape(30.dp))
      .testTag("apple_frosted_dock"),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
      dockApps.forEach { entry ->
        key(entry.componentKey, iconPackKey) {
          SquircleAppItem(
            entry = entry,
            iconSizeDp = profile.dockIconSizeDp,
            iconRenderPx = profile.iconRenderPx,
            labelFontSp = profile.labelFontSp,
            labelLineHeightSp = profile.labelLineHeightSp,
            labelMaxLines = 1,
            showLabel = false,
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
      }
    }
  }
}

@Composable
private fun AppQuickActionsSheet(
  entry: AppEntry,
  isPinnedToDock: Boolean,
  iconRenderPx: Int,
  iconPackKey: String,
  activePackTitle: String,
  canPickPackIcon: Boolean,
  isUnsupportedInPack: Boolean,
  hasManualOverride: Boolean,
  getCachedIcon: (String, Int) -> RenderedSquircleIcon?,
  loadIcon: suspend (AppEntry, Int) -> RenderedSquircleIcon?,
  onLaunch: () -> Unit,
  onTogglePinDock: () -> Unit,
  onChoosePackIcon: () -> Unit,
  onResetPackIcon: () -> Unit,
  onAppInfo: () -> Unit,
) {
  var asset by remember(entry.componentKey, iconRenderPx, iconPackKey) {
    mutableStateOf(getCachedIcon(entry.componentKey, iconRenderPx))
  }
  LaunchedEffect(entry.componentKey, iconRenderPx, iconPackKey) {
    val cached = getCachedIcon(entry.componentKey, iconRenderPx)
    if (cached != null) {
      asset = cached
    } else {
      asset = loadIcon(entry, iconRenderPx)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 24.dp)
      .padding(bottom = 24.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      SquircleAppBadge(
        label = entry.label,
        bitmap = asset?.bitmap,
        sizeDp = 60.dp,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = entry.label,
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = entry.packageName,
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.6f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        if (isUnsupportedInPack && !hasManualOverride) {
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            color = Color(0xFFF59E0B).copy(alpha = 0.18f),
            contentColor = Color(0xFFFCD34D),
            shape = CircleShape,
            modifier = Modifier
              .border(0.75.dp, Color(0xFFF59E0B).copy(alpha = 0.45f), CircleShape)
              .testTag("unsupported_icon_badge"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = stringResource(id = R.string.unsupported_icon_badge),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              )
            }
          }
        } else if (hasManualOverride) {
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            color = Color(0xFF22D3EE).copy(alpha = 0.18f),
            contentColor = Color(0xFF67E8F9),
            shape = CircleShape,
            modifier = Modifier
              .border(0.75.dp, Color(0xFF22D3EE).copy(alpha = 0.45f), CircleShape)
              .testTag("custom_pack_icon_badge"),
          ) {
            Text(
              text = stringResource(id = R.string.custom_pack_icon_badge, activePackTitle),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(Color.White.copy(alpha = 0.08f)),
    ) {
      QuickActionRow(
        icon = Icons.AutoMirrored.Rounded.OpenInNew,
        label = "Open ${entry.label}",
        tint = Color.White,
        onClick = onLaunch,
        testTag = "action_open_app",
      )

      if (canPickPackIcon) {
        QuickActionRow(
          icon = Icons.Rounded.ImageSearch,
          label = stringResource(id = R.string.choose_icon_from_active_pack, activePackTitle),
          tint = Color(0xFF67E8F9),
          onClick = onChoosePackIcon,
          testTag = "action_choose_pack_icon",
        )
      }

      if (hasManualOverride) {
        QuickActionRow(
          icon = Icons.Rounded.RestartAlt,
          label = stringResource(id = R.string.reset_pack_icon_default),
          tint = Color(0xFFFCD34D),
          onClick = onResetPackIcon,
          testTag = "action_reset_pack_icon",
        )
      }

      QuickActionRow(
        icon = Icons.Rounded.PushPin,
        label = if (isPinnedToDock) {
          stringResource(id = R.string.remove_from_dock)
        } else {
          stringResource(id = R.string.pin_to_dock)
        },
        tint = if (isPinnedToDock) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.92f),
        onClick = onTogglePinDock,
        testTag = "action_toggle_pin_dock",
      )

      QuickActionRow(
        icon = Icons.Rounded.Info,
        label = stringResource(id = R.string.app_info),
        tint = Color.White.copy(alpha = 0.9f),
        onClick = onAppInfo,
        testTag = "action_app_info",
      )
    }
  }
}

@Composable
private fun ActivePackIconPickerSheet(
  targetApp: AppEntry,
  lockedPackId: String,
  lockedPackTitle: String,
  choices: List<PackIconChoiceItem>,
  isLoading: Boolean,
  hasManualOverride: Boolean,
  loadChoicePreview: suspend (String, String, Int) -> ImageBitmap?,
  onSelectChoice: (String) -> Unit,
  onResetOverride: () -> Unit,
  onClose: () -> Unit,
) {
  var filterQuery by remember(lockedPackId, targetApp.componentKey) { mutableStateOf("") }
  val filteredChoices = remember(choices, filterQuery) {
    val q = filterQuery.trim().lowercase(Locale.ROOT)
    if (q.isEmpty()) choices
    else choices.filter {
      it.label.lowercase(Locale.ROOT).contains(q) ||
        it.choiceKey.lowercase(Locale.ROOT).contains(q)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 20.dp)
      .padding(bottom = 20.dp)
      .testTag("active_pack_icon_picker_sheet"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(id = R.string.active_pack_picker_title, lockedPackTitle),
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = targetApp.label,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.72f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (hasManualOverride) {
        Surface(
          color = Color(0xFFF59E0B).copy(alpha = 0.16f),
          contentColor = Color(0xFFFCD34D),
          shape = CircleShape,
          modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onResetOverride)
            .testTag("picker_reset_override_button"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.RestartAlt,
              contentDescription = stringResource(id = R.string.reset_pack_icon_default),
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Reset",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }

      IconButton(
        onClick = onClose,
        modifier = Modifier.testTag("picker_close_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = stringResource(id = R.string.thally_ace_folder_tap_collapse),
          tint = Color.White.copy(alpha = 0.85f),
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Surface(
      color = Color(0xFF22D3EE).copy(alpha = 0.12f),
      contentColor = Color(0xFF67E8F9),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(0.75.dp, Color(0xFF22D3EE).copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Lock,
          contentDescription = null,
          modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(id = R.string.active_pack_picker_subtitle),
          style = MaterialTheme.typography.labelSmall,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
      color = Color.White.copy(alpha = 0.10f),
      shape = CircleShape,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .border(0.75.dp, Color.White.copy(alpha = 0.18f), CircleShape),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Search,
          contentDescription = null,
          tint = Color.White.copy(alpha = 0.7f),
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          contentAlignment = Alignment.CenterStart,
          modifier = Modifier.weight(1f),
        ) {
          if (filterQuery.isEmpty()) {
            Text(
              text = stringResource(id = R.string.active_pack_picker_search_hint, lockedPackTitle),
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.55f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          BasicTextField(
            value = filterQuery,
            onValueChange = { filterQuery = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            cursorBrush = SolidColor(Color(0xFF22D3EE)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("active_pack_picker_search"),
          )
        }
        if (filterQuery.isNotEmpty()) {
          IconButton(
            onClick = { filterQuery = "" },
            modifier = Modifier.size(32.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = stringResource(id = R.string.clear_search),
              tint = Color.White.copy(alpha = 0.8f),
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 240.dp, max = 380.dp),
    ) {
      when {
        isLoading -> {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
          ) {
            CircularProgressIndicator(color = Color(0xFF22D3EE))
          }
        }

        filteredChoices.isEmpty() -> {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
          ) {
            Text(
              text = stringResource(id = R.string.active_pack_picker_empty),
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.65f),
            )
          }
        }

        else -> {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 84.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier
              .fillMaxSize()
              .testTag("active_pack_picker_grid"),
          ) {
            items(
              items = filteredChoices,
              key = { "${lockedPackId}_${it.choiceKey}" },
            ) { item ->
              val previewBitmap by produceState<ImageBitmap?>(
                initialValue = null,
                key1 = lockedPackId,
                key2 = item.choiceKey,
              ) {
                value = loadChoicePreview(lockedPackId, item.choiceKey, 128)
              }

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { onSelectChoice(item.choiceKey) }
                  .padding(horizontal = 6.dp, vertical = 8.dp)
                  .testTag("pack_icon_choice_${item.choiceKey}"),
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(54.dp)
                    .clip(DefaultAppleSquircleShape)
                    .background(Color.White.copy(alpha = 0.08f), DefaultAppleSquircleShape),
                ) {
                  val bmp = previewBitmap
                  if (bmp != null) {
                    Image(
                      bitmap = bmp,
                      contentDescription = item.label,
                      contentScale = ContentScale.Crop,
                      filterQuality = FilterQuality.High,
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = item.label,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.88f),
                  textAlign = TextAlign.Center,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
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
private fun QuickActionRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  tint: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 52.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .testTag(testTag),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = tint,
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.width(14.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.titleMedium,
      color = tint,
    )
  }
}

@Composable
private fun rememberLiveClockMillis(): Long {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var timeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

  DisposableEffect(context, lifecycleOwner) {
    val timeReceiver = object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        timeMillis = System.currentTimeMillis()
      }
    }
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_TIME_TICK)
      addAction(Intent.ACTION_TIME_CHANGED)
      addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }
    context.registerReceiver(timeReceiver, filter)

    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        timeMillis = System.currentTimeMillis()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
      runCatching { context.unregisterReceiver(timeReceiver) }
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  return timeMillis
}
