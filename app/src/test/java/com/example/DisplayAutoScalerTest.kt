package com.example

import com.example.display.DisplayAutoScaler
import com.example.display.DisplayTier
import com.example.display.HardwareDisplayInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayAutoScalerTest {

  @Test
  fun `compact phone gets 4 columns, multiline labels, and large squircle icons`() {
    val profile = DisplayAutoScaler.computeProfile(
      availableWidthDp = 320f,
      availableHeightDp = 640f,
      density = 2.0f,
      systemFontScale = 1.0f,
      hardwareInfo = HardwareDisplayInfo(640, 1280, 320, 60),
    )

    assertEquals(DisplayTier.COMPACT_PHONE, profile.tier)
    assertEquals(4, profile.columns)
    assertEquals(2, profile.labelMaxLines)
    assertTrue(profile.rows >= 3)
    assertEquals(profile.columns * profile.rows, profile.itemsPerPage)
    assertTrue(profile.iconSizeDp.value >= 52f)
    assertFalse(profile.isLandscape)
  }

  @Test
  fun `standard 1080x2400 phone gets 4 Apple-like columns and big squircle icons`() {
    val profile = DisplayAutoScaler.computeProfile(
      availableWidthDp = 412f,
      availableHeightDp = 915f,
      density = 2.625f,
      systemFontScale = 1.0f,
      hardwareInfo = HardwareDisplayInfo(1080, 2400, 420, 120),
    )

    assertEquals(DisplayTier.LARGE_PHONE, profile.tier)
    assertEquals(4, profile.columns)
    assertEquals(2, profile.labelMaxLines)
    assertTrue(profile.rows in 4..5)
    assertEquals(profile.columns * profile.rows, profile.itemsPerPage)
    assertTrue(profile.iconSizeDp.value in 60f..74f)
    assertTrue(profile.iconRenderPx >= 96)
  }

  @Test
  fun `wide tablet automatically scales up horizontal columns and big squircle icons`() {
    val profile = DisplayAutoScaler.computeProfile(
      availableWidthDp = 1024f,
      availableHeightDp = 768f,
      density = 2.0f,
      systemFontScale = 1.0f,
      hardwareInfo = HardwareDisplayInfo(2048, 1536, 320, 120),
    )

    assertEquals(DisplayTier.FOLDABLE_OR_SMALL_TABLET, profile.tier)
    assertTrue(profile.isLandscape)
    assertEquals(2, profile.labelMaxLines)
    assertTrue(profile.columns >= 6)
    assertTrue(profile.rows >= 3)
    assertTrue(profile.iconSizeDp.value >= 68f)
  }
}
