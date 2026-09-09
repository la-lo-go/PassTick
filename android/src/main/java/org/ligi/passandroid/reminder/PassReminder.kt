package org.ligi.passandroid.reminder

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
    return timeline.days.asSequence()
        .flatMap { it.events.asSequence() }
        .filter { it.endsAt.toEpochMilli() > nowMillis }
        .flatMap { event ->
            val eventLeadMinutes = when (val override = overrides[event.pass.passId]) {
                PassReminderOverride.Disabled -> emptySet()
                is PassReminderOverride.LeadTime -> setOf(override.minutes)
                PassReminderOverride.ExactAtEvent -> setOf(0)
                null -> leadMinutes
            }
            val safeLeadMinutes = eventLeadMinutes.map { it.coerceIn(0, 10_080) }.distinct()
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
        .filter { it.triggerAtMillis >= nowMillis || it.ownsLifecycle }
        .distinctBy(PassReminder::id)
        .toList()
}
