package org.ligi.passandroid.widget

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.repository.PassFieldSnapshot
import org.ligi.passandroid.repository.PassLocationSnapshot
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.repository.PassTimeSpanSnapshot
import org.ligi.passandroid.repository.defaultHiddenHomeCardSections
import org.ligi.passandroid.repository.defaultHomeCardSectionOrder
import org.ligi.passandroid.repository.defaultPassCategories
import org.threeten.bp.ZonedDateTime

class PassWidgetSnapshotPublisherTest {
    @Test
    fun `protected passes stay out of the widget snapshot`() {
        val passes = listOf(
            pass("public", "Train ticket"),
            pass("private", "Private ticket").copy(isProtected = true),
        )

        val widgetPasses = publishedWidgetPasses(passes)

        assertThat(widgetPasses.map(WidgetPass::id)).containsExactly("public")
    }

    @Test
    fun `lock all passes empties the widget snapshot`() {
        val passes = listOf(pass("public", "Train ticket"))

        val widgetPasses = publishedWidgetPasses(passes, lockAllPasses = true)

        assertThat(widgetPasses).isEmpty()
    }

    @Test
    fun `archived and excluded passes stay out of the widget snapshot`() {
        val passes = listOf(
            pass("visible", "Train ticket"),
            pass("archived", "Old ticket").copy(isArchived = true),
            pass("excluded", "Hidden ticket").copy(categoryId = "trash"),
        )

        val widgetPasses = publishedWidgetPasses(passes, excludedCategoryIds = setOf("trash"))

        assertThat(widgetPasses.map(WidgetPass::id)).containsExactly("visible")
    }

    @Test
    fun `visible pass keeps its schedule location and supporting text`() {
        val snapshot = pass("visible", "Train ticket").copy(
            locations = listOf(PassLocationSnapshot("Central station", 40.4, -3.7)),
            fields = listOf(PassFieldSnapshot("seat", "Seat", "12A", hidden = false, hint = null)),
        )

        val widgetPass = publishedWidgetPasses(listOf(snapshot)).single()

        assertThat(widgetPass.title).isEqualTo("Train ticket")
        assertThat(widgetPass.location).isEqualTo("Central station")
        assertThat(widgetPass.supportingText).isEqualTo("12A")
        assertThat(widgetPass.startsAtEpochMillis).isEqualTo(time("2026-09-09T10:00:00Z"))
    }

    @Test
    fun `visible pass carries issuer type pin state and barcode for the code widget`() {
        val snapshot = pass("visible", "Train ticket").copy(
            creator = "Renfe",
            isFavorite = true,
            barcodeFormat = org.ligi.passandroid.model.pass.PassBarCodeFormat.QR_CODE,
            barcodeMessage = "https://example.test/visible",
        )

        val widgetPass = publishedWidgetPasses(listOf(snapshot)).single()

        assertThat(widgetPass.issuer).isEqualTo("Renfe")
        assertThat(widgetPass.type).isEqualTo(PassType.EVENT)
        assertThat(widgetPass.isPinned).isTrue()
        assertThat(widgetPass.barcodeFormat).isEqualTo(org.ligi.passandroid.model.pass.PassBarCodeFormat.QR_CODE)
        assertThat(widgetPass.barcodeMessage).isEqualTo("https://example.test/visible")
    }

    @Test
    fun `primary line is the first visible home card line`() {
        val snapshot = pass("visible", "Train ticket").copy(
            fields = listOf(PassFieldSnapshot("seat", "Seat", "12A", hidden = false, hint = "primaryFields")),
        )

        val widgetPass = publishedWidgetPasses(
            listOf(snapshot),
            hiddenSections = setOf(HomeCardSection.TITLE),
        ).single()

        assertThat(widgetPass.primaryLine).isEqualTo("12A")
    }

    @Test
    fun `primary line falls back to the description when every line is hidden`() {
        val snapshot = pass("visible", "Train ticket").copy(fields = emptyList())

        val widgetPass = publishedWidgetPasses(
            listOf(snapshot),
            hiddenSections = HomeCardSection.entries.toSet(),
        ).single()

        assertThat(widgetPass.primaryLine).isEqualTo("Train ticket")
    }

    private fun publishedWidgetPasses(
        passes: List<PassSnapshot>,
        excludedCategoryIds: Set<String> = emptySet(),
        lockAllPasses: Boolean = false,
        hiddenSections: Set<HomeCardSection> = defaultHiddenHomeCardSections,
    ) = widgetPasses(
        passes,
        excludedCategoryIds,
        lockAllPasses,
        defaultHomeCardSectionOrder,
        hiddenSections,
        defaultPassCategories,
    )

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
