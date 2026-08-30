package org.ligi.passandroid.widget

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PassWidgetSnapshotTest {
    @Test
    fun `selects active passes using the device day`() {
        val snapshot = PassWidgetSnapshot(
            passes = listOf(
                WidgetPass(
                    id = "today",
                    title = "Evening train",
                    startsAtEpochMillis = Instant.parse("2026-08-30T21:30:00Z").toEpochMilli(),
                    endsAtEpochMillis = Instant.parse("2026-08-30T22:30:00Z").toEpochMilli(),
                    location = "Madrid",
                    supportingText = null,
                ),
            ),
        )

        val selection = snapshot.select(
            now = Instant.parse("2026-08-30T20:00:00Z"),
            zoneId = ZoneId.of("Europe/Madrid"),
        )

        assertThat(selection.today.map(WidgetPass::id)).containsExactly("today")
        assertThat(selection.currentOrNext?.id).isEqualTo("today")
        assertThat(selection.active.map(WidgetPass::id)).containsExactly("today")
    }

    @Test
    fun `refreshes immediately after the next pass boundary`() {
        val boundary = Instant.parse("2026-08-30T21:30:00Z")
        val snapshot = PassWidgetSnapshot(
            passes = listOf(
                WidgetPass(
                    id = "today",
                    title = "Evening train",
                    startsAtEpochMillis = boundary.toEpochMilli(),
                    endsAtEpochMillis = Instant.parse("2026-08-30T22:30:00Z").toEpochMilli(),
                    location = null,
                    supportingText = null,
                ),
            ),
        )

        assertThat(
            snapshot.nextRefreshAt(
                now = Instant.parse("2026-08-30T20:00:00Z"),
                zoneId = ZoneId.of("Europe/Madrid"),
            ),
        ).isEqualTo(boundary.plusMillis(1))
    }

    @Test
    fun `refreshes at local midnight when no earlier boundary exists`() {
        val snapshot = PassWidgetSnapshot()

        assertThat(
            snapshot.nextRefreshAt(
                now = Instant.parse("2026-08-30T20:00:00Z"),
                zoneId = ZoneId.of("Europe/Madrid"),
            ),
        ).isEqualTo(Instant.parse("2026-08-30T22:00:00Z"))
    }
}
