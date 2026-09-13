package org.ligi.passandroid.platform

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.CalendarContract
import androidx.annotation.VisibleForTesting
import androidx.core.content.FileProvider
import org.ligi.passandroid.R
import org.ligi.passandroid.functions.createIntent
import org.ligi.passandroid.functions.CalendarEvent
import org.ligi.passandroid.repository.PassImageExportOptions
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.printing.doPrint
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.TimeZone

data class PlatformLocation(
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
)

interface PlatformActions {
    fun addToCalendar(event: CalendarEvent)
    fun addToCalendarAutomatically(event: CalendarEvent): Boolean
    fun isCalendarEventPresent(event: CalendarEvent): Boolean = false
    fun share(uri: Uri, mimeType: String)
    fun printImage(jobName: String, bitmap: Bitmap)
    fun openLocation(location: PlatformLocation)
    fun openUrl(url: String)
    fun shareImage(pass: PassUiModel, options: PassImageExportOptions)
}

class AndroidPlatformActions(
    private val context: Context,
    private val activityProvider: () -> Context? = { null },
) : PlatformActions {
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

    override fun printImage(jobName: String, bitmap: Bitmap) =
        doPrint(activityProvider() ?: context, jobName, bitmap)

    override fun openLocation(location: PlatformLocation) {
        context.startActivity(createLocationIntent(location).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun openUrl(url: String) {
        context.startActivity(createUrlIntent(url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun shareImage(pass: PassUiModel, options: PassImageExportOptions) {
        val directory = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(directory, "${pass.id}.png")
        val bitmap = PassImageExporter.renderBitmap(pass, options)
        try {
            FileOutputStream(file).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode PNG" }
            }
        } finally {
            bitmap.recycle()
        }
        share(FileProvider.getUriForFile(context, context.getString(R.string.authority_fileprovider), file), "image/png")
    }
}

@VisibleForTesting
internal fun createUrlIntent(url: String): Intent =
    Intent(Intent.ACTION_VIEW, url.toUri())

@VisibleForTesting
internal fun createLocationIntent(location: PlatformLocation): Intent =
    Intent(Intent.ACTION_VIEW, geoUri(location.address, location.latitude, location.longitude).toUri())

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
