package org.ligi.passandroid.domain.timeline

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.PassLocationSnapshot
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.repository.PassTimeSpanSnapshot
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class PassTimelineTest {
    @Test
    fun `classifies exact local-day boundaries`() {
        val zone = ZoneId.of("Europe/Madrid")
        val timeline = buildPassTimeline(
            passes = listOf(
                pass("past", "2026-08-29T22:00:00Z", "2026-08-29T22:00:00Z"),
                pass("today", "2026-08-29T22:00:01Z", "2026-08-30T01:00:00Z"),
                pass("upcoming", "2026-08-30T22:00:00Z", "2026-08-30T23:00:00Z"),
            ),
            now = Instant.parse("2026-08-30T10:00:00Z"),
            zoneId = zone,
        )

        assertThat(timeline.days.flatMap(TimelineDay::events).associate { it.pass.passId to it.temporalState })
            .containsExactlyEntriesOf(
                linkedMapOf(
                    "past" to EventTemporalState.PAST,
                    "today" to EventTemporalState.TODAY,
                    "upcoming" to EventTemporalState.UPCOMING,
                ),
            )
    }

    @Test
    fun `groups the same instant by the selected display zone`() {
        val pass = pass("flight", "2026-08-30T00:30:00Z", "2026-08-30T02:30:00Z")

        val madrid = buildPassTimeline(listOf(pass), Instant.parse("2026-08-29T12:00:00Z"), ZoneId.of("Europe/Madrid"))
        val losAngeles = buildPassTimeline(listOf(pass), Instant.parse("2026-08-29T12:00:00Z"), ZoneId.of("America/Los_Angeles"))

        assertThat(madrid.days.single().date).isEqualTo(LocalDate.of(2026, 8, 30))
        assertThat(losAngeles.days.single().date).isEqualTo(LocalDate.of(2026, 8, 29))
    }

    @Test
    fun `uses DST day boundaries instead of a fixed day duration`() {
        val zone = ZoneId.of("Europe/Madrid")
        val timeline = buildPassTimeline(
            passes = listOf(
                pass("late", "2026-03-29T21:59:59Z", "2026-03-29T22:30:00Z"),
                pass("tomorrow", "2026-03-29T22:00:00Z", "2026-03-29T23:00:00Z"),
            ),
            now = Instant.parse("2026-03-29T10:00:00Z"),
            zoneId = zone,
        )

        val states = timeline.days.flatMap(TimelineDay::events).associate { it.pass.passId to it.temporalState }
        assertThat(states["late"]).isEqualTo(EventTemporalState.TODAY)
        assertThat(states["tomorrow"]).isEqualTo(EventTemporalState.UPCOMING)
    }

    @Test
    fun `focuses the next unfinished event and keeps stable pass link data`() {
        val timeline = buildPassTimeline(
            passes = listOf(
                pass("finished", "2026-08-30T08:00:00Z", "2026-08-30T09:00:00Z"),
                pass("next", "2026-08-30T11:00:00Z", "2026-08-30T12:00:00Z", "Platform 4"),
            ),
            now = Instant.parse("2026-08-30T10:00:00Z"),
            zoneId = ZoneId.of("UTC"),
        )

        val next = timeline.days.flatMap(TimelineDay::events).single { it.pass.passId == "next" }
        assertThat(timeline.nearestEventId).isEqualTo("pass:next:event")
        assertThat(next.pass).isEqualTo(PassDeepLinkData("next"))
        assertThat(next.location).isEqualTo("Platform 4")
    }

    private fun pass(id: String, start: String, end: String, location: String? = null) = PassSnapshot(
        id = id,
        description = "Event $id",
        creator = null,
        type = PassType.EVENT,
        accentColor = 0,
        barcodeFormat = null,
        barcodeMessage = null,
        barcodeAlternativeText = null,
        fields = emptyList(),
        locations = location?.let { listOf(PassLocationSnapshot(it, 0.0, 0.0)) }.orEmpty(),
        calendarTimeSpan = PassTimeSpanSnapshot(
            from = ZonedDateTime.parse(start),
            to = ZonedDateTime.parse(end),
        ),
    )
}
