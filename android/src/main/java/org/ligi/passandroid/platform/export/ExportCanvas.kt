package org.ligi.passandroid.platform.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import com.google.zxing.common.BitMatrix
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.ui.barcode.CrispBarcodeRenderer
import org.ligi.passandroid.ui.state.PassArtworkUiModel
import java.io.Closeable
import java.util.Locale

/**
 * A part of an export with a known height. Painters measure blocks first and draw them later, so a page can be
 * sized, scaled, or split before anything is drawn.
 */
internal class Block(val height: Float, val draw: Canvas.(left: Float, top: Float) -> Unit)

internal fun List<Block>.totalHeight(gap: Float): Float =
    if (isEmpty()) 0f else sumOf { it.height.toDouble() }.toFloat() + gap * (size - 1)

internal fun Canvas.drawBlocks(blocks: List<Block>, left: Float, top: Float, gap: Float) {
    var y = top
    blocks.forEach { block ->
        block.draw(this, left, y)
        y += block.height + gap
    }
}

/** A stack of blocks as one block. */
internal fun column(blocks: List<Block>, gap: Float): Block =
    Block(blocks.totalHeight(gap)) { left, top -> drawBlocks(blocks, left, top, gap) }

internal fun spacer(height: Float) = Block(height) { _, _ -> }

/** The colors of the colored ticket part. */
internal class ExportColors(
    val container: Int,
    val content: Int,
    val label: Int,
    val tonal: Int,
    /** A light container needs an outline, or its edge disappears on a white page. */
    val outlined: Boolean,
) {
    val edge: Int get() = if (outlined) OUTLINE else container

    companion object {
        private const val LIGHT_CONTAINER_LUMINANCE = 0.45
        private const val WHITE_CONTAINER_LUMINANCE = 0.9
        private const val LABEL_ALPHA = 0.74f
        private const val TONAL_ALPHA = 0.12f
        const val INK = 0xFF1B1B1F.toInt()
        const val OUTLINE = 0xFFC6C6CC.toInt()
        private const val MUTED_INK = 0xFF46464F.toInt()

        fun of(accentColor: Int, neutral: Boolean, monochrome: Boolean): ExportColors = when {
            monochrome -> ExportColors(Color.WHITE, Color.BLACK, MUTED_INK, 0xFFEEEEEE.toInt(), outlined = true)
            neutral -> ExportColors(0xFFECECF1.toInt(), INK, MUTED_INK, 0xFFDEDEE4.toInt(), outlined = false)
            else -> {
                val container = accentColor or 0xFF000000.toInt()
                val luminance = ColorUtils.calculateLuminance(container)
                val content = if (luminance > LIGHT_CONTAINER_LUMINANCE) INK else Color.WHITE
                ExportColors(
                    container,
                    content,
                    ColorUtils.setAlphaComponent(content, (LABEL_ALPHA * 255).toInt()),
                    ColorUtils.setAlphaComponent(content, (TONAL_ALPHA * 255).toInt()),
                    outlined = luminance > WHITE_CONTAINER_LUMINANCE,
                )
            }
        }
    }
}

internal enum class Weight(val value: Int) { REGULAR(400), SEMI_BOLD(600), BOLD(700), EXTRA_BOLD(800) }

private val typefaces = Weight.entries.associateWith { Typeface.create(Typeface.DEFAULT, it.value, false) }

/** Measured text. Sizes are design units: one unit is one dp of the pass view ticket. */
internal fun text(
    value: String,
    width: Float,
    size: Float,
    color: Int,
    weight: Weight = Weight.REGULAR,
    align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    maxLines: Int = Int.MAX_VALUE,
    letterSpacing: Float = 0f,
): Block {
    // Linear text keeps glyph sizes exact when the canvas is scaled for a bitmap or a PDF page.
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textSize = size
        this.color = color
        typeface = typefaces.getValue(weight)
        this.letterSpacing = letterSpacing
    }
    val layoutWidth = width.toInt().coerceAtLeast(1)
    val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, layoutWidth)
        .setAlignment(align)
        .setIncludePad(false)
        .setMaxLines(maxLines)
        .setEllipsize(if (maxLines == Int.MAX_VALUE) null else android.text.TextUtils.TruncateAt.END)
        .build()
    return Block(layout.height.toFloat()) { left, top -> withTranslation(left, top) { layout.draw(this) } }
}

/** The width of one line of text, with the same paint settings as [text]. */
internal fun textWidth(value: String, size: Float, weight: Weight = Weight.REGULAR, letterSpacing: Float = 0f): Float =
    TextPaint(Paint.LINEAR_TEXT_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textSize = size
        typeface = typefaces.getValue(weight)
        this.letterSpacing = letterSpacing
    }.measureText(value)

/** A field label in small capitals, as in the pass view. */
internal fun label(value: String, width: Float, color: Int, locale: Locale, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): Block? =
    value.takeIf(String::isNotBlank)?.let {
        text(it.uppercase(locale), width, LABEL_SIZE, color, Weight.SEMI_BOLD, align, letterSpacing = LABEL_LETTER_SPACING)
    }

internal const val LABEL_SIZE = 11f
internal const val LABEL_LETTER_SPACING = 0.05f

internal fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

/** Decoded artwork for one export. The images are released when the export ends. */
internal class ExportImages(private val maxEdge: Int) : Closeable {
    private val decoded = mutableMapOf<PassArtworkUiModel, Bitmap?>()

    operator fun get(artwork: PassArtworkUiModel?): Bitmap? = artwork?.let { decoded.getOrPut(it) { decode(it.bytes) } }

    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxEdge) sampleSize *= 2
        return runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        }.getOrNull()
    }

    override fun close() {
        decoded.values.forEach { it?.recycle() }
        decoded.clear()
    }
}

private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

/** Draws [image] scaled to fill [bounds], cropped to the center, with rounded corners. */
internal fun Canvas.drawCropped(image: Bitmap, bounds: RectF, radius: Float) {
    val edge = minOf(image.width, image.height)
    val left = (image.width - edge) / 2
    val top = (image.height - edge) / 2
    withSave {
        clipPath(Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) })
        drawBitmap(image, Rect(left, top, left + edge, top + edge), bounds, imagePaint)
    }
}

/** An image block that fits inside [maxWidth] by [maxHeight], centered, never larger than its decoded size allows. */
internal fun imageBlock(image: Bitmap, maxWidth: Float, maxHeight: Float, radius: Float): Block {
    val scale = minOf(maxWidth / image.width, maxHeight / image.height)
    val width = image.width * scale
    val height = image.height * scale
    return Block(height) { left, top ->
        val x = left + (maxWidth - width) / 2f
        val bounds = RectF(x, top, x + width, top + height)
        withSave {
            clipPath(Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) })
            drawBitmap(image, null, bounds, imagePaint)
        }
    }
}

// Codes are rendered at this many matrix pixels per design unit. Integer module scaling needs the extra resolution.
private const val CODE_OVERSAMPLING = 8
private const val QUADRATIC_HEIGHT_RATIO = 0.78f
private const val WIDE_CODE_ASPECT = 2.6f

/** The height budget of a code in a slot of [width] units, like the pass view. */
internal fun codeHeight(format: PassBarCodeFormat, width: Float, maxQuadratic: Float): Float =
    if (format.isQuadratic()) minOf(width * QUADRATIC_HEIGHT_RATIO, maxQuadratic) else width / WIDE_CODE_ASPECT

/**
 * A code drawn as black rectangles on white, so it stays sharp in a PDF at any zoom. The block is centered in
 * [width] and is null when the code cannot be encoded.
 */
internal fun codeBlock(format: PassBarCodeFormat, message: String, width: Float, maxHeight: Float): Block? {
    val rendered = CrispBarcodeRenderer.renderMatrix(
        message,
        format,
        (width * CODE_OVERSAMPLING).toInt(),
        (maxHeight * CODE_OVERSAMPLING).toInt(),
    ) ?: return null
    val matrix = rendered.pixels
    val unit = 1f / CODE_OVERSAMPLING
    val codeWidth = matrix.width * unit
    val codeHeight = matrix.height * unit
    val modules = modulePath(matrix, rendered.moduleScale, CrispBarcodeRenderer.isLinear(format), unit)
    val black = Paint().apply { color = Color.BLACK }
    val white = Paint().apply { color = Color.WHITE }
    return Block(codeHeight) { left, top ->
        val x0 = left + (width - codeWidth) / 2f
        drawRect(x0, top, x0 + codeWidth, top + codeHeight, white)
        withTranslation(x0, top) { drawPath(modules, black) }
    }
}

// All modules form one path. Separate rectangles leave hairline seams between rows when the canvas is scaled.
private fun modulePath(matrix: BitMatrix, moduleScale: Int, linear: Boolean, unit: Float): Path {
    val modules = Path()
    // A linear code has full-height bars, so one scan of the middle row finds every bar.
    val rows = if (linear) listOf(matrix.height / 2) else (0 until matrix.height step moduleScale).toList()
    rows.forEach { y ->
        val (rowTop, rowBottom) = if (linear) barExtent(matrix, y) else y to y + moduleScale
        darkRuns(matrix, y).forEach { (start, end) ->
            modules.addRect(start * unit, rowTop * unit, end * unit, rowBottom * unit, Path.Direction.CW)
        }
    }
    return modules
}

/** The dark runs of one matrix row as start and end columns. */
private fun darkRuns(matrix: BitMatrix, y: Int): List<Pair<Int, Int>> = buildList {
    var x = 0
    while (x < matrix.width) {
        val start = x
        while (x < matrix.width && matrix[x, y]) x++
        if (x > start) add(start to x) else x++
    }
}

private fun barExtent(matrix: BitMatrix, row: Int): Pair<Int, Int> {
    val x = (0 until matrix.width).firstOrNull { matrix[it, row] } ?: return row to row + 1
    var top = row
    while (top > 0 && matrix[x, top - 1]) top--
    var bottom = row + 1
    while (bottom < matrix.height && matrix[x, bottom]) bottom++
    return top to bottom
}
