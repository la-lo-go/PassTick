package org.ligi.passandroid.widget

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.PassFieldSnapshot
import org.ligi.passandroid.repository.PassLocationSnapshot
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.repository.PassTimeSpanSnapshot
import org.threeten.bp.ZonedDateTime

class PassWidgetSnapshotPublisherTest {
    @Test
    fun `protected passes stay out of the widget snapshot`() {
        val passes = listOf(
            pass("public", "Train ticket"),
            pass("private", "Private ticket").copy(isProtected = true),
        )

        val widgetPasses = widgetPasses(passes, excludedCategoryIds = emptySet(), lockAllPasses = false)

        assertThat(widgetPasses.map(WidgetPass::id)).containsExactly("public")
    }

    @Test
    fun `lock all passes empties the widget snapshot`() {
        val passes = listOf(pass("public", "Train ticket"))

        val widgetPasses = widgetPasses(passes, excludedCategoryIds = emptySet(), lockAllPasses = true)

        assertThat(widgetPasses).isEmpty()
    }

    @Test
    fun `archived and excluded passes stay out of the widget snapshot`() {
        val passes = listOf(
            pass("visible", "Train ticket"),
            pass("archived", "Old ticket").copy(isArchived = true),
            pass("excluded", "Hidden ticket").copy(categoryId = "trash"),
        )

        val widgetPasses = widgetPasses(passes, excludedCategoryIds = setOf("trash"), lockAllPasses = false)

        assertThat(widgetPasses.map(WidgetPass::id)).containsExactly("visible")
    }

    @Test
    fun `visible pass keeps its schedule location and supporting text`() {
        val snapshot = pass("visible", "Train ticket").copy(
            locations = listOf(PassLocationSnapshot("Central station", 40.4, -3.7)),
            fields = listOf(PassFieldSnapshot("seat", "Seat", "12A", hidden = false, hint = null)),
        )

        val widgetPass = widgetPasses(listOf(snapshot), excludedCategoryIds = emptySet(), lockAllPasses = false).single()

        assertThat(widgetPass.title).isEqualTo("Train ticket")
        assertThat(widgetPass.location).isEqualTo("Central station")
        assertThat(widgetPass.supportingText).isEqualTo("12A")
        assertThat(widgetPass.startsAtEpochMillis).isEqualTo(time("2026-09-09T10:00:00Z"))
    }

    private fun pass(id: String, description: String) = PassSnapshot(
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
            from = ZonedDateTime.parse("2026-09-09T10:00:00Z"),
            to = ZonedDateTime.parse("2026-09-09T12:00:00Z"),
        ),
    )

    private fun time(value: String) = ZonedDateTime.parse(value).toInstant().toEpochMilli()
}
