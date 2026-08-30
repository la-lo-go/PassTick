package org.ligi.passandroid.functions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.VisibleForTesting
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import timber.log.Timber
import java.util.*


fun generateBitmapDrawable(resources: Resources, data: String, type: PassBarCodeFormat): BitmapDrawable? {
    val bitmap = generateBarCodeBitmap(data, type) ?: return null

    return BitmapDrawable(resources, bitmap).apply {
        isFilterBitmap = false
        setAntiAlias(false)
    }
}

@VisibleForTesting
fun generateBarCodeBitmap(data: String, type: PassBarCodeFormat): Bitmap? {

    if (data.isEmpty()) {
        return null
    }

    try {
        val matrix = getBitMatrix(data, type)
        val is1D = matrix.height == 1

        val width = matrix.width
        val height = if (is1D) width / 5 else matrix.height

        // RGB_565 keeps barcode pixels opaque on print and screen surfaces.
        val barcodeImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (y in 0 until height) {
            for (x in 0 until width) {
                barcodeImage.setPixel(x, y, if (matrix.get(x, if (is1D) 0 else y)) 0 else 0xFFFFFF)
            }
        }

        return barcodeImage
    } catch (e: com.google.zxing.WriterException) {
        Timber.w(e, "could not write image")
        return null
    } catch (e: IllegalArgumentException) {
        Timber.w("could not write image: $e")
        return null
    } catch (e: ArrayIndexOutOfBoundsException) {
        Timber.w("could not write image: $e")
        return null
    }

}

@VisibleForTesting
fun getBitMatrix(data: String, type: PassBarCodeFormat) = MultiFormatWriter().encode(data, type.zxingBarCodeFormat(), 0, 0)!!

