package org.ligi.passandroid.reminder

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.domain.timeline.EventTemporalState
import org.ligi.passandroid.domain.timeline.PassDeepLinkData
import org.ligi.passandroid.domain.timeline.PassEvent
import org.ligi.passandroid.domain.timeline.PassTimeline
import org.ligi.passandroid.domain.timeline.TimelineDay
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset

class PassReminderTest {
    @Test
    fun `uses lead time and skips an elapsed trigger`() {
        val now = Instant.parse("2026-08-30T10:00:00Z")
        val timeline = PassTimeline(
            zoneId = ZoneOffset.UTC,
            days = listOf(
                TimelineDay(
                    LocalDate.of(2026, 8, 30),
                    listOf(event("later", "2026-08-30T12:00:00Z"), event("soon", "2026-08-30T10:30:00Z")),
                ),
            ),
            nearestEventId = "pass:soon:event",
        )

        val reminders = buildPassReminders(timeline, now, leadMinutes = 60).associateBy(PassReminder::passId)

        assertThat(reminders.getValue("later").triggerAtMillis).isEqualTo(Instant.parse("2026-08-30T11:00:00Z").toEpochMilli())
        assertThat(reminders).doesNotContainKey("soon")
    }
}

private fun event(passId: String, startsAt: String): PassEvent {
    val start = Instant.parse(startsAt)
    return PassEvent(
        id = "pass:$passId:event",
        pass = PassDeepLinkData(passId),
        title = "Pass $passId",
        startsAt = start,
        endsAt = start.plusSeconds(3600),
        location = null,
        temporalState = EventTemporalState.TODAY,
    )
}
