package org.ligi.passandroid.ui.state

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.repository.PassFieldSnapshot
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.repository.PassTimeSpanSnapshot
import org.threeten.bp.ZonedDateTime

class CodePassPickerRowsTest {

    @Test
    fun `rows follow the configured sort order`() {
        val passes = listOf(
            pass("late", "Late ticket", "2026-09-09T18:00:00Z"),
            pass("early", "Early ticket", "2026-09-09T10:00:00Z"),
        )

        val rows = codePassPickerRows(passes, AppSettings(sortOrder = PassSortOrder.DATE_ASC))

        assertThat(rows.map(CodePassPickerRow::passId)).containsExactly("early", "late")
    }

    @Test
    fun `row title and subtitle use the configured card lines`() {
        val snapshot = pass("visible", "Train ticket", "2026-09-09T10:00:00Z").copy(
            fields = listOf(PassFieldSnapshot("seat", "Seat", "12A", hidden = false, hint = "primaryFields")),
        )

        val rows = codePassPickerRows(
            listOf(snapshot),
            AppSettings(hiddenHomeCardSections = setOf(HomeCardSection.TITLE)),
        )

        assertThat(rows.single().title).isEqualTo("12A")
    }

    @Test
    fun `protected and archived passes stay out of the picker`() {
        val passes = listOf(
            pass("visible", "Train ticket", "2026-09-09T10:00:00Z"),
            pass("private", "Private ticket", "2026-09-09T10:00:00Z").copy(isProtected = true),
            pass("archived", "Old ticket", "2026-09-09T10:00:00Z").copy(isArchived = true),
        )

        val rows = codePassPickerRows(passes, AppSettings())

        assertThat(rows.map(CodePassPickerRow::passId)).containsExactly("visible")
    }

    @Test
    fun `the chosen pass is marked as selected`() {
        val passes = listOf(
            pass("first", "First ticket", "2026-09-09T10:00:00Z"),
            pass("second", "Second ticket", "2026-09-09T18:00:00Z"),
        )

        val rows = codePassPickerRows(passes, AppSettings(codePassId = "second"))

        assertThat(rows.filter(CodePassPickerRow::selected).map(CodePassPickerRow::passId))
            .containsExactly("second")
    }

    private fun pass(id: String, description: String, start: String) = PassSnapshot(
        id = id,
        description = description,
        creator = null,
        type = PassType.EVENT,
        accentColor = 0,
        barcodeFormat = null,
        barcodeMessage = null,
        barcodeAlternativeText = null,
        fields = emptyList(),
        locations = emptyList(),
        calendarTimeSpan = PassTimeSpanSnapshot(
            from = ZonedDateTime.parse(start),
            to = ZonedDateTime.parse(start).plusHours(2),
        ),
    )
}
