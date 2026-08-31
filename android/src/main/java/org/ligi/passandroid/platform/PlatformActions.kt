package org.ligi.passandroid.platform

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.annotation.VisibleForTesting
import org.ligi.passandroid.functions.createIntent
import org.ligi.passandroid.functions.CalendarEvent
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.printing.doPrint
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import java.util.TimeZone

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
    fun addToCalendarAutomatically(event: CalendarEvent): Boolean
    fun isCalendarEventPresent(event: CalendarEvent): Boolean = false
    fun share(uri: Uri, mimeType: String)
    fun print(pass: PrintablePass)
    fun openLocation(location: PlatformLocation)
}

class AndroidPlatformActions(private val context: Context) : PlatformActions {
    override fun addToCalendar(event: CalendarEvent) {
        context.startActivity(createIntent(event).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun addToCalendarAutomatically(event: CalendarEvent): Boolean {
        val calendarId = findWritableCalendarId() ?: return false
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.DTSTART, event.beginTimeMillis)
            put(CalendarContract.Events.DTEND, event.endTimeMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.DESCRIPTION, event.description)
        }
        return context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) != null
    }

    override fun isCalendarEventPresent(event: CalendarEvent): Boolean {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return false
        return context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.DESCRIPTION} = ? AND ${CalendarContract.Events.DTSTART} = ?",
            arrayOf(event.description, event.beginTimeMillis.toString()),
            null,
        )?.use { it.moveToFirst() } == true
    }

    private fun findWritableCalendarId(): Long? = context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        arrayOf(CalendarContract.Calendars._ID),
        "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
        arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
        "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars._ID} ASC",
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getLong(0) else null
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
