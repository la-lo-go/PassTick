package org.ligi.passandroid.ui.barcode

import android.content.pm.ActivityInfo
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
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun PassCodePreview(
    format: PassBarCodeFormat,
    message: String,
    onHoldChanged: (Boolean) -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier.pointerInput(format, message) {
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
        CrispPassCode(
            format = format,
            message = message,
            widthPx = constraints.maxWidth,
            heightPx = constraints.maxHeight,
            contentDescription = "Pass code. Hold or tap to enlarge",
        )
    }
}

@Composable
fun PassCodeImage(
    format: PassBarCodeFormat,
    message: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "Pass code",
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        CrispPassCode(
            format = format,
            message = message,
            widthPx = constraints.maxWidth,
            heightPx = constraints.maxHeight,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun ExpandedPassCodeDialog(
    format: PassBarCodeFormat,
    message: String,
    alternativeText: String?,
    enhanceBrightness: Boolean,
    onDismiss: () -> Unit,
) {
    if (enhanceBrightness) MaxBrightnessAndHdrEffect()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).widthIn(max = 960.dp)
                    .clickable {},
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                contentColor = Color.Black,
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BoxWithConstraints(
                        Modifier.fillMaxWidth().aspectRatio(if (format.isQuadratic()) 1f else 2.6f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CrispPassCode(
                            format = format,
                            message = message,
                            widthPx = constraints.maxWidth,
                            heightPx = constraints.maxHeight,
                            contentDescription = "Expanded pass code",
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

@Composable
private fun CrispPassCode(
    format: PassBarCodeFormat,
    message: String,
    widthPx: Int,
    heightPx: Int,
    contentDescription: String,
) {
    val density = LocalDensity.current
    val bitmap = remember(format, message, widthPx, heightPx) {
        CrispBarcodeRenderer.renderBitmap(message, format, widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1))
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

@Composable
private fun MaxBrightnessAndHdrEffect() {
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
