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

fun buildPassReminders(
    timeline: PassTimeline,
    now: Instant,
    leadMinutes: Int,
): List<PassReminder> {
    val nowMillis = now.toEpochMilli()
    val leadMillis = leadMinutes.coerceIn(0, 10_080) * 60_000L
    return timeline.days.asSequence()
        .flatMap { it.events.asSequence() }
        .filter { it.endsAt.toEpochMilli() > nowMillis }
        .map { event ->
            val triggerAtMillis = event.startsAt.toEpochMilli() - leadMillis
            PassReminder(
                id = event.id,
                passId = event.pass.passId,
                title = event.title,
                eventAtMillis = event.startsAt.toEpochMilli(),
                triggerAtMillis = triggerAtMillis,
            )
        }
        .filter { it.triggerAtMillis >= nowMillis }
        .distinctBy(PassReminder::id)
        .toList()
}
