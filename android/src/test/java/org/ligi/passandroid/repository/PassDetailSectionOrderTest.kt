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
    fun `an order saved before the action sections gets them right after the ticket`() {
        val saved = listOf(ARTWORK, BARCODE, CALENDAR, FIELDS, LOCATIONS, NOTES)

        assertThat(normalizePassDetailSectionOrder(saved))
            .containsExactly(ARTWORK, BARCODE, CALENDAR, FIELDS, ACTIONS, BACK_FIELDS, LOCATIONS, NOTES)
    }

    @Test
    fun `a complete order is kept as it is`() {
        val saved = listOf(NOTES, ACTIONS, FIELDS, BARCODE, BACK_FIELDS, ARTWORK, CALENDAR, LOCATIONS)

        assertThat(normalizePassDetailSectionOrder(saved)).containsExactlyElementsOf(saved)
    }

    @Test
    fun `an empty saved order falls back to the default order`() {
        assertThat(normalizePassDetailSectionOrder(emptyList()))
            .containsExactly(ARTWORK, BARCODE, FIELDS, ACTIONS, BACK_FIELDS, CALENDAR, LOCATIONS, NOTES)
    }
}
