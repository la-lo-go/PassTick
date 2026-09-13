package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.ui.state.PassDraft
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationDraft

class PassDraftValidationTest {
    @Test
    fun `accepts a text address without coordinates`() {
        val draft = validDraft().copy(locations = listOf(PassLocationDraft("Atocha station, Madrid", "", "")))

        assertThat(validatePassDraft(draft)).isNull()
    }

    @Test
    fun `rejects a blank description`() {
        assertThat(validatePassDraft(validDraft().copy(description = " ")))
            .isEqualTo(PassDraftIssue.DESCRIPTION_MISSING)
    }

    @Test
    fun `rejects a barcode format without data`() {
        val draft = validDraft().copy(barcodeFormat = PassBarCodeFormat.QR_CODE, barcodeMessage = "")

        assertThat(validatePassDraft(draft)).isEqualTo(PassDraftIssue.BARCODE_MISSING)
    }

    @Test
    fun `rejects an unparsable start date`() {
        assertThat(validatePassDraft(validDraft().copy(calendarStart = "not-a-date")))
            .isEqualTo(PassDraftIssue.CALENDAR_START_INVALID)
    }

    @Test
    fun `rejects an unparsable end date`() {
        assertThat(validatePassDraft(validDraft().copy(calendarEnd = "not-a-date")))
            .isEqualTo(PassDraftIssue.CALENDAR_END_INVALID)
    }

    @Test
    fun `rejects a partial coordinate pair`() {
        val draft = validDraft().copy(locations = listOf(PassLocationDraft("Station", "40.4", "")))

        assertThat(validatePassDraft(draft)).isEqualTo(PassDraftIssue.LOCATION_COORDINATES_PARTIAL)
    }

    @Test
    fun `rejects non numeric coordinates`() {
        val draft = validDraft().copy(locations = listOf(PassLocationDraft("Station", "north", "1.0")))

        assertThat(validatePassDraft(draft)).isEqualTo(PassDraftIssue.LOCATION_COORDINATES_INVALID)
    }

    @Test
    fun `rejects a latitude outside the valid range`() {
        val draft = validDraft().copy(locations = listOf(PassLocationDraft("Station", "91.0", "1.0")))

        assertThat(validatePassDraft(draft)).isEqualTo(PassDraftIssue.LATITUDE_RANGE)
    }

    @Test
    fun `rejects a longitude outside the valid range`() {
        val draft = validDraft().copy(locations = listOf(PassLocationDraft("Station", "1.0", "181.0")))

        assertThat(validatePassDraft(draft)).isEqualTo(PassDraftIssue.LONGITUDE_RANGE)
    }

    @Test
    fun `rejects an unnamed location without coordinates`() {
        val draft = validDraft().copy(locations = listOf(PassLocationDraft("", "", "")))

        assertThat(validatePassDraft(draft)).isEqualTo(PassDraftIssue.LOCATION_EMPTY)
    }

    @Test
    fun `rejects an incomplete field`() {
        val draft = validDraft().copy(fields = listOf(PassFieldUiModel("key", "", "", false, null)))

        assertThat(validatePassDraft(draft)).isEqualTo(PassDraftIssue.FIELD_EMPTY)
    }

    @Test
    fun `rejects an end date before its start`() {
        val draft = validDraft().copy(
            calendarStart = "2026-09-02T12:00:00+02:00[Europe/Madrid]",
            calendarEnd = "2026-09-02T10:00:00+02:00[Europe/Madrid]",
        )

        assertThat(validatePassDraft(draft)).isEqualTo(PassDraftIssue.CALENDAR_END_BEFORE_START)
    }

    private fun validDraft() = PassDraft(
        description = "Train ticket",
        creator = "Rail operator",
        type = PassType.EVENT,
        accentColor = 0xFF2859C5.toInt(),
        barcodeFormat = null,
        barcodeMessage = "",
        barcodeAlternativeText = "",
        fields = emptyList(),
    )
}
