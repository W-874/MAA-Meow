package com.aliothmoon.maameow.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

object MaaAnimations {

    internal const val SHARED_AXIS_DURATION_MILLIS = 300
    internal const val SHARED_AXIS_SLIDE_DIVISOR = 3

    /**
     * Ease-out cubic bezier (0.32, 0.72, 0.0, 1.0): fast start, smooth settle.
     * Control-point y stays within [0, 1], so it does not overshoot.
     */
    val springEasing = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)

    private val sharedAxisEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    private fun slideEnter(offsetX: (Int) -> Int): EnterTransition =
        slideInHorizontally(
            initialOffsetX = offsetX,
            animationSpec = tween(SHARED_AXIS_DURATION_MILLIS, easing = sharedAxisEasing)
        ) +
                fadeIn(animationSpec = tween(SHARED_AXIS_DURATION_MILLIS, easing = sharedAxisEasing))

    private fun slideExit(offsetX: (Int) -> Int): ExitTransition =
        slideOutHorizontally(
            targetOffsetX = offsetX,
            animationSpec = tween(SHARED_AXIS_DURATION_MILLIS, easing = sharedAxisEasing)
        ) +
                fadeOut(animationSpec = tween(SHARED_AXIS_DURATION_MILLIS, easing = sharedAxisEasing))

    val sharedAxisForwardEnter: EnterTransition =
        slideEnter { fullWidth -> fullWidth / SHARED_AXIS_SLIDE_DIVISOR }
    val sharedAxisForwardExit: ExitTransition =
        slideExit { fullWidth -> -fullWidth / SHARED_AXIS_SLIDE_DIVISOR }
    val sharedAxisPopEnter: EnterTransition =
        slideEnter { fullWidth -> -fullWidth / SHARED_AXIS_SLIDE_DIVISOR }
    val sharedAxisPopExit: ExitTransition =
        slideExit { fullWidth -> fullWidth / SHARED_AXIS_SLIDE_DIVISOR }
}
