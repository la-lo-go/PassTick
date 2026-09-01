package org.ligi.passandroid.functions

import android.content.Intent
import android.provider.CalendarContract
import androidx.annotation.VisibleForTesting
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassImpl

const val DEFAULT_EVENT_LENGTH_IN_HOURS = 2L

data class CalendarEvent(
    val title: String?,
    val beginTimeMillis: Long,
    val endTimeMillis: Long,
    val location: String?,
    val description: String? = null,
)

@VisibleForTesting
fun createCalendarEvent(pass: Pass, timeSpan: PassImpl.TimeSpan): CalendarEvent {
    if (timeSpan.from == null && timeSpan.to == null) {
        throw IllegalArgumentException("span must have either a to or a from")
    }

    val from = timeSpan.from ?: timeSpan.to!!.minusHours(DEFAULT_EVENT_LENGTH_IN_HOURS)
    val to = timeSpan.to ?: timeSpan.from!!.plusHours(DEFAULT_EVENT_LENGTH_IN_HOURS)
    return CalendarEvent(
        title = pass.description,
        beginTimeMillis = from.toEpochSecond() * 1000,
        endTimeMillis = to.toEpochSecond() * 1000,
        location = pass.locations.firstOrNull()?.name,
    )
}

@VisibleForTesting
fun createIntent(pass: Pass, timeSpan: PassImpl.TimeSpan) = createIntent(createCalendarEvent(pass, timeSpan))

fun createIntent(event: CalendarEvent) = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
    putCalendarEvent(event)
}

private fun Intent.putCalendarEvent(event: CalendarEvent) {
    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.beginTimeMillis)
    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endTimeMillis)
    putExtra("title", event.title)
    event.location?.let { putExtra("eventLocation", it) }
    event.description?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
}


