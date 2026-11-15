package com.example.data

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.window.SplashScreen
import androidx.compose.ui.geometry.Rect
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.icons.ExternalIconPackLoader
import com.example.model.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt

private val Context.launchStatsDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "orbit_launch_stats"
)

class AppRepository(private val context: Context) {

  private val ownPackageName = context.packageName

  suspend fun loadInstalledApps(): List<AppEntry> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val resolved: List<ResolveInfo> =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(
          launcherIntent,
          PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
        )
      } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(launcherIntent, 0)
      }

    val statsPrefs = runCatching { context.launchStatsDataStore.data.first() }.getOrNull()

    val entries = ArrayList<AppEntry>(resolved.size)
    val seenKeys = HashSet<String>(resolved.size)

    for (info in resolved) {
      val activityInfo = info.activityInfo ?: continue
      val pkg = activityInfo.packageName ?: continue
      if (pkg == ownPackageName) continue

      val actName = activityInfo.name ?: continue
      val key = "$pkg/$actName"
      if (!seenKeys.add(key)) continue

      val rawLabel = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
      val isSystem =
        (activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
          (activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

      val count = statsPrefs?.get(countKey(key)) ?: 0
      val lastAt = statsPrefs?.get(lastKey(key)) ?: 0L
      val appCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        activityInfo.applicationInfo.category
      } else {
        ApplicationInfo.CATEGORY_UNDEFINED
      }

      entries.add(
        AppEntry(
          packageName = pkg,
          activityName = actName,
          componentKey = key,
          label = rawLabel,
          normalizedLabel = AppEntry.buildNormalized(rawLabel),
          acronym = AppEntry.buildAcronym(rawLabel),
          sectionChar = AppEntry.buildSectionChar(rawLabel),
          isSystemApp = isSystem,
          launchCount = count,
          lastLaunchedAt = lastAt,
          androidCategory = appCategory,
        )
      )
    }

    entries.sortWith(
      compareBy<AppEntry> { it.sectionChar == '#' }
        .thenBy { it.normalizedLabel }
    )
    entries
  }

  fun observePackageChanges(): Flow<String?> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        val pkg = intent?.data?.schemeSpecificPart
        trySend(pkg)
      }
    }
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_PACKAGE_ADDED)
      addAction(Intent.ACTION_PACKAGE_REMOVED)
      addAction(Intent.ACTION_PACKAGE_REPLACED)
      addAction(Intent.ACTION_PACKAGE_CHANGED)
      addDataScheme("package")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      context.registerReceiver(receiver, filter)
    }
    awaitClose {
      runCatching { context.unregisterReceiver(receiver) }
    }
  }

  suspend fun recordAppLaunch(componentKey: String) {
    withContext(Dispatchers.IO) {
      runCatching {
        val now = System.currentTimeMillis()
        context.launchStatsDataStore.edit { prefs ->
          val cKey = countKey(componentKey)
          val lKey = lastKey(componentKey)
          prefs[cKey] = (prefs[cKey] ?: 0) + 1
          prefs[lKey] = now
        }
      }
    }
  }

  fun launchApp(
    entry: AppEntry,
    hostView: View? = null,
    originBounds: Rect? = null,
    afterSplashOverlay: Boolean = originBounds != null,
  ): Boolean {
    val launchCtx = hostView?.context ?: context
    val optionsBundle: Bundle? = buildMaterialSplashOptions(
      launchCtx = launchCtx,
      hostView = hostView,
      originBounds = originBounds,
      afterSplashOverlay = afterSplashOverlay,
    )

    return runCatching {
      val intent = Intent.makeMainActivity(ComponentName(entry.packageName, entry.activityName)).apply {
        addFlags(
          Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
      }
      if (optionsBundle != null) {
        launchCtx.startActivity(intent, optionsBundle)
      } else {
        launchCtx.startActivity(intent)
      }
      true
    }.getOrElse {
      runCatching {
        val fallback = context.packageManager.getLaunchIntentForPackage(entry.packageName)
        if (fallback != null) {
          fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          if (optionsBundle != null) {
            launchCtx.startActivity(fallback, optionsBundle)
          } else {
            launchCtx.startActivity(fallback)
          }
          true
        } else {
          false
        }
      }.getOrDefault(false)
    }
  }

  private fun buildMaterialSplashOptions(
    launchCtx: Context,
    hostView: View?,
    originBounds: Rect?,
    afterSplashOverlay: Boolean,
  ): Bundle? {
    val options = when {
      afterSplashOverlay -> {
        runCatching {
          ActivityOptions.makeCustomAnimation(
            launchCtx,
            com.example.R.anim.orbit_app_enter_fade,
            com.example.R.anim.orbit_instant_none,
          )
        }.getOrNull()
      }
      hostView != null && originBounds != null && originBounds.width > 0f && originBounds.height > 0f -> {
        runCatching {
          ActivityOptions.makeClipRevealAnimation(
            hostView,
            originBounds.left.roundToInt(),
            originBounds.top.roundToInt(),
            originBounds.width.roundToInt().coerceAtLeast(1),
            originBounds.height.roundToInt().coerceAtLeast(1),
          )
        }.getOrElse {
          runCatching {
            ActivityOptions.makeScaleUpAnimation(
              hostView,
              originBounds.left.roundToInt(),
              originBounds.top.roundToInt(),
              originBounds.width.roundToInt().coerceAtLeast(1),
              originBounds.height.roundToInt().coerceAtLeast(1),
            )
          }.getOrNull()
        }
      }
      else -> null
    }

    if (options != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      runCatching {
        options.setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON)
      }
    }
    return options?.toBundle()
  }

  suspend fun loadPinnedDockKeys(): List<String> = withContext(Dispatchers.IO) {
    val prefs = runCatching { context.launchStatsDataStore.data.first() }.getOrNull()
    val raw = prefs?.get(pinnedDockOrderKey).orEmpty()
    if (raw.isBlank()) emptyList() else raw.split("|").filter { it.isNotBlank() }
  }

  suspend fun togglePinnedDockKey(componentKey: String, maxDockCount: Int): List<String> =
    withContext(Dispatchers.IO) {
      var updated: List<String> = emptyList()
      runCatching {
        context.launchStatsDataStore.edit { prefs ->
          val current = prefs[pinnedDockOrderKey]
            ?.split("|")
            ?.filter { it.isNotBlank() }
            ?.toMutableList()
            ?: mutableListOf()
          if (current.contains(componentKey)) {
            current.remove(componentKey)
          } else {
            if (current.size >= maxDockCount.coerceAtLeast(1)) {
              current.removeAt(0)
            }
            current.add(componentKey)
          }
          updated = current.toList()
          prefs[pinnedDockOrderKey] = current.joinToString("|")
        }
      }
      updated
    }

  fun openAppInfo(packageName: String) {
    runCatching {
      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }
  }

  fun openDefaultHomeSettings() {
    runCatching {
      val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }
  }

  fun buildSmartDock(
    allApps: List<AppEntry>,
    maxCount: Int,
    pinnedComponentKeys: List<String> = emptyList(),
    excludedSmartPackages: Set<String> = emptySet(),
  ): List<AppEntry> {
    if (allApps.isEmpty() || maxCount <= 0) return emptyList()

    val byKey = allApps.associateBy { it.componentKey }
    val pinnedApps = pinnedComponentKeys.mapNotNull { byKey[it] }.take(maxCount)
    val pinnedSet = pinnedApps.mapTo(HashSet()) { it.componentKey }

    if (pinnedApps.size >= maxCount) return pinnedApps

    val priorityKeywords = listOf(
      "phone", "dialer", "messages", "sms", "camera", "chrome", "browser", "photos", "settings"
    )

    fun defaultPriorityScore(app: AppEntry): Int {
      val name = app.normalizedLabel
      val pkg = app.packageName.lowercase()
      val idx = priorityKeywords.indexOfFirst { kw -> name.contains(kw) || pkg.contains(kw) }
      return if (idx >= 0) priorityKeywords.size - idx else 0
    }

    val remainingCount = maxCount - pinnedApps.size
    val smartFilled = allApps
      .asSequence()
      .filterNot { it.componentKey in pinnedSet }
      .filterNot { it.packageName.lowercase() in excludedSmartPackages }
      .sortedWith(
        compareByDescending<AppEntry> { it.launchCount }
          .thenByDescending { it.lastLaunchedAt }
          .thenByDescending { defaultPriorityScore(it) }
          .thenBy { it.normalizedLabel }
      )
      .take(remainingCount)
      .toList()

    return pinnedApps + smartFilled
  }

  suspend fun loadActiveIconPackId(): String = withContext(Dispatchers.IO) {
    val prefs = runCatching { context.launchStatsDataStore.data.first() }.getOrNull()
    prefs?.get(activeIconPackIdKey)?.takeIf { it.isNotBlank() }
      ?: ExternalIconPackLoader.PACK_ID_ORBIT_FUSION
  }

  suspend fun saveActiveIconPackId(packId: String) {
    withContext(Dispatchers.IO) {
      runCatching {
        context.launchStatsDataStore.edit { prefs ->
          prefs[activeIconPackIdKey] = packId
        }
      }
    }
  }

  suspend fun loadThallyFolderDisbanded(): Boolean = withContext(Dispatchers.IO) {
    val prefs = runCatching { context.launchStatsDataStore.data.first() }.getOrNull()
    prefs?.get(thallyFolderDisbandedKey) ?: false
  }

  suspend fun saveThallyFolderDisbanded(disbanded: Boolean) {
    withContext(Dispatchers.IO) {
      runCatching {
        context.launchStatsDataStore.edit { prefs ->
          prefs[thallyFolderDisbandedKey] = disbanded
        }
      }
    }
  }

  suspend fun loadManualIconOverrides(): Map<String, Map<String, String>> =
    withContext(Dispatchers.IO) {
      val prefs = runCatching { context.launchStatsDataStore.data.first() }.getOrNull()
      val raw = prefs?.get(manualIconOverridesKey).orEmpty()
      decodeManualOverridesJson(raw)
    }

  suspend fun setManualIconOverride(
    packId: String,
    componentKey: String,
    choiceKey: String,
  ): Map<String, Map<String, String>> = withContext(Dispatchers.IO) {
    if (packId.isBlank() || componentKey.isBlank() || choiceKey.isBlank()) {
      return@withContext loadManualIconOverrides()
    }
    var updated: Map<String, Map<String, String>> = emptyMap()
    runCatching {
      context.launchStatsDataStore.edit { prefs ->
        val mutable = decodeManualOverridesJson(prefs[manualIconOverridesKey].orEmpty())
          .mapValues { (_, compMap) -> compMap.toMutableMap() }
          .toMutableMap()
        val packMap = mutable.getOrPut(packId) { LinkedHashMap() }
        packMap[componentKey] = choiceKey
        prefs[manualIconOverridesKey] = encodeManualOverridesJson(mutable)
        updated = mutable.mapValues { (_, v) -> v.toMap() }
      }
    }
    updated
  }

  suspend fun clearManualIconOverride(
    packId: String,
    componentKey: String,
  ): Map<String, Map<String, String>> = withContext(Dispatchers.IO) {
    var updated: Map<String, Map<String, String>> = emptyMap()
    runCatching {
      context.launchStatsDataStore.edit { prefs ->
        val mutable = decodeManualOverridesJson(prefs[manualIconOverridesKey].orEmpty())
          .mapValues { (_, compMap) -> compMap.toMutableMap() }
          .toMutableMap()
        mutable[packId]?.let { packMap ->
          packMap.remove(componentKey)
          if (packMap.isEmpty()) mutable.remove(packId)
        }
        prefs[manualIconOverridesKey] = encodeManualOverridesJson(mutable)
        updated = mutable.mapValues { (_, v) -> v.toMap() }
      }
    }
    updated
  }

  suspend fun clearOverridesForPack(packId: String): Map<String, Map<String, String>> =
    withContext(Dispatchers.IO) {
      var updated: Map<String, Map<String, String>> = emptyMap()
      runCatching {
        context.launchStatsDataStore.edit { prefs ->
          val mutable = decodeManualOverridesJson(prefs[manualIconOverridesKey].orEmpty())
            .mapValues { (_, compMap) -> compMap.toMutableMap() }
            .toMutableMap()
          mutable.remove(packId)
          prefs[manualIconOverridesKey] = encodeManualOverridesJson(mutable)
          updated = mutable.mapValues { (_, v) -> v.toMap() }
        }
      }
      updated
    }

  private fun decodeManualOverridesJson(raw: String): Map<String, Map<String, String>> {
    if (raw.isBlank()) return emptyMap()
    return runCatching {
      val root = JSONObject(raw)
      val outer = LinkedHashMap<String, Map<String, String>>(root.length())
      val packKeys = root.keys()
      while (packKeys.hasNext()) {
        val packId = packKeys.next()
        val innerJson = root.optJSONObject(packId) ?: continue
        val inner = LinkedHashMap<String, String>(innerJson.length())
        val compKeys = innerJson.keys()
        while (compKeys.hasNext()) {
          val comp = compKeys.next()
          val choice = innerJson.optString(comp)
          if (choice.isNotBlank()) {
            inner[comp] = choice
          }
        }
        if (inner.isNotEmpty()) {
          outer[packId] = inner
        }
      }
      outer
    }.getOrDefault(emptyMap())
  }

  private fun encodeManualOverridesJson(map: Map<String, Map<String, String>>): String {
    val root = JSONObject()
    for ((packId, compMap) in map) {
      if (compMap.isEmpty()) continue
      val inner = JSONObject()
      for ((comp, choice) in compMap) {
        if (choice.isNotBlank()) inner.put(comp, choice)
      }
      if (inner.length() > 0) root.put(packId, inner)
    }
    return root.toString()
  }

  private val pinnedDockOrderKey = stringPreferencesKey("pinned_dock_order")
  private val activeIconPackIdKey = stringPreferencesKey("active_icon_pack_id")
  private val thallyFolderDisbandedKey = booleanPreferencesKey("thally_folder_disbanded")
  private val manualIconOverridesKey = stringPreferencesKey("manual_icon_overrides_v1")
  private fun countKey(componentKey: String) = intPreferencesKey("cnt_${componentKey.hashCode()}")
  private fun lastKey(componentKey: String) = longPreferencesKey("ts_${componentKey.hashCode()}")
}
