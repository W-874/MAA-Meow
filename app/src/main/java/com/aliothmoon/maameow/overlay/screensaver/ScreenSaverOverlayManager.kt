package com.aliothmoon.maameow.overlay.screensaver

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.service.MaaSessionLogger
import com.aliothmoon.maameow.theme.MaaNyanTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class ScreenSaverOverlayManager(
    private val context: Context,
    private val sessionLogger: MaaSessionLogger
) : LifecycleOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var insetsController: WindowInsetsControllerCompat? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    private val _showing = MutableStateFlow(false)
    val showing: StateFlow<Boolean> = _showing.asStateFlow()

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun show(activity: Activity? = null) {
        if (_showing.value) return
        activity?.window?.let { window ->
            enterImmersiveMode(window)
        }

        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@ScreenSaverOverlayManager)
            setViewTreeSavedStateRegistryOwner(this@ScreenSaverOverlayManager)

            setContent {
                MaaNyanTheme(themeMode = AppSettingsManager.ThemeMode.DARK) {
                    val baseDensity = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(
                            density = baseDensity.density,
                            fontScale = baseDensity.fontScale.coerceIn(0.85f, 1.3f)
                        )
                    ) {
                        ScreenSaverView(
                            sessionLogger = sessionLogger,
                            onUnlock = { hide() }
                        )
                    }
                }
            }
        }

        val layoutParams = createLayoutParams()
        try {
            windowManager.addView(composeView, layoutParams)
            composeView?.let { view ->
                applyLegacyImmersiveFlags(view)
                // The bottom-sheet window is disposed asynchronously and can restore system bars.
                // Reapply immersive mode once the screen-saver overlay is attached.
                activity?.window?.let { window ->
                    view.post { enterImmersiveMode(window) }
                }
            }
            // 排除底部手势区域，防止系统后退手势拦截横向滑动
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                composeView?.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                    val barHeightPx = (v.resources.displayMetrics.density * 120).toInt()
                    v.systemGestureExclusionRects =
                        listOf(Rect(0, v.height - barHeightPx, v.width, v.height))
                }
            }
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            _showing.value = true
        } catch (e: Exception) {
            Timber.e(e, "Failed to show screen saver overlay")
            hide()
        }
    }

    fun hide() {
        insetsController?.show(WindowInsetsCompat.Type.systemBars())
        insetsController = null

        val view = composeView ?: return
        composeView = null
        _showing.value = false

        try {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            windowManager.removeView(view)
        } catch (e: Exception) {
            Timber.e(e, "Failed to hide screen saver overlay")
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        @Suppress("DEPRECATION")
        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            screenBrightness = 0.01f // 设置屏幕亮度极暗以省电和防烧屏
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun enterImmersiveMode(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        applyLegacyImmersiveFlags(window.decorView)
        WindowCompat.getInsetsController(window, window.decorView).also { controller ->
            controller.isAppearanceLightNavigationBars = false
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController = controller
        }
    }

    @Suppress("DEPRECATION")
    private fun applyLegacyImmersiveFlags(view: View) {
        view.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}
