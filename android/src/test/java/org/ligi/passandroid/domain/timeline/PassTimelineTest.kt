package org.ligi.passandroid.domain.timeline

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassBarCodeFormat
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
                pass("past", "2026-08-29T21:59:59Z", "2026-08-29T22:00:00Z"),
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
    fun `event that ends today but starts yesterday is past`() {
        val timeline = buildPassTimeline(
            passes = listOf(pass("overnight", "2026-08-28T23:00:00Z", "2026-08-29T23:59:00Z")),
            now = Instant.parse("2026-08-29T12:00:00Z"),
            zoneId = ZoneId.of("UTC"),
        )

        assertThat(timeline.days.single().events.single().temporalState).isEqualTo(EventTemporalState.PAST)
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

    @Test
    fun `carries notification facts from the pass`() {
        val snapshot = pass("protected", "2026-08-30T11:00:00Z", "2026-08-30T12:00:00Z", "Station").copy(
            barcodeFormat = PassBarCodeFormat.QR_CODE,
            barcodeMessage = "ticket",
            isProtected = true,
            locations = listOf(PassLocationSnapshot("Station", 40.4, -3.7)),
        )

        val event = buildPassTimeline(
            listOf(snapshot),
            Instant.parse("2026-08-30T10:00:00Z"),
            ZoneId.of("UTC"),
        ).days.single().events.single()

        assertThat(event.hasBarcode).isTrue()
        assertThat(event.isProtected).isTrue()
        assertThat(event.latitude).isEqualTo(40.4)
        assertThat(event.longitude).isEqualTo(-3.7)
    }

    @Test
    fun `uses label and coordinates from the same location`() {
        val snapshot = pass("locations", "2026-08-30T11:00:00Z", "2026-08-30T12:00:00Z").copy(
            locations = listOf(
                PassLocationSnapshot(null, 1.0, 2.0),
                PassLocationSnapshot("Station", 40.4, -3.7),
            ),
        )

        val event = buildPassTimeline(
            listOf(snapshot),
            Instant.parse("2026-08-30T10:00:00Z"),
            ZoneId.of("UTC"),
        ).days.single().events.single()

        assertThat(event.location).isEqualTo("Station")
        assertThat(event.latitude).isEqualTo(40.4)
        assertThat(event.longitude).isEqualTo(-3.7)
    }

    @Test
    fun `normalizes an end-only event to the default duration`() {
        val end = ZonedDateTime.parse("2026-08-30T12:00:00Z")
        val snapshot = pass("end-only", "2026-08-30T10:00:00Z", "2026-08-30T12:00:00Z").copy(
            calendarTimeSpan = PassTimeSpanSnapshot(from = null, to = end),
        )

        val normalized = snapshot.normalizedTimeSpan()

        assertThat(normalized?.startsAt).isEqualTo(end.minusHours(2).toInstant())
        assertThat(normalized?.endsAt).isEqualTo(end.toInstant())
    }

    @Test
    fun `clamps an inverted event to its start`() {
        val snapshot = pass("inverted", "2026-08-30T12:00:00Z", "2026-08-30T10:00:00Z")

        val normalized = snapshot.normalizedTimeSpan()

        assertThat(normalized?.endsAt).isEqualTo(normalized?.startsAt)
    }

    @Test
    fun `carries the validity end as the event expiry`() {
        val snapshot = pass("expiring", "2026-08-30T11:00:00Z", "2026-08-30T12:00:00Z").copy(
            expiresAt = ZonedDateTime.parse("2026-08-30T18:00:00Z"),
        )

        val event = buildPassTimeline(
            listOf(snapshot),
            Instant.parse("2026-08-30T10:00:00Z"),
            ZoneId.of("UTC"),
        ).days.single().events.single()

        assertThat(event.expiresAt).isEqualTo(Instant.parse("2026-08-30T18:00:00Z"))
    }

    @Test
    fun `reports expiry only after the validity end`() {
        val event = buildPassTimeline(
            listOf(
                pass("expiring", "2026-08-30T11:00:00Z", "2026-08-30T12:00:00Z").copy(
                    expiresAt = ZonedDateTime.parse("2026-08-30T18:00:00Z"),
                ),
            ),
            Instant.parse("2026-08-30T10:00:00Z"),
            ZoneId.of("UTC"),
        ).days.single().events.single()

        assertThat(event.isExpired(Instant.parse("2026-08-30T17:59:59Z"))).isFalse()
        assertThat(event.isExpired(Instant.parse("2026-08-30T18:00:00Z"))).isFalse()
        assertThat(event.isExpired(Instant.parse("2026-08-30T18:00:01Z"))).isTrue()
    }

    @Test
    fun `an event without a validity end never expires`() {
        val event = buildPassTimeline(
            listOf(pass("open", "2026-08-30T11:00:00Z", "2026-08-30T12:00:00Z")),
            Instant.parse("2026-08-30T10:00:00Z"),
            ZoneId.of("UTC"),
        ).days.single().events.single()

        assertThat(event.expiresAt).isNull()
        assertThat(event.isExpired(Instant.parse("2030-01-01T00:00:00Z"))).isFalse()
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
