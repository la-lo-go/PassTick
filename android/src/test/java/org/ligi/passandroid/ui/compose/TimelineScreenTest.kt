package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.domain.timeline.EventTemporalState
import org.ligi.passandroid.domain.timeline.PassDeepLinkData
import org.ligi.passandroid.domain.timeline.PassEvent
import org.ligi.passandroid.domain.timeline.PassTimeline
import org.ligi.passandroid.domain.timeline.TimelineDay
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

class TimelineScreenTest {
    @Test
    fun `nearest event index follows reversed day and event order`() {
        val timeline = PassTimeline(
            zoneId = ZoneId.of("UTC"),
            days = listOf(
                TimelineDay(LocalDate.of(2026, 1, 1), listOf(event("first"), event("second"))),
                TimelineDay(LocalDate.of(2026, 1, 2), listOf(event("third"))),
            ),
            nearestEventId = "second",
        )

        assertThat(timelineItemIndex(timeline, timeline.nearestEventId)).isEqualTo(3)
    }

    private fun event(id: String) = PassEvent(
        id = id,
        pass = PassDeepLinkData(id),
        title = id,
        startsAt = Instant.EPOCH,
        endsAt = Instant.EPOCH,
        location = null,
        temporalState = EventTemporalState.PAST,
    )
}
