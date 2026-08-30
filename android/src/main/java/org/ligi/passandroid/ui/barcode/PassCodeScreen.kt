package org.ligi.passandroid.ui.barcode

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.ligi.passandroid.model.pass.PassBarCodeFormat

sealed interface CodeScreenAction {
    data object Back : CodeScreenAction
    data class SetCodeScale(val scale: Float) : CodeScreenAction
    data object ResetCodeScale : CodeScreenAction
    data class SetFlashlightEnabled(val enabled: Boolean) : CodeScreenAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassCodeScreen(
    format: PassBarCodeFormat,
    message: String,
    alternativeText: String?,
    codeScale: Float,
    automaticBrightness: Boolean,
    flashlightAvailable: Boolean,
    flashlightEnabled: Boolean,
    onAction: (CodeScreenAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(activity, automaticBrightness) {
        val window = activity?.window
        val previousBrightness = window?.attributes?.screenBrightness
        if (automaticBrightness && window != null) {
            window.attributes = window.attributes.apply { screenBrightness = 1f }
        }
        onDispose {
            if (automaticBrightness && window != null && previousBrightness != null) {
                window.attributes = window.attributes.apply { screenBrightness = previousBrightness }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { onAction(CodeScreenAction.SetFlashlightEnabled(false)) }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) onAction(CodeScreenAction.SetFlashlightEnabled(false))
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Pass code") },
                navigationIcon = {
                    IconButton(onClick = { onAction(CodeScreenAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onAction(CodeScreenAction.SetFlashlightEnabled(!flashlightEnabled)) },
                        enabled = flashlightAvailable || flashlightEnabled,
                    ) {
                        Icon(
                            if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            if (!flashlightAvailable && !flashlightEnabled) "Flashlight unavailable"
                            else if (flashlightEnabled) "Turn flashlight off" else "Turn flashlight on",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                val density = LocalDensity.current
                val safeScale = codeScale.coerceIn(MIN_CODE_SCALE, MAX_CODE_SCALE)
                val targetWidthPx = (constraints.maxWidth * safeScale).toInt().coerceAtLeast(1)
                val targetHeightPx = constraints.maxHeight.coerceAtLeast(1)
                val bitmap = remember(format, message, targetWidthPx, targetHeightPx) {
                    CrispBarcodeRenderer.renderBitmap(message, format, targetWidthPx, targetHeightPx)
                }
                if (bitmap == null) {
                    Text("Code cannot be displayed", color = Color.Black)
                } else {
                    val width = with(density) { bitmap.width.toDp() }
                    val height = with(density) { bitmap.height.toDp() }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "${format.accessibleName()} pass code",
                        modifier = Modifier.requiredSize(width, height).semantics {
                            contentDescription = "${format.accessibleName()} pass code"
                        },
                        contentScale = ContentScale.None,
                        filterQuality = FilterQuality.None,
                    )
                }
            }

            alternativeText?.takeIf(String::isNotBlank)?.let { text ->
                SelectionContainer {
                    Text(text, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                }
            }

            Text("Code size", style = MaterialTheme.typography.labelLarge, color = Color.Black)
            Slider(
                value = codeScale.coerceIn(MIN_CODE_SCALE, MAX_CODE_SCALE),
                onValueChange = { onAction(CodeScreenAction.SetCodeScale(it)) },
                valueRange = MIN_CODE_SCALE..MAX_CODE_SCALE,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Code size" },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = { onAction(CodeScreenAction.ResetCodeScale) }) { Text("Reset size") }
            }
        }
    }
}

private const val MIN_CODE_SCALE = 0.55f
private const val MAX_CODE_SCALE = 1f

private fun PassBarCodeFormat.accessibleName() = name.replace('_', ' ').lowercase()
