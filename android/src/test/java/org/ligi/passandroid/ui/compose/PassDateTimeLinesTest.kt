package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.ui.state.PassTimeSpanUiModel
import org.ligi.passandroid.ui.state.PassUiModel
import org.threeten.bp.ZonedDateTime

class PassDateTimeLinesTest {
    @Test
    fun `does not show a synthetic end when the pass only has a start`() {
        val start = ZonedDateTime.parse("2026-09-01T10:15:00+02:00[Europe/Madrid]")
        val pass = PassUiModel(
            id = "renfe-start-only",
            description = "Train ticket",
            creator = "Renfe",
            type = PassType.BOARDING,
            accentColor = 0,
            barcodeFormat = null,
            barcodeMessage = null,
            barcodeAlternativeText = null,
            fields = emptyList(),
            locations = emptyList(),
            calendarEvent = org.ligi.passandroid.functions.CalendarEvent(
                title = "Train ticket",
                beginTimeMillis = start.toInstant().toEpochMilli(),
                endTimeMillis = start.plusHours(2).toInstant().toEpochMilli(),
                location = null,
            ),
            calendarTimeSpan = PassTimeSpanUiModel(from = start, to = null),
        )

        val lines = pass.calendarDateTimeLines()

        assertThat(lines).hasSize(1)
        assertThat(lines.single()).startsWith("Starts:").contains("10:15")
        assertThat(lines).noneMatch { it.startsWith("Ends:") }
    }
}
