package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.domain.timeline.EventTemporalState
import org.ligi.passandroid.domain.timeline.PassDeepLinkData
import org.ligi.passandroid.domain.timeline.PassEvent
import org.ligi.passandroid.domain.timeline.PassTimeline
import org.ligi.passandroid.domain.timeline.TimelineDay
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.ZoneId

class AgendaCalendarTest {
    @Test
    fun `month starting sunday pads nothing when week starts sunday`() {
        val cells = monthGridCells(YearMonth.of(2026, 2), DayOfWeek.SUNDAY)

        assertThat(cells).hasSize(42)
        assertThat(cells[0]).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(cells[27]).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(cells.drop(28)).containsOnlyNulls()
    }

    @Test
    fun `month starting sunday pads six days when week starts monday`() {
        val cells = monthGridCells(YearMonth.of(2026, 2), DayOfWeek.MONDAY)

        assertThat(cells).hasSize(42)
        repeat(6) { assertThat(cells[it]).isNull() }
        assertThat(cells[6]).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(cells[33]).isEqualTo(LocalDate.of(2026, 2, 28))
    }

    @Test
    fun `month starting saturday pads five days when week starts sunday`() {
        val cells = monthGridCells(YearMonth.of(2026, 8), DayOfWeek.SUNDAY)

        assertThat(cells).hasSize(42)
        repeat(6) { assertThat(cells[it]).isNull() }
        assertThat(cells[6]).isEqualTo(LocalDate.of(2026, 8, 1))
        assertThat(cells[36]).isEqualTo(LocalDate.of(2026, 8, 31))
    }

    @Test
    fun `month starting monday pads nothing when week starts monday`() {
        val cells = monthGridCells(YearMonth.of(2026, 6), DayOfWeek.MONDAY)

        assertThat(cells).hasSize(42)
        assertThat(cells[0]).isEqualTo(LocalDate.of(2026, 6, 1))
        assertThat(cells[29]).isEqualTo(LocalDate.of(2026, 6, 30))
        assertThat(cells.drop(30)).containsOnlyNulls()
    }

    @Test
    fun `grid cells always fill forty two entries`() {
        for (month in 1..12) {
            val cells = monthGridCells(YearMonth.of(2026, month), DayOfWeek.SUNDAY)
            assertThat(cells).hasSize(42)
            assertThat(cells.filterNotNull().map(LocalDate::lengthOfMonth))
            assertThat(cells.filterNotNull().first().dayOfMonth).isEqualTo(1)
            assertThat(cells.filterNotNull().last().dayOfMonth)
                .isEqualTo(YearMonth.of(2026, month).lengthOfMonth())
        }
    }

    @Test
    fun `timeline day index resolves header in reversed order`() {
        val timeline = timeline()

        assertThat(timelineDayIndex(timeline, LocalDate.of(2026, 1, 2))).isEqualTo(0)
        assertThat(timelineDayIndex(timeline, LocalDate.of(2026, 1, 1))).isEqualTo(2)
    }

    @Test
    fun `timeline day index is null for unknown date`() {
        assertThat(timelineDayIndex(timeline(), LocalDate.of(2026, 3, 1))).isNull()
    }

    private fun timeline() = PassTimeline(
        zoneId = ZoneId.of("UTC"),
        days = listOf(
            TimelineDay(LocalDate.of(2026, 1, 1), listOf(event("first"), event("second"))),
            TimelineDay(LocalDate.of(2026, 1, 2), listOf(event("third"))),
        ),
        nearestEventId = null,
    )

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
