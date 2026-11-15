package com.example.ui.components

import android.graphics.Path as AndroidPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Continuous-curvature Apple-style squircle (superellipse) shape for both Compose UI
 * and off-thread Android Bitmap clipping.
 */
class AppleSquircleShape(private val exponent: Float = 4.4f) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
  ): Outline {
    return Outline.Generic(buildComposeSquirclePath(size.width, size.height, exponent))
  }
}

object SquirclePathBuilder {
  private const val STEPS = 192

  fun buildComposePath(width: Float, height: Float, exponent: Float = 4.5f): Path =
    buildComposeSquirclePath(width, height, exponent)

  fun buildAndroidPath(width: Float, height: Float, exponent: Float = 4.5f): AndroidPath {
    val path = AndroidPath()
    if (width <= 0f || height <= 0f) return path

    val rx = width / 2f
    val ry = height / 2f
    val cx = rx
    val cy = ry
    val power = 2.0 / exponent.toDouble()

    for (i in 0..STEPS) {
      val theta = (2.0 * PI * i) / STEPS
      val cosT = cos(theta)
      val sinT = sin(theta)
      val x = cx + rx * sign(cosT).toFloat() * abs(cosT).pow(power).toFloat()
      val y = cy + ry * sign(sinT).toFloat() * abs(sinT).pow(power).toFloat()
      if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
  }
}

private fun buildComposeSquirclePath(width: Float, height: Float, exponent: Float): Path {
  val path = Path()
  if (width <= 0f || height <= 0f) return path

  val rx = width / 2f
  val ry = height / 2f
  val cx = rx
  val cy = ry
  val power = 2.0 / exponent.toDouble()
  val steps = 192

  for (i in 0..steps) {
    val theta = (2.0 * PI * i) / steps
    val cosT = cos(theta)
    val sinT = sin(theta)
    val x = cx + rx * sign(cosT).toFloat() * abs(cosT).pow(power).toFloat()
    val y = cy + ry * sign(sinT).toFloat() * abs(sinT).pow(power).toFloat()
    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
  }
  path.close()
  return path
}
