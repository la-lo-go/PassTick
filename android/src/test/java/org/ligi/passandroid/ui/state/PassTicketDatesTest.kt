package org.ligi.passandroid.ui.state

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.threeten.bp.LocalDate

class PassTicketDatesTest {
    private val date = LocalDate.of(2025, 2, 4)

    @Test
    fun `numeric dates match day first, month first, and two digit years`() {
        assertThat("04/02/2025 (VOSE)19:30".mentionsDate(date)).isTrue()
        assertThat("02/04/2025".mentionsDate(date)).isTrue()
        assertThat("4.2.25".mentionsDate(date)).isTrue()
        assertThat("05/02/2025".mentionsDate(date)).isFalse()
    }

    @Test
    fun `iso and month name dates match`() {
        assertThat("2025-02-04T19:30".mentionsDate(date)).isTrue()
        assertThat("4 FEB 2025".mentionsDate(date)).isTrue()
        assertThat("4 de febrero de 2025".mentionsDate(date)).isTrue()
        assertThat("4 MAR 2025".mentionsDate(date)).isFalse()
    }

    @Test
    fun `a field repeats the date only when its label adds nothing`() {
        assertThat(field("", "04/02/2025 19:30").repeatsDate(date)).isTrue()
        assertThat(field("04/02/25 19:30", "Babymetal").repeatsDate(date)).isTrue()
        assertThat(field("Viaje a: AVILA", "04/02/2025").repeatsDate(date)).isTrue()
        assertThat(field("F. NACIMIENTO", "04/02/2025").repeatsDate(date)).isFalse()
        assertThat(field("Fecha de emisión", "03/02/2025 21:39").repeatsDate(date)).isFalse()
    }

    private fun field(label: String, value: String) = PassFieldUiModel(null, label, value, false, "headerFields")
}
