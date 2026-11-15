package com.example.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.example.R
import kotlin.math.roundToInt

enum class OrbitPackStyle {
  FUSION,
  OREO_CONTRAST,
  OBSIDIAN_DARK,
  IPHONE,
}

/**
 * Renders Orbit's three built-in Material icon styles into 2x supersampled raw bitmaps
 * ready for Orbit's squircle mask, glass sheen, and molded inner edge bevel:
 * - FUSION: Apple x Android luminous multi-stop diagonal gradient + frosted halo
 * - OREO_CONTRAST: Android 8.0 Oreo high-contrast Material plate + 45-degree elevation shadow
 * - OBSIDIAN_DARK: Deep obsidian backdrop + colored atmospheric glow + neon-rimmed medallion
 */
object FusionIconRenderer {

  // Multicolor brand glyphs that look sharpest on Oreo's pure ceramic-white adaptive plate
  private val OREO_WHITE_PLATE_DRAWABLES: Set<Int> = setOf(
    R.drawable.ic_fusion_chrome,
    R.drawable.ic_fusion_photos,
    R.drawable.ic_fusion_maps,
    R.drawable.ic_fusion_playstore,
    R.drawable.ic_fusion_gemini,
    R.drawable.ic_fusion_assistant,
    R.drawable.ic_fusion_search,
  )

  fun renderRawCanvasBitmap(
    context: Context,
    spec: FusionIconSpec,
    superPx: Int,
    style: OrbitPackStyle = OrbitPackStyle.FUSION,
  ): Bitmap? = when (style) {
    OrbitPackStyle.FUSION -> renderFusionStyle(context, spec, superPx)
    OrbitPackStyle.OREO_CONTRAST -> renderOreoContrastStyle(context, spec, superPx)
    OrbitPackStyle.OBSIDIAN_DARK -> renderObsidianDarkStyle(context, spec, superPx)
    OrbitPackStyle.IPHONE -> renderIphoneStyle(context, spec, superPx)
  }

  private fun renderFusionStyle(
    context: Context,
    spec: FusionIconSpec,
    superPx: Int,
  ): Bitmap? {
    val fgDrawable = ContextCompat.getDrawable(context, spec.drawableRes) ?: return null
    val bitmap = Bitmap.createBitmap(superPx, superPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fSize = superPx.toFloat()

    // 1. Multi-stop diagonal Apple-inspired luminous gradient backdrop
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        0f,
        0f,
        fSize,
        fSize,
        intArrayOf(spec.gradientStart, spec.gradientMid, spec.gradientEnd),
        floatArrayOf(0f, 0.52f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRect(0f, 0f, fSize, fSize, bgPaint)

    // 2. Upper-left soft Apple specular bloom & lower-right tonal depth
    val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = RadialGradient(
        fSize * 0.28f,
        fSize * 0.24f,
        fSize * 0.72f,
        intArrayOf(spec.haloGlow, AndroidColor.TRANSPARENT),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRect(0f, 0f, fSize, fSize, bloomPaint)

    val depthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        0f,
        fSize * 0.45f,
        0f,
        fSize,
        intArrayOf(AndroidColor.TRANSPARENT, AndroidColor.argb(46, 0, 0, 0)),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRect(0f, 0f, fSize, fSize, depthPaint)

    // 3. Soft Material-You frosted inner halo disc behind the glyph
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = AndroidColor.argb(26, 255, 255, 255)
    }
    canvas.drawCircle(fSize * 0.5f, fSize * 0.5f, fSize * 0.36f, haloPaint)

    // 4. Crisp Material-Fusion vector glyph sized cleanly (8% inset -> generous 84% visual frame)
    val inset = (superPx * 0.08f).roundToInt()
    fgDrawable.setBounds(inset, inset, superPx - inset, superPx - inset)
    fgDrawable.draw(canvas)

    // 5. Optional sleek variant pill badge
    val badge = spec.badgeText
    if (!badge.isNullOrBlank()) {
      drawVariantBadge(canvas, badge, fSize, isObsidian = false)
    }

    return bitmap
  }

  /**
   * Android 8.0 Oreo better-contrast style: bold high-contrast surface, paper-edge lip,
   * crisp 45-degree Material shadow cast behind the emblem, and high-visibility foreground.
   */
  private fun renderOreoContrastStyle(
    context: Context,
    spec: FusionIconSpec,
    superPx: Int,
  ): Bitmap? {
    val fgDrawable = ContextCompat.getDrawable(context, spec.drawableRes)?.mutate() ?: return null
    val bitmap = Bitmap.createBitmap(superPx, superPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fSize = superPx.toFloat()

    val usesWhitePlate = spec.drawableRes in OREO_WHITE_PLATE_DRAWABLES

    if (usesWhitePlate) {
      // Crisp Oreo pure ceramic-white adaptive plate
      val whitePlatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
          0f,
          0f,
          0f,
          fSize,
          intArrayOf(0xFFFFFFFF.toInt(), 0xFFF1F5F9.toInt(), 0xFFE2E8F0.toInt()),
          floatArrayOf(0f, 0.72f, 1f),
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRect(0f, 0f, fSize, fSize, whitePlatePaint)

      // Subtle Oreo tinted inner band on top for depth
      val subtleTint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = withAlpha(spec.gradientMid, 16)
      }
      canvas.drawRect(0f, 0f, fSize, fSize, subtleTint)
    } else {
      // High-contrast deepened Material 700/800 Oreo plate so white foregrounds pop sharply
      val topContrast = deepenForContrast(spec.gradientStart, minDarken = 0.76f)
      val bottomContrast = deepenForContrast(spec.gradientEnd, minDarken = 0.56f)
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
          0f,
          0f,
          fSize * 0.25f,
          fSize,
          intArrayOf(topContrast, bottomContrast),
          floatArrayOf(0f, 1f),
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRect(0f, 0f, fSize, fSize, bgPaint)

      // Oreo Material crisp central circular well for strong foreground/background contrast
      val wellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(30, 0, 0, 0)
      }
      canvas.drawCircle(fSize * 0.5f, fSize * 0.5f, fSize * 0.375f, wellPaint)
    }

    // Top 1dp Material paper edge highlight & bottom shade
    val topLip = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        0f,
        0f,
        0f,
        fSize * 0.08f,
        intArrayOf(AndroidColor.argb(54, 255, 255, 255), AndroidColor.TRANSPARENT),
        null,
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRect(0f, 0f, fSize, fSize * 0.08f, topLip)

    val inset = (superPx * 0.10f).roundToInt()

    // Draw 45-degree Material Oreo long-shadow cast underneath the foreground glyph
    val shadowBitmap = Bitmap.createBitmap(superPx, superPx, Bitmap.Config.ARGB_8888)
    val shadowCanvas = Canvas(shadowBitmap)
    val shadowMask = ContextCompat.getDrawable(context, spec.drawableRes)?.mutate()
    if (shadowMask != null) {
      shadowMask.colorFilter = PorterDuffColorFilter(
        if (usesWhitePlate) AndroidColor.argb(48, 15, 23, 42) else AndroidColor.argb(82, 0, 0, 0),
        PorterDuff.Mode.SRC_IN,
      )
      shadowMask.setBounds(inset, inset, superPx - inset, superPx - inset)
      shadowMask.draw(shadowCanvas)

      val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
      val steps = 6
      val stepOffset = (superPx * 0.011f).coerceAtLeast(1.5f)
      for (i in steps downTo 1) {
        shadowPaint.alpha = ((220f * (1f - i / (steps + 1.5f))) / steps * 2.4f)
          .roundToInt()
          .coerceIn(12, 95)
        canvas.drawBitmap(shadowBitmap, i * stepOffset, i * stepOffset, shadowPaint)
      }
    }
    shadowBitmap.recycle()

    // Crisp foreground emblem
    fgDrawable.setBounds(inset, inset, superPx - inset, superPx - inset)
    fgDrawable.draw(canvas)

    val badge = spec.badgeText
    if (!badge.isNullOrBlank()) {
      drawVariantBadge(canvas, badge, fSize, isObsidian = false)
    }

    return bitmap
  }

  /**
   * Obsidian Dark style: matches Orbit's custom Obsidian Thally Ace XML aesthetic
   * with deep #080B12->#142033 gradient, brand neon atmospheric glow, and glowing rim medallion.
   */
  private fun renderObsidianDarkStyle(
    context: Context,
    spec: FusionIconSpec,
    superPx: Int,
  ): Bitmap? {
    val fgDrawable = ContextCompat.getDrawable(context, spec.drawableRes)?.mutate() ?: return null
    val bitmap = Bitmap.createBitmap(superPx, superPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fSize = superPx.toFloat()

    // 1. Deep Obsidian backdrop (aligned with ic_thally_* drawables)
    val obsidianBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        0f,
        0f,
        fSize,
        fSize,
        intArrayOf(0xFF070A11.toInt(), 0xFF0D1423.toInt(), 0xFF142034.toInt()),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRect(0f, 0f, fSize, fSize, obsidianBg)

    // 2. Signature brand atmospheric glow
    val glowPrimary = boostObsidianNeon(spec.gradientStart)
    val glowSecondary = boostObsidianNeon(spec.gradientMid)

    val radialGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = RadialGradient(
        fSize * 0.50f,
        fSize * 0.38f,
        fSize * 0.68f,
        intArrayOf(
          withAlpha(glowPrimary, 92),
          withAlpha(glowSecondary, 36),
          AndroidColor.TRANSPARENT,
        ),
        floatArrayOf(0f, 0.48f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRect(0f, 0f, fSize, fSize, radialGlow)

    // 3. Recessed Obsidian dark glass medallion + neon hairline rim
    val radius = fSize * 0.375f
    val medFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        fSize * 0.2f,
        fSize * 0.18f,
        fSize * 0.8f,
        fSize * 0.85f,
        intArrayOf(0xFF162136.toInt(), 0xFF0A101C.toInt()),
        null,
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawCircle(fSize * 0.5f, fSize * 0.5f, radius, medFill)

    val neonBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = (fSize * 0.020f).coerceAtLeast(2.4f)
      shader = LinearGradient(
        fSize * 0.18f,
        fSize * 0.18f,
        fSize * 0.82f,
        fSize * 0.82f,
        intArrayOf(withAlpha(glowPrimary, 215), withAlpha(glowSecondary, 165)),
        null,
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawCircle(fSize * 0.5f, fSize * 0.5f, radius, neonBorder)

    // 4. Subtle tinted neon bloom behind foreground glyph + crisp emblem
    val inset = (superPx * 0.12f).roundToInt()
    val glowMask = ContextCompat.getDrawable(context, spec.drawableRes)?.mutate()
    if (glowMask != null && spec.drawableRes !in OREO_WHITE_PLATE_DRAWABLES) {
      glowMask.colorFilter = PorterDuffColorFilter(withAlpha(glowPrimary, 110), PorterDuff.Mode.SRC_IN)
      val gInset = (inset - fSize * 0.018f).roundToInt()
      glowMask.setBounds(gInset, gInset, superPx - gInset, superPx - gInset)
      glowMask.draw(canvas)
    }

    fgDrawable.setBounds(inset, inset, superPx - inset, superPx - inset)
    fgDrawable.draw(canvas)

    val badge = spec.badgeText
    if (!badge.isNullOrBlank()) {
      drawVariantBadge(canvas, badge, fSize, isObsidian = true, accentColor = glowPrimary)
    }

    return bitmap
  }

  /**
   * iPhone (iOS) style: clean top-to-bottom Apple vertical gradients or bright iOS ceramic-white
   * plates for multicolor emblems, top specular glass edge sheen, and soft vertical ambient drop.
   */
  private fun renderIphoneStyle(
    context: Context,
    spec: FusionIconSpec,
    superPx: Int,
  ): Bitmap? {
    val fgDrawable = ContextCompat.getDrawable(context, spec.drawableRes)?.mutate() ?: return null
    val bitmap = Bitmap.createBitmap(superPx, superPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fSize = superPx.toFloat()

    val usesWhitePlate = spec.drawableRes in OREO_WHITE_PLATE_DRAWABLES

    if (usesWhitePlate) {
      val whitePlatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
          0f,
          0f,
          0f,
          fSize,
          intArrayOf(0xFFFFFFFF.toInt(), 0xFFF7F8FA.toInt(), 0xFFECEFF4.toInt()),
          floatArrayOf(0f, 0.70f, 1f),
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRect(0f, 0f, fSize, fSize, whitePlatePaint)
    } else {
      val (iosTop, iosBottom) = resolveIphoneVerticalPalette(spec)
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
          0f,
          0f,
          0f,
          fSize,
          intArrayOf(iosTop, iosBottom),
          floatArrayOf(0f, 1f),
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRect(0f, 0f, fSize, fSize, bgPaint)
    }

    // Classic iOS top-edge glossy highlight
    val topGlossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        0f,
        0f,
        0f,
        fSize * 0.36f,
        intArrayOf(
          if (usesWhitePlate) AndroidColor.argb(65, 255, 255, 255) else AndroidColor.argb(46, 255, 255, 255),
          AndroidColor.TRANSPARENT,
        ),
        null,
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRect(0f, 0f, fSize, fSize * 0.36f, topGlossPaint)

    val inset = (superPx * 0.12f).roundToInt()

    // Subtle iOS vertical ambient shadow directly under the glyph (no diagonal Material cast)
    val ambientMask = ContextCompat.getDrawable(context, spec.drawableRes)?.mutate()
    if (ambientMask != null) {
      ambientMask.colorFilter = PorterDuffColorFilter(
        if (usesWhitePlate) AndroidColor.argb(36, 15, 23, 42) else AndroidColor.argb(64, 0, 0, 0),
        PorterDuff.Mode.SRC_IN,
      )
      ambientMask.setBounds(inset, inset, superPx - inset, superPx - inset)
      val shadowBitmap = Bitmap.createBitmap(superPx, superPx, Bitmap.Config.ARGB_8888)
      ambientMask.draw(Canvas(shadowBitmap))
      val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
      canvas.drawBitmap(shadowBitmap, 0f, (superPx * 0.018f).coerceAtLeast(2f), shadowPaint)
      shadowBitmap.recycle()
    }

    fgDrawable.setBounds(inset, inset, superPx - inset, superPx - inset)
    fgDrawable.draw(canvas)

    val badge = spec.badgeText
    if (!badge.isNullOrBlank()) {
      drawVariantBadge(canvas, badge, fSize, isObsidian = false)
    }

    return bitmap
  }

  private fun resolveIphoneVerticalPalette(spec: FusionIconSpec): Pair<Int, Int> = when (spec.category) {
    AppCategory.SMS_MESSAGING,
    AppCategory.DIALER, -> 0xFF42E66B.toInt() to 0xFF00A83F.toInt()
    AppCategory.CAMERA -> 0xFF565B64.toInt() to 0xFF21242B.toInt()
    AppCategory.SETTINGS_SYSTEM -> 0xFF8C929D.toInt() to 0xFF4F5560.toInt()
    AppCategory.CLOCK -> 0xFF282B33.toInt() to 0xFF101216.toInt()
    AppCategory.CALCULATOR -> 0xFF3A3E47.toInt() to 0xFF1A1C22.toInt()
    AppCategory.WEATHER -> 0xFF56BDFB.toInt() to 0xFF1C64E8.toInt()
    AppCategory.CALENDAR -> 0xFFFF453A.toInt() to 0xFFD70015.toInt()
    AppCategory.NOTES_TASKS -> 0xFFF59E0B.toInt() to 0xFFB45309.toInt()
    AppCategory.MUSIC_AUDIO -> 0xFFFF5A79.toInt() to 0xFFFA233B.toInt()
    AppCategory.WALLET_FINANCE -> 0xFF1E2430.toInt() to 0xFF0A0D14.toInt()
    else -> boostIphoneVerticalTop(spec.gradientStart) to deepenForIosContrast(spec.gradientEnd)
  }

  private fun boostIphoneVerticalTop(color: Int): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color, hsv)
    hsv[1] = hsv[1].coerceIn(0.42f, 0.92f)
    hsv[2] = hsv[2].coerceIn(0.54f, 0.86f)
    return AndroidColor.HSVToColor(255, hsv)
  }

  private fun deepenForIosContrast(color: Int): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color, hsv)
    hsv[1] = (hsv[1] * 1.08f).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * 0.78f).coerceIn(0.24f, 0.72f)
    return AndroidColor.HSVToColor(255, hsv)
  }

  private fun deepenForContrast(color: Int, minDarken: Float): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color, hsv)
    hsv[1] = (hsv[1] * 1.18f).coerceIn(0f, 1f)
    // Prevent bright yellow/cyan plates from washing out pure white Oreo foregrounds
    hsv[2] = (hsv[2] * minDarken).coerceIn(0.18f, 0.66f)
    return AndroidColor.HSVToColor(255, hsv)
  }

  private fun boostObsidianNeon(color: Int): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color, hsv)
    hsv[1] = hsv[1].coerceAtLeast(0.55f)
    hsv[2] = hsv[2].coerceAtLeast(0.78f)
    return AndroidColor.HSVToColor(255, hsv)
  }

  private fun withAlpha(color: Int, alpha: Int): Int =
    AndroidColor.argb(
      alpha.coerceIn(0, 255),
      AndroidColor.red(color),
      AndroidColor.green(color),
      AndroidColor.blue(color),
    )

  private fun drawVariantBadge(
    canvas: Canvas,
    text: String,
    sizePx: Float,
    isObsidian: Boolean,
    accentColor: Int = 0xFFFFFFFF.toInt(),
  ) {
    val textSize = sizePx * 0.135f
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = AndroidColor.WHITE
      this.textSize = textSize
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    val textWidth = textPaint.measureText(text)
    val padH = sizePx * 0.055f
    val pillW = (textWidth + padH * 2f).coerceAtLeast(sizePx * 0.22f)
    val pillH = sizePx * 0.19f
    val right = sizePx * 0.87f
    val bottom = sizePx * 0.87f
    val rect = RectF(right - pillW, bottom - pillH, right, bottom)
    val radius = pillH * 0.5f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = if (isObsidian) AndroidColor.argb(240, 10, 14, 24) else AndroidColor.argb(232, 12, 18, 32)
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = (sizePx * 0.012f).coerceAtLeast(1.5f)
      color = if (isObsidian) withAlpha(accentColor, 200) else AndroidColor.argb(185, 255, 255, 255)
    }
    canvas.drawRoundRect(rect, radius, radius, bgPaint)
    canvas.drawRoundRect(rect, radius, radius, strokePaint)

    val fm = textPaint.fontMetrics
    val textY = rect.centerY() - ((fm.ascent + fm.descent) * 0.5f)
    canvas.drawText(text, rect.centerX(), textY, textPaint)
  }

  @Suppress("unused")
  fun isWhitePlateOreoDrawable(@DrawableRes resId: Int): Boolean =
    resId in OREO_WHITE_PLATE_DRAWABLES
}
