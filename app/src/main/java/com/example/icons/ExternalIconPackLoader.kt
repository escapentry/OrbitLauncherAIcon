package com.example.icons

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Xml
import androidx.core.content.res.ResourcesCompat
import java.io.StringReader
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt
import org.xmlpull.v1.XmlPullParser

data class IconPackOption(
  val id: String,
  val title: String,
  val subtitle: String,
  val packageName: String? = null,
  val isImported: Boolean = false,
)

data class DiscoveredIconPack(
  val packageName: String,
  val label: String,
)

data class PackIconChoiceItem(
  val choiceKey: String,
  val label: String,
)

data class ParsedAppFilter(
  val componentToDrawable: Map<String, String>,
  val packageToDrawable: Map<String, String>,
  val calendarPrefixes: Map<String, String>,
  val iconBacks: List<String>,
  val iconMask: String?,
  val iconUpon: String?,
  val scaleFactor: Float,
)

data class ExternalPackResolvedDrawable(
  val drawable: Drawable,
  val isMaskedLegacyComposite: Boolean = false,
)

/**
 * Discovers installed Android icon packs (ADW, Nova, GO, Apex themes), reads imported packs,
 * parses `appfilter.xml`, and enforces active-pack-scoped lookup + intelligent keyword/category rules.
 */
object ExternalIconPackLoader {

  const val PACK_ID_ORBIT_FUSION = "orbit_fusion"
  const val PACK_ID_ORBIT_OREO = "orbit_oreo"
  const val PACK_ID_ORBIT_OBSIDIAN = "orbit_obsidian"
  const val PACK_ID_IPHONE = "orbit_iphone"
  const val PACK_ID_SYSTEM_DEFAULT = "system_default"
  const val PACK_ID_EXTERNAL_PREFIX = "pkg:"

  fun isOrbitBuiltInPack(packId: String): Boolean =
    packId == PACK_ID_ORBIT_FUSION ||
      packId == PACK_ID_ORBIT_OREO ||
      packId == PACK_ID_ORBIT_OBSIDIAN ||
      packId == PACK_ID_IPHONE

  fun orbitStyleForPackId(packId: String): OrbitPackStyle? = when (packId) {
    PACK_ID_ORBIT_FUSION -> OrbitPackStyle.FUSION
    PACK_ID_ORBIT_OREO -> OrbitPackStyle.OREO_CONTRAST
    PACK_ID_ORBIT_OBSIDIAN -> OrbitPackStyle.OBSIDIAN_DARK
    PACK_ID_IPHONE -> OrbitPackStyle.IPHONE
    else -> null
  }

  private val ICON_PACK_ACTIONS = listOf(
    "org.adw.launcher.THEMES",
    "com.novalauncher.THEME",
    "com.gau.go.launcherex.theme",
    "com.anddoes.launcher.THEME",
  )

  private val parsedPackCache = ConcurrentHashMap<String, ParsedAppFilter>()

  fun externalPackId(packageName: String): String = "$PACK_ID_EXTERNAL_PREFIX$packageName"

  fun extractExternalPackageName(packId: String): String? =
    if (packId.startsWith(PACK_ID_EXTERNAL_PREFIX)) {
      packId.removePrefix(PACK_ID_EXTERNAL_PREFIX).takeIf { it.isNotBlank() }
    } else {
      null
    }

  fun clearParsedCache(packageName: String? = null) {
    if (packageName == null) {
      parsedPackCache.clear()
    } else {
      parsedPackCache.remove(packageName)
    }
  }

  fun discoverInstalledPacks(context: Context): List<DiscoveredIconPack> {
    val pm = context.packageManager
    val seen = LinkedHashMap<String, DiscoveredIconPack>()

    for (action in ICON_PACK_ACTIONS) {
      val intent = Intent(action)
      val results: List<ResolveInfo> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
          )
        } else {
          @Suppress("DEPRECATION")
          pm.queryIntentActivities(intent, 0)
        }
      }.getOrDefault(emptyList())

      for (info in results) {
        val pkg = info.activityInfo?.packageName ?: continue
        if (pkg == context.packageName || seen.containsKey(pkg)) continue
        val label = runCatching {
          val appInfo = pm.getApplicationInfo(pkg, 0)
          pm.getApplicationLabel(appInfo).toString().trim()
        }.getOrElse {
          info.loadLabel(pm)?.toString()?.trim().orEmpty()
        }.ifEmpty { pkg }
        seen[pkg] = DiscoveredIconPack(packageName = pkg, label = label)
      }
    }

    return seen.values.sortedBy { it.label.lowercase(Locale.ROOT) }
  }

  fun loadOrParsePack(context: Context, packPackageName: String): ParsedAppFilter? {
    parsedPackCache[packPackageName]?.let { return it }

    val packResources = runCatching {
      context.packageManager.getResourcesForApplication(packPackageName)
    }.getOrNull() ?: return null

    val xmlParsed = parsePackResources(packResources, packPackageName)
    if (xmlParsed != null) {
      parsedPackCache[packPackageName] = xmlParsed
    }
    return xmlParsed
  }

  private fun parsePackResources(packResources: Resources, packPackageName: String): ParsedAppFilter? {
    // 1. Prefer compiled res/xml/appfilter.xml if available
    val xmlResId = packResources.getIdentifier("appfilter", "xml", packPackageName)
    if (xmlResId != 0) {
      runCatching {
        packResources.getXml(xmlResId).use { parser ->
          return parseAppFilterXml(parser)
        }
      }
    }

    // 2. Fallback to assets/appfilter.xml
    val assetParsed = runCatching {
      packResources.assets.open("appfilter.xml").use { input ->
        val parser = Xml.newPullParser().apply {
          setInput(input, "UTF-8")
        }
        parseAppFilterXml(parser)
      }
    }.getOrNull()

    if (assetParsed != null) return assetParsed

    // 3. Even packs without appfilter.xml can still match drawables by keyword/category
    return ParsedAppFilter(
      componentToDrawable = emptyMap(),
      packageToDrawable = emptyMap(),
      calendarPrefixes = emptyMap(),
      iconBacks = emptyList(),
      iconMask = null,
      iconUpon = null,
      scaleFactor = 0.78f,
    )
  }

  fun parseAppFilterXml(xmlContent: String): ParsedAppFilter {
    val parser = Xml.newPullParser().apply {
      setInput(StringReader(xmlContent))
    }
    return parseAppFilterXml(parser)
  }

  fun parseAppFilterXml(parser: XmlPullParser): ParsedAppFilter {
    val componentToDrawable = HashMap<String, String>(512)
    val packageToDrawable = HashMap<String, String>(512)
    val calendarPrefixes = HashMap<String, String>(16)
    val iconBacks = ArrayList<String>(4)
    var iconMask: String? = null
    var iconUpon: String? = null
    var scaleFactor = 0.80f

    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        when (parser.name?.lowercase(Locale.ROOT)) {
          "item" -> {
            val componentAttr = parser.getAttributeValue(null, "component")?.trim()
            val drawableAttr = parser.getAttributeValue(null, "drawable")?.trim()
            if (!componentAttr.isNullOrEmpty() && !drawableAttr.isNullOrEmpty()) {
              val parsed = parseComponentAttribute(componentAttr)
              if (parsed != null) {
                val (pkgLower, fullComponentLower) = parsed
                componentToDrawable.putIfAbsent(fullComponentLower, drawableAttr)
                packageToDrawable.putIfAbsent(pkgLower, drawableAttr)
              }
            }
          }

          "calendar" -> {
            val componentAttr = parser.getAttributeValue(null, "component")?.trim()
            val prefixAttr = parser.getAttributeValue(null, "prefix")?.trim()
            if (!componentAttr.isNullOrEmpty() && !prefixAttr.isNullOrEmpty()) {
              val parsed = parseComponentAttribute(componentAttr)
              if (parsed != null) {
                calendarPrefixes.putIfAbsent(parsed.second, prefixAttr)
                calendarPrefixes.putIfAbsent(parsed.first, prefixAttr)
              }
            }
          }

          "iconback" -> {
            for (i in 0 until parser.attributeCount) {
              val attrName = parser.getAttributeName(i)?.lowercase(Locale.ROOT) ?: continue
              if (attrName.startsWith("img")) {
                val value = parser.getAttributeValue(i)?.trim()
                if (!value.isNullOrEmpty()) {
                  iconBacks.add(value)
                }
              }
            }
          }

          "iconmask" -> {
            val value = parser.getAttributeValue(null, "img1")?.trim()
              ?: if (parser.attributeCount > 0) parser.getAttributeValue(0)?.trim() else null
            if (!value.isNullOrEmpty()) {
              iconMask = value
            }
          }

          "iconupon" -> {
            val value = parser.getAttributeValue(null, "img1")?.trim()
              ?: if (parser.attributeCount > 0) parser.getAttributeValue(0)?.trim() else null
            if (!value.isNullOrEmpty()) {
              iconUpon = value
            }
          }

          "scale" -> {
            val factor = parser.getAttributeValue(null, "factor")?.toFloatOrNull()
            if (factor != null && factor in 0.25f..1.4f) {
              scaleFactor = factor
            }
          }
        }
      }
      eventType = parser.next()
    }

    return ParsedAppFilter(
      componentToDrawable = componentToDrawable,
      packageToDrawable = packageToDrawable,
      calendarPrefixes = calendarPrefixes,
      iconBacks = iconBacks,
      iconMask = iconMask,
      iconUpon = iconUpon,
      scaleFactor = scaleFactor,
    )
  }

  private fun parseComponentAttribute(component: String): Pair<String, String>? {
    val start = component.indexOf('{')
    val end = component.lastIndexOf('}')
    val inner = if (start >= 0 && end > start) {
      component.substring(start + 1, end)
    } else {
      component
    }
    val slash = inner.indexOf('/')
    if (slash <= 0 || slash >= inner.lastIndex) return null
    val pkg = inner.substring(0, slash).trim().lowercase(Locale.ROOT)
    val clsRaw = inner.substring(slash + 1).trim()
    val fullCls = if (clsRaw.startsWith(".")) "$pkg$clsRaw" else clsRaw
    return pkg to "$pkg/${fullCls.lowercase(Locale.ROOT)}"
  }

  /**
   * Resolves a drawable from an external icon pack using:
   * 1. Exact component/package or dynamic calendar match
   * 2. Intelligent keyword & Android category association (`IconPackMatcher`)
   * 3. Pack `iconback` / `iconmask` / `iconupon` composition over the app's native icon
   */
  fun resolveExternalPackDrawable(
    context: Context,
    packPackageName: String,
    targetPackageName: String,
    targetActivityName: String,
    targetLabel: String,
    targetAndroidCategory: Int,
    nativeDrawableProvider: () -> Drawable?,
    targetSizePx: Int,
    manualChoiceKey: String? = null,
  ): ExternalPackResolvedDrawable? {
    val packData = loadOrParsePack(context, packPackageName) ?: return null
    val packResources = runCatching {
      context.packageManager.getResourcesForApplication(packPackageName)
    }.getOrNull() ?: return null

    // 0. Strict pack-scoped manual selection if chosen from this pack
    if (!manualChoiceKey.isNullOrBlank()) {
      loadPackDrawableByName(packResources, packPackageName, manualChoiceKey)?.let {
        return ExternalPackResolvedDrawable(it, isMaskedLegacyComposite = false)
      }
    }

    // 1. Dynamic calendar check
    val pkgLower = targetPackageName.lowercase(Locale.ROOT)
    val fullActLower = if (targetActivityName.startsWith(".")) {
      "$pkgLower${targetActivityName.lowercase(Locale.ROOT)}"
    } else {
      targetActivityName.lowercase(Locale.ROOT)
    }
    val compLower = "$pkgLower/$fullActLower"

    val calPrefix = packData.calendarPrefixes[compLower] ?: packData.calendarPrefixes[pkgLower]
    if (calPrefix != null) {
      val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
      loadPackDrawableByName(packResources, packPackageName, "$calPrefix$dayOfMonth")?.let {
        return ExternalPackResolvedDrawable(it)
      }
    }

    // 2. Direct appfilter match (exact component, then exact package)
    val directDrawableName = resolveDrawableNameFromFilter(
      packData = packData,
      packageName = targetPackageName,
      activityName = targetActivityName,
      label = targetLabel,
      androidCategory = targetAndroidCategory,
    )
    if (directDrawableName != null) {
      loadPackDrawableByName(packResources, packPackageName, directDrawableName)?.let {
        return ExternalPackResolvedDrawable(it)
      }
    }

    // 3. Rule-based drawable name search in pack resources via keywords & category
    val match = IconPackMatcher.classify(targetPackageName, targetLabel, targetAndroidCategory)
    if (match.category != AppCategory.OTHER) {
      val keywordCandidates = IconPackMatcher.drawableKeywordsForCategory(match.category)
      for (candidate in keywordCandidates) {
        loadPackDrawableByName(packResources, packPackageName, candidate)?.let {
          return ExternalPackResolvedDrawable(it)
        }
      }
    }

    // 4. Pack mask / iconback / iconupon fallback around native app icon
    val nativeDrawable = nativeDrawableProvider() ?: return null
    val hasMaskOrBack = packData.iconBacks.isNotEmpty() || packData.iconMask != null || packData.iconUpon != null
    if (!hasMaskOrBack) {
      return null
    }

    val composited = composeWithPackMask(
      packResources = packResources,
      packPackageName = packPackageName,
      packData = packData,
      nativeDrawable = nativeDrawable,
      keyHash = pkgLower.hashCode(),
      sizePx = targetSizePx.coerceIn(128, 384),
    )
    return composited?.let {
      ExternalPackResolvedDrawable(
        drawable = BitmapDrawable(context.resources, it),
        isMaskedLegacyComposite = true,
      )
    }
  }

  /**
   * Resolves a drawable strictly from a user-imported `.zip` / `.apk` icon pack.
   */
  fun resolveImportedPackDrawable(
    context: Context,
    packId: String,
    targetPackageName: String,
    targetActivityName: String,
    targetLabel: String,
    targetAndroidCategory: Int,
    nativeDrawableProvider: () -> Drawable?,
    targetSizePx: Int,
    manualChoiceKey: String? = null,
  ): ExternalPackResolvedDrawable? {
    val importedData = ImportedIconPackStore.loadManifest(context, packId) ?: return null
    val packData = importedData.appFilter

    if (!manualChoiceKey.isNullOrBlank()) {
      ImportedIconPackStore.loadDrawable(context, importedData, manualChoiceKey)?.let {
        return ExternalPackResolvedDrawable(it, isMaskedLegacyComposite = false)
      }
    }

    val pkgLower = targetPackageName.lowercase(Locale.ROOT)
    val fullActLower = if (targetActivityName.startsWith(".")) {
      "$pkgLower${targetActivityName.lowercase(Locale.ROOT)}"
    } else {
      targetActivityName.lowercase(Locale.ROOT)
    }
    val compLower = "$pkgLower/$fullActLower"

    val calPrefix = packData.calendarPrefixes[compLower] ?: packData.calendarPrefixes[pkgLower]
    if (calPrefix != null) {
      val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
      ImportedIconPackStore.loadDrawable(context, importedData, "$calPrefix$dayOfMonth")?.let {
        return ExternalPackResolvedDrawable(it)
      }
    }

    val directDrawableName = resolveDrawableNameFromFilter(
      packData = packData,
      packageName = targetPackageName,
      activityName = targetActivityName,
      label = targetLabel,
      androidCategory = targetAndroidCategory,
    )
    if (directDrawableName != null) {
      ImportedIconPackStore.loadDrawable(context, importedData, directDrawableName)?.let {
        return ExternalPackResolvedDrawable(it)
      }
    }

    val match = IconPackMatcher.classify(targetPackageName, targetLabel, targetAndroidCategory)
    if (match.category != AppCategory.OTHER) {
      for (candidate in IconPackMatcher.drawableKeywordsForCategory(match.category)) {
        ImportedIconPackStore.loadDrawable(context, importedData, candidate)?.let {
          return ExternalPackResolvedDrawable(it)
        }
      }
    }
    for (token in match.matchedTokens) {
      if (token.length < 4) continue
      val keyMatch = importedData.drawableFileMap.keys.firstOrNull { name -> name.contains(token) }
      if (keyMatch != null) {
        ImportedIconPackStore.loadDrawable(context, importedData, keyMatch)?.let {
          return ExternalPackResolvedDrawable(it)
        }
      }
    }

    val nativeDrawable = nativeDrawableProvider() ?: return null
    val hasMaskOrBack = packData.iconBacks.isNotEmpty() || packData.iconMask != null || packData.iconUpon != null
    if (!hasMaskOrBack) return null

    val composited = composeWithImportedPackMask(
      context = context,
      importedData = importedData,
      nativeDrawable = nativeDrawable,
      keyHash = pkgLower.hashCode(),
      sizePx = targetSizePx.coerceIn(128, 384),
    )
    return composited?.let {
      ExternalPackResolvedDrawable(
        drawable = BitmapDrawable(context.resources, it),
        isMaskedLegacyComposite = true,
      )
    }
  }

  /**
   * Pure lookup helper so intelligent appfilter matching can also be verified in Robolectric tests.
   */
  fun resolveDrawableNameFromFilter(
    packData: ParsedAppFilter,
    packageName: String,
    activityName: String,
    label: String,
    androidCategory: Int,
  ): String? {
    val pkgLower = packageName.lowercase(Locale.ROOT)
    val fullActLower = if (activityName.startsWith(".")) {
      "$pkgLower${activityName.lowercase(Locale.ROOT)}"
    } else {
      activityName.lowercase(Locale.ROOT)
    }
    val compLower = "$pkgLower/$fullActLower"

    packData.componentToDrawable[compLower]?.let { return it }
    packData.packageToDrawable[pkgLower]?.let { return it }

    // Intelligent category/keyword matching against canonical archetype packages inside appfilter.xml
    val match = IconPackMatcher.classify(packageName, label, androidCategory)
    if (match.category != AppCategory.OTHER) {
      for (canonicalPkg in IconPackMatcher.canonicalPackagesForCategory(match.category)) {
        packData.packageToDrawable[canonicalPkg.lowercase(Locale.ROOT)]?.let { return it }
      }
      // Also look up any package already inside the pack that matches the same keyword tokens
      for (token in match.matchedTokens) {
        if (token.length < 4) continue
        val fuzzyEntry = packData.packageToDrawable.entries.firstOrNull { (packPkg, _) ->
          packPkg.contains(token)
        }
        if (fuzzyEntry != null) return fuzzyEntry.value
      }
    }

    return null
  }

  private fun loadPackDrawableByName(
    packResources: Resources,
    packPackageName: String,
    drawableName: String,
  ): Drawable? {
    val cleanName = drawableName.trim()
    if (cleanName.isEmpty()) return null
    val resId = packResources.getIdentifier(cleanName, "drawable", packPackageName)
    if (resId == 0) return null
    return runCatching {
      ResourcesCompat.getDrawable(packResources, resId, null)
    }.getOrNull()
  }

  private fun composeWithPackMask(
    packResources: Resources,
    packPackageName: String,
    packData: ParsedAppFilter,
    nativeDrawable: Drawable,
    keyHash: Int,
    sizePx: Int,
  ): Bitmap? {
    val iconBackDrawable = if (packData.iconBacks.isNotEmpty()) {
      val idx = abs(keyHash) % packData.iconBacks.size
      loadPackDrawableByName(packResources, packPackageName, packData.iconBacks[idx])
    } else {
      null
    }
    val iconMaskDrawable = packData.iconMask?.let {
      loadPackDrawableByName(packResources, packPackageName, it)
    }
    val iconUponDrawable = packData.iconUpon?.let {
      loadPackDrawableByName(packResources, packPackageName, it)
    }

    if (iconBackDrawable == null && iconMaskDrawable == null && iconUponDrawable == null) {
      return null
    }

    return compositeMaskLayers(
      iconBackDrawable = iconBackDrawable,
      iconMaskDrawable = iconMaskDrawable,
      iconUponDrawable = iconUponDrawable,
      nativeDrawable = nativeDrawable,
      scaleFactor = packData.scaleFactor,
      sizePx = sizePx,
    )
  }

  private fun composeWithImportedPackMask(
    context: Context,
    importedData: ImportedPackData,
    nativeDrawable: Drawable,
    keyHash: Int,
    sizePx: Int,
  ): Bitmap? {
    val filter = importedData.appFilter
    val iconBackDrawable = if (filter.iconBacks.isNotEmpty()) {
      val idx = abs(keyHash) % filter.iconBacks.size
      ImportedIconPackStore.loadDrawable(context, importedData, filter.iconBacks[idx])
    } else {
      null
    }
    val iconMaskDrawable = filter.iconMask?.let {
      ImportedIconPackStore.loadDrawable(context, importedData, it)
    }
    val iconUponDrawable = filter.iconUpon?.let {
      ImportedIconPackStore.loadDrawable(context, importedData, it)
    }
    return compositeMaskLayers(
      iconBackDrawable = iconBackDrawable,
      iconMaskDrawable = iconMaskDrawable,
      iconUponDrawable = iconUponDrawable,
      nativeDrawable = nativeDrawable,
      scaleFactor = filter.scaleFactor,
      sizePx = sizePx,
    )
  }

  private fun compositeMaskLayers(
    iconBackDrawable: Drawable?,
    iconMaskDrawable: Drawable?,
    iconUponDrawable: Drawable?,
    nativeDrawable: Drawable,
    scaleFactor: Float,
    sizePx: Int,
  ): Bitmap? {
    if (iconBackDrawable == null && iconMaskDrawable == null && iconUponDrawable == null) {
      return null
    }

    val result = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    val scaledLayer = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val scaledCanvas = Canvas(scaledLayer)
    val scale = scaleFactor.coerceIn(0.45f, 1.15f)
    val inset = ((sizePx * (1f - scale)) * 0.5f).roundToInt()
    nativeDrawable.setBounds(inset, inset, sizePx - inset, sizePx - inset)
    nativeDrawable.draw(scaledCanvas)

    if (iconMaskDrawable != null) {
      val maskBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
      val maskCanvas = Canvas(maskBitmap)
      iconMaskDrawable.setBounds(0, 0, sizePx, sizePx)
      iconMaskDrawable.draw(maskCanvas)

      val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
      }
      scaledCanvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint)
      maskBitmap.recycle()
    }

    iconBackDrawable?.let { back ->
      back.setBounds(0, 0, sizePx, sizePx)
      back.draw(canvas)
    }

    canvas.drawBitmap(scaledLayer, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    scaledLayer.recycle()

    iconUponDrawable?.let { upon ->
      upon.setBounds(0, 0, sizePx, sizePx)
      upon.draw(canvas)
    }

    return result
  }

  fun buildAvailableOptions(context: Context): List<IconPackOption> {
    val options = mutableListOf(
      IconPackOption(
        id = PACK_ID_ORBIT_FUSION,
        title = "Fusion",
        subtitle = "Apple × Android gradient + smart matching",
      ),
      IconPackOption(
        id = PACK_ID_ORBIT_OREO,
        title = "Oreo 8.1",
        subtitle = "Android 8.1 high-contrast plates + 45° shadow",
      ),
      IconPackOption(
        id = PACK_ID_ORBIT_OBSIDIAN,
        title = "Obsidian",
        subtitle = "Obsidian dark medallions + brand neon rims",
      ),
      IconPackOption(
        id = PACK_ID_IPHONE,
        title = "iPhone",
        subtitle = "Clean iOS glass plates & vertical gradients",
      ),
      IconPackOption(
        id = PACK_ID_SYSTEM_DEFAULT,
        title = "System",
        subtitle = "Original app icons + Obsidian suite",
      ),
    )
    for (imported in ImportedIconPackStore.listImportedPacks(context)) {
      options.add(
        IconPackOption(
          id = imported.id,
          title = imported.title,
          subtitle = "Imported pack (${imported.iconCount} icons)",
          isImported = true,
        )
      )
    }
    for (pack in discoverInstalledPacks(context)) {
      options.add(
        IconPackOption(
          id = externalPackId(pack.packageName),
          title = pack.label,
          subtitle = "Installed pack + smart matching",
          packageName = pack.packageName,
        )
      )
    }
    return options
  }

  /**
   * Lists selectable icon choices strictly scoped to [packId]. Never returns icons from any
   * other icon pack so cross-applying across packs is impossible.
   */
  fun listChoicesForActivePack(context: Context, packId: String): List<PackIconChoiceItem> {
    return when {
      isOrbitBuiltInPack(packId) -> {
        FusionIconRegistry.ORBIT_CATALOG_CHOICES.map {
          PackIconChoiceItem(choiceKey = it.key, label = it.label)
        }
      }

      ImportedIconPackStore.isImportedPackId(packId) -> {
        val data = ImportedIconPackStore.loadManifest(context, packId) ?: return emptyList()
        val seen = LinkedHashMap<String, String>()
        for ((_, drawableName) in data.appFilter.packageToDrawable) {
          val clean = drawableName.trim().lowercase(Locale.ROOT)
          if (clean.isNotEmpty() && !seen.containsKey(clean) && data.drawableFileMap.containsKey(clean)) {
            seen[clean] = formatDrawableChoiceLabel(clean)
          }
        }
        for (drawableName in data.drawableFileMap.keys.sorted()) {
          if (!seen.containsKey(drawableName) && !isHelperMaskName(drawableName, data.appFilter)) {
            seen[drawableName] = formatDrawableChoiceLabel(drawableName)
          }
        }
        seen.entries.take(500).map { PackIconChoiceItem(choiceKey = it.key, label = it.value) }
      }

      packId.startsWith(PACK_ID_EXTERNAL_PREFIX) -> {
        val packPkg = extractExternalPackageName(packId) ?: return emptyList()
        val packData = loadOrParsePack(context, packPkg) ?: return emptyList()
        val packResources = runCatching {
          context.packageManager.getResourcesForApplication(packPkg)
        }.getOrNull() ?: return emptyList()

        val seen = LinkedHashMap<String, String>()
        for (drawableName in packData.packageToDrawable.values) {
          val clean = drawableName.trim()
          if (clean.isNotEmpty() && !seen.containsKey(clean) && !isHelperMaskName(clean, packData)) {
            if (packResources.getIdentifier(clean, "drawable", packPkg) != 0) {
              seen[clean] = formatDrawableChoiceLabel(clean)
            }
          }
          if (seen.size >= 400) break
        }
        seen.entries.map { PackIconChoiceItem(choiceKey = it.key, label = it.value) }
      }

      else -> emptyList()
    }
  }

  fun resolveSingleChoiceDrawableForExternalOrImported(
    context: Context,
    packId: String,
    choiceKey: String,
  ): Drawable? {
    return when {
      ImportedIconPackStore.isImportedPackId(packId) -> {
        val data = ImportedIconPackStore.loadManifest(context, packId) ?: return null
        ImportedIconPackStore.loadDrawable(context, data, choiceKey)
      }

      packId.startsWith(PACK_ID_EXTERNAL_PREFIX) -> {
        val packPkg = extractExternalPackageName(packId) ?: return null
        val packResources = runCatching {
          context.packageManager.getResourcesForApplication(packPkg)
        }.getOrNull() ?: return null
        loadPackDrawableByName(packResources, packPkg, choiceKey)
      }

      else -> null
    }
  }

  /**
   * Returns true if the active icon pack automatically supports this app without falling back
   * to the unstyled native icon or generic composite mask.
   */
  fun isAppSupportedInPack(
    context: Context,
    packId: String,
    packageName: String,
    activityName: String,
    label: String,
    androidCategory: Int,
    thallyAcePackages: Set<String>,
  ): Boolean {
    val pkgLower = packageName.lowercase(Locale.ROOT)
    if (packId == PACK_ID_SYSTEM_DEFAULT) return true

    if (isOrbitBuiltInPack(packId)) {
      if (pkgLower in thallyAcePackages) return true
      return FusionIconRegistry.resolveForApp(packageName, label, androidCategory) != null
    }

    if (ImportedIconPackStore.isImportedPackId(packId)) {
      val data = ImportedIconPackStore.loadManifest(context, packId) ?: return false
      if (resolveDrawableNameFromFilter(data.appFilter, packageName, activityName, label, androidCategory) != null) {
        return true
      }
      val match = IconPackMatcher.classify(packageName, label, androidCategory)
      if (match.category != AppCategory.OTHER) {
        val hit = IconPackMatcher.drawableKeywordsForCategory(match.category)
          .any { data.drawableFileMap.containsKey(it) }
        if (hit) return true
      }
      return false
    }

    if (packId.startsWith(PACK_ID_EXTERNAL_PREFIX)) {
      val packPkg = extractExternalPackageName(packId) ?: return false
      val packData = loadOrParsePack(context, packPkg) ?: return false
      val direct = resolveDrawableNameFromFilter(packData, packageName, activityName, label, androidCategory)
      if (direct != null) return true
      val packRes = runCatching {
        context.packageManager.getResourcesForApplication(packPkg)
      }.getOrNull() ?: return false
      val match = IconPackMatcher.classify(packageName, label, androidCategory)
      if (match.category != AppCategory.OTHER) {
        return IconPackMatcher.drawableKeywordsForCategory(match.category)
          .any { packRes.getIdentifier(it, "drawable", packPkg) != 0 }
      }
      return false
    }

    return true
  }

  private fun isHelperMaskName(name: String, filter: ParsedAppFilter): Boolean {
    val lower = name.lowercase(Locale.ROOT)
    if (lower == filter.iconMask?.lowercase(Locale.ROOT) || lower == filter.iconUpon?.lowercase(Locale.ROOT)) {
      return true
    }
    return filter.iconBacks.any { it.equals(name, ignoreCase = true) }
  }

  private fun formatDrawableChoiceLabel(raw: String): String =
    raw.removePrefix("ic_")
      .replace('_', ' ')
      .replace('-', ' ')
      .trim()
      .split(Regex("\\s+"))
      .filter { it.isNotBlank() }
      .joinToString(" ") { part ->
        part.replaceFirstChar { ch ->
          if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
        }
      }
      .ifEmpty { raw }

  // Convenience method returning null if a component name isn't valid
  fun safeComponentName(packageName: String, activityName: String): ComponentName? =
    if (packageName.isBlank() || activityName.isBlank()) null else ComponentName(packageName, activityName)
}
