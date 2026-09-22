package org.ligi.passandroid.reminder

import org.ligi.passandroid.domain.timeline.PassEvent
import org.ligi.passandroid.domain.timeline.PassTimeline
import org.threeten.bp.Instant

data class PassReminder(
    val id: String,
    val passId: String,
    val title: String,
    val eventAtMillis: Long,
    val triggerAtMillis: Long,
    val endAtMillis: Long = eventAtMillis,
    val locationLabel: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hasBarcode: Boolean = false,
    val isProtected: Boolean = false,
    val exactTiming: Boolean? = null,
    val enabledActions: Set<NotificationAction>? = null,
    val ownsLifecycle: Boolean = true,
    /** True for the validity-end reminder, which is not the event itself. */
    val isExpiration: Boolean = false,
)

sealed interface PassReminderOverride {
    data object Disabled : PassReminderOverride
    data class LeadTime(val minutes: Int) : PassReminderOverride
    data object ExactAtEvent : PassReminderOverride
}

fun buildPassReminders(
    timeline: PassTimeline,
    now: Instant,
    leadMinutes: Int,
    overrides: Map<String, PassReminderOverride> = emptyMap(),
): List<PassReminder> = buildPassReminders(timeline, now, setOf(leadMinutes), overrides)

fun buildPassReminders(
    timeline: PassTimeline,
    now: Instant,
    leadMinutes: Set<Int>,
    overrides: Map<String, PassReminderOverride> = emptyMap(),
    actionOverrides: Map<String, Set<NotificationAction>> = emptyMap(),
): List<PassReminder> {
    val nowMillis = now.toEpochMilli()
    val events = timeline.days.asSequence().flatMap { it.events.asSequence() }
    val eventReminders = events
        .filter { it.endsAt.toEpochMilli() > nowMillis }
        .flatMap { event ->
            val safeLeadMinutes = event.reminderLeadMinutes(leadMinutes, overrides)
            val lifecycleLeadMinutes = safeLeadMinutes.maxOrNull()
            safeLeadMinutes.asSequence().map { safeMinutes ->
                PassReminder(
                    id = "${event.id}:$safeMinutes",
                    passId = event.pass.passId,
                    title = event.title,
                    eventAtMillis = event.startsAt.toEpochMilli(),
                    triggerAtMillis = event.startsAt.toEpochMilli() - safeMinutes * 60_000L,
                    endAtMillis = event.endsAt.toEpochMilli(),
                    locationLabel = event.location,
                    exactTiming = true.takeIf { overrides[event.pass.passId] == PassReminderOverride.ExactAtEvent },
                    enabledActions = actionOverrides[event.pass.passId],
                    latitude = event.latitude,
                    longitude = event.longitude,
                    hasBarcode = event.hasBarcode,
                    isProtected = event.isProtected,
                    ownsLifecycle = safeMinutes == lifecycleLeadMinutes,
                )
            }
        }
    val expirationReminders = events.mapNotNull { event ->
        event.expirationReminder(nowMillis, leadMinutes, overrides, actionOverrides)
    }
    return (eventReminders + expirationReminders)
        .filter { it.triggerAtMillis >= nowMillis || it.ownsLifecycle }
        .distinctBy(PassReminder::id)
        .toList()
}

private fun PassEvent.reminderLeadMinutes(
    globalLeadMinutes: Set<Int>,
    overrides: Map<String, PassReminderOverride>,
): Set<Int> = when (val override = overrides[pass.passId]) {
    PassReminderOverride.Disabled -> emptySet()
    is PassReminderOverride.LeadTime -> setOf(override.minutes)
    PassReminderOverride.ExactAtEvent -> setOf(0)
    null -> globalLeadMinutes
}.mapTo(linkedSetOf()) { it.coerceIn(0, 10_080) }

/**
 * The expiration reminder uses the earliest event lead and never owns the lifecycle,
 * so one event reminder stays in charge of the notification lifecycle.
 */
private fun PassEvent.expirationReminder(
    nowMillis: Long,
    globalLeadMinutes: Set<Int>,
    overrides: Map<String, PassReminderOverride>,
    actionOverrides: Map<String, Set<NotificationAction>>,
): PassReminder? {
    val expiresAtMillis = expiresAt?.toEpochMilli()?.takeIf { it > nowMillis } ?: return null
    val minutes = reminderLeadMinutes(globalLeadMinutes, overrides).maxOrNull() ?: return null
    return PassReminder(
        id = "$id:expiration",
        passId = pass.passId,
        title = title,
        eventAtMillis = expiresAtMillis,
        triggerAtMillis = expiresAtMillis - minutes * 60_000L,
        endAtMillis = expiresAtMillis,
        locationLabel = location,
        exactTiming = true.takeIf { overrides[pass.passId] == PassReminderOverride.ExactAtEvent },
        enabledActions = actionOverrides[pass.passId],
        latitude = latitude,
        longitude = longitude,
        hasBarcode = hasBarcode,
        isProtected = isProtected,
        ownsLifecycle = false,
        isExpiration = true,
    )
}
