package com.example.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ImportedIconPackInfo(
  val id: String,
  val title: String,
  val iconCount: Int,
)

/**
 * Persists user-imported `.zip` / `.apk` icon packs inside app-private storage using Android's
 * zero-permission Storage Access Framework (`OpenDocument`). Supports both plain `appfilter.xml`
 * and compiled Android binary XML (`AXML`), plus raster PNG/WebP/JPG icon drawables.
 */
object ImportedIconPackStore {

  const val PACK_ID_IMPORTED_PREFIX = "imported:"

  private const val DIR_ROOT = "imported_icon_packs"
  private const val DIR_ICONS = "icons"
  private const val FILE_MANIFEST = "manifest.json"
  private const val MAX_SINGLE_ENTRY_BYTES = 16 * 1024 * 1024
  private const val MAX_PACK_ICONS = 3500

  fun isImportedPackId(packId: String): Boolean =
    packId.startsWith(PACK_ID_IMPORTED_PREFIX)

  fun extractSlug(packId: String): String? =
    if (isImportedPackId(packId)) {
      packId.removePrefix(PACK_ID_IMPORTED_PREFIX).takeIf { it.isNotBlank() }
    } else {
      null
    }

  private fun rootDir(context: Context): File =
    File(context.filesDir, DIR_ROOT).apply { mkdirs() }

  private fun packDir(context: Context, slug: String): File =
    File(rootDir(context), slug)

  fun listImportedPacks(context: Context): List<ImportedIconPackInfo> {
    val root = rootDir(context)
    val dirs = root.listFiles { file -> file.isDirectory } ?: return emptyList()
    val result = ArrayList<ImportedIconPackInfo>(dirs.size)
    for (dir in dirs) {
      val manifestFile = File(dir, FILE_MANIFEST)
      if (!manifestFile.isFile) continue
      runCatching {
        val json = JSONObject(manifestFile.readText(Charsets.UTF_8))
        val slug = json.optString("slug", dir.name)
        val title = json.optString("title", slug)
        val icons = json.optJSONArray("drawableNames")?.length() ?: 0
        if (icons > 0) {
          result.add(
            ImportedIconPackInfo(
              id = "$PACK_ID_IMPORTED_PREFIX$slug",
              title = title,
              iconCount = icons,
            )
          )
        }
      }
    }
    return result.sortedBy { it.title.lowercase(Locale.ROOT) }
  }

  fun deleteImportedPack(context: Context, packId: String): Boolean {
    val slug = extractSlug(packId) ?: return false
    val dir = packDir(context, slug)
    return if (dir.exists()) dir.deleteRecursively() else false
  }

  suspend fun importFromUri(context: Context, uri: Uri): Result<ImportedIconPackInfo> =
    withContext(Dispatchers.IO) {
      runCatching {
        val rawDisplayName = queryFileDisplayName(context, uri)
          ?.substringBeforeLast('.')
          ?.trim()
          ?.takeIf { it.isNotBlank() }
          ?: "Imported Pack"
        val cleanTitle = rawDisplayName
          .replace(Regex("[_\\-]+"), " ")
          .replace(Regex("\\s+"), " ")
          .trim()
          .take(40)
          .ifEmpty { "Imported Pack" }

        val slug = "${cleanTitle.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "pack" }}_${System.currentTimeMillis() % 100000}"
        val targetDir = packDir(context, slug)
        val iconsDir = File(targetDir, DIR_ICONS)
        if (targetDir.exists()) targetDir.deleteRecursively()
        iconsDir.mkdirs()

        val iconPriorities = HashMap<String, Int>()
        val iconFiles = HashMap<String, String>() // drawableName -> relative filename
        var plainAppFilterBytes: ByteArray? = null
        var binaryAppFilterBytes: ByteArray? = null
        var importedCount = 0

        val stream = context.contentResolver.openInputStream(uri)
          ?: throw IllegalStateException("Could not open selected icon pack file")

        stream.use { rawIn ->
          ZipInputStream(rawIn).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
              if (!entry.isDirectory) {
                val entryPath = entry.name.replace('\\', '/')
                val lowerPath = entryPath.lowercase(Locale.ROOT)
                val fileName = lowerPath.substringAfterLast('/')

                if (fileName == "appfilter.xml") {
                  val bytes = readLimitedZipBytes(zip)
                  if (bytes != null && bytes.isNotEmpty()) {
                    if (isBinaryXml(bytes)) {
                      if (binaryAppFilterBytes == null || lowerPath.contains("/xml/")) {
                        binaryAppFilterBytes = bytes
                      }
                    } else {
                      plainAppFilterBytes = bytes
                    }
                  }
                } else if (isSupportedIconImage(lowerPath) && importedCount < MAX_PACK_ICONS) {
                  val drawableName = fileName.substringBeforeLast('.')
                    .lowercase(Locale.ROOT)
                    .trim()
                  if (isImportableIconEntry(lowerPath, drawableName)) {
                    val priority = scoreDensityPath(lowerPath)
                    val existingPriority = iconPriorities[drawableName] ?: Int.MIN_VALUE
                    if (priority >= existingPriority) {
                      val bytes = readLimitedZipBytes(zip)
                      if (bytes != null && bytes.isNotEmpty() && isValidBitmapBytes(bytes)) {
                        val ext = fileName.substringAfterLast('.', "png")
                        val storedFileName = "$drawableName.$ext"
                        // Remove previous lower-DPI variant if extension differed
                        iconFiles[drawableName]?.let { prevName ->
                          if (prevName != storedFileName) {
                            File(iconsDir, prevName).delete()
                          }
                        }
                        File(iconsDir, storedFileName).writeBytes(bytes)
                        iconPriorities[drawableName] = priority
                        if (!iconFiles.containsKey(drawableName)) {
                          importedCount++
                        }
                        iconFiles[drawableName] = storedFileName
                      }
                    }
                  }
                }
              }
              zip.closeEntry()
              entry = zip.nextEntry
            }
          }
        }

        if (iconFiles.isEmpty()) {
          targetDir.deleteRecursively()
          throw IllegalStateException("No icon drawables found inside selected archive")
        }

        val parsedFilter: ParsedAppFilter = when {
          plainAppFilterBytes != null -> {
            runCatching {
              ExternalIconPackLoader.parseAppFilterXml(
                plainAppFilterBytes.toString(StandardCharsets.UTF_8)
              )
            }.getOrNull()
          }
          binaryAppFilterBytes != null -> {
            runCatching { parseBinaryAppFilter(binaryAppFilterBytes) }.getOrNull()
          }
          else -> null
        } ?: ParsedAppFilter(
          componentToDrawable = emptyMap(),
          packageToDrawable = emptyMap(),
          calendarPrefixes = emptyMap(),
          iconBacks = emptyList(),
          iconMask = null,
          iconUpon = null,
          scaleFactor = 0.80f,
        )

        val manifestJson = JSONObject().apply {
          put("slug", slug)
          put("title", cleanTitle)
          put("scaleFactor", parsedFilter.scaleFactor.toDouble())
          put("iconMask", parsedFilter.iconMask ?: "")
          put("iconUpon", parsedFilter.iconUpon ?: "")
          put("iconBacks", JSONArray(parsedFilter.iconBacks))
          put("componentToDrawable", JSONObject(parsedFilter.componentToDrawable))
          put("packageToDrawable", JSONObject(parsedFilter.packageToDrawable))
          put("calendarPrefixes", JSONObject(parsedFilter.calendarPrefixes))
          put("drawableNames", JSONArray(iconFiles.keys.sorted()))
          put("drawableFileMap", JSONObject(iconFiles))
        }
        File(targetDir, FILE_MANIFEST).writeText(manifestJson.toString(), Charsets.UTF_8)

        ImportedIconPackInfo(
          id = "$PACK_ID_IMPORTED_PREFIX$slug",
          title = cleanTitle,
          iconCount = iconFiles.size,
        )
      }
    }

  fun loadManifest(context: Context, packId: String): ImportedPackData? {
    val slug = extractSlug(packId) ?: return null
    val dir = packDir(context, slug)
    val manifestFile = File(dir, FILE_MANIFEST)
    if (!manifestFile.isFile) return null
    return runCatching {
      val json = JSONObject(manifestFile.readText(Charsets.UTF_8))
      val filter = ParsedAppFilter(
        componentToDrawable = json.optJSONObject("componentToDrawable").toStringMap(),
        packageToDrawable = json.optJSONObject("packageToDrawable").toStringMap(),
        calendarPrefixes = json.optJSONObject("calendarPrefixes").toStringMap(),
        iconBacks = json.optJSONArray("iconBacks").toStringList(),
        iconMask = json.optString("iconMask").takeIf { it.isNotBlank() },
        iconUpon = json.optString("iconUpon").takeIf { it.isNotBlank() },
        scaleFactor = json.optDouble("scaleFactor", 0.80).toFloat(),
      )
      val fileMap = json.optJSONObject("drawableFileMap").toStringMap()
      ImportedPackData(
        id = packId,
        title = json.optString("title", slug),
        iconsDir = File(dir, DIR_ICONS),
        appFilter = filter,
        drawableFileMap = fileMap,
      )
    }.getOrNull()
  }

  fun loadDrawable(context: Context, data: ImportedPackData, drawableName: String): Drawable? {
    val clean = drawableName.trim().lowercase(Locale.ROOT)
    if (clean.isEmpty()) return null
    val rel = data.drawableFileMap[clean] ?: return null
    val file = File(data.iconsDir, rel)
    if (!file.isFile) return null
    val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return null
    return BitmapDrawable(context.resources, bmp)
  }

  private fun isSupportedIconImage(lowerPath: String): Boolean =
    lowerPath.endsWith(".png") || lowerPath.endsWith(".webp") || lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")

  private fun isImportableIconEntry(lowerPath: String, drawableName: String): Boolean {
    if (drawableName.isBlank() || drawableName.startsWith("ic_launcher")) return false
    if (lowerPath.contains("mipmap")) return false
    return lowerPath.contains("drawable") || lowerPath.contains("assets/") || lowerPath.contains("icons/") || !lowerPath.contains('/')
  }

  private fun scoreDensityPath(lowerPath: String): Int = when {
    lowerPath.contains("xxxhdpi") -> 70
    lowerPath.contains("xxhdpi") -> 60
    lowerPath.contains("xhdpi") -> 50
    lowerPath.contains("nodpi") || lowerPath.contains("anydpi") -> 45
    lowerPath.contains("hdpi") -> 40
    lowerPath.contains("drawable/") || lowerPath.contains("icons/") || lowerPath.contains("assets/") -> 35
    lowerPath.contains("mdpi") -> 20
    else -> 10
  }

  private fun isValidBitmapBytes(bytes: ByteArray): Boolean {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    return opts.outWidth > 0 && opts.outHeight > 0
  }

  private fun readLimitedZipBytes(zip: ZipInputStream): ByteArray? {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(8192)
    var total = 0
    while (true) {
      val read = zip.read(buf)
      if (read <= 0) break
      total += read
      if (total > MAX_SINGLE_ENTRY_BYTES) return null
      out.write(buf, 0, read)
    }
    return out.toByteArray()
  }

  private fun queryFileDisplayName(context: Context, uri: Uri): String? {
    runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
          if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) return cursor.getString(idx)
          }
        }
    }
    return uri.lastPathSegment
  }

  private fun isBinaryXml(bytes: ByteArray): Boolean =
    bytes.size >= 4 &&
      (bytes[0].toInt() and 0xFF) == 0x03 &&
      (bytes[1].toInt() and 0xFF) == 0x00 &&
      (bytes[2].toInt() and 0xFF) == 0x08 &&
      (bytes[3].toInt() and 0xFF) == 0x00

  /**
   * Minimal compiled Android Binary XML (AXML) parser for APK `res/xml/appfilter.xml`.
   */
  fun parseBinaryAppFilter(bytes: ByteArray): ParsedAppFilter? {
    if (!isBinaryXml(bytes)) return null
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    if (buf.capacity() < 8) return null
    buf.position(8) // Skip RES_XML_TYPE header

    var strings: List<String> = emptyList()
    val componentToDrawable = HashMap<String, String>(256)
    val packageToDrawable = HashMap<String, String>(256)
    val calendarPrefixes = HashMap<String, String>(16)
    val iconBacks = ArrayList<String>(4)
    var iconMask: String? = null
    var iconUpon: String? = null
    var scaleFactor = 0.80f

    while (buf.remaining() >= 8) {
      val chunkStart = buf.position()
      val chunkType = buf.short.toInt() and 0xFFFF
      val headerSize = buf.short.toInt() and 0xFFFF
      val chunkSize = buf.int
      if (chunkSize < 8 || chunkStart + chunkSize > bytes.size) break

      when (chunkType) {
        0x0001 -> { // RES_STRING_POOL_TYPE
          strings = decodeAxmlStringPool(bytes, chunkStart, headerSize, chunkSize)
        }

        0x0102 -> { // RES_XML_START_ELEMENT_TYPE
          if (chunkStart + 36 <= bytes.size && strings.isNotEmpty()) {
            buf.position(chunkStart + 20)
            val nameIdx = buf.int
            val attrStartRel = buf.short.toInt() and 0xFFFF
            val attrSize = (buf.short.toInt() and 0xFFFF).coerceAtLeast(20)
            val attrCount = buf.short.toInt() and 0xFFFF
            val tagName = strings.getOrNull(nameIdx)?.lowercase(Locale.ROOT)
            val attrsBase = chunkStart + 16 + attrStartRel

            if (tagName != null && attrCount > 0) {
              val attrs = HashMap<String, String>(attrCount)
              for (i in 0 until attrCount) {
                val attrPos = attrsBase + i * attrSize
                if (attrPos + 20 > chunkStart + chunkSize) break
                buf.position(attrPos + 4)
                val attrNameIdx = buf.int
                val rawValueIdx = buf.int
                buf.position(attrPos + 15)
                val dataType = buf.get().toInt() and 0xFF
                val data = buf.int

                val attrName = strings.getOrNull(attrNameIdx)?.lowercase(Locale.ROOT) ?: continue
                val attrValue = when {
                  rawValueIdx in strings.indices -> strings[rawValueIdx]
                  dataType == 0x03 && data in strings.indices -> strings[data]
                  dataType == 0x04 -> Float.fromBits(data).toString()
                  dataType == 0x10 -> data.toString()
                  else -> null
                }
                if (attrValue != null) {
                  attrs[attrName] = attrValue.trim()
                }
              }

              when (tagName) {
                "item" -> {
                  val comp = attrs["component"]
                  val drawable = attrs["drawable"]
                  if (!comp.isNullOrEmpty() && !drawable.isNullOrEmpty()) {
                    parseComponentKey(comp)?.let { (pkgLower, compLower) ->
                      componentToDrawable.putIfAbsent(compLower, drawable)
                      packageToDrawable.putIfAbsent(pkgLower, drawable)
                    }
                  }
                }

                "calendar" -> {
                  val comp = attrs["component"]
                  val prefix = attrs["prefix"]
                  if (!comp.isNullOrEmpty() && !prefix.isNullOrEmpty()) {
                    parseComponentKey(comp)?.let { (pkgLower, compLower) ->
                      calendarPrefixes.putIfAbsent(compLower, prefix)
                      calendarPrefixes.putIfAbsent(pkgLower, prefix)
                    }
                  }
                }

                "iconback" -> {
                  attrs.entries
                    .filter { it.key.startsWith("img") && it.value.isNotEmpty() }
                    .forEach { iconBacks.add(it.value) }
                }

                "iconmask" -> {
                  iconMask = attrs["img1"] ?: attrs.values.firstOrNull { it.isNotEmpty() }
                }

                "iconupon" -> {
                  iconUpon = attrs["img1"] ?: attrs.values.firstOrNull { it.isNotEmpty() }
                }

                "scale" -> {
                  attrs["factor"]?.toFloatOrNull()?.takeIf { it in 0.25f..1.4f }?.let {
                    scaleFactor = it
                  }
                }
              }
            }
          }
        }
      }

      buf.position(chunkStart + chunkSize)
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

  private fun decodeAxmlStringPool(
    bytes: ByteArray,
    chunkStart: Int,
    headerSize: Int,
    chunkSize: Int,
  ): List<String> {
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    buf.position(chunkStart + 8)
    val stringCount = buf.int.coerceAtLeast(0)
    @Suppress("UNUSED_VARIABLE")
    val styleCount = buf.int
    val flags = buf.int
    val stringsStart = buf.int
    val isUtf8 = (flags and 0x100) != 0

    val offsetsPos = chunkStart + headerSize
    val poolBase = chunkStart + stringsStart
    val chunkEnd = chunkStart + chunkSize
    val out = ArrayList<String>(stringCount)

    for (i in 0 until stringCount) {
      val offPos = offsetsPos + i * 4
      if (offPos + 4 > chunkEnd) break
      val strRel = buf.getInt(offPos)
      var pos = poolBase + strRel
      if (pos < poolBase || pos >= chunkEnd) {
        out.add("")
        continue
      }

      if (isUtf8) {
        // Skip charLen varint (1 or 2 bytes)
        val c0 = bytes[pos].toInt() and 0xFF
        pos += if ((c0 and 0x80) != 0) 2 else 1
        if (pos >= chunkEnd) {
          out.add("")
          continue
        }
        val b0 = bytes[pos].toInt() and 0xFF
        val byteLen = if ((b0 and 0x80) != 0 && pos + 1 < chunkEnd) {
          val b1 = bytes[pos + 1].toInt() and 0xFF
          pos += 2
          ((b0 and 0x7F) shl 8) or b1
        } else {
          pos += 1
          b0
        }
        val safeLen = byteLen.coerceIn(0, chunkEnd - pos)
        out.add(String(bytes, pos, safeLen, StandardCharsets.UTF_8))
      } else {
        if (pos + 2 > chunkEnd) {
          out.add("")
          continue
        }
        val u0 = buf.getShort(pos).toInt() and 0xFFFF
        val charLen = if ((u0 and 0x8000) != 0 && pos + 4 <= chunkEnd) {
          val u1 = buf.getShort(pos + 2).toInt() and 0xFFFF
          pos += 4
          ((u0 and 0x7FFF) shl 16) or u1
        } else {
          pos += 2
          u0
        }
        val byteLen = (charLen * 2).coerceIn(0, chunkEnd - pos)
        out.add(String(bytes, pos, byteLen, StandardCharsets.UTF_16LE))
      }
    }
    return out
  }

  private fun parseComponentKey(component: String): Pair<String, String>? {
    val start = component.indexOf('{')
    val end = component.lastIndexOf('}')
    val inner = if (start >= 0 && end > start) component.substring(start + 1, end) else component
    val slash = inner.indexOf('/')
    if (slash <= 0 || slash >= inner.lastIndex) return null
    val pkg = inner.substring(0, slash).trim().lowercase(Locale.ROOT)
    val clsRaw = inner.substring(slash + 1).trim()
    val fullCls = if (clsRaw.startsWith(".")) "$pkg$clsRaw" else clsRaw
    return pkg to "$pkg/${fullCls.lowercase(Locale.ROOT)}"
  }

  private fun JSONObject?.toStringMap(): Map<String, String> {
    if (this == null) return emptyMap()
    val map = HashMap<String, String>(length())
    val keys = keys()
    while (keys.hasNext()) {
      val k = keys.next()
      map[k] = optString(k)
    }
    return map
  }

  private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { idx -> optString(idx).takeIf { it.isNotBlank() } }
  }
}

data class ImportedPackData(
  val id: String,
  val title: String,
  val iconsDir: File,
  val appFilter: ParsedAppFilter,
  val drawableFileMap: Map<String, String>,
)
