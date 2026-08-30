package org.ligi.passandroid.reminder

import org.ligi.passandroid.domain.timeline.PassTimeline
import org.threeten.bp.Instant

data class PassReminder(
    val id: String,
    val passId: String,
    val title: String,
    val eventAtMillis: Long,
    val triggerAtMillis: Long,
)

sealed interface PassReminderOverride {
    data object Disabled : PassReminderOverride
    data class LeadTime(val minutes: Int) : PassReminderOverride
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
): List<PassReminder> {
    val nowMillis = now.toEpochMilli()
    return timeline.days.asSequence()
        .flatMap { it.events.asSequence() }
        .filter { it.endsAt.toEpochMilli() > nowMillis }
        .flatMap { event ->
            val eventLeadMinutes = when (val override = overrides[event.pass.passId]) {
                PassReminderOverride.Disabled -> emptySet()
                is PassReminderOverride.LeadTime -> setOf(override.minutes)
                null -> leadMinutes
            }
            eventLeadMinutes.asSequence().map { minutes ->
                val safeMinutes = minutes.coerceIn(0, 10_080)
                PassReminder(
                    id = "${event.id}:$safeMinutes",
                    passId = event.pass.passId,
                    title = event.title,
                    eventAtMillis = event.startsAt.toEpochMilli(),
                    triggerAtMillis = event.startsAt.toEpochMilli() - safeMinutes * 60_000L,
                )
            }
        }
        .filter { it.triggerAtMillis >= nowMillis }
        .distinctBy(PassReminder::id)
        .toList()
}
