package com.example.display

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class DisplayTier(val label: String) {
  COMPACT_PHONE("Compact"),
  STANDARD_PHONE("Standard"),
  LARGE_PHONE("Large"),
  FOLDABLE_OR_SMALL_TABLET("Foldable"),
  EXPANDED_TABLET("Tablet"),
}

@Immutable
data class DisplayProfile(
  val tier: DisplayTier,
  val isLandscape: Boolean,
  val availableWidthDp: Float,
  val availableHeightDp: Float,
  val screenWidthPx: Int,
  val screenHeightPx: Int,
  val densityDpi: Int,
  val density: Float,
  val systemFontScale: Float,
  val refreshRateHz: Int,
  val columns: Int,
  val rows: Int,
  val itemsPerPage: Int,
  val dockColumns: Int,
  val iconSizeDp: Dp,
  val dockIconSizeDp: Dp,
  val iconRenderPx: Int,
  val labelFontSp: TextUnit,
  val labelLineHeightSp: TextUnit,
  val labelMaxLines: Int,
  val searchFontSp: TextUnit,
  val clockFontSp: TextUnit,
  val dateFontSp: TextUnit,
  val horizontalPaddingDp: Dp,
  val gridHorizontalSpacingDp: Dp,
  val gridVerticalSpacingDp: Dp,
)

object DisplayAutoScaler {

  fun resolveHardwareMetrics(context: Context): HardwareDisplayInfo {
    val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    val refreshRate =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching { context.display?.refreshRate?.roundToInt() }.getOrNull() ?: 60
      } else {
        @Suppress("DEPRECATION")
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.refreshRate?.roundToInt() ?: 60
      }

    return HardwareDisplayInfo(
      screenWidthPx = displayMetrics.widthPixels,
      screenHeightPx = displayMetrics.heightPixels,
      densityDpi = displayMetrics.densityDpi,
      refreshRateHz = refreshRate.coerceIn(60, 240),
    )
  }

  fun computeProfile(
    availableWidthDp: Float,
    availableHeightDp: Float,
    density: Float,
    systemFontScale: Float,
    hardwareInfo: HardwareDisplayInfo,
  ): DisplayProfile {
    val safeWidthDp = availableWidthDp.coerceAtLeast(280f)
    val safeHeightDp = availableHeightDp.coerceAtLeast(320f)
    val isLandscape = safeWidthDp > safeHeightDp * 1.08f
    val shortestSideDp = minOf(safeWidthDp, safeHeightDp)

    val tier =
      when {
        shortestSideDp < 360f -> DisplayTier.COMPACT_PHONE
        shortestSideDp < 410f -> DisplayTier.STANDARD_PHONE
        shortestSideDp < 600f -> DisplayTier.LARGE_PHONE
        shortestSideDp < 840f -> DisplayTier.FOLDABLE_OR_SMALL_TABLET
        else -> DisplayTier.EXPANDED_TABLET
      }

    val horizontalPaddingDp =
      when {
        safeWidthDp < 360f -> 12.dp
        safeWidthDp < 540f -> 18.dp
        else -> 30.dp
      }

    val usableGridWidthDp = (safeWidthDp - (horizontalPaddingDp.value * 2f)).coerceAtLeast(240f)

    val targetCellWidthDp =
      when (tier) {
        DisplayTier.COMPACT_PHONE -> 76f
        DisplayTier.STANDARD_PHONE -> 84f
        DisplayTier.LARGE_PHONE -> 88f
        DisplayTier.FOLDABLE_OR_SMALL_TABLET -> 104f
        DisplayTier.EXPANDED_TABLET -> 118f
      }

    val rawColumns = (usableGridWidthDp / targetCellWidthDp).roundToInt()
    val columns =
      when {
        safeWidthDp < 440f -> rawColumns.coerceIn(4, 4)
        safeWidthDp < 600f -> rawColumns.coerceIn(4, 5)
        safeWidthDp < 840f -> rawColumns.coerceIn(5, 6)
        else -> rawColumns.coerceIn(6, 8)
      }

    val gridHorizontalSpacingDp = if (safeWidthDp < 380f) 8.dp else 12.dp
    val totalHSpacingDp = gridHorizontalSpacingDp.value * (columns - 1)
    val actualCellWidthDp = ((usableGridWidthDp - totalHSpacingDp) / columns).coerceAtLeast(56f)

    val minIconForTier = if (safeWidthDp < 350f) 52f else 58f
    val maxIconForTier = if (tier >= DisplayTier.FOLDABLE_OR_SMALL_TABLET) 84f else 74f
    val computedIconDp = (actualCellWidthDp * 0.80f).coerceIn(minIconForTier, maxIconForTier)
    val iconSizeDp = computedIconDp.roundToInt().dp
    val dockIconSizeDp = (computedIconDp + 2f).coerceAtMost(84f).roundToInt().dp

    val iconRenderPx = ((dockIconSizeDp.value * density) * 1.25f).roundToInt().coerceIn(96, 384)

    val effectiveScaleFactor = (actualCellWidthDp / 86f).coerceIn(0.90f, 1.20f)
    val fontGuard = if (systemFontScale > 1.25f) 1.25f / systemFontScale else 1f
    val labelSize = (12.0f * effectiveScaleFactor * fontGuard).coerceIn(11.0f, 14.0f)
    val labelFontSp = labelSize.sp
    val labelLineHeightSp = (labelSize * 1.20f).sp
    val labelMaxLines = 2
    val searchFontSp = (15.0f * effectiveScaleFactor).coerceIn(14f, 17f).sp

    val clockSize =
      when {
        safeHeightDp < 480f -> 28f
        tier == DisplayTier.COMPACT_PHONE -> 36f
        tier == DisplayTier.STANDARD_PHONE -> 40f
        tier == DisplayTier.LARGE_PHONE -> 44f
        else -> 50f
      } * fontGuard
    val dateSize = (clockSize * 0.34f).coerceIn(13f, 17f)

    // Reserve height for top search bar (~66dp) and bottom indicator + dock
    val searchBarEstimatedDp = if (safeHeightDp < 500f) 56f else 68f
    val indicatorAndDockDp = dockIconSizeDp.value + 64f
    val gridAvailableHeightDp = (safeHeightDp - searchBarEstimatedDp - indicatorAndDockDp).coerceAtLeast(160f)

    // Reserve space for 2-line labels cleanly (~35dp below icon)
    val cellHeightDp = iconSizeDp.value + 40f
    val minRowGapDp = if (safeHeightDp < 520f) 8f else 12f

    val rawRows =
      ((gridAvailableHeightDp - minRowGapDp) / (cellHeightDp + minRowGapDp)).toInt()
    val rows =
      if (isLandscape && safeHeightDp < 520f) {
        rawRows.coerceIn(2, 3)
      } else {
        rawRows.coerceIn(3, 5)
      }

    val leftoverHeightDp = (gridAvailableHeightDp - (rows * cellHeightDp)).coerceAtLeast(0f)
    val balancedRowGapDp = (leftoverHeightDp / (rows + 1)).coerceIn(8f, 26f)
    val gridVerticalSpacingDp = balancedRowGapDp.roundToInt().dp

    val dockColumns =
      when {
        safeWidthDp < 520f -> columns.coerceIn(4, 4)
        else -> columns.coerceIn(5, 6)
      }

    return DisplayProfile(
      tier = tier,
      isLandscape = isLandscape,
      availableWidthDp = safeWidthDp,
      availableHeightDp = safeHeightDp,
      screenWidthPx = hardwareInfo.screenWidthPx,
      screenHeightPx = hardwareInfo.screenHeightPx,
      densityDpi = hardwareInfo.densityDpi,
      density = density,
      systemFontScale = systemFontScale,
      refreshRateHz = hardwareInfo.refreshRateHz,
      columns = columns,
      rows = rows,
      itemsPerPage = columns * rows,
      dockColumns = dockColumns,
      iconSizeDp = iconSizeDp,
      dockIconSizeDp = dockIconSizeDp,
      iconRenderPx = iconRenderPx,
      labelFontSp = labelFontSp,
      labelLineHeightSp = labelLineHeightSp,
      labelMaxLines = labelMaxLines,
      searchFontSp = searchFontSp,
      clockFontSp = clockSize.sp,
      dateFontSp = dateSize.sp,
      horizontalPaddingDp = horizontalPaddingDp,
      gridHorizontalSpacingDp = gridHorizontalSpacingDp,
      gridVerticalSpacingDp = gridVerticalSpacingDp,
    )
  }
}

@Immutable
data class HardwareDisplayInfo(
  val screenWidthPx: Int,
  val screenHeightPx: Int,
  val densityDpi: Int,
  val refreshRateHz: Int,
)
