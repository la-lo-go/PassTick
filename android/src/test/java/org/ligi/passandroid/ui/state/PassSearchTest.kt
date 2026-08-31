package org.ligi.passandroid.ui.state

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType

class PassSearchTest {
    @Test
    fun `search normalizes accents case and matches heterogeneous fields`() {
        val pass = PassUiModel(
            id = "id",
            description = "Billete de avión",
            creator = "Compañía Áerea",
            type = PassType.BOARDING,
            accentColor = 0,
            barcodeFormat = null,
            barcodeMessage = null,
            barcodeAlternativeText = null,
            fields = listOf(PassFieldUiModel("seat", "Asiento", "12Á", false, "auxiliaryFields")),
            locations = listOf(PassLocationUiModel("Málaga", 0.0, 0.0)),
            calendarEvent = null,
        )

        val document = pass.searchDocument()

        assertThat("AVION malaga 12a".searchTerms().all(document::contains)).isTrue()
        assertThat("boarding".searchTerms().all(document::contains)).isTrue()
        assertThat("tren".searchTerms().all(document::contains)).isFalse()
    }

    @Test
    fun `blank query has no terms`() {
        assertThat("   ".searchTerms()).isEmpty()
    }
}
