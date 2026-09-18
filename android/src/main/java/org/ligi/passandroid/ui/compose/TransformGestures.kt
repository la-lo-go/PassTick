package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Zoom and pan that consumes events only with two pointers or while already zoomed in,
 * so a single-finger drag at the default scale still scrolls the parent container.
 */
@Composable
internal fun Modifier.transformGestures(
    currentScale: () -> Float,
    onGesture: (zoomChange: Float, panChange: Offset) -> Unit,
    onGestureEnd: () -> Unit = {},
): Modifier {
    val scaleProvider = rememberUpdatedState(currentScale)
    val gestureHandler = rememberUpdatedState(onGesture)
    val gestureEndHandler = rememberUpdatedState(onGestureEnd)
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var transformed = false
            var event = awaitPointerEvent()
            while (event.changes.any { it.pressed }) {
                val pressedCount = event.changes.count { it.pressed }
                if (pressedCount > 1 || scaleProvider.value() > 1f) {
                    transformed = true
                    gestureHandler.value(event.calculateZoom(), event.calculatePan())
                    event.changes.forEach { it.consume() }
                }
                event = awaitPointerEvent()
            }
            if (transformed) gestureEndHandler.value()
        }
    }
}
