package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.max

private val Context.wallpaperDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "orbit_wallpaper_prefs"
)

enum class WallpaperPreset(val id: String) {
  ORBIT_NEBULA("orbit_nebula"),
  MIDNIGHT_AURORA("midnight_aurora"),
  OBSIDIAN_DUSK("obsidian_dusk"),
  EMERALD_MIST("emerald_mist"),
  NOCTURNE_SAPPHIRE("nocturne_sapphire"),
  CUSTOM_PHOTO("custom_photo");

  companion object {
    fun fromId(id: String?): WallpaperPreset =
      entries.firstOrNull { it.id == id } ?: ORBIT_NEBULA
  }
}

@Immutable
data class WallpaperState(
  val activePreset: WallpaperPreset = WallpaperPreset.ORBIT_NEBULA,
  val customBitmap: ImageBitmap? = null,
  val customVersion: Int = 0,
)

class WallpaperRepository(private val context: Context) {

  private val presetKey = stringPreferencesKey("active_wallpaper_preset")
  private val customWallpaperFile = File(context.filesDir, "orbit_custom_wallpaper.jpg")

  suspend fun loadWallpaperState(): WallpaperState = withContext(Dispatchers.IO) {
    val prefs = runCatching { context.wallpaperDataStore.data.first() }.getOrNull()
    val savedPreset = WallpaperPreset.fromId(prefs?.get(presetKey))
    val customBmp = if (customWallpaperFile.exists()) {
      decodeBitmapFile(customWallpaperFile)?.asImageBitmap()
    } else {
      null
    }
    val effectivePreset =
      if (savedPreset == WallpaperPreset.CUSTOM_PHOTO && customBmp == null) {
        WallpaperPreset.ORBIT_NEBULA
      } else {
        savedPreset
      }

    WallpaperState(
      activePreset = effectivePreset,
      customBitmap = customBmp,
      customVersion = if (customBmp != null) 1 else 0,
    )
  }

  suspend fun selectPreset(preset: WallpaperPreset) {
    withContext(Dispatchers.IO) {
      runCatching {
        context.wallpaperDataStore.edit { prefs ->
          prefs[presetKey] = preset.id
        }
      }
    }
  }

  suspend fun saveCustomPhotoFromUri(uri: Uri): ImageBitmap? = withContext(Dispatchers.IO) {
    val decoded = decodeScaledUri(uri, maxDimensionPx = 2560) ?: return@withContext null
    runCatching {
      FileOutputStream(customWallpaperFile).use { out ->
        decoded.compress(Bitmap.CompressFormat.JPEG, 92, out)
      }
      context.wallpaperDataStore.edit { prefs ->
        prefs[presetKey] = WallpaperPreset.CUSTOM_PHOTO.id
      }
    }.onFailure {
      decoded.recycle()
      return@withContext null
    }
    decoded.asImageBitmap()
  }

  private fun decodeBitmapFile(file: File): Bitmap? = runCatching {
    BitmapFactory.decodeFile(file.absolutePath)
  }.getOrNull()

  private fun decodeScaledUri(uri: Uri, maxDimensionPx: Int): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val source = ImageDecoder.createSource(context.contentResolver, uri)
      ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val srcW = info.size.width
        val srcH = info.size.height
        val longest = max(srcW, srcH)
        if (longest > maxDimensionPx && longest > 0) {
          val scale = maxDimensionPx.toFloat() / longest.toFloat()
          decoder.setTargetSize(
            (srcW * scale).toInt().coerceAtLeast(1),
            (srcH * scale).toInt().coerceAtLeast(1),
          )
        }
      }
    } else {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
      }
      var sampleSize = 1
      val longest = max(bounds.outWidth, bounds.outHeight)
      while (longest / sampleSize > maxDimensionPx) {
        sampleSize *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOpts)
      }
    }
  }.getOrNull()
}
