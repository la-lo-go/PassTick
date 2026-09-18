package org.ligi.passandroid.imports

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.ligi.passandroid.model.pass.PassBarCodeFormat

@RunWith(AndroidJUnit4::class)
class DocumentImportProcessorInstrumentedTest {

    @Test
    fun detectsAGeneratedCode128Barcode() {
        val bitmap = code128Bitmap(MESSAGE)
        try {
            val codes = DocumentImportProcessor.detectCodes(bitmap)

            assertThat(codes).anyMatch { it.format == PassBarCodeFormat.CODE_128 && it.message == MESSAGE }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun fullTurnRotationKeepsTheSize() {
        val bitmap = code128Bitmap(MESSAGE)
        val rotated = DocumentImportProcessor.rotate(bitmap, 720)
        try {
            assertThat(rotated.width).isEqualTo(CODE_WIDTH)
            assertThat(rotated.height).isEqualTo(CODE_HEIGHT)
        } finally {
            if (rotated !== bitmap) rotated.recycle()
            bitmap.recycle()
        }
    }

    @Test
    fun fullCropKeepsTheSize() {
        val bitmap = code128Bitmap(MESSAGE)
        val cropped = DocumentImportProcessor.crop(bitmap, NormalizedRect.Full)
        try {
            assertThat(cropped.width).isEqualTo(CODE_WIDTH)
            assertThat(cropped.height).isEqualTo(CODE_HEIGHT)
        } finally {
            if (cropped !== bitmap) cropped.recycle()
            bitmap.recycle()
        }
    }

    private fun code128Bitmap(content: String): Bitmap =
        MultiFormatWriter().encode(content, BarcodeFormat.CODE_128, CODE_WIDTH, CODE_HEIGHT).toBitmap()

    private fun BitMatrix.toBitmap(): Bitmap {
        val matrixWidth = width
        val matrixHeight = height
        val pixels = IntArray(matrixWidth * matrixHeight)
        for (y in 0 until matrixHeight) {
            val offset = y * matrixWidth
            for (x in 0 until matrixWidth) {
                pixels[offset + x] = if (this[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
    }

    private companion object {
        const val CODE_WIDTH = 1000
        const val CODE_HEIGHT = 300
        const val MESSAGE = "PASSTICK-IMPORT-42"
    }
}
