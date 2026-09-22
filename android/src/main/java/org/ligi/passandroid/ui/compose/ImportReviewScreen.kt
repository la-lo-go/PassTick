package org.ligi.passandroid.ui.compose

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.R
import org.ligi.passandroid.imports.NormalizedRect
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.ui.state.ImportReviewAction
import org.ligi.passandroid.ui.state.ImportReviewUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private const val MinCropSize = 0.1f
private const val CropZoomDelayMillis = 2_000L
private const val CropZoomAnimationMillis = 250
private const val CropZoomFill = 0.92f
private const val CropZoomMax = 3.5f
private const val CropZoomEpsilon = 0.001f
private const val CodeTagMaxChars = 30
private val PreviewMinHeight = 240.dp
private val PreviewMaxHeight = 520.dp

private val AccentPalette = listOf(
    0xFF3D73E9.toInt(),
    0xFF006C4C.toInt(),
    0xFF6750A4.toInt(),
    0xFFB3261E.toInt(),
    0xFFE07A00.toInt(),
    0xFF00639B.toInt(),
    0xFF6D4C41.toInt(),
    0xFF546E7A.toInt(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportReviewScreen(
    state: ImportReviewUiState,
    onAction: (ImportReviewAction) -> Unit,
) {
    var discardConfirmationVisible by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_review_title)) },
                navigationIcon = {
                    IconButton(onClick = { discardConfirmationVisible = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.import_review_discard))
                    }
                },
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallFloatingActionButton(
                    onClick = { if (!state.isBusy) discardConfirmationVisible = true },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Default.Close, stringResource(R.string.import_review_discard))
                }
                ExtendedFloatingActionButton(
                    onClick = { if (!state.isBusy) onAction(ImportReviewAction.Confirm) },
                ) {
                    if (state.isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Check, null)
                    }
                    Text(stringResource(R.string.import_review_save))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Preview(state, onAction)
            TitleField(state, onAction)
            AccentColorRow(state, onAction)
            CodeSection(state, onAction)
        }
    }
    if (discardConfirmationVisible) {
        DiscardConfirmationDialog(
            onConfirm = {
                discardConfirmationVisible = false
                onAction(ImportReviewAction.Discard)
            },
            onDismiss = { discardConfirmationVisible = false },
        )
    }
}

@Composable
private fun Preview(state: ImportReviewUiState, onAction: (ImportReviewAction) -> Unit) {
    val bytes = if (state.cropEditing) state.previewPng else state.displayPng
    val image = remember(bytes) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val imageAspect = image?.takeIf { it.height > 0 }?.let { it.width.toFloat() / it.height } ?: 1.5f
        val frameHeight = (maxWidth / imageAspect).coerceIn(PreviewMinHeight, PreviewMaxHeight)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(frameHeight)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            if (image != null) {
                if (state.cropEditing) {
                    CropPreview(
                        bitmap = image,
                        crop = state.crop,
                        onCropChange = { onAction(ImportReviewAction.SetCrop(it)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    ZoomablePreview(bitmap = image, modifier = Modifier.fillMaxSize())
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                shadowElevation = 4.dp,
            ) {
                Row(Modifier.padding(horizontal = 4.dp)) {
                    IconButton(
                        onClick = { onAction(ImportReviewAction.Rotate) },
                        enabled = !state.isBusy,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, stringResource(R.string.import_review_rotate))
                    }
                    if (state.cropEditing) {
                        FilledIconButton(
                            onClick = { onAction(ImportReviewAction.SetCropEditing(false)) },
                            enabled = !state.isBusy,
                            colors = IconButtonDefaults.filledIconButtonColors(),
                        ) {
                            Icon(Icons.Default.Check, stringResource(R.string.import_review_crop_done))
                        }
                    } else {
                        IconButton(
                            onClick = { onAction(ImportReviewAction.SetCropEditing(true)) },
                            enabled = !state.isBusy,
                        ) {
                            Icon(Icons.Default.Crop, stringResource(R.string.import_review_crop))
                        }
                    }
                }
            }
            if (state.isBusy) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.import_review_preparing))
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomablePreview(bitmap: ImageBitmap, modifier: Modifier) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .pointerInput(bitmap) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .transformGestures(
                currentScale = { scale },
                onGesture = { zoomChange, panChange ->
                    scale = (scale * zoomChange).coerceIn(1f, 6f)
                    offset = if (scale > 1f) offset + panChange else Offset.Zero
                },
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

@Composable
private fun CropPreview(
    bitmap: ImageBitmap,
    crop: NormalizedRect,
    onCropChange: (NormalizedRect) -> Unit,
    modifier: Modifier,
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var draggedCrop by remember { mutableStateOf(crop) }
    var interactionKey by remember { mutableIntStateOf(0) }
    val zoomAnimation = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(crop) { draggedCrop = crop }
    val imageRect = remember(boxSize, bitmap) {
        displayedImageRect(boxSize, bitmap.width, bitmap.height)
    }
    val cropRect = draggedCrop.toRect(imageRect)
    val targetScale = targetScaleFor(cropRect, boxSize)
    LaunchedEffect(interactionKey, boxSize, targetScale) {
        if (boxSize == IntSize.Zero) return@LaunchedEffect
        delay(CropZoomDelayMillis)
        zoomAnimation.animateTo(targetScale, tween(CropZoomAnimationMillis))
    }
    val zoom = zoomAnimation.value
    val translation = cropZoomTranslation(boxSize, cropRect, zoom, targetScale)
    Box(
        modifier = modifier
            .onSizeChanged { boxSize = it }
            .transformGestures(
                currentScale = { 1f },
                onGesture = { zoomChange, _ ->
                    if (abs(zoomChange - 1f) > CropZoomEpsilon) {
                        draggedCrop = draggedCrop.scaledAroundCenter(1f / zoomChange)
                        val scaledCropRect = draggedCrop.toRect(imageRect)
                        val scale = targetScaleFor(scaledCropRect, boxSize)
                        coroutineScope.launch { zoomAnimation.snapTo(scale) }
                    }
                },
                onGestureEnd = {
                    onCropChange(draggedCrop)
                    interactionKey++
                },
            ),
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = translation.x
                    translationY = translation.y
                },
        )
        CropOverlay(
            cropRect = cropRect.map(translation, zoom, boxSize),
            boxSize = boxSize,
            onCropDragStart = { coroutineScope.launch { zoomAnimation.snapTo(1f) } },
            onCropDrag = { corner, delta ->
                val localDelta = if (zoom > CropZoomEpsilon) delta / zoom else delta
                draggedCrop = draggedCrop.draggedBy(corner, localDelta, imageRect)
            },
            onCropDragEnd = {
                onCropChange(draggedCrop)
                interactionKey++
            },
        )
    }
}

private fun targetScaleFor(cropRect: Rect, boxSize: IntSize): Float {
    val cropHasArea = cropRect.width > 1f && cropRect.height > 1f
    val boxHasArea = boxSize.width > 0 && boxSize.height > 0
    if (!cropHasArea || !boxHasArea) return 1f
    return (min(boxSize.width / cropRect.width, boxSize.height / cropRect.height) * CropZoomFill)
        .coerceIn(1f, CropZoomMax)
}

/** Matches the graphics layer: scale around the layer center plus the translation that centers the crop. */
private fun cropZoomTranslation(
    boxSize: IntSize,
    cropRect: Rect,
    zoom: Float,
    targetScale: Float,
): Offset {
    if (boxSize.width <= 0 || boxSize.height <= 0) return Offset.Zero
    if (targetScale <= 1f + CropZoomEpsilon || zoom <= 1f + CropZoomEpsilon) return Offset.Zero
    val centerX = boxSize.width / 2f
    val centerY = boxSize.height / 2f
    val progress = ((zoom - 1f) / (targetScale - 1f)).coerceIn(0f, 1f)
    val pivotEndX = (centerX - cropRect.center.x * targetScale) / (1f - targetScale)
    val pivotEndY = (centerY - cropRect.center.y * targetScale) / (1f - targetScale)
    val pivotX = cropRect.center.x + (pivotEndX - cropRect.center.x) * progress
    val pivotY = cropRect.center.y + (pivotEndY - cropRect.center.y) * progress
    return Offset((pivotX - centerX) * (1f - zoom), (pivotY - centerY) * (1f - zoom))
}

private fun Rect.map(translation: Offset, zoom: Float, boxSize: IntSize): Rect {
    val centerX = boxSize.width / 2f
    val centerY = boxSize.height / 2f
    fun transformX(x: Float) = centerX + (x - centerX) * zoom + translation.x
    fun transformY(y: Float) = centerY + (y - centerY) * zoom + translation.y
    return Rect(transformX(left), transformY(top), transformX(right), transformY(bottom))
}

@Composable
private fun CropOverlay(
    cropRect: Rect,
    boxSize: IntSize,
    onCropDragStart: () -> Unit,
    onCropDrag: (CropCorner, Offset) -> Unit,
    onCropDragEnd: () -> Unit,
) {
    val dragStart = rememberUpdatedState(onCropDragStart)
    val drag = rememberUpdatedState(onCropDrag)
    val dragEnd = rememberUpdatedState(onCropDragEnd)
    Canvas(Modifier.fillMaxSize().clipToBounds()) {
        val dim = Color.Black.copy(alpha = 0.55f)
        drawDimRect(dim, left = 0f, top = 0f, right = size.width, bottom = cropRect.top)
        drawDimRect(dim, left = 0f, top = cropRect.bottom, right = size.width, bottom = size.height)
        drawDimRect(dim, left = 0f, top = cropRect.top, right = cropRect.left, bottom = cropRect.bottom)
        drawDimRect(dim, left = cropRect.right, top = cropRect.top, right = size.width, bottom = cropRect.bottom)
        drawRect(
            color = Color.White,
            topLeft = cropRect.topLeft,
            size = cropRect.size,
            style = Stroke(width = 2.dp.toPx()),
        )
        CropCorner.entries.forEach { corner ->
            val center = cropRect.corner(corner)
            drawCircle(Color.White, radius = 9.dp.toPx(), center = center)
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = 9.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
    CropCorner.entries.forEach { corner ->
        Box(
            modifier = Modifier
                .offset {
                    val radius = 22.dp.toPx()
                    val x = cropRect.corner(corner).x.coerceIn(radius, (boxSize.width - radius).coerceAtLeast(radius))
                    val y = cropRect.corner(corner).y.coerceIn(radius, (boxSize.height - radius).coerceAtLeast(radius))
                    IntOffset((x - radius).roundToInt(), (y - radius).roundToInt())
                }
                .size(44.dp)
                .pointerInput(corner) {
                    detectDragGestures(
                        onDragStart = { dragStart.value() },
                        onDragEnd = { dragEnd.value() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            drag.value(corner, dragAmount)
                        },
                    )
                },
        )
    }
}

/** Draws one dim rect with all edges clipped to the canvas so it never has a negative size. */
private fun DrawScope.drawDimRect(color: Color, left: Float, top: Float, right: Float, bottom: Float) {
    val clampedLeft = left.coerceIn(0f, size.width)
    val clampedTop = top.coerceIn(0f, size.height)
    val clampedRight = right.coerceIn(0f, size.width)
    val clampedBottom = bottom.coerceIn(0f, size.height)
    drawRect(
        color = color,
        topLeft = Offset(clampedLeft, clampedTop),
        size = Size(
            width = (clampedRight - clampedLeft).coerceAtLeast(0f),
            height = (clampedBottom - clampedTop).coerceAtLeast(0f),
        ),
    )
}

@Composable
private fun TitleField(state: ImportReviewUiState, onAction: (ImportReviewAction) -> Unit) {
    OutlinedTextField(
        value = state.title,
        onValueChange = { onAction(ImportReviewAction.SetTitle(it)) },
        label = { Text(stringResource(R.string.import_review_name)) },
        singleLine = true,
        enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentColorRow(state: ImportReviewUiState, onAction: (ImportReviewAction) -> Unit) {
    val swatches = remember(state.suggestedAccentColor) {
        listOf(state.suggestedAccentColor) + AccentPalette.filter { it != state.suggestedAccentColor }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.import_review_color), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            swatches.forEach { color ->
                AccentSwatch(
                    color = color,
                    selected = color == state.accentColor,
                    onClick = { onAction(ImportReviewAction.SetAccentColor(color)) },
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(color: Int, selected: Boolean, onClick: () -> Unit) {
    val swatchColor = Color(color)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (swatchColor.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CodeSection(state: ImportReviewUiState, onAction: (ImportReviewAction) -> Unit) {
    val selectedCode = state.selectedCodeIndices.minOrNull()?.let(state.detectedCodes::getOrNull)
    val title = if (selectedCode != null) {
        stringResource(R.string.import_review_code_detected, selectedCode.format.codeTypeLabel())
    } else {
        stringResource(R.string.import_review_code)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        if (state.detectedCodes.isEmpty()) {
            Text(
                text = stringResource(R.string.import_review_code_not_detected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.detectedCodes.forEachIndexed { index, code ->
                    val selected = index in state.selectedCodeIndices
                    FilterChip(
                        selected = selected,
                        onClick = { onAction(ImportReviewAction.SelectCode(index)) },
                        label = { Text(truncateCodeMessage(code.message)) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
                FilterChip(
                    selected = state.selectedCodeIndices.isEmpty(),
                    onClick = { onAction(ImportReviewAction.SelectCode(null)) },
                    label = { Text(stringResource(R.string.import_review_code_none)) },
                )
            }
        }
    }
}

private fun PassBarCodeFormat.codeTypeLabel(): String = when (this) {
    PassBarCodeFormat.QR_CODE -> "QR code"
    PassBarCodeFormat.AZTEC -> "Aztec"
    PassBarCodeFormat.CODABAR -> "Codabar"
    PassBarCodeFormat.CODE_39 -> "Code 39"
    PassBarCodeFormat.CODE_93 -> "Code 93"
    PassBarCodeFormat.CODE_128 -> "Code 128"
    PassBarCodeFormat.DATA_MATRIX -> "Data Matrix"
    PassBarCodeFormat.EAN_8 -> "EAN-8"
    PassBarCodeFormat.EAN_13 -> "EAN-13"
    PassBarCodeFormat.ITF -> "ITF"
    PassBarCodeFormat.PDF_417 -> "PDF417"
    PassBarCodeFormat.UPC_A -> "UPC-A"
    PassBarCodeFormat.UPC_E -> "UPC-E"
}

private fun truncateCodeMessage(message: String): String =
    if (message.length > CodeTagMaxChars) message.take(CodeTagMaxChars) + "…" else message

@Composable
private fun DiscardConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_review_discard_title)) },
        text = { Text(stringResource(R.string.import_review_discard_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.import_review_discard))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.import_review_keep_editing))
            }
        },
    )
}

/** Maps the fitted image into the preview box exactly as [ContentScale.Fit] centers it. */
private fun displayedImageRect(boxSize: IntSize, imageWidth: Int, imageHeight: Int): Rect {
    if (boxSize.width <= 0 || boxSize.height <= 0) return Rect.Zero
    if (imageWidth <= 0 || imageHeight <= 0) return Rect.Zero
    val scale = min(boxSize.width.toFloat() / imageWidth, boxSize.height.toFloat() / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    val left = (boxSize.width - width) / 2f
    val top = (boxSize.height - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun NormalizedRect.toRect(image: Rect): Rect = Rect(
    left = image.left + this.left * image.width,
    top = image.top + this.top * image.height,
    right = image.left + this.right * image.width,
    bottom = image.top + this.bottom * image.height,
)

private fun NormalizedRect.scaledAroundCenter(factor: Float): NormalizedRect {
    val centerX = (left + right) / 2f
    val centerY = (top + bottom) / 2f
    val newWidth = (width * factor).coerceIn(MinCropSize, 1f)
    val newHeight = (height * factor).coerceIn(MinCropSize, 1f)
    val newLeft = (centerX - newWidth / 2f).coerceIn(0f, 1f - newWidth)
    val newTop = (centerY - newHeight / 2f).coerceIn(0f, 1f - newHeight)
    return NormalizedRect(newLeft, newTop, newLeft + newWidth, newTop + newHeight)
}

private fun NormalizedRect.draggedBy(corner: CropCorner, delta: Offset, image: Rect): NormalizedRect {
    if (image.width <= 0f || image.height <= 0f) return this
    val dx = delta.x / image.width
    val dy = delta.y / image.height
    return when (corner) {
        CropCorner.TopLeft -> copy(
            left = (left + dx).coerceIn(0f, right - MinCropSize),
            top = (top + dy).coerceIn(0f, bottom - MinCropSize),
        )
        CropCorner.TopRight -> copy(
            right = (right + dx).coerceIn(left + MinCropSize, 1f),
            top = (top + dy).coerceIn(0f, bottom - MinCropSize),
        )
        CropCorner.BottomLeft -> copy(
            left = (left + dx).coerceIn(0f, right - MinCropSize),
            bottom = (bottom + dy).coerceIn(top + MinCropSize, 1f),
        )
        CropCorner.BottomRight -> copy(
            right = (right + dx).coerceIn(left + MinCropSize, 1f),
            bottom = (bottom + dy).coerceIn(top + MinCropSize, 1f),
        )
    }
}

private fun Rect.corner(corner: CropCorner): Offset = when (corner) {
    CropCorner.TopLeft -> Offset(left, top)
    CropCorner.TopRight -> Offset(right, top)
    CropCorner.BottomLeft -> Offset(left, bottom)
    CropCorner.BottomRight -> Offset(right, bottom)
}

private enum class CropCorner { TopLeft, TopRight, BottomLeft, BottomRight }
