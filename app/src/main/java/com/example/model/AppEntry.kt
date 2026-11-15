package com.example.model

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
data class AppEntry(
  val packageName: String,
  val activityName: String,
  val componentKey: String,
  val label: String,
  val normalizedLabel: String,
  val acronym: String,
  val sectionChar: Char,
  val isSystemApp: Boolean,
  val androidCategory: Int = ApplicationInfo.CATEGORY_UNDEFINED,
  val launchCount: Int = 0,
  val lastLaunchedAt: Long = 0L,
) {
  companion object {
    fun buildNormalized(rawLabel: String): String =
      rawLabel.trim().lowercase(Locale.ROOT)

    fun buildAcronym(rawLabel: String): String {
      val words = rawLabel.trim().split(Regex("[\\s._-]+")).filter { it.isNotEmpty() }
      if (words.size <= 1) return ""
      return buildString(words.size) {
        words.forEach { append(it[0].lowercaseChar()) }
      }
    }

    fun buildSectionChar(rawLabel: String): Char {
      val first = rawLabel.trimStart().firstOrNull()?.uppercaseChar() ?: '#'
      return if (first in 'A'..'Z') first else '#'
    }
  }
}
