package org.ligi.passandroid.platform

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.barcode.CrispBarcodeRenderer

enum class PassImageExportMode { FULL, BARCODE, CUSTOM }
data class PassImageExportSelection(val artwork: Boolean, val text: Boolean, val barcode: Boolean)

/** Writes a shareable PNG without exposing the app's private pass files. */
object PassImageExporter {
    fun write(
        resolver: ContentResolver,
        uri: Uri,
        pass: PassUiModel,
        mode: PassImageExportMode,
        selection: PassImageExportSelection? = null,
    ) {
        val bitmap = renderBitmap(pass, mode, selection)
        try {
            resolver.openOutputStream(uri)?.use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode PNG" }
            } ?: error("Could not open image destination")
        } finally { bitmap.recycle() }
    }

    internal fun renderBitmap(
        pass: PassUiModel,
        mode: PassImageExportMode,
        selection: PassImageExportSelection? = null,
    ): Bitmap {
        val custom = selection ?: PassImageExportSelection(true, true, true)
        val includeArtwork = if (mode == PassImageExportMode.CUSTOM) custom.artwork else mode != PassImageExportMode.BARCODE
        val includeText = if (mode == PassImageExportMode.CUSTOM) custom.text else mode != PassImageExportMode.BARCODE
        val includeBarcode = if (mode == PassImageExportMode.CUSTOM) custom.barcode else true
        require(mode != PassImageExportMode.CUSTOM || includeArtwork || includeText || includeBarcode) { "Select content to export" }
        val artwork = decodeArtwork(pass)
        val barcode = pass.barcodeMessage?.takeIf(String::isNotBlank)
        require(mode != PassImageExportMode.BARCODE || (!barcode.isNullOrBlank() && pass.barcodeFormat != null)) {
            "Barcode export requires a barcode"
        }
        return try {
            val layout = createLayout(pass, artwork, includeArtwork, includeText, includeBarcode, mode)
            Bitmap.createBitmap(WIDTH, layout.height, Bitmap.Config.ARGB_8888).also { bitmap ->
                drawContent(bitmap, pass, artwork, barcode, layout, includeArtwork, includeText, includeBarcode, mode)
            }
        } finally { artwork?.recycle() }
    }

    private const val WIDTH = 1200

    private data class ExportLayout(val text: StaticLayout?, val artworkWidth: Int, val artworkHeight: Int, val height: Int)

    private fun decodeArtwork(pass: PassUiModel): Bitmap? = pass.artwork.firstOrNull()?.bytes?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }

    private fun createLayout(pass: PassUiModel, artwork: Bitmap?, includeArtwork: Boolean, includeText: Boolean, includeBarcode: Boolean, mode: PassImageExportMode): ExportLayout {
        val luminance = .299 * android.graphics.Color.red(pass.accentColor) + .587 * android.graphics.Color.green(pass.accentColor) + .114 * android.graphics.Color.blue(pass.accentColor)
        val text = if (includeText) createTextLayout(pass, if (luminance < 150) android.graphics.Color.WHITE else android.graphics.Color.BLACK) else null
        val (artworkWidth, artworkHeight) = artworkSize(artwork, includeArtwork)
        val barcodeHeight = if (includeBarcode) 360 else 0
        val height = if (mode == PassImageExportMode.BARCODE) 420 else 70 + artworkHeight + 24 + (text?.height ?: 0) + 24 + barcodeHeight
        return ExportLayout(text, artworkWidth, artworkHeight, height)
    }

    private fun artworkSize(artwork: Bitmap?, included: Boolean): Pair<Int, Int> {
        if (!included || artwork == null) return 0 to 0
        val scale = minOf(WIDTH.toFloat() / artwork.width, 500f / artwork.height)
        return (artwork.width * scale).toInt() to (artwork.height * scale).toInt()
    }

    private fun createTextLayout(pass: PassUiModel, color: Int): StaticLayout {
        val lines = buildList {
            add(pass.description)
            pass.creator?.takeIf(String::isNotBlank)?.let { add("Created by: $it") }
            pass.fields.filterNot { it.hidden }.forEach { add("${it.label}: ${it.value}") }
            pass.calendarTimeSpan?.from?.let { add("Starts: $it") }
            pass.calendarTimeSpan?.to?.let { add("Ends: $it") }
            pass.locations.forEach { add("Location: ${it.name ?: "${it.latitude}, ${it.longitude}"}") }
        }.joinToString("\n")
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 40f; this.color = color }
        return StaticLayout.Builder.obtain(lines, 0, lines.length, paint, WIDTH - 96).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build()
    }

    private fun drawContent(bitmap: Bitmap, pass: PassUiModel, artwork: Bitmap?, barcode: String?, layout: ExportLayout, includeArtwork: Boolean, includeText: Boolean, includeBarcode: Boolean, mode: PassImageExportMode) {
        val luminance = .299 * android.graphics.Color.red(pass.accentColor) + .587 * android.graphics.Color.green(pass.accentColor) + .114 * android.graphics.Color.blue(pass.accentColor)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (luminance < 150) android.graphics.Color.WHITE else android.graphics.Color.BLACK }
        Canvas(bitmap).apply {
            drawColor(pass.accentColor)
            val afterArtwork = drawArtworkSection(this, artwork, paint, layout, includeArtwork)
            val afterText = drawTextSection(this, layout.text, afterArtwork, includeText)
            drawBarcodeSection(this, pass, barcode, paint, afterText, includeBarcode, mode)
        }
    }

    private fun drawArtworkSection(canvas: Canvas, artwork: Bitmap?, paint: Paint, layout: ExportLayout, included: Boolean): Float {
        if (!included || artwork == null) return 70f
        canvas.drawArtwork(artwork, paint, layout.artworkWidth, layout.artworkHeight)
        return 70f + layout.artworkHeight + 24f
    }

    private fun drawTextSection(canvas: Canvas, text: StaticLayout?, y: Float, included: Boolean): Float {
        if (!included || text == null) return y
        canvas.drawTextLayout(text, y)
        return y + text.height + 24f
    }

    private fun drawBarcodeSection(canvas: Canvas, pass: PassUiModel, value: String?, paint: Paint, y: Float, included: Boolean, mode: PassImageExportMode) {
        if (!included || value.isNullOrBlank()) return
        canvas.drawBarcode(pass, value, paint, if (mode == PassImageExportMode.BARCODE) 40f else y + 24f)
    }

    private fun Canvas.drawArtwork(artwork: Bitmap, paint: Paint, width: Int, height: Int) {
        val left = (WIDTH - width) / 2
        drawBitmap(artwork, null, android.graphics.Rect(left, 70, left + width, 70 + height), paint)
    }
    private fun Canvas.drawTextLayout(layout: StaticLayout, y: Float) { save(); translate(48f, y); layout.draw(this); restore() }
    private fun Canvas.drawBarcode(pass: PassUiModel, value: String, paint: Paint, top: Float) {
        val code = pass.barcodeFormat?.let { CrispBarcodeRenderer.renderBitmap(value, it, WIDTH - 96, 300) } ?: error("Barcode format cannot be rendered")
        drawBitmap(code, 48f, top, paint); code.recycle()
    }

}
