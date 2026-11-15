package com.example.ui

import android.app.Application
import android.net.Uri
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AppRepository
import com.example.data.RenderedSquircleIcon
import com.example.data.SquircleIconRenderer
import com.example.data.WallpaperPreset
import com.example.data.WallpaperRepository
import com.example.data.WallpaperState
import com.example.icons.ExternalIconPackLoader
import com.example.icons.IconPackOption
import com.example.icons.ImportedIconPackStore
import com.example.icons.PackIconChoiceItem
import com.example.model.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

sealed interface AppsLoadState {
  data object Loading : AppsLoadState

  data class Ready(
    val allApps: List<AppEntry>,
    val pages: List<List<AppEntry>>,
    val thallyAceApps: List<AppEntry>,
    val thallyAceGradientColors: List<Color>,
    val searchResults: List<AppEntry>,
    val dockApps: List<AppEntry>,
    val dockGradientColors: List<Color>,
  ) : AppsLoadState
}

enum class LaunchTransitionPhase {
  IDLE,
  LAUNCHING,
  LAUNCHED_WAITING_RETURN,
  CLOSING,
}

data class LaunchTransitionState(
  val phase: LaunchTransitionPhase = LaunchTransitionPhase.IDLE,
  val pendingEntry: AppEntry? = null,
  val componentKey: String? = null,
  val label: String = "",
  val originRect: Rect? = null,
  val iconAsset: RenderedSquircleIcon? = null,
  val splashColor: Color = Color(0xFF22D3EE),
  val secondarySplashColor: Color = Color(0xFF0F172A),
  val eventId: Int = 0,
)

data class LauncherUiState(
  val loadState: AppsLoadState = AppsLoadState.Loading,
  val wallpaperState: WallpaperState = WallpaperState(),
  val isWallpaperMenuOpen: Boolean = false,
  val searchQuery: String = "",
  val itemsPerPage: Int = 16,
  val dockSlotCount: Int = 4,
  val pinnedDockKeys: Set<String> = emptySet(),
  val selectedAppForActions: AppEntry? = null,
  val transitionState: LaunchTransitionState = LaunchTransitionState(),
  val activeIconPackId: String = ExternalIconPackLoader.PACK_ID_ORBIT_FUSION,
  val availableIconPacks: List<IconPackOption> = listOf(
    IconPackOption(
      id = ExternalIconPackLoader.PACK_ID_ORBIT_FUSION,
      title = "Fusion",
      subtitle = "Apple × Android gradient + smart matching",
    ),
    IconPackOption(
      id = ExternalIconPackLoader.PACK_ID_ORBIT_OREO,
      title = "Oreo 8.1",
      subtitle = "Android 8.1 high-contrast plates + 45° shadow",
    ),
    IconPackOption(
      id = ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN,
      title = "Obsidian",
      subtitle = "Obsidian dark medallions + brand neon rims",
    ),
    IconPackOption(
      id = ExternalIconPackLoader.PACK_ID_IPHONE,
      title = "iPhone",
      subtitle = "Clean iOS glass plates & vertical gradients",
    ),
    IconPackOption(
      id = ExternalIconPackLoader.PACK_ID_SYSTEM_DEFAULT,
      title = "System",
      subtitle = "Original app icons + Obsidian suite",
    ),
  ),
  val isThallyFolderDisbanded: Boolean = false,
  val iconRenderEpoch: Int = 0,
  val manualOverridesByPack: Map<String, Map<String, String>> = emptyMap(),
  val isImportingIconPack: Boolean = false,
  val iconImportStatusMessage: String? = null,
  val isIconImportError: Boolean = false,
  val iconPickerTargetApp: AppEntry? = null,
  val iconPickerPackId: String? = null,
  val iconPickerPackTitle: String = "",
  val iconPickerChoices: List<PackIconChoiceItem> = emptyList(),
  val isLoadingIconPicker: Boolean = false,
)

sealed interface HomeNavigationAction {
  data object None : HomeNavigationAction
  data class ScrollToPage(val targetPage: Int) : HomeNavigationAction
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = AppRepository(application)
  private val wallpaperRepository = WallpaperRepository(application)
  private val iconRenderer = SquircleIconRenderer(application)

  private val _uiState = MutableStateFlow(LauncherUiState())
  val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

  private var cachedAllApps: List<AppEntry> = emptyList()
  private var cachedPinnedKeys: List<String> = emptyList()
  private var lastPrewarmedPx: Int = 0
  private var lastReturnedFromAppAtMs: Long = 0L
  private var hasPendingRecomputeAfterTransition: Boolean = false

  init {
    loadWallpaper()
    loadLauncherPreferences()
    refreshApps()
    observePackageBroadcasts()
  }

  private fun loadWallpaper() {
    viewModelScope.launch {
      val state = wallpaperRepository.loadWallpaperState()
      _uiState.update { it.copy(wallpaperState = state) }
    }
  }

  private fun loadLauncherPreferences() {
    viewModelScope.launch {
      val savedPackId = repository.loadActiveIconPackId()
      val disbanded = repository.loadThallyFolderDisbanded()
      val overrides = repository.loadManualIconOverrides()
      val options = withContext(Dispatchers.IO) {
        ExternalIconPackLoader.buildAvailableOptions(getApplication())
      }
      val resolvedPackId = if (options.any { it.id == savedPackId }) {
        savedPackId
      } else {
        ExternalIconPackLoader.PACK_ID_ORBIT_FUSION
      }
      _uiState.update {
        it.copy(
          activeIconPackId = resolvedPackId,
          availableIconPacks = options,
          isThallyFolderDisbanded = disbanded,
          manualOverridesByPack = overrides,
          iconRenderEpoch = it.iconRenderEpoch + 1,
        )
      }
      if (lastPrewarmedPx > 0 && cachedAllApps.isNotEmpty()) {
        prewarmIcons(cachedAllApps, lastPrewarmedPx, resolvedPackId, overrides)
      } else if (cachedAllApps.isNotEmpty()) {
        recomputeState()
      }
    }
  }

  fun refreshIconPacks() {
    viewModelScope.launch {
      val options = withContext(Dispatchers.IO) {
        ExternalIconPackLoader.buildAvailableOptions(getApplication())
      }
      _uiState.update { state ->
        val validActive = if (options.any { it.id == state.activeIconPackId }) {
          state.activeIconPackId
        } else {
          ExternalIconPackLoader.PACK_ID_ORBIT_FUSION
        }
        state.copy(
          availableIconPacks = options,
          activeIconPackId = validActive,
        )
      }
    }
  }

  private fun observePackageBroadcasts() {
    viewModelScope.launch {
      repository.observePackageChanges().collect { pkg ->
        if (!pkg.isNullOrBlank()) {
          iconRenderer.evictPackage(pkg)
          ExternalIconPackLoader.clearParsedCache(pkg)
        }
        refreshIconPacks()
        refreshApps()
      }
    }
  }

  fun refreshApps() {
    viewModelScope.launch {
      cachedPinnedKeys = repository.loadPinnedDockKeys()
      val apps = repository.loadInstalledApps()
      cachedAllApps = apps
      val phase = _uiState.value.transitionState.phase
      if (phase == LaunchTransitionPhase.IDLE) {
        recomputeState()
      } else {
        hasPendingRecomputeAfterTransition = true
      }
      if (lastPrewarmedPx > 0) {
        prewarmIcons(apps, lastPrewarmedPx, _uiState.value.activeIconPackId)
      }
    }
  }

  fun updateLayoutMetrics(itemsPerPage: Int, dockColumns: Int, targetIconRenderPx: Int) {
    val current = _uiState.value
    val layoutChanged = current.itemsPerPage != itemsPerPage || current.dockSlotCount != dockColumns
    if (layoutChanged) {
      _uiState.update {
        it.copy(
          itemsPerPage = itemsPerPage.coerceAtLeast(1),
          dockSlotCount = dockColumns.coerceAtLeast(1),
        )
      }
      if (cachedAllApps.isNotEmpty()) {
        recomputeState()
      }
    }
    if (targetIconRenderPx > 0 && targetIconRenderPx != lastPrewarmedPx) {
      lastPrewarmedPx = targetIconRenderPx
      if (cachedAllApps.isNotEmpty()) {
        prewarmIcons(cachedAllApps, targetIconRenderPx, _uiState.value.activeIconPackId)
      }
    }
  }

  fun openWallpaperMenu() {
    if (_uiState.value.transitionState.phase != LaunchTransitionPhase.IDLE) return
    refreshIconPacks()
    _uiState.update {
      it.copy(
        isWallpaperMenuOpen = true,
        selectedAppForActions = null,
        iconPickerTargetApp = null,
        iconPickerPackId = null,
        iconImportStatusMessage = null,
      )
    }
  }

  fun selectIconPack(packId: String) {
    if (_uiState.value.activeIconPackId == packId) return
    _uiState.update {
      it.copy(
        activeIconPackId = packId,
        iconPickerTargetApp = null,
        iconPickerPackId = null,
        iconPickerChoices = emptyList(),
      )
    }
    viewModelScope.launch {
      repository.saveActiveIconPackId(packId)
    }
    val overrides = _uiState.value.manualOverridesByPack
    if (lastPrewarmedPx > 0 && cachedAllApps.isNotEmpty()) {
      prewarmIcons(cachedAllApps, lastPrewarmedPx, packId, overrides)
    } else if (cachedAllApps.isNotEmpty()) {
      recomputeState()
    }
  }

  fun importIconPack(uri: Uri) {
    if (_uiState.value.isImportingIconPack) return
    _uiState.update {
      it.copy(
        isImportingIconPack = true,
        iconImportStatusMessage = null,
        isIconImportError = false,
      )
    }
    viewModelScope.launch {
      val app = getApplication<Application>()
      val result = ImportedIconPackStore.importFromUri(app, uri)
      result.fold(
        onSuccess = { imported ->
          val options = withContext(Dispatchers.IO) {
            ExternalIconPackLoader.buildAvailableOptions(app)
          }
          repository.saveActiveIconPackId(imported.id)
          val msg = app.getString(R.string.imported_icon_pack_success, imported.title, imported.iconCount)
          _uiState.update { state ->
            state.copy(
              isImportingIconPack = false,
              iconImportStatusMessage = msg,
              isIconImportError = false,
              availableIconPacks = options,
              activeIconPackId = imported.id,
              iconRenderEpoch = state.iconRenderEpoch + 1,
            )
          }
          if (lastPrewarmedPx > 0 && cachedAllApps.isNotEmpty()) {
            prewarmIcons(cachedAllApps, lastPrewarmedPx, imported.id, _uiState.value.manualOverridesByPack)
          } else if (cachedAllApps.isNotEmpty()) {
            recomputeState()
          }
        },
        onFailure = { error ->
          val msg = error.message?.takeIf { it.isNotBlank() }
            ?: app.getString(R.string.imported_icon_pack_error)
          _uiState.update {
            it.copy(
              isImportingIconPack = false,
              iconImportStatusMessage = msg,
              isIconImportError = true,
            )
          }
        },
      )
    }
  }

  fun deleteImportedIconPack(packId: String) {
    if (!ImportedIconPackStore.isImportedPackId(packId)) return
    viewModelScope.launch {
      val app = getApplication<Application>()
      withContext(Dispatchers.IO) {
        ImportedIconPackStore.deleteImportedPack(app, packId)
      }
      iconRenderer.evictPack(packId)
      val updatedOverrides = repository.clearOverridesForPack(packId)
      val options = withContext(Dispatchers.IO) {
        ExternalIconPackLoader.buildAvailableOptions(app)
      }
      val wasActive = _uiState.value.activeIconPackId == packId
      val nextPackId = if (wasActive) ExternalIconPackLoader.PACK_ID_ORBIT_FUSION else _uiState.value.activeIconPackId
      if (wasActive) {
        repository.saveActiveIconPackId(nextPackId)
      }
      _uiState.update { state ->
        state.copy(
          availableIconPacks = options,
          activeIconPackId = nextPackId,
          manualOverridesByPack = updatedOverrides,
          iconRenderEpoch = state.iconRenderEpoch + 1,
          iconPickerTargetApp = if (state.iconPickerPackId == packId) null else state.iconPickerTargetApp,
          iconPickerPackId = if (state.iconPickerPackId == packId) null else state.iconPickerPackId,
        )
      }
      if (lastPrewarmedPx > 0 && cachedAllApps.isNotEmpty()) {
        prewarmIcons(cachedAllApps, lastPrewarmedPx, nextPackId, updatedOverrides)
      } else if (cachedAllApps.isNotEmpty()) {
        recomputeState()
      }
    }
  }

  fun toggleThallyFolderDisbanded() {
    val next = !_uiState.value.isThallyFolderDisbanded
    _uiState.update { it.copy(isThallyFolderDisbanded = next) }
    viewModelScope.launch {
      repository.saveThallyFolderDisbanded(next)
    }
  }

  fun dismissWallpaperMenu() {
    _uiState.update {
      it.copy(
        isWallpaperMenuOpen = false,
        iconImportStatusMessage = null,
      )
    }
  }

  fun selectWallpaperPreset(preset: WallpaperPreset) {
    _uiState.update { state ->
      state.copy(
        wallpaperState = state.wallpaperState.copy(activePreset = preset),
      )
    }
    viewModelScope.launch {
      wallpaperRepository.selectPreset(preset)
    }
  }

  fun saveCustomWallpaper(uri: Uri) {
    viewModelScope.launch {
      val bitmap = wallpaperRepository.saveCustomPhotoFromUri(uri)
      if (bitmap != null) {
        _uiState.update { state ->
          state.copy(
            wallpaperState = state.wallpaperState.copy(
              activePreset = WallpaperPreset.CUSTOM_PHOTO,
              customBitmap = bitmap,
              customVersion = state.wallpaperState.customVersion + 1,
            ),
            isWallpaperMenuOpen = false,
          )
        }
      }
    }
  }

  fun onSearchQueryChanged(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    recomputeState()
  }

  fun clearSearch() {
    if (_uiState.value.searchQuery.isNotEmpty()) {
      _uiState.update { it.copy(searchQuery = "") }
      recomputeState()
    }
  }

  fun showAppActions(entry: AppEntry) {
    if (_uiState.value.transitionState.phase != LaunchTransitionPhase.IDLE) return
    _uiState.update {
      it.copy(
        selectedAppForActions = entry,
        isWallpaperMenuOpen = false,
        iconPickerTargetApp = null,
        iconPickerPackId = null,
      )
    }
  }

  fun dismissAppActions() {
    _uiState.update { it.copy(selectedAppForActions = null) }
  }

  fun getActivePackTitle(): String {
    val state = _uiState.value
    return state.availableIconPacks.firstOrNull { it.id == state.activeIconPackId }?.title
      ?: "Fusion"
  }

  fun hasManualOverrideInActivePack(componentKey: String): Boolean {
    val state = _uiState.value
    return state.manualOverridesByPack[state.activeIconPackId]?.containsKey(componentKey) == true
  }

  suspend fun isAppSupportedInActivePack(entry: AppEntry): Boolean {
    val packId = _uiState.value.activeIconPackId
    return withContext(Dispatchers.IO) {
      ExternalIconPackLoader.isAppSupportedInPack(
        context = getApplication(),
        packId = packId,
        packageName = entry.packageName,
        activityName = entry.activityName,
        label = entry.label,
        androidCategory = entry.androidCategory,
        thallyAcePackages = THALLY_ACE_PACKAGES,
      )
    }
  }

  /**
   * Opens the manual icon selector strictly locked to the currently active icon pack.
   * Cross-applying icons out of other packs is strictly disabled.
   */
  fun openIconPickerForApp(entry: AppEntry) {
    val state = _uiState.value
    val lockedPackId = state.activeIconPackId
    if (lockedPackId == ExternalIconPackLoader.PACK_ID_SYSTEM_DEFAULT) return
    val lockedPackTitle = state.availableIconPacks.firstOrNull { it.id == lockedPackId }?.title
      ?: "Active Pack"

    _uiState.update {
      it.copy(
        selectedAppForActions = null,
        iconPickerTargetApp = entry,
        iconPickerPackId = lockedPackId,
        iconPickerPackTitle = lockedPackTitle,
        iconPickerChoices = emptyList(),
        isLoadingIconPicker = true,
      )
    }

    viewModelScope.launch {
      val choices = withContext(Dispatchers.IO) {
        ExternalIconPackLoader.listChoicesForActivePack(getApplication(), lockedPackId)
      }
      _uiState.update { current ->
        if (current.iconPickerTargetApp?.componentKey == entry.componentKey &&
          current.iconPickerPackId == lockedPackId
        ) {
          current.copy(
            iconPickerChoices = choices,
            isLoadingIconPicker = false,
          )
        } else {
          current
        }
      }
    }
  }

  fun dismissIconPicker() {
    _uiState.update {
      it.copy(
        iconPickerTargetApp = null,
        iconPickerPackId = null,
        iconPickerPackTitle = "",
        iconPickerChoices = emptyList(),
        isLoadingIconPicker = false,
      )
    }
  }

  fun applyManualIconFromActivePack(choiceKey: String) {
    val state = _uiState.value
    val targetApp = state.iconPickerTargetApp ?: return
    val lockedPackId = state.iconPickerPackId ?: return
    // Enforce strict active-pack invariant: never allow applying into a mismatched pack
    if (lockedPackId != state.activeIconPackId || lockedPackId == ExternalIconPackLoader.PACK_ID_SYSTEM_DEFAULT) {
      dismissIconPicker()
      return
    }
    if (state.iconPickerChoices.none { it.choiceKey == choiceKey }) {
      return
    }

    viewModelScope.launch {
      val updatedOverrides = repository.setManualIconOverride(
        packId = lockedPackId,
        componentKey = targetApp.componentKey,
        choiceKey = choiceKey,
      )
      iconRenderer.evictComponent(targetApp.componentKey, lockedPackId)
      val targetPx = lastPrewarmedPx.takeIf { it > 0 } ?: 160
      iconRenderer.loadOrRender(
        packageName = targetApp.packageName,
        activityName = targetApp.activityName,
        componentKey = targetApp.componentKey,
        targetSizePx = targetPx,
        label = targetApp.label,
        androidCategory = targetApp.androidCategory,
        packId = lockedPackId,
        manualChoiceKey = choiceKey,
      )
      _uiState.update { current ->
        current.copy(
          manualOverridesByPack = updatedOverrides,
          iconRenderEpoch = current.iconRenderEpoch + 1,
          iconPickerTargetApp = null,
          iconPickerPackId = null,
          iconPickerPackTitle = "",
          iconPickerChoices = emptyList(),
          isLoadingIconPicker = false,
        )
      }
      if (cachedAllApps.isNotEmpty()) {
        recomputeState()
      }
    }
  }

  fun resetManualIconOverride(entry: AppEntry) {
    val activePackId = _uiState.value.activeIconPackId
    _uiState.update {
      it.copy(
        selectedAppForActions = null,
        iconPickerTargetApp = null,
        iconPickerPackId = null,
        iconPickerChoices = emptyList(),
      )
    }
    viewModelScope.launch {
      val updatedOverrides = repository.clearManualIconOverride(activePackId, entry.componentKey)
      iconRenderer.evictComponent(entry.componentKey, activePackId)
      val targetPx = lastPrewarmedPx.takeIf { it > 0 } ?: 160
      iconRenderer.loadOrRender(
        packageName = entry.packageName,
        activityName = entry.activityName,
        componentKey = entry.componentKey,
        targetSizePx = targetPx,
        label = entry.label,
        androidCategory = entry.androidCategory,
        packId = activePackId,
        manualChoiceKey = null,
      )
      _uiState.update { current ->
        current.copy(
          manualOverridesByPack = updatedOverrides,
          iconRenderEpoch = current.iconRenderEpoch + 1,
        )
      }
      if (cachedAllApps.isNotEmpty()) {
        recomputeState()
      }
    }
  }

  suspend fun loadPickerChoicePreview(
    packId: String,
    choiceKey: String,
    sizePx: Int,
  ): ImageBitmap? {
    // Only render choices belonging to the locked active picker pack
    val state = _uiState.value
    if (packId != state.activeIconPackId) return null
    return iconRenderer.renderPackChoicePreview(
      packId = packId,
      choiceKey = choiceKey,
      targetSizePx = sizePx,
    )
  }

  fun launchApp(
    entry: AppEntry,
    hostView: View? = null,
    originBounds: Rect? = null,
    iconAsset: RenderedSquircleIcon? = null,
  ) {
    val activePhase = _uiState.value.transitionState.phase
    if (activePhase == LaunchTransitionPhase.LAUNCHING || activePhase == LaunchTransitionPhase.CLOSING) {
      return
    }

    if (originBounds != null && originBounds.width > 0f && originBounds.height > 0f) {
      // Stage 1: Show full circular splash expansion from the exact icon before executing startActivity
      _uiState.update { state ->
        state.copy(
          selectedAppForActions = null,
          isWallpaperMenuOpen = false,
          searchQuery = "",
          transitionState = LaunchTransitionState(
            phase = LaunchTransitionPhase.LAUNCHING,
            pendingEntry = entry,
            componentKey = entry.componentKey,
            label = entry.label,
            originRect = originBounds,
            iconAsset = iconAsset,
            splashColor = iconAsset?.dominantColor ?: Color(0xFF1F2937),
            secondarySplashColor = iconAsset?.secondaryColor ?: Color(0xFF0F172A),
            eventId = state.transitionState.eventId + 1,
          ),
        )
      }
    } else {
      // No icon origin bounds (e.g., search GO chip or sheet action): launch directly
      _uiState.update {
        it.copy(
          selectedAppForActions = null,
          isWallpaperMenuOpen = false,
          searchQuery = "",
        )
      }
      val launched = repository.launchApp(
        entry = entry,
        hostView = hostView,
        originBounds = null,
        afterSplashOverlay = false,
      )
      if (launched) {
        recordLaunchDeferred(entry.componentKey, recomputeImmediately = true)
      }
    }
  }

  /**
   * Called by the splash overlay once the launch ripple fills the screen, ensuring the launch
   * animation is visibly completed before executing the real activity launch.
   */
  fun commitPendingLaunch(eventId: Int, hostView: View? = null) {
    val tState = _uiState.value.transitionState
    if (tState.phase != LaunchTransitionPhase.LAUNCHING || tState.eventId != eventId) return
    val entry = tState.pendingEntry ?: run {
      clearLaunchTransition()
      return
    }

    _uiState.update { state ->
      if (state.transitionState.eventId == eventId) {
        state.copy(
          transitionState = state.transitionState.copy(
            phase = LaunchTransitionPhase.LAUNCHED_WAITING_RETURN,
          )
        )
      } else {
        state
      }
    }

    val launched = repository.launchApp(
      entry = entry,
      hostView = hostView,
      originBounds = tState.originRect,
      afterSplashOverlay = true,
    )
    if (launched) {
      // Defer recomputing dock order until after close animation completes so icon positions stay aligned
      recordLaunchDeferred(entry.componentKey, recomputeImmediately = false)
    } else {
      clearLaunchTransition()
    }
  }

  private fun recordLaunchDeferred(componentKey: String, recomputeImmediately: Boolean) {
    viewModelScope.launch {
      repository.recordAppLaunch(componentKey)
      cachedAllApps = cachedAllApps.map { item ->
        if (item.componentKey == componentKey) {
          item.copy(launchCount = item.launchCount + 1, lastLaunchedAt = System.currentTimeMillis())
        } else {
          item
        }
      }
      if (recomputeImmediately && _uiState.value.transitionState.phase == LaunchTransitionPhase.IDLE) {
        recomputeState()
      } else {
        hasPendingRecomputeAfterTransition = true
      }
    }
  }

  fun launchTopMatchIfSingleOrExact(): Boolean {
    val ready = _uiState.value.loadState as? AppsLoadState.Ready ?: return false
    val query = _uiState.value.searchQuery.trim().lowercase()
    if (query.isEmpty() || ready.searchResults.isEmpty()) return false

    val first = ready.searchResults.first()
    if (ready.searchResults.size == 1 || first.normalizedLabel == query) {
      launchApp(first)
      return true
    }
    return false
  }

  fun onReturnedToLauncher(): Boolean {
    val currentTransition = _uiState.value.transitionState
    val isPendingReturn =
      currentTransition.phase == LaunchTransitionPhase.LAUNCHED_WAITING_RETURN ||
        currentTransition.phase == LaunchTransitionPhase.LAUNCHING

    if (isPendingReturn && currentTransition.originRect != null) {
      lastReturnedFromAppAtMs = System.currentTimeMillis()
      _uiState.update { state ->
        state.copy(
          transitionState = currentTransition.copy(
            phase = LaunchTransitionPhase.CLOSING,
            eventId = currentTransition.eventId + 1,
          )
        )
      }
      return true
    }
    return false
  }

  fun handleHomeButtonPressed(currentPagerPage: Int, appPageCount: Int): HomeNavigationAction {
    if (onReturnedToLauncher()) {
      return HomeNavigationAction.None
    }
    val current = _uiState.value
    if (
      current.transitionState.phase == LaunchTransitionPhase.CLOSING ||
        (System.currentTimeMillis() - lastReturnedFromAppAtMs) < 400L
    ) {
      return HomeNavigationAction.None
    }

    if (current.searchQuery.isNotEmpty()) {
      clearSearch()
    }
    _uiState.update {
      it.copy(
        selectedAppForActions = null,
        isWallpaperMenuOpen = false,
        iconPickerTargetApp = null,
        iconPickerPackId = null,
        iconPickerChoices = emptyList(),
      )
    }
    val target = resolveHomeTargetPage(currentPagerPage, appPageCount)
    return if (target != currentPagerPage) {
      HomeNavigationAction.ScrollToPage(target)
    } else {
      HomeNavigationAction.None
    }
  }

  fun clearLaunchTransition() {
    _uiState.update { state ->
      if (state.transitionState.phase == LaunchTransitionPhase.IDLE) {
        state
      } else {
        state.copy(transitionState = LaunchTransitionState())
      }
    }
    if (hasPendingRecomputeAfterTransition) {
      hasPendingRecomputeAfterTransition = false
      if (cachedAllApps.isNotEmpty()) {
        recomputeState()
      }
    }
  }

  fun openAppInfo(entry: AppEntry) {
    _uiState.update { it.copy(selectedAppForActions = null) }
    repository.openAppInfo(entry.packageName)
  }

  fun togglePinToDock(entry: AppEntry) {
    val maxSlots = _uiState.value.dockSlotCount
    val activePackId = _uiState.value.activeIconPackId
    _uiState.update { it.copy(selectedAppForActions = null) }
    viewModelScope.launch {
      cachedPinnedKeys = repository.togglePinnedDockKey(entry.componentKey, maxSlots)
      if (cachedAllApps.isNotEmpty()) {
        recomputeState()
        if (lastPrewarmedPx > 0) {
          val targetPx = lastPrewarmedPx
          val manualKey = _uiState.value.manualOverridesByPack[activePackId]?.get(entry.componentKey)
          iconRenderer.loadOrRender(
            packageName = entry.packageName,
            activityName = entry.activityName,
            componentKey = entry.componentKey,
            targetSizePx = targetPx,
            label = entry.label,
            androidCategory = entry.androidCategory,
            packId = activePackId,
            manualChoiceKey = manualKey,
          )
          recomputeState()
        }
      }
    }
  }

  fun openDefaultHomeSettings() {
    _uiState.update { it.copy(isWallpaperMenuOpen = false) }
    repository.openDefaultHomeSettings()
  }

  fun getCachedIcon(componentKey: String, sizePx: Int): RenderedSquircleIcon? {
    val state = _uiState.value
    val packId = state.activeIconPackId
    val manualKey = state.manualOverridesByPack[packId]?.get(componentKey)
    return iconRenderer.getCached(componentKey, sizePx, packId, manualKey)
  }

  suspend fun loadIcon(entry: AppEntry, sizePx: Int): RenderedSquircleIcon? {
    val state = _uiState.value
    val packId = state.activeIconPackId
    val manualKey = state.manualOverridesByPack[packId]?.get(entry.componentKey)
    return iconRenderer.loadOrRender(
      packageName = entry.packageName,
      activityName = entry.activityName,
      componentKey = entry.componentKey,
      targetSizePx = sizePx,
      label = entry.label,
      androidCategory = entry.androidCategory,
      packId = packId,
      manualChoiceKey = manualKey,
    )
  }

  private fun recomputeState() {
    val state = _uiState.value
    val allApps = cachedAllApps
    val pinned = cachedPinnedKeys
    val (regularApps, thallyAceApps) = partitionThallyAceApps(allApps)
    val dockApps = repository.buildSmartDock(
      allApps = allApps,
      maxCount = state.dockSlotCount,
      pinnedComponentKeys = pinned,
      excludedSmartPackages = THALLY_ACE_PACKAGES,
    )
    val pages = chunkIntoHorizontalPages(regularApps, state.itemsPerPage)
    val searchResults = filterAndRankApps(allApps, state.searchQuery)
    val dockColors = resolveDockColors(dockApps, state.activeIconPackId, state.manualOverridesByPack)
    val thallyAceColors = resolveDockColors(thallyAceApps, state.activeIconPackId, state.manualOverridesByPack)

    _uiState.update {
      it.copy(
        pinnedDockKeys = pinned.toSet(),
        loadState = AppsLoadState.Ready(
          allApps = allApps,
          pages = pages,
          thallyAceApps = thallyAceApps,
          thallyAceGradientColors = thallyAceColors,
          searchResults = searchResults,
          dockApps = dockApps,
          dockGradientColors = dockColors,
        ),
      )
    }
  }

  private fun resolveDockColors(
    apps: List<AppEntry>,
    packId: String,
    overridesByPack: Map<String, Map<String, String>>,
  ): List<Color> {
    if (apps.isEmpty() || lastPrewarmedPx <= 0) return emptyList()
    val packOverrides = overridesByPack[packId].orEmpty()
    return apps.mapNotNull { entry ->
      iconRenderer.getCached(
        componentKey = entry.componentKey,
        sizePx = lastPrewarmedPx,
        packId = packId,
        manualChoiceKey = packOverrides[entry.componentKey],
      )?.dominantColor
    }
  }

  private fun prewarmIcons(
    apps: List<AppEntry>,
    targetPx: Int,
    packId: String,
    overridesByPack: Map<String, Map<String, String>> = _uiState.value.manualOverridesByPack,
  ) {
    val packOverrides = overridesByPack[packId].orEmpty()
    viewModelScope.launch(Dispatchers.Default) {
      val concurrency = Semaphore(4)
      val dockApps = repository.buildSmartDock(
        allApps = apps,
        maxCount = _uiState.value.dockSlotCount,
        pinnedComponentKeys = cachedPinnedKeys,
        excludedSmartPackages = THALLY_ACE_PACKAGES,
      )
      val priorityApps = (dockApps + apps.filter { it.packageName.lowercase() in THALLY_ACE_PACKAGES })
        .distinctBy { it.componentKey }
      for (priorityEntry in priorityApps) {
        iconRenderer.loadOrRender(
          packageName = priorityEntry.packageName,
          activityName = priorityEntry.activityName,
          componentKey = priorityEntry.componentKey,
          targetSizePx = targetPx,
          label = priorityEntry.label,
          androidCategory = priorityEntry.androidCategory,
          packId = packId,
          manualChoiceKey = packOverrides[priorityEntry.componentKey],
        )
      }
      if (_uiState.value.transitionState.phase == LaunchTransitionPhase.IDLE) {
        recomputeState()
      }

      val priorityKeys = priorityApps.mapTo(HashSet()) { it.componentKey }
      val remaining = apps.filterNot { it.componentKey in priorityKeys }
      for (entry in remaining) {
        launch {
          concurrency.withPermit {
            iconRenderer.loadOrRender(
              packageName = entry.packageName,
              activityName = entry.activityName,
              componentKey = entry.componentKey,
              targetSizePx = targetPx,
              label = entry.label,
              androidCategory = entry.androidCategory,
              packId = packId,
              manualChoiceKey = packOverrides[entry.componentKey],
            )
          }
        }
      }
    }
  }

  companion object {
    val THALLY_ACE_PACKAGES: Set<String> = setOf(
      "com.aistudio.aiomedia.mvtqkp",
      "com.thallyace.explored",
      "com.thallyace.folklore",
      "com.thallyace.nececity",
      "com.thallyace.determinal",
    )

    fun partitionThallyAceApps(apps: List<AppEntry>): Pair<List<AppEntry>, List<AppEntry>> {
      if (apps.isEmpty()) return emptyList<AppEntry>() to emptyList()
      val regular = ArrayList<AppEntry>(apps.size)
      val thallyAce = ArrayList<AppEntry>(THALLY_ACE_PACKAGES.size)
      for (app in apps) {
        if (app.packageName.lowercase() in THALLY_ACE_PACKAGES) {
          thallyAce.add(app)
        } else {
          regular.add(app)
        }
      }
      return regular to thallyAce
    }

    fun chunkIntoHorizontalPages(apps: List<AppEntry>, itemsPerPage: Int): List<List<AppEntry>> {
      if (apps.isEmpty()) return emptyList()
      val pageSize = itemsPerPage.coerceAtLeast(1)
      return apps.chunked(pageSize)
    }

    fun filterAndRankApps(apps: List<AppEntry>, query: String): List<AppEntry> {
      if (query.isBlank()) return apps
      val q = query.trim().lowercase()

      val prefixMatches = ArrayList<AppEntry>()
      val wordMatches = ArrayList<AppEntry>()
      val acronymMatches = ArrayList<AppEntry>()
      val substringMatches = ArrayList<AppEntry>()

      for (app in apps) {
        val label = app.normalizedLabel
        when {
          label.startsWith(q) -> prefixMatches.add(app)
          label.contains(" $q") -> wordMatches.add(app)
          app.acronym.startsWith(q) && app.acronym.isNotEmpty() -> acronymMatches.add(app)
          label.contains(q) -> substringMatches.add(app)
        }
      }
      return prefixMatches + wordMatches + acronymMatches + substringMatches
    }

    fun resolveHomeTargetPage(currentPage: Int, appPageCount: Int): Int {
      if (appPageCount <= 0) return 0
      return when {
        currentPage > 1 -> 1
        currentPage == 1 -> 0
        else -> 1
      }
    }
  }
}
