package org.ligi.passandroid.platform

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassImageAspectRatio
import org.ligi.passandroid.repository.PassImageContent
import org.ligi.passandroid.repository.PassImageExportOptions
import org.ligi.passandroid.repository.PassImageOrientation
import org.ligi.passandroid.ui.barcode.CrispBarcodeRenderer
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.displayArtwork
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

/** Writes a shareable PNG without exposing the app's private pass files. */
object PassImageExporter {
    const val EXPORT_WIDTH = 2000

    private val HEADER_ARTWORK_KINDS = listOf(PassArtworkKind.STRIP, PassArtworkKind.LOGO, PassArtworkKind.THUMBNAIL)
    private val THUMBNAIL_ARTWORK_KINDS = listOf(PassArtworkKind.ICON, PassArtworkKind.THUMBNAIL, PassArtworkKind.LOGO)

    fun write(resolver: ContentResolver, uri: Uri, pass: PassUiModel, options: PassImageExportOptions) {
        val bitmap = renderBitmap(pass, options)
        try {
            val output = resolver.openOutputStream(uri) ?: error("Could not open image destination")
            output.use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "Could not encode PNG" }
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun writeToGallery(
        resolver: ContentResolver,
        displayName: String,
        pass: PassUiModel,
        options: PassImageExportOptions,
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PassTick")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create a gallery entry")
        val bitmap = renderBitmap(pass, options)
        try {
            val output = resolver.openOutputStream(uri, "w") ?: error("Could not open the gallery destination")
            output.use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "Could not encode PNG" }
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        } finally {
            bitmap.recycle()
        }
        return uri
    }

    internal fun renderBitmap(
        pass: PassUiModel,
        options: PassImageExportOptions,
        width: Int = EXPORT_WIDTH,
    ): Bitmap {
        require(width > 0) { "Width must be positive" }
        val base = width.toFloat()
        val content = options.content
        val barcodeOnly = isBarcodeOnly(content)
        val cardMargin = base * if (barcodeOnly) BARCODE_ONLY_MARGIN_RATIO else CARD_MARGIN_RATIO
        val padding = base * if (barcodeOnly) BARCODE_ONLY_PADDING_RATIO else CARD_PADDING_RATIO
        val gap = base * BLOCK_GAP_RATIO
        val contentWidth = (width - 2 * cardMargin - 2 * padding).toInt().coerceAtLeast(1)
        val imagePaint = Paint(Paint.FILTER_BITMAP_FLAG)

        val artwork = if (content.artwork) decodeArtwork(pass, HEADER_ARTWORK_KINDS, contentWidth) else null
        val thumbnail = if (content.details && pass.description.isNotBlank()) {
            decodeArtwork(pass, THUMBNAIL_ARTWORK_KINDS, (base * TITLE_ICON_RATIO * 2).toInt())
        } else {
            null
        }
        val barcode = renderBarcode(pass, options, contentWidth, base, barcodeOnly)
        try {
            val blocks = buildContentBlocks(pass, options, artwork, thumbnail, barcode, contentWidth, base, imagePaint, barcodeOnly)
            return drawExportBitmap(blocks, options, cardMargin, padding, gap, contentWidth, width, base, imagePaint)
        } finally {
            artwork?.recycle()
            thumbnail?.recycle()
            barcode?.recycle()
        }
    }

    private fun isBarcodeOnly(content: PassImageContent): Boolean =
        content.barcode && !hasNonBarcodeContent(content)

    private fun hasNonBarcodeContent(content: PassImageContent): Boolean =
        content.artwork || content.details || content.dateTime || content.location

    private fun drawExportBitmap(
        blocks: List<ContentBlock>,
        options: PassImageExportOptions,
        cardMargin: Float,
        padding: Float,
        gap: Float,
        contentWidth: Int,
        width: Int,
        base: Float,
        imagePaint: Paint,
    ): Bitmap {
        val contentHeight = measureContentHeight(blocks, gap)
        val cardWidth = width - 2 * cardMargin
        val autoHeight = options.aspectRatio.ratioWidth == 0f
        val canvasHeight: Int
        val cardHeight: Float
        if (autoHeight) {
            cardHeight = 2 * padding + contentHeight
            canvasHeight = (2 * cardMargin + cardHeight).toInt()
        } else {
            canvasHeight = (width * orientedHeightFactor(options.aspectRatio, options.orientation)).toInt()
            cardHeight = canvasHeight - 2 * cardMargin
        }

        val bitmap = createBitmap(width, canvasHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)
        drawCard(canvas, cardMargin, cardMargin, cardWidth, cardHeight, base)
        drawBlocksOnCanvas(canvas, blocks, contentWidth, contentHeight, cardMargin, padding, cardWidth, cardHeight, gap, autoHeight, imagePaint)
        return bitmap
    }

    private fun drawBlocksOnCanvas(
        canvas: Canvas,
        blocks: List<ContentBlock>,
        contentWidth: Int,
        contentHeight: Float,
        cardMargin: Float,
        padding: Float,
        cardWidth: Float,
        cardHeight: Float,
        gap: Float,
        autoHeight: Boolean,
        imagePaint: Paint,
    ) {
        if (blocks.isEmpty() || contentHeight <= 0f) return
        val innerLeft = cardMargin + padding
        val innerTop = cardMargin + padding
        if (autoHeight) {
            drawBlocks(canvas, blocks, innerLeft, innerTop, gap)
        } else {
            drawScaledContent(
                canvas,
                blocks,
                contentWidth,
                contentHeight,
                innerLeft,
                innerTop,
                cardWidth - 2 * padding,
                cardHeight - 2 * padding,
                gap,
                imagePaint,
            )
        }
    }

    private class ContentBlock(
        val height: Float,
        val draw: (canvas: Canvas, left: Float, top: Float) -> Unit,
    )

    private fun buildContentBlocks(
        pass: PassUiModel,
        options: PassImageExportOptions,
        artwork: Bitmap?,
        thumbnail: Bitmap?,
        barcode: Bitmap?,
        contentWidth: Int,
        width: Float,
        imagePaint: Paint,
        barcodeOnly: Boolean,
    ): List<ContentBlock> = buildList {
        val content = options.content
        if (content.details && pass.description.isNotBlank()) {
            add(titleBlock(pass, thumbnail, contentWidth, width, imagePaint))
            add(dividerBlock(pass, contentWidth, width))
        }
        if (barcode != null) {
            val quietZone = if (barcodeOnly) 0f else width * BARCODE_PADDING_RATIO
            add(ContentBlock(barcode.height + 2 * quietZone) { canvas, left, top ->
                val x = left + (contentWidth - barcode.width) / 2f
                canvas.drawBitmap(barcode, x, top + quietZone, imagePaint)
            })
        }
        if (artwork != null) {
            val scale = minOf(
                contentWidth / artwork.width.toFloat(),
                width * MAX_ARTWORK_HEIGHT_RATIO / artwork.height.toFloat(),
                1f,
            )
            val drawWidth = (artwork.width * scale).toInt().coerceAtLeast(1)
            val drawHeight = (artwork.height * scale).toInt().coerceAtLeast(1)
            add(ContentBlock(drawHeight.toFloat()) { canvas, left, top ->
                val x = left + (contentWidth - drawWidth) / 2f
                canvas.drawBitmap(artwork, null, RectF(x, top, x + drawWidth, top + drawHeight), imagePaint)
            })
        }
        val lines = detailLines(pass, content)
        if (lines.isNotEmpty()) {
            val details = textLayout(
                text = lines.joinToString("\n"),
                layoutWidth = contentWidth,
                textSize = width * DETAILS_TEXT_RATIO,
                color = Color.BLACK,
                bold = false,
                centered = false,
                lineSpacingAdd = width * DETAILS_TEXT_RATIO * 0.35f,
            )
            add(ContentBlock(details.height.toFloat()) { canvas, left, top -> canvas.drawLayout(details, left, top) })
        }
    }

    private fun titleBlock(
        pass: PassUiModel,
        thumbnail: Bitmap?,
        contentWidth: Int,
        width: Float,
        imagePaint: Paint,
    ): ContentBlock {
        val iconSize = width * TITLE_ICON_RATIO
        val iconGap = width * TITLE_ICON_GAP_RATIO
        val title = textLayout(
            text = pass.description,
            layoutWidth = (contentWidth - iconSize - iconGap).toInt().coerceAtLeast(1),
            textSize = width * TITLE_TEXT_RATIO,
            color = TITLE_COLOR,
            bold = true,
            centered = false,
            lineSpacingAdd = 0f,
        )
        val blockHeight = maxOf(iconSize, title.height.toFloat())
        return ContentBlock(blockHeight) { canvas, left, top ->
            canvas.drawTitleIcon(pass, thumbnail, left, top + (blockHeight - iconSize) / 2f, iconSize, imagePaint)
            canvas.drawLayout(title, left + iconSize + iconGap, top + (blockHeight - title.height) / 2f)
        }
    }

    private fun dividerBlock(pass: PassUiModel, contentWidth: Int, width: Float): ContentBlock {
        val height = width * DIVIDER_HEIGHT_RATIO
        val lineWidth = contentWidth * DIVIDER_WIDTH_RATIO
        val stroke = width * DIVIDER_STROKE_RATIO
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pass.accentColor }
        return ContentBlock(height) { canvas, left, top ->
            val x = left + (contentWidth - lineWidth) / 2f
            val y = top + height / 2f
            canvas.drawRoundRect(x, y - stroke / 2f, x + lineWidth, y + stroke / 2f, stroke / 2f, stroke / 2f, paint)
        }
    }

    private fun detailLines(pass: PassUiModel, content: PassImageContent): List<String> = buildList {
        if (content.details) {
            pass.creator?.takeIf { it.isNotBlank() }?.let { add("Created by: $it") }
            pass.notes.takeIf(String::isNotBlank)?.let { add("Note: $it") }
            pass.fields.filter { !it.hidden || content.hiddenFields }.forEach { add("${it.label}: ${it.value}") }
        }
        if (content.dateTime) {
            pass.calendarTimeSpan?.from?.let { add("Starts: ${it.format(DATE_TIME_FORMATTER)}") }
            pass.calendarTimeSpan?.to?.let { add("Ends: ${it.format(DATE_TIME_FORMATTER)}") }
        }
        if (content.location) {
            pass.locations.forEach { location ->
                val name = location.name?.takeIf { it.isNotBlank() }
                add("Location: ${name ?: "${location.latitude}, ${location.longitude}"}")
            }
        }
    }

    private fun textLayout(
        text: String,
        layoutWidth: Int,
        textSize: Float,
        color: Int,
        bold: Boolean,
        centered: Boolean,
        lineSpacingAdd: Float,
    ): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.textSize = textSize
            this.color = color
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, layoutWidth)
            .setAlignment(if (centered) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(lineSpacingAdd, 1f)
            .build()
    }

    private fun decodeArtwork(pass: PassUiModel, kinds: List<PassArtworkKind>, maxWidth: Int): Bitmap? {
        val model = pass.displayArtwork(kinds) ?: pass.artwork.firstOrNull() ?: return null
        val bytes = model.bytes
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxWidth && sampleSize <= Int.MAX_VALUE / 2) sampleSize *= 2
        return runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        }.getOrNull()
    }

    private fun renderBarcode(
        pass: PassUiModel,
        options: PassImageExportOptions,
        contentWidth: Int,
        width: Float,
        barcodeOnly: Boolean,
    ): Bitmap? {
        if (!options.content.barcode) return null
        val format = pass.barcodeFormat ?: return null
        val value = pass.barcodeMessage?.takeIf { it.isNotBlank() } ?: return null
        val availableWidth = (contentWidth - 2 * width * BARCODE_PADDING_RATIO).toInt().coerceAtLeast(1)
        val linear = CrispBarcodeRenderer.isLinear(format)
        val maxWidth = if (barcodeOnly) contentWidth else availableWidth
        val maxHeight = when {
            linear -> (width * LINEAR_BARCODE_HEIGHT_RATIO).toInt()
            barcodeOnly -> contentWidth
            else -> (width * BARCODE_HEIGHT_RATIO).toInt()
        }.coerceAtLeast(1)
        return runCatching {
            CrispBarcodeRenderer.renderBitmap(value, format, maxWidth, maxHeight)
        }.getOrNull()
    }

    private fun measureContentHeight(blocks: List<ContentBlock>, gap: Float): Float {
        if (blocks.isEmpty()) return 0f
        var height = gap * (blocks.size - 1)
        blocks.forEach { height += it.height }
        return height
    }

    private fun drawBlocks(canvas: Canvas, blocks: List<ContentBlock>, left: Float, top: Float, gap: Float) {
        var y = top
        blocks.forEachIndexed { index, block ->
            if (index > 0) y += gap
            block.draw(canvas, left, y)
            y += block.height
        }
    }

    private fun drawScaledContent(
        canvas: Canvas,
        blocks: List<ContentBlock>,
        contentWidth: Int,
        contentHeight: Float,
        innerLeft: Float,
        innerTop: Float,
        innerWidth: Float,
        innerHeight: Float,
        gap: Float,
        imagePaint: Paint,
    ) {
        val contents = createBitmap(contentWidth, contentHeight.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        try {
            contents.eraseColor(Color.TRANSPARENT)
            drawBlocks(Canvas(contents), blocks, 0f, 0f, gap)
            val scale = minOf(innerWidth / contentWidth, innerHeight / contentHeight, 1f).coerceIn(0f, 1f)
            val drawWidth = contentWidth * scale
            val drawHeight = contentHeight * scale
            val left = innerLeft + (innerWidth - drawWidth) / 2f
            val top = innerTop + (innerHeight - drawHeight) / 2f
            canvas.drawBitmap(contents, null, RectF(left, top, left + drawWidth, top + drawHeight), imagePaint)
        } finally {
            contents.recycle()
        }
    }

    private fun drawCard(canvas: Canvas, left: Float, top: Float, cardWidth: Float, cardHeight: Float, width: Float) {
        val radius = width * CARD_CORNER_RATIO
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(width * CARD_SHADOW_RADIUS_RATIO, 0f, width * CARD_SHADOW_DY_RATIO, CARD_SHADOW_COLOR)
        }
        canvas.drawRoundRect(left, top, left + cardWidth, top + cardHeight, radius, radius, paint)
    }

    private fun Canvas.drawTitleIcon(
        pass: PassUiModel,
        thumbnail: Bitmap?,
        left: Float,
        top: Float,
        size: Float,
        imagePaint: Paint,
    ) {
        val radius = size * 0.22f
        val bounds = RectF(left, top, left + size, top + size)
        if (thumbnail != null) {
            val clip = Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) }
            withSave {
                clipPath(clip)
                val edge = minOf(thumbnail.width, thumbnail.height)
                val sourceLeft = (thumbnail.width - edge) / 2
                val sourceTop = (thumbnail.height - edge) / 2
                drawBitmap(thumbnail, Rect(sourceLeft, sourceTop, sourceLeft + edge, sourceTop + edge), bounds, imagePaint)
            }
        } else {
            drawRoundRect(bounds, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TITLE_ICON_BACKGROUND })
            val initial = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = TITLE_COLOR
                textSize = size * 0.5f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val baseline = top + size / 2f - (initial.descent() + initial.ascent()) / 2f
            drawText(pass.homeCardInitial(), left + size / 2f, baseline, initial)
        }
    }

    // Orientation selects the long dimension, so a wide ratio becomes tall in portrait and stays wide in landscape.
    private fun orientedHeightFactor(ratio: PassImageAspectRatio, orientation: PassImageOrientation): Float {
        val value = ratio.ratioWidth / ratio.ratioHeight
        return if (orientation == PassImageOrientation.PORTRAIT) maxOf(value, 1f / value) else minOf(value, 1f / value)
    }

    private fun Canvas.drawLayout(layout: StaticLayout, left: Float, top: Float) {
        withSave {
            translate(left, top)
            layout.draw(this)
        }
    }

    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · HH:mm z", Locale.getDefault())
    private val TITLE_COLOR = 0xFF1A1A1A.toInt()
    private val TITLE_ICON_BACKGROUND = 0xFFEDEDED.toInt()

    private const val CARD_MARGIN_RATIO = 0.03f
    private const val CARD_PADDING_RATIO = 0.05f
    private const val BARCODE_ONLY_MARGIN_RATIO = 0.015f
    private const val BARCODE_ONLY_PADDING_RATIO = 0.02f
    private const val CARD_CORNER_RATIO = 0.04f
    private const val CARD_SHADOW_RADIUS_RATIO = 0.02f
    private const val CARD_SHADOW_DY_RATIO = 0.01f
    private const val CARD_SHADOW_COLOR = 0x33000000
    private const val BLOCK_GAP_RATIO = 0.03f
    private const val TITLE_TEXT_RATIO = 0.055f
    private const val TITLE_ICON_RATIO = 0.075f
    private const val TITLE_ICON_GAP_RATIO = 0.025f
    private const val DETAILS_TEXT_RATIO = 0.032f
    private const val DIVIDER_HEIGHT_RATIO = 0.012f
    private const val DIVIDER_WIDTH_RATIO = 0.18f
    private const val DIVIDER_STROKE_RATIO = 0.005f
    private const val MAX_ARTWORK_HEIGHT_RATIO = 0.5f
    private const val BARCODE_HEIGHT_RATIO = 0.32f
    private const val LINEAR_BARCODE_HEIGHT_RATIO = 0.14f
    private const val BARCODE_PADDING_RATIO = 0.01f
}
