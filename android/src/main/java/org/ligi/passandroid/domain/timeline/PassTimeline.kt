package org.ligi.passandroid.domain.timeline

import org.ligi.passandroid.repository.PassSnapshot
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

private val DefaultEventDuration: Duration = Duration.ofHours(2)

enum class EventTemporalState {
    TODAY,
    UPCOMING,
    PAST,
}

data class PassDeepLinkData(val passId: String) {
    init {
        require(passId.isNotBlank()) { "Pass id must not be blank" }
    }
}

data class PassEvent(
    val id: String,
    val pass: PassDeepLinkData,
    val title: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val location: String?,
    val temporalState: EventTemporalState,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hasBarcode: Boolean = false,
    val isProtected: Boolean = false,
)

data class NormalizedPassTimeSpan(val startsAt: Instant, val endsAt: Instant)

fun PassSnapshot.normalizedTimeSpan(): NormalizedPassTimeSpan? {
    val span = calendarTimeSpan ?: return null
    val start = span.from ?: span.to?.minus(DefaultEventDuration) ?: return null
    val rawEnd = span.to ?: start.plus(DefaultEventDuration)
    val end = if (rawEnd.isBefore(start)) start else rawEnd
    return NormalizedPassTimeSpan(start.toInstant(), end.toInstant())
}

data class TimelineDay(
    val date: LocalDate,
    val events: List<PassEvent>,
)

data class PassTimeline(
    val zoneId: ZoneId,
    val days: List<TimelineDay>,
    val nearestEventId: String?,
) {
    companion object {
        fun empty(zoneId: ZoneId = ZoneId.systemDefault()) = PassTimeline(zoneId, emptyList(), null)
    }
}

fun buildPassTimeline(
    passes: List<PassSnapshot>,
    now: Instant,
    zoneId: ZoneId,
): PassTimeline {
    val today = now.atZone(zoneId).toLocalDate()
    val todayStart = today.atStartOfDay(zoneId).toInstant()
    val tomorrowStart = today.plusDays(1).atStartOfDay(zoneId).toInstant()
    val events = passes.mapNotNull { pass ->
        pass.toEvent(todayStart, tomorrowStart)
    }.sortedWith(compareBy(PassEvent::startsAt, PassEvent::endsAt, PassEvent::id))

    val days = events
        .groupBy { it.startsAt.atZone(zoneId).toLocalDate() }
        .map { (date, dayEvents) -> TimelineDay(date, dayEvents.toList()) }

    return PassTimeline(
        zoneId = zoneId,
        days = days,
        nearestEventId = events.firstOrNull { event ->
            event.endsAt.isAfter(now) || !event.startsAt.isBefore(now)
        }?.id,
    )
}

private fun PassSnapshot.toEvent(todayStart: Instant, tomorrowStart: Instant): PassEvent? {
    val span = normalizedTimeSpan() ?: return null
    val startInstant = span.startsAt
    val endInstant = span.endsAt
    val eventLocation = locations.firstOrNull { !it.name.isNullOrBlank() } ?: locations.firstOrNull()
    // The start date defines the day of an event. An event that ends today is still a today event.
    val temporalState = when {
        startInstant.isBefore(todayStart) -> EventTemporalState.PAST
        !startInstant.isBefore(tomorrowStart) -> EventTemporalState.UPCOMING
        else -> EventTemporalState.TODAY
    }

    return PassEvent(
        id = "pass:$id:event",
        pass = PassDeepLinkData(id),
        title = description,
        startsAt = startInstant,
        endsAt = endInstant,
        location = eventLocation?.name?.takeIf(String::isNotBlank),
        temporalState = temporalState,
        latitude = eventLocation?.latitude,
        longitude = eventLocation?.longitude,
        hasBarcode = barcodeFormat != null && !barcodeMessage.isNullOrBlank(),
        isProtected = isProtected,
    )
}

private fun Instant.atZone(zoneId: ZoneId): ZonedDateTime = ZonedDateTime.ofInstant(this, zoneId)
