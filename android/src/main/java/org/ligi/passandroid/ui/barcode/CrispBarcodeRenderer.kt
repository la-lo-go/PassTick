package org.ligi.passandroid.ui.barcode

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import org.ligi.passandroid.model.pass.PassBarCodeFormat

data class RenderedBarcodeMatrix(
    val pixels: BitMatrix,
    val moduleScale: Int,
)

object CrispBarcodeRenderer {
    private val supportedFormats = PassBarCodeFormat.entries.toSet()

    fun supports(format: PassBarCodeFormat): Boolean = format in supportedFormats

    fun isLinear(format: PassBarCodeFormat): Boolean = format.isLinear()

    fun renderMatrix(
        data: String,
        format: PassBarCodeFormat,
        maxWidthPx: Int,
        maxHeightPx: Int,
        extraQuietZone: Boolean = false,
    ): RenderedBarcodeMatrix? {
        if (!hasRenderableInput(data, format) || !hasRenderableBounds(maxWidthPx, maxHeightPx)) return null

        val source = encodeSource(data, format) ?: return null
        val quietZone = format.quietZonePixels(extraQuietZone)
        val contentWidth = source.width + quietZone.horizontal * 2
        val contentHeight = source.height + quietZone.vertical * 2
        val isLinear = format.isLinear()
        val scale = moduleScale(isLinear, maxWidthPx, maxHeightPx, contentWidth, contentHeight)
        if (scale < 1) return null

        val outputWidth = contentWidth * scale
        // Linear bars fill the available height. A vertical zone needs module-aligned row bounds,
        // so the bar region and both zones stay integer module multiples.
        val barRegion = if (isLinear) linearBarRegion(maxHeightPx, quietZone.vertical, scale) else null
        val outputHeight = if (isLinear) barRegion?.outputHeight ?: maxHeightPx else contentHeight * scale
        val output = BitMatrix(outputWidth, outputHeight)
        val left = quietZone.horizontal * scale
        val top = if (isLinear) barRegion?.top ?: 0 else quietZone.vertical * scale
        val barHeight = if (isLinear) outputHeight - top * 2 else scale

        paintModules(source, output, scale, isLinear, left, top, barHeight)

        return RenderedBarcodeMatrix(output, scale)
    }

    private data class LinearBarRegion(val outputHeight: Int, val top: Int)

    private fun linearBarRegion(maxHeightPx: Int, verticalModules: Int, scale: Int): LinearBarRegion? {
        if (verticalModules == 0) return null
        val rows = maxHeightPx / scale
        if (rows <= verticalModules * 2) return null
        return LinearBarRegion(outputHeight = rows * scale, top = verticalModules * scale)
    }

    private fun hasRenderableInput(data: String, format: PassBarCodeFormat): Boolean =
        data.isNotEmpty() && supports(format)

    private fun hasRenderableBounds(maxWidthPx: Int, maxHeightPx: Int): Boolean =
        maxWidthPx > 0 && maxHeightPx > 0

    private fun encodeSource(data: String, format: PassBarCodeFormat): BitMatrix? = try {
        MultiFormatWriter().encode(
            data,
            format.zxingBarCodeFormat(),
            1,
            1,
            mapOf(EncodeHintType.MARGIN to 0),
        )
    } catch (_: WriterException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: ArrayIndexOutOfBoundsException) {
        null
    }

    private fun moduleScale(
        isLinear: Boolean,
        maxWidthPx: Int,
        maxHeightPx: Int,
        contentWidth: Int,
        contentHeight: Int,
    ): Int = if (isLinear) {
        maxWidthPx / contentWidth
    } else {
        minOf(maxWidthPx / contentWidth, maxHeightPx / contentHeight)
    }

    private fun paintModules(
        source: BitMatrix,
        output: BitMatrix,
        scale: Int,
        isLinear: Boolean,
        left: Int,
        top: Int,
        barHeight: Int,
    ) {
        for (sourceY in 0 until source.height) {
            for (sourceX in 0 until source.width) {
                if (!source[sourceX, sourceY]) continue
                val outputX = left + sourceX * scale
                val outputY = if (isLinear) top else top + sourceY * scale
                val outputBarHeight = if (isLinear) barHeight else scale
                output.setRegion(outputX, outputY, scale, outputBarHeight)
            }
        }
    }

    fun renderBitmap(
        data: String,
        format: PassBarCodeFormat,
        maxWidthPx: Int,
        maxHeightPx: Int,
        extraQuietZone: Boolean = false,
    ): Bitmap? {
        val rendered = renderMatrix(data, format, maxWidthPx, maxHeightPx, extraQuietZone) ?: return null
        val matrix = rendered.pixels
        val colors = IntArray(matrix.width * matrix.height)
        var index = 0
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                colors[index++] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(colors, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    }
}

private data class QuietZone(val horizontal: Int, val vertical: Int)

private fun PassBarCodeFormat.isLinear() = when (this) {
    PassBarCodeFormat.CODABAR,
    PassBarCodeFormat.CODE_39,
    PassBarCodeFormat.CODE_93,
    PassBarCodeFormat.CODE_128,
    PassBarCodeFormat.EAN_8,
    PassBarCodeFormat.EAN_13,
    PassBarCodeFormat.ITF,
    PassBarCodeFormat.UPC_A,
    PassBarCodeFormat.UPC_E,
    -> true

    else -> false
}

// One extra module pair. Linear formats gain a vertical zone because their base vertical zone is 0.
private const val ExtraQuietZoneModules = 4

private fun PassBarCodeFormat.quietZonePixels(extraQuietZone: Boolean): QuietZone {
    val base = baseQuietZonePixels()
    if (!extraQuietZone) return base
    return QuietZone(
        horizontal = base.horizontal + ExtraQuietZoneModules,
        vertical = base.vertical + ExtraQuietZoneModules,
    )
}

private fun PassBarCodeFormat.baseQuietZonePixels() = when (this) {
    PassBarCodeFormat.QR_CODE -> QuietZone(horizontal = 4, vertical = 4)
    PassBarCodeFormat.AZTEC -> QuietZone(horizontal = 2, vertical = 2)
    PassBarCodeFormat.DATA_MATRIX -> QuietZone(horizontal = 1, vertical = 1)
    PassBarCodeFormat.PDF_417 -> QuietZone(horizontal = 2, vertical = 8)
    PassBarCodeFormat.CODABAR,
    PassBarCodeFormat.CODE_39,
    PassBarCodeFormat.CODE_93,
    PassBarCodeFormat.CODE_128,
    PassBarCodeFormat.EAN_8,
    PassBarCodeFormat.EAN_13,
    PassBarCodeFormat.ITF,
    PassBarCodeFormat.UPC_A,
    PassBarCodeFormat.UPC_E,
    -> QuietZone(horizontal = 10, vertical = 0)
}
