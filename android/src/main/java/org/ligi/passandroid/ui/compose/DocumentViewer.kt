package org.ligi.passandroid.ui.compose

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ligi.passandroid.R
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.displayArtwork

/** Public because the public [PassDetailScreen] exposes it as a parameter. */
fun interface PassDocumentPages {
    suspend fun page(passId: String, pageIndex: Int, targetWidthPx: Int): ByteArray?
}

private val DocumentMinHeight = 200.dp
private val DocumentMaxHeight = 560.dp
private const val PdfPageAspect = 0.7071f

@Composable
internal fun DocumentViewer(
    pass: PassUiModel,
    documentPages: PassDocumentPages?,
    modifier: Modifier = Modifier,
) {
    val artwork = remember(pass.id, pass.artwork) { pass.displayArtwork(listOf(PassArtworkKind.STRIP)) }
    BoxWithConstraints(modifier) {
        when {
            pass.hasDocument && pass.documentPageCount > 0 -> {
                val height = (maxWidth / PdfPageAspect).coerceIn(DocumentMinHeight, DocumentMaxHeight)
                DocumentFrame(Modifier.fillMaxWidth().height(height)) {
                    PagedDocument(pass, documentPages, Modifier.fillMaxSize())
                }
            }
            pass.isDocumentPass && artwork != null -> {
                val aspect = remember(artwork.bytes) { imageAspect(artwork.bytes) }
                val height = (maxWidth / aspect).coerceIn(DocumentMinHeight, DocumentMaxHeight)
                DocumentFrame(Modifier.fillMaxWidth().height(height)) {
                    ArtworkDocument(pass.id, artwork.bytes, Modifier.fillMaxSize())
                }
            }
        }
    }
}

/** Reads only the header, so a landscape photo does not reserve portrait space. */
private fun imageAspect(bytes: ByteArray): Float {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    return if (options.outWidth > 0 && options.outHeight > 0) {
        options.outWidth.toFloat() / options.outHeight
    } else {
        1f
    }
}

@Composable
private fun DocumentFrame(modifier: Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = content,
    )
}

@Composable
private fun PagedDocument(pass: PassUiModel, documentPages: PassDocumentPages?, modifier: Modifier) {
    val pageCount = pass.documentPageCount
    val pagerState = rememberPagerState(pageCount = { pageCount })
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val targetWidthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
            PdfPage(
                passId = pass.id,
                pageIndex = pageIndex,
                documentPages = documentPages,
                targetWidthPx = targetWidthPx,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (pageCount > 1) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    text = stringResource(R.string.pass_detail_document_page, pagerState.currentPage + 1, pageCount),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PdfPage(
    passId: String,
    pageIndex: Int,
    documentPages: PassDocumentPages?,
    targetWidthPx: Int,
    modifier: Modifier = Modifier,
) {
    var image by remember(passId, pageIndex) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(passId, pageIndex, targetWidthPx) {
        val bytes = documentPages?.page(passId, pageIndex, targetWidthPx) ?: return@LaunchedEffect
        image = decodeDocument(bytes)
    }
    PageContent(image, modifier)
}

@Composable
private fun ArtworkDocument(passId: String, bytes: ByteArray, modifier: Modifier) {
    var image by remember(passId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(passId, bytes) {
        image = decodeDocument(bytes)
    }
    PageContent(image, modifier)
}

@Composable
private fun PageContent(image: ImageBitmap?, modifier: Modifier) {
    if (image == null) {
        Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        ZoomableImage(image, modifier)
    }
}

@Composable
private fun ZoomableImage(bitmap: ImageBitmap, modifier: Modifier = Modifier) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    val description = stringResource(R.string.pass_detail_document_description)
    val zoomLabel = stringResource(R.string.pass_detail_document_zoom)
    val toggleZoom = {
        if (scale > 1f) {
            scale = 1f
            offset = Offset.Zero
        } else {
            scale = 2.5f
        }
    }
    Image(
        bitmap = bitmap,
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .pointerInput(bitmap) {
                detectTapGestures(onDoubleTap = { toggleZoom() })
            }
            .transformGestures(
                currentScale = { scale },
                onGesture = { zoomChange, panChange ->
                    scale = (scale * zoomChange).coerceIn(1f, 6f)
                    offset = if (scale > 1f) offset + panChange else Offset.Zero
                },
            )
            .semantics { onClick(label = zoomLabel) { toggleZoom(); true } }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

private suspend fun decodeDocument(bytes: ByteArray): ImageBitmap? = withContext(Dispatchers.Default) {
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}
