package org.ligi.passandroid.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import org.ligi.passandroid.functions.createIntent
import org.ligi.passandroid.functions.CalendarEvent
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.printing.doPrint
import androidx.core.net.toUri

data class PlatformLocation(
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
)
data class PrintableField(val label: String, val value: String)
data class PrintablePass(
    val description: String,
    val barcodeFormat: PassBarCodeFormat?,
    val barcodeMessage: String?,
    val barcodeAlternativeText: String?,
    val fields: List<PrintableField>,
)

interface PlatformActions {
    fun addToCalendar(event: CalendarEvent)
    fun share(uri: Uri, mimeType: String)
    fun print(pass: PrintablePass)
    fun openLocation(location: PlatformLocation)
}

class AndroidPlatformActions(private val context: Context) : PlatformActions {
    override fun addToCalendar(event: CalendarEvent) {
        context.startActivity(createIntent(event).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun share(uri: Uri, mimeType: String) {
        val shareIntent = createShareIntent(uri, mimeType)
        context.startActivity(Intent.createChooser(shareIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun print(pass: PrintablePass) = doPrint(context, pass)

    override fun openLocation(location: PlatformLocation) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, geoUri(location.address, location.latitude, location.longitude).toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@VisibleForTesting
internal fun geoUri(address: String?, latitude: Double?, longitude: Double?): String {
    val query = java.net.URLEncoder.encode(
        address?.takeIf(String::isNotBlank) ?: listOfNotNull(latitude, longitude).joinToString(","),
        java.nio.charset.StandardCharsets.UTF_8.name(),
    ).replace("+", "%20")
    return if (latitude != null && longitude != null) {
        "geo:$latitude,$longitude?q=$query"
    } else {
        "geo:0,0?q=$query"
    }
}

@VisibleForTesting
fun createShareIntent(uri: Uri, mimeType: String) = Intent(Intent.ACTION_SEND).apply {
    type = mimeType
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
