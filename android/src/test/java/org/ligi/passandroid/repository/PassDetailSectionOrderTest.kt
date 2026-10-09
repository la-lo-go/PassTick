package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.repository.PassDetailSection.ACTIONS
import org.ligi.passandroid.repository.PassDetailSection.ARTWORK
import org.ligi.passandroid.repository.PassDetailSection.BACK_FIELDS
import org.ligi.passandroid.repository.PassDetailSection.BARCODE
import org.ligi.passandroid.repository.PassDetailSection.CALENDAR
import org.ligi.passandroid.repository.PassDetailSection.FIELDS
import org.ligi.passandroid.repository.PassDetailSection.LOCATIONS
import org.ligi.passandroid.repository.PassDetailSection.NOTES

class PassDetailSectionOrderTest {
    @Test
    fun `the old unchanged default moves to the new default`() {
        val saved = listOf(ARTWORK, BARCODE, CALENDAR, FIELDS, LOCATIONS, NOTES)

        assertThat(normalizePassDetailSectionOrder(saved)).isEqualTo(defaultPassDetailSectionOrder)
    }

    @Test
    fun `the old default saved with the action sections also moves to the new default`() {
        val saved = listOf(ARTWORK, BARCODE, CALENDAR, FIELDS, ACTIONS, BACK_FIELDS, LOCATIONS, NOTES)

        assertThat(normalizePassDetailSectionOrder(saved)).isEqualTo(defaultPassDetailSectionOrder)
    }

    @Test
    fun `a custom order saved before the action sections gets them right after the ticket`() {
        val saved = listOf(NOTES, BARCODE, FIELDS, ARTWORK, CALENDAR, LOCATIONS)

        assertThat(normalizePassDetailSectionOrder(saved))
            .containsExactly(NOTES, BARCODE, FIELDS, ACTIONS, BACK_FIELDS, ARTWORK, CALENDAR, LOCATIONS)
    }

    @Test
    fun `a complete order is kept as it is`() {
        val saved = listOf(NOTES, ACTIONS, FIELDS, BARCODE, BACK_FIELDS, ARTWORK, CALENDAR, LOCATIONS)

        assertThat(normalizePassDetailSectionOrder(saved)).containsExactlyElementsOf(saved)
    }

    @Test
    fun `an empty saved order falls back to the default order`() {
        assertThat(normalizePassDetailSectionOrder(emptyList()))
            .containsExactly(ARTWORK, FIELDS, BARCODE, ACTIONS, BACK_FIELDS, CALENDAR, LOCATIONS, NOTES)
    }
}
