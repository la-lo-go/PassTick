package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.ui.state.PassDraft
import org.ligi.passandroid.ui.state.PassLocationDraft

class PassDraftValidationTest {
    @Test
    fun `accepts a text address without coordinates`() {
        val draft = validDraft().copy(locations = listOf(PassLocationDraft("Atocha station, Madrid", "", "")))

        assertThat(validatePassDraft(draft)).isNull()
    }

    @Test
    fun `rejects a partial coordinate pair`() {
        val draft = validDraft().copy(locations = listOf(PassLocationDraft("Station", "40.4", "")))

        assertThat(validatePassDraft(draft)).isEqualTo("Enter both coordinates or clear both.")
    }

    @Test
    fun `rejects an end date before its start`() {
        val draft = validDraft().copy(
            calendarStart = "2026-09-02T12:00:00+02:00[Europe/Madrid]",
            calendarEnd = "2026-09-02T10:00:00+02:00[Europe/Madrid]",
        )

        assertThat(validatePassDraft(draft)).isEqualTo("The end date must be after the start date.")
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
