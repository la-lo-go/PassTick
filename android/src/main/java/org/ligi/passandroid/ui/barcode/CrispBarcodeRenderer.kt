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

    fun renderMatrix(
        data: String,
        format: PassBarCodeFormat,
        maxWidthPx: Int,
        maxHeightPx: Int,
    ): RenderedBarcodeMatrix? {
        if (data.isEmpty() || !supports(format) || maxWidthPx <= 0 || maxHeightPx <= 0) return null

        val source = try {
            MultiFormatWriter().encode(
                data,
                format.zxingBarCodeFormat(),
                1,
                1,
                mapOf(EncodeHintType.MARGIN to 0),
            )
        } catch (_: WriterException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        } catch (_: ArrayIndexOutOfBoundsException) {
            return null
        }

        val quietZone = format.quietZonePixels()
        val contentWidth = source.width + quietZone.horizontal * 2
        val contentHeight = source.height + quietZone.vertical * 2
        val isLinear = format.isLinear()
        val scale = if (isLinear) {
            maxWidthPx / contentWidth
        } else {
            minOf(maxWidthPx / contentWidth, maxHeightPx / contentHeight)
        }
        if (scale < 1) return null

        val outputWidth = contentWidth * scale
        val outputHeight = if (isLinear) maxHeightPx else contentHeight * scale
        val output = BitMatrix(outputWidth, outputHeight)
        val left = quietZone.horizontal * scale
        val top = if (isLinear) 0 else quietZone.vertical * scale

        for (sourceY in 0 until source.height) {
            for (sourceX in 0 until source.width) {
                if (!source[sourceX, sourceY]) continue
                val outputX = left + sourceX * scale
                val outputY = if (isLinear) 0 else top + sourceY * scale
                val outputBarHeight = if (isLinear) outputHeight else scale
                output.setRegion(outputX, outputY, scale, outputBarHeight)
            }
        }

        return RenderedBarcodeMatrix(output, scale)
    }

    fun renderBitmap(
        data: String,
        format: PassBarCodeFormat,
        maxWidthPx: Int,
        maxHeightPx: Int,
    ): Bitmap? {
        val rendered = renderMatrix(data, format, maxWidthPx, maxHeightPx) ?: return null
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
    PassBarCodeFormat.CODE_39,
    PassBarCodeFormat.CODE_128,
    PassBarCodeFormat.EAN_8,
    PassBarCodeFormat.EAN_13,
    PassBarCodeFormat.ITF,
    -> true

    else -> false
}

private fun PassBarCodeFormat.quietZonePixels() = when (this) {
    PassBarCodeFormat.QR_CODE -> QuietZone(horizontal = 4, vertical = 4)
    PassBarCodeFormat.AZTEC -> QuietZone(horizontal = 2, vertical = 2)
    PassBarCodeFormat.DATA_MATRIX -> QuietZone(horizontal = 1, vertical = 1)
    PassBarCodeFormat.PDF_417 -> QuietZone(horizontal = 2, vertical = 8)
    PassBarCodeFormat.CODE_39,
    PassBarCodeFormat.CODE_128,
    PassBarCodeFormat.EAN_8,
    PassBarCodeFormat.EAN_13,
    PassBarCodeFormat.ITF,
    -> QuietZone(horizontal = 10, vertical = 0)
}
