package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.ui.LauncherScreen
import com.example.ui.LauncherViewModel
import com.example.ui.theme.OrbitTheme

class MainActivity : ComponentActivity() {

  private val viewModel: LauncherViewModel by viewModels()
  private var homePressSignal by mutableIntStateOf(0)

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    super.onCreate(savedInstanceState)
    enableHighestRefreshRate()
    disableSystemSlideTransition()
    setContent {
      OrbitTheme {
        LauncherScreen(
          viewModel = viewModel,
          homePressSignal = homePressSignal,
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    disableSystemSlideTransition()
    if (intent.action == Intent.ACTION_MAIN) {
      homePressSignal++
    }
  }

  override fun onResume() {
    super.onResume()
    disableSystemSlideTransition()
    viewModel.onReturnedToLauncher()
    viewModel.refreshApps()
  }

  override fun onPause() {
    super.onPause()
    disableSystemSlideTransition()
  }

  private fun enableHighestRefreshRate() {
    runCatching {
      val currentDisplay: Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          display
        } else {
          @Suppress("DEPRECATION")
          windowManager.defaultDisplay
        }

      if (currentDisplay != null) {
        val currentMode = currentDisplay.mode
        val bestMode = currentDisplay.supportedModes
          .filter {
            it.physicalWidth == currentMode.physicalWidth &&
              it.physicalHeight == currentMode.physicalHeight
          }
          .maxByOrNull { it.refreshRate }

        val lp = window.attributes
        if (bestMode != null) {
          lp.preferredDisplayModeId = bestMode.modeId
          lp.preferredRefreshRate = bestMode.refreshRate
        }
        window.attributes = lp
      }
    }
  }

  private fun disableSystemSlideTransition() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.orbit_instant_none, R.anim.orbit_instant_none)
      overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.orbit_instant_none, R.anim.orbit_instant_none)
    } else {
      @Suppress("DEPRECATION")
      overridePendingTransition(0, 0)
    }
  }
}
