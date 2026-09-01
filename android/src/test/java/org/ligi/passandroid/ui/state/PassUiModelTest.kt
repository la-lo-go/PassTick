package org.ligi.passandroid.ui.state

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType

class PassUiModelTest {
    @Test
    fun `uses the primary field to distinguish a generic pass on the home card`() {
        val pass = pass(
            description = "ReservaEntradas",
            fields = listOf(
                PassFieldUiModel("location-event", "CINE EMBAJADORES", "Film title", false, "primaryFields"),
            ),
        )

        assertThat(pass.homeCardDetail()).isEqualTo("Film title")
    }

    @Test
    fun `does not repeat the home card description as its detail`() {
        val pass = pass(
            description = "Concert",
            fields = listOf(PassFieldUiModel("event", "Event", "Concert", false, "primaryFields")),
        )

        assertThat(pass.homeCardDetail()).isNull()
    }

    @Test
    fun `uses the primary field initial for a pass without artwork`() {
        val pass = pass(
            description = "Reservation",
            fields = listOf(PassFieldUiModel("event", "Event", "concert", false, "primaryFields")),
        )

        assertThat(pass.homeCardInitial()).isEqualTo("C")
    }

    @Test
    fun `falls back to the description initial when the primary field is empty`() {
        val pass = pass(
            description = " reservation",
            fields = listOf(PassFieldUiModel("event", "Event", " ", false, "primaryFields")),
        )

        assertThat(pass.homeCardInitial()).isEqualTo("R")
    }

    private fun pass(description: String, fields: List<PassFieldUiModel>) = PassUiModel(
        id = "pass",
        description = description,
        creator = null,
        type = PassType.EVENT,
        accentColor = 0,
        barcodeFormat = null,
        barcodeMessage = null,
        barcodeAlternativeText = null,
        fields = fields,
        locations = emptyList(),
        calendarEvent = null,
    )
}
