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
        val bitmap = barcodeBitmap(MESSAGE, BarcodeFormat.CODE_128)
        try {
            val codes = DocumentImportProcessor.detectCodes(bitmap)

            assertThat(codes).anyMatch { it.format == PassBarCodeFormat.CODE_128 && it.message == MESSAGE }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun detectsAGeneratedCodabarBarcode() {
        val bitmap = barcodeBitmap(CODABAR_MESSAGE, BarcodeFormat.CODABAR)
        try {
            val codes = DocumentImportProcessor.detectCodes(bitmap)

            // The Codabar reader removes the start and stop characters.
            assertThat(codes).anyMatch { it.format == PassBarCodeFormat.CODABAR && it.message == CODABAR_TEXT }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun detectsAGeneratedCode93Barcode() {
        val bitmap = barcodeBitmap(CODE_93_MESSAGE, BarcodeFormat.CODE_93)
        try {
            val codes = DocumentImportProcessor.detectCodes(bitmap)

            assertThat(codes).anyMatch { it.format == PassBarCodeFormat.CODE_93 && it.message == CODE_93_MESSAGE }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun detectsAGeneratedUpcABarcode() {
        val bitmap = barcodeBitmap(UPC_A_MESSAGE, BarcodeFormat.UPC_A)
        try {
            val codes = DocumentImportProcessor.detectCodes(bitmap)

            // UPC-A and EAN-13 overlap; the reader can report the EAN-13 alias with a leading zero.
            assertThat(codes).anyMatch { code ->
                code.format in UPC_A_FORMATS && (code.message == UPC_A_MESSAGE || code.message == "0$UPC_A_MESSAGE")
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun detectsAGeneratedUpcEBarcode() {
        val bitmap = barcodeBitmap(UPC_E_MESSAGE, BarcodeFormat.UPC_E)
        try {
            val codes = DocumentImportProcessor.detectCodes(bitmap)

            assertThat(codes).anyMatch { it.format == PassBarCodeFormat.UPC_E && it.message == UPC_E_MESSAGE }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun fullTurnRotationKeepsTheSize() {
        val bitmap = barcodeBitmap(MESSAGE, BarcodeFormat.CODE_128)
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
        val bitmap = barcodeBitmap(MESSAGE, BarcodeFormat.CODE_128)
        val cropped = DocumentImportProcessor.crop(bitmap, NormalizedRect.Full)
        try {
            assertThat(cropped.width).isEqualTo(CODE_WIDTH)
            assertThat(cropped.height).isEqualTo(CODE_HEIGHT)
        } finally {
            if (cropped !== bitmap) cropped.recycle()
            bitmap.recycle()
        }
    }

    private fun barcodeBitmap(content: String, format: BarcodeFormat): Bitmap =
        MultiFormatWriter().encode(content, format, CODE_WIDTH, CODE_HEIGHT).toBitmap()

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
        const val CODABAR_MESSAGE = "A1234567890B"
        const val CODABAR_TEXT = "1234567890"
        const val CODE_93_MESSAGE = "TEST93"
        const val UPC_A_MESSAGE = "036000291452"
        const val UPC_E_MESSAGE = "01234565"
        val UPC_A_FORMATS = setOf(PassBarCodeFormat.UPC_A, PassBarCodeFormat.EAN_13)
    }
}
