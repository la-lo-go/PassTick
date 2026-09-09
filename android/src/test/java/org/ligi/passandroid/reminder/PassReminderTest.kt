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
    fun `uses lead time and retains an elapsed lifecycle reminder`() {
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
        assertThat(reminders.getValue("later").endAtMillis).isEqualTo(Instant.parse("2026-08-30T13:00:00Z").toEpochMilli())
        assertThat(reminders.getValue("soon").ownsLifecycle).isTrue()
    }

    @Test
    fun `uses global custom and disabled pass preferences`() {
        val now = Instant.parse("2026-08-30T08:00:00Z")
        val timeline = PassTimeline(
            zoneId = ZoneOffset.UTC,
            days = listOf(
                TimelineDay(
                    LocalDate.of(2026, 8, 30),
                    listOf(
                        event("global", "2026-08-30T12:00:00Z"),
                        event("custom", "2026-08-30T12:00:00Z"),
                        event("disabled", "2026-08-30T12:00:00Z"),
                    ),
                ),
            ),
            nearestEventId = null,
        )

        val reminders = buildPassReminders(
            timeline = timeline,
            now = now,
            leadMinutes = 60,
            overrides = mapOf(
                "custom" to PassReminderOverride.LeadTime(30),
                "disabled" to PassReminderOverride.Disabled,
            ),
        ).associateBy(PassReminder::passId)

        assertThat(reminders.keys).containsExactlyInAnyOrder("global", "custom")
        assertThat(reminders.getValue("global").triggerAtMillis)
            .isEqualTo(Instant.parse("2026-08-30T11:00:00Z").toEpochMilli())
        assertThat(reminders.getValue("custom").triggerAtMillis)
            .isEqualTo(Instant.parse("2026-08-30T11:30:00Z").toEpochMilli())
    }

    @Test
    fun `copies notification facts into the scheduled payload`() {
        val base = event("context", "2026-08-30T12:00:00Z")
        val event = base.copy(
            location = "Central station",
            latitude = 40.4,
            longitude = -3.7,
            hasBarcode = true,
            isProtected = true,
        )
        val timeline = PassTimeline(
            zoneId = ZoneOffset.UTC,
            days = listOf(TimelineDay(LocalDate.of(2026, 8, 30), listOf(event))),
            nearestEventId = event.id,
        )

        val reminder = buildPassReminders(
            timeline,
            Instant.parse("2026-08-30T10:00:00Z"),
            leadMinutes = 60,
        ).single()

        assertThat(reminder.locationLabel).isEqualTo("Central station")
        assertThat(reminder.latitude).isEqualTo(40.4)
        assertThat(reminder.longitude).isEqualTo(-3.7)
        assertThat(reminder.hasBarcode).isTrue()
        assertThat(reminder.isProtected).isTrue()
    }

    @Test
    fun `exact at event override schedules only at the event time`() {
        val event = event("exact", "2026-08-30T12:00:00Z")
        val timeline = PassTimeline(
            zoneId = ZoneOffset.UTC,
            days = listOf(TimelineDay(LocalDate.of(2026, 8, 30), listOf(event))),
            nearestEventId = event.id,
        )

        val reminder = buildPassReminders(
            timeline,
            Instant.parse("2026-08-30T10:00:00Z"),
            leadMinutes = setOf(30, 60),
            overrides = mapOf("exact" to PassReminderOverride.ExactAtEvent),
        ).single()

        assertThat(reminder.triggerAtMillis).isEqualTo(event.startsAt.toEpochMilli())
        assertThat(reminder.exactTiming).isTrue()
    }

    @Test
    fun `only earliest reminder owns lifecycle and survives resync`() {
        val event = event("multi", "2026-08-30T12:00:00Z")
        val timeline = PassTimeline(
            zoneId = ZoneOffset.UTC,
            days = listOf(TimelineDay(LocalDate.of(2026, 8, 30), listOf(event))),
            nearestEventId = event.id,
        )

        val reminders = buildPassReminders(
            timeline,
            Instant.parse("2026-08-30T11:40:00Z"),
            leadMinutes = setOf(15, 30, 60),
        )

        assertThat(reminders.map { it.id.substringAfterLast(':') }).containsExactlyInAnyOrder("15", "60")
        assertThat(reminders.single { it.ownsLifecycle }.id).endsWith(":60")
    }

    @Test
    fun `active event retains one lifecycle reminder`() {
        val event = event("active", "2026-08-30T12:00:00Z")
        val timeline = PassTimeline(
            zoneId = ZoneOffset.UTC,
            days = listOf(TimelineDay(LocalDate.of(2026, 8, 30), listOf(event))),
            nearestEventId = event.id,
        )

        val reminders = buildPassReminders(
            timeline,
            Instant.parse("2026-08-30T12:30:00Z"),
            leadMinutes = setOf(15, 60),
        )

        assertThat(reminders).singleElement().extracting(PassReminder::ownsLifecycle).isEqualTo(true)
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
