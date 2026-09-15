package org.ligi.passandroid.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ligi.passandroid.platform.PassImageExporter
import org.ligi.passandroid.repository.PassImageAspectRatio
import org.ligi.passandroid.repository.PassImageContent
import org.ligi.passandroid.repository.PassImageExportOptions
import org.ligi.passandroid.repository.PassImageOrientation
import org.ligi.passandroid.ui.state.PassImageExportAction
import org.ligi.passandroid.ui.state.PassUiModel
import androidx.compose.ui.res.stringResource
import org.ligi.passandroid.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExportImageScreen(
    pass: PassUiModel?,
    options: PassImageExportOptions,
    isBusy: Boolean,
    onAction: (PassImageExportAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_export_image)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { onAction(PassImageExportAction.Back) }, shape = CircleShape) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pass_detail_back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (pass != null) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    floatingActionButton = {
                        FloatingToolbarDefaults.StandardFloatingActionButton(
                            onClick = { if (!isBusy) onAction(PassImageExportAction.Save) },
                        ) {
                            if (isBusy) {
                                LoadingIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            } else {
                                Icon(Icons.Default.Save, stringResource(R.string.export_save_png))
                            }
                        }
                    },
                ) {
                    IconButton(onClick = { if (!isBusy) onAction(PassImageExportAction.Share) }) {
                        Icon(Icons.Default.Share, stringResource(R.string.export_share_image))
                    }
                    IconButton(onClick = { if (!isBusy) onAction(PassImageExportAction.Print) }) {
                        Icon(Icons.Default.Print, stringResource(R.string.export_print_pass))
                    }
                }
            }
        },
    ) { padding ->
        if (pass == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.pass_detail_pass_not_found))
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                ExportPreview(pass, options, Modifier.fillMaxWidth().weight(1f).padding(12.dp))
                ExportOptions(pass, options, onAction)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExportOptions(
    pass: PassUiModel,
    options: PassImageExportOptions,
    onAction: (PassImageExportAction) -> Unit,
) {
    fun setOptions(updated: PassImageExportOptions) = onAction(PassImageExportAction.SetOptions(updated))
    val content = options.content
    fun setContent(updated: PassImageContent) = setOptions(options.copy(content = updated))
    val motion = MaterialTheme.motionScheme
    val spatial = remember { spring<IntSize>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f) }
    val effects = remember(motion) { motion.fastEffectsSpec<Float>() }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 88.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PassImageAspectRatio.entries.forEach { ratio ->
                    FilterChip(
                        selected = options.aspectRatio == ratio,
                        onClick = { setOptions(options.copy(aspectRatio = ratio)) },
                        label = { Text(aspectRatioLabel(ratio, options.orientation)) },
                    )
                }
            }
            AnimatedVisibility(
                visible = options.aspectRatio != PassImageAspectRatio.AUTO_HEIGHT,
                enter = fadeIn(effects) + expandVertically(spatial, expandFrom = Alignment.Top),
                exit = fadeOut(effects) + shrinkVertically(spatial, shrinkTowards = Alignment.Top),
            ) {
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PassImageOrientation.entries.forEach { orientation ->
                        val portrait = orientation == PassImageOrientation.PORTRAIT
                        FilterChip(
                            selected = options.orientation == orientation,
                            onClick = { setOptions(options.copy(orientation = orientation)) },
                            label = { Text(if (portrait) stringResource(R.string.export_vertical) else stringResource(R.string.export_horizontal)) },
                            leadingIcon = {
                                Icon(if (portrait) Icons.Default.CropPortrait else Icons.Default.CropLandscape, null)
                            },
                        )
                    }
                }
            }
            FlowRow(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (pass.artwork.isNotEmpty()) {
                    ContentChip(stringResource(R.string.export_artwork), content.artwork) { setContent(content.copy(artwork = !content.artwork)) }
                }
                ContentChip(stringResource(R.string.export_details), content.details) { setContent(content.copy(details = !content.details)) }
                if (pass.notes.isNotBlank()) {
                    ContentChip(stringResource(R.string.export_notes), content.notes) { setContent(content.copy(notes = !content.notes)) }
                }
                if (pass.barcodeFormat != null && !pass.barcodeMessage.isNullOrBlank()) {
                    ContentChip(stringResource(R.string.edit_pass_code), content.barcode) { setContent(content.copy(barcode = !content.barcode)) }
                }
                if (pass.calendarTimeSpan?.from != null || pass.calendarTimeSpan?.to != null) {
                    ContentChip(stringResource(R.string.export_date), content.dateTime) { setContent(content.copy(dateTime = !content.dateTime)) }
                }
                if (pass.locations.isNotEmpty()) {
                    ContentChip(stringResource(R.string.export_location), content.location) { setContent(content.copy(location = !content.location)) }
                }
                if (pass.fields.any { it.hidden }) {
                    ContentChip(stringResource(R.string.export_hidden), content.hiddenFields) { setContent(content.copy(hiddenFields = !content.hiddenFields)) }
                }
            }
        }
    }
}

@Composable
private fun ContentChip(label: String, selected: Boolean, onToggle: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { Text(label) },
    )
}

@Composable
private fun aspectRatioLabel(ratio: PassImageAspectRatio, orientation: PassImageOrientation): String {
    if (ratio == PassImageAspectRatio.A4) return "A4"
    if (ratio.ratioWidth == 0f) return stringResource(R.string.export_auto)
    val (width, height) = if (orientation == PassImageOrientation.LANDSCAPE) {
        ratio.ratioHeight to ratio.ratioWidth
    } else {
        ratio.ratioWidth to ratio.ratioHeight
    }
    return "${width.toInt()}:${height.toInt()}"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExportPreview(pass: PassUiModel, options: PassImageExportOptions, modifier: Modifier) {
    BoxWithConstraints(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth
        val targetWidth = remember(maxWidthPx) { maxWidthPx.coerceIn(1, with(density) { 1000.dp.toPx() }.toInt()) }
        val preview by produceState<ImageBitmap?>(null, pass, options, targetWidth) {
            value = withContext(Dispatchers.Default) {
                runCatching {
                    PassImageExporter.renderBitmap(pass, options, targetWidth).asImageBitmap()
                }.getOrNull()
            }
        }
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 6f)
            offset = if (scale > 1f) offset + panChange else Offset.Zero
        }
        val motion = MaterialTheme.motionScheme
        val effects = remember(motion) { motion.fastEffectsSpec<Float>() }
        Crossfade(targetState = preview, animationSpec = effects, label = "preview") { image ->
            if (image == null) {
                LoadingIndicator()
            } else {
                Image(
                    bitmap = image,
                    contentDescription = "Export preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(transformState)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                )
            }
        }
    }
}
