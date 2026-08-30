package org.ligi.passandroid.widget

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PassWidgetSnapshotTest {
    @Test
    fun `selects passes using the device day and ignores a stale quick code id`() {
        val snapshot = PassWidgetSnapshot(
            passes = listOf(
                WidgetPass(
                    id = "today",
                    title = "Evening train",
                    startsAtEpochMillis = Instant.parse("2026-08-30T21:30:00Z").toEpochMilli(),
                    endsAtEpochMillis = Instant.parse("2026-08-30T22:30:00Z").toEpochMilli(),
                    location = "Madrid",
                    supportingText = null,
                    hasCode = true,
                ),
            ),
            quickCodePassId = "deleted",
        )

        val selection = snapshot.select(
            now = Instant.parse("2026-08-30T20:00:00Z"),
            zoneId = ZoneId.of("Europe/Madrid"),
        )

        assertThat(selection.today.map(WidgetPass::id)).containsExactly("today")
        assertThat(selection.currentOrNext?.id).isEqualTo("today")
        assertThat(selection.quickCode).isNull()
    }
}
