package org.ligi.passandroid.ui.barcode

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull
import org.ligi.passandroid.ui.compose.transformGestures

/** Scanner compatibility options shared by every code surface. */
data class CodeViewOptions(
    val sizeStep: Int = 1,
    val whiteSurround: Boolean = false,
    val extraQuietZone: Boolean = false,
    val rotateQuarterTurn: Boolean = false,
    val keepScreenOn: Boolean = true,
)

internal fun codeSizeStepFactor(sizeStep: Int): Float = when (sizeStep.coerceIn(0, 2)) {
    0 -> 0.85f
    2 -> 1.25f
    else -> 1f
}

@Composable
fun PassCodePreview(
    format: PassBarCodeFormat,
    message: String,
    options: CodeViewOptions,
    onHoldChanged: (Boolean) -> Unit,
    onPin: () -> Unit,
    baseHeight: Dp,
    zoomState: CodeZoomState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier.height(baseHeight * zoomState.layoutScale).pinchCodeSize(zoomState).pointerInput(format, message) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val gestureResult = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) return@withTimeoutOrNull true
                        if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                            return@withTimeoutOrNull false
                        }
                    }
                    @Suppress("UNREACHABLE_CODE")
                    false
                }
                if (gestureResult == true) {
                    onPin()
                } else if (gestureResult == null) {
                    onHoldChanged(true)
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                            change?.consume()
                            if (change == null || !change.pressed) break
                        }
                    } finally {
                        onHoldChanged(false)
                    }
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        ZoomedCode(
            format = format,
            message = message,
            options = options,
            baseHeightPx = with(LocalDensity.current) { baseHeight.roundToPx() },
            zoomState = zoomState,
            contentDescription = "Pass code. Hold or tap to enlarge",
            widthPx = constraints.maxWidth,
        )
    }
}

@Composable
fun PassCodeImage(
    format: PassBarCodeFormat,
    message: String,
    options: CodeViewOptions = CodeViewOptions(),
    modifier: Modifier = Modifier,
    contentDescription: String = "Pass code",
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        CrispPassCode(
            format = format,
            message = message,
            widthPx = constraints.maxWidth,
            heightPx = constraints.maxHeight,
            options = options,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun ExpandedPassCodeDialog(
    format: PassBarCodeFormat,
    message: String,
    alternativeText: String?,
    options: CodeViewOptions,
    enhanceBrightness: Boolean,
    onDismiss: () -> Unit,
) {
    if (enhanceBrightness) PassCodeBrightnessEffect()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(if (options.whiteSurround) Color.White else Color.Black.copy(alpha = 0.72f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).widthIn(max = 960.dp)
                    .clickable {}
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                    },
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                contentColor = Color.Black,
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val aspectRatio = if (format.isQuadratic()) 1f else 2.6f
                    val zoomState = rememberCodeZoomState()
                    val baseAspect = aspectRatio / codeSizeStepFactor(options.sizeStep)
                    BoxWithConstraints(
                        Modifier.fillMaxWidth().aspectRatio(baseAspect / zoomState.layoutScale).pinchCodeSize(zoomState),
                        contentAlignment = Alignment.Center,
                    ) {
                        ZoomedCode(
                            format = format,
                            message = message,
                            options = options,
                            baseHeightPx = (constraints.maxWidth / baseAspect).roundToInt(),
                            zoomState = zoomState,
                            contentDescription = "Expanded pass code",
                            widthPx = constraints.maxWidth,
                        )
                    }
                    alternativeText?.takeIf(String::isNotBlank)?.let { locator ->
                        SelectionContainer {
                            Text(locator, style = MaterialTheme.typography.headlineSmall, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

private const val MinCodeZoom = 0.4f
private const val MaxCodeZoom = 3f

/**
 * The pinch zoom of a code. Zooming in grows the surface that holds the code, so nothing is cut off, until the
 * code is as wide as its parent. Zooming out keeps the surface and leaves a white margin around the code.
 */
@Stable
class CodeZoomState {
    private var requested by mutableFloatStateOf(1f)

    /** The largest zoom before the code would pass the width of its parent. Set by the layout. */
    var max by mutableFloatStateOf(1f)

    val zoom: Float get() = requested.coerceIn(MinCodeZoom, max.coerceAtLeast(1f))

    /** How much the surface of the code grows. It never shrinks below its configured size. */
    val layoutScale: Float get() = zoom.coerceAtLeast(1f)

    internal fun pinch(factor: Float) {
        requested = (zoom * factor).coerceIn(MinCodeZoom, max.coerceAtLeast(1f))
    }
}

@Composable
fun rememberCodeZoomState() = remember { CodeZoomState() }

/**
 * Pinch changes only the drawn size of the code for as long as the surface stays composed.
 * A smaller code leaves a white margin around it, which scanners read as quiet zone.
 */
@Composable
private fun Modifier.pinchCodeSize(zoomState: CodeZoomState): Modifier = this
    .clipToBounds()
    .then(if (zoomState.zoom == 1f) Modifier else Modifier.background(Color.White))
    .transformGestures(currentScale = { 1f }, onGesture = { change, _ -> zoomState.pinch(change) })

/**
 * Draws the code at the configured size times the zoom. The largest zoom is the one at which the code is as wide as
 * the component, because the surface grows in height and only the width of the parent can cut the code off.
 */
@Composable
private fun ZoomedCode(
    format: PassBarCodeFormat,
    message: String,
    options: CodeViewOptions,
    baseHeightPx: Int,
    zoomState: CodeZoomState,
    contentDescription: String,
    widthPx: Int,
) {
    val limit = rememberMaxCodeZoom(format, message, widthPx, baseHeightPx, options)
    SideEffect { zoomState.max = limit }
    CrispPassCode(
        format = format,
        message = message,
        widthPx = widthPx,
        heightPx = baseHeightPx,
        options = options,
        contentDescription = contentDescription,
        zoom = zoomState.zoom,
    )
}

@Composable
private fun rememberMaxCodeZoom(
    format: PassBarCodeFormat,
    message: String,
    widthPx: Int,
    heightPx: Int,
    options: CodeViewOptions,
): Float = remember(passCodeBitmapKey(format, message, widthPx, heightPx, options)) {
    renderPassCodeBitmap(format, message, widthPx, heightPx, options)?.let { fitted ->
        (widthPx / fitted.width.toFloat()).coerceIn(1f, MaxCodeZoom)
    } ?: 1f
}

@Composable
private fun CrispPassCode(
    format: PassBarCodeFormat,
    message: String,
    widthPx: Int,
    heightPx: Int,
    options: CodeViewOptions,
    contentDescription: String,
    zoom: Float = 1f,
) {
    val density = LocalDensity.current
    val zoomedWidth = (widthPx * zoom).roundToInt()
    val zoomedHeight = (heightPx * zoom).roundToInt()
    val bitmap = remember(passCodeBitmapKey(format, message, zoomedWidth, zoomedHeight, options)) {
        renderPassCodeBitmap(format, message, zoomedWidth, zoomedHeight, options)
    }
    if (bitmap == null) {
        Text("Code cannot be displayed", color = Color.Black)
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier.requiredSize(
                with(density) { bitmap.width.toDp() },
                with(density) { bitmap.height.toDp() },
            ).semantics { this.contentDescription = contentDescription },
            contentScale = ContentScale.None,
            filterQuality = FilterQuality.None,
        )
    }
}

// A rotated code renders against the swapped target, so the result fits the layout bounds.
private fun renderPassCodeBitmap(
    format: PassBarCodeFormat,
    message: String,
    widthPx: Int,
    heightPx: Int,
    options: CodeViewOptions,
): Bitmap? {
    val targetWidth = if (options.rotateQuarterTurn) heightPx else widthPx
    val targetHeight = if (options.rotateQuarterTurn) widthPx else heightPx
    val rendered = CrispBarcodeRenderer.renderBitmap(
        message,
        format,
        targetWidth.coerceAtLeast(1),
        targetHeight.coerceAtLeast(1),
        options.extraQuietZone,
    ) ?: return null
    return if (options.rotateQuarterTurn) rendered.rotatedQuarterTurn() else rendered
}

private fun Bitmap.rotatedQuarterTurn(): Bitmap =
    Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(90f) }, true)

@Composable
fun PassCodeBrightnessEffect() {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val window = activity?.window
        val previousBrightness = window?.attributes?.screenBrightness
        val previousColorMode = window?.colorMode
        val previousHeadroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window?.desiredHdrHeadroom
        } else {
            null
        }
        if (window != null) {
            window.attributes = window.attributes.apply { screenBrightness = 1f }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.colorMode = ActivityInfo.COLOR_MODE_HDR
                window.desiredHdrHeadroom = 2f
            }
        }
        onDispose {
            if (window != null) {
                if (previousBrightness != null) {
                    window.attributes = window.attributes.apply { screenBrightness = previousBrightness }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    if (previousColorMode != null) window.colorMode = previousColorMode
                    window.desiredHdrHeadroom = previousHeadroom ?: 0f
                }
            }
        }
    }
}
