package org.ligi.passandroid.model.pass

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import com.squareup.moshi.JsonClass
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.functions.generateBitmapDrawable
import timber.log.Timber
import java.util.*

@JsonClass(generateAdapter = false)
class BarCode(val format: PassBarCodeFormat?, val message: String? = UUID.randomUUID().toString().uppercase(Locale.ROOT)) : KoinComponent {

    val tracker: Tracker by inject ()
    var alternativeText: String? = null

    fun getBitmap(resources: Resources): BitmapDrawable? {
        if (message == null) {
            tracker.trackException("No Barcode in pass - strange", false)
            return null
        }

        if (format == null) {
            Timber.w("Barcode format is null - fallback to QR")
            tracker.trackException("Barcode format is null - fallback to QR", false)
            return generateBitmapDrawable(resources, message, PassBarCodeFormat.QR_CODE)
        }

        return generateBitmapDrawable(resources, message, format)

    }

    companion object {

        fun getFormatFromString(format: String): PassBarCodeFormat {
            val normalized = format.uppercase(Locale.ENGLISH).replace('-', '_')
            return when {
                normalized.contains("417") -> PassBarCodeFormat.PDF_417
                normalized.contains("AZTEC") -> PassBarCodeFormat.AZTEC
                normalized.contains("CODABAR") -> PassBarCodeFormat.CODABAR
                normalized.contains("93") -> PassBarCodeFormat.CODE_93
                normalized.contains("128") -> PassBarCodeFormat.CODE_128
                normalized.contains("39") -> PassBarCodeFormat.CODE_39
                normalized.contains("UPC_E") || normalized.contains("UPCE") -> PassBarCodeFormat.UPC_E
                normalized.contains("UPC_A") || normalized.contains("UPCA") -> PassBarCodeFormat.UPC_A

                else -> PassBarCodeFormat.QR_CODE

            }


        }
    }

}
