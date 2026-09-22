package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.ui.state.EXPIRED_PASSES_CATEGORY_ID
import org.ligi.passandroid.ui.state.PassUiModel
import org.threeten.bp.ZonedDateTime

class HomePassSectionsTest {
    @Test
    fun `Today precedes Pinned and Other without duplicates`() {
        val sections = sections(
            Pass("today-pinned", today = true, pinned = true),
            Pass("pinned", pinned = true),
            Pass("other"),
        )

        assertThat(sections.today.map(Pass::id)).containsExactly("today-pinned")
        assertThat(sections.pinned.map(Pass::id)).containsExactly("pinned")
        assertThat(sections.other.map(Pass::id)).containsExactly("other")
    }

    @Test
    fun `Pinned with a remainder requires an Other passes heading`() {
        val sections = sections(Pass("pinned", pinned = true), Pass("other"))

        assertThat(sections.showOtherHeading).isTrue()
    }

    @Test
    fun `Today alone produces only the Today section`() {
        val sections = sections(Pass("today", today = true))

        assertThat(sections.today.map(Pass::id)).containsExactly("today")
        assertThat(sections.pinned).isEmpty()
        assertThat(sections.other).isEmpty()
        assertThat(sections.showOtherHeading).isFalse()
    }

    @Test
    fun `ordinary passes do not require a section heading`() {
        val sections = sections(Pass("first"), Pass("second"))

        assertThat(sections.today).isEmpty()
        assertThat(sections.pinned).isEmpty()
        assertThat(sections.other.map(Pass::id)).containsExactly("first", "second")
        assertThat(sections.showOtherHeading).isFalse()
    }

    @Test
    fun `disabled Today highlighting keeps pinned passes separate`() {
        val sections = sections(
            Pass("today-pinned", today = true, pinned = true),
            Pass("today", today = true),
            highlightTodayPasses = false,
        )

        assertThat(sections.today).isEmpty()
        assertThat(sections.pinned.map(Pass::id)).containsExactly("today-pinned")
        assertThat(sections.other.map(Pass::id)).containsExactly("today")
    }

    @Test
    fun `the Expired filter keeps only passes whose validity end passed`() {
        val now = ZonedDateTime.parse("2026-08-30T12:00:00Z")

        val selected = selectHomePasses(
            passes = listOf(
                uiPass("expired", expiresAt = now.minusMinutes(1)),
                uiPass("active", expiresAt = now.plusMinutes(1)),
                uiPass("at-end", expiresAt = now),
                uiPass("open"),
            ),
            selectedCategoryId = EXPIRED_PASSES_CATEGORY_ID,
            hiddenCategoryIds = emptySet(),
            protectedPassIds = emptySet(),
            now = now,
        )

        assertThat(selected.map(PassUiModel::id)).containsExactly("expired")
    }

    @Test
    fun `the Expired filter ignores archive state`() {
        val now = ZonedDateTime.parse("2026-08-30T12:00:00Z")

        val selected = selectHomePasses(
            passes = listOf(
                uiPass("expired", expiresAt = now.minusMinutes(1)).copy(isArchived = true),
                uiPass("hidden", expiresAt = now.minusMinutes(1), categoryId = "archive"),
            ),
            selectedCategoryId = EXPIRED_PASSES_CATEGORY_ID,
            hiddenCategoryIds = setOf("archive"),
            protectedPassIds = emptySet(),
            now = now,
        )

        assertThat(selected.map(PassUiModel::id)).containsExactly("expired", "hidden")
    }

    private fun uiPass(
        id: String,
        expiresAt: ZonedDateTime? = null,
        categoryId: String = "new",
    ) = PassUiModel(
        id = id,
        description = "Pass $id",
        creator = null,
        type = PassType.EVENT,
        accentColor = 0,
        barcodeFormat = null,
        barcodeMessage = null,
        barcodeAlternativeText = null,
        fields = emptyList(),
        locations = emptyList(),
        calendarEvent = null,
        expiresAt = expiresAt,
        categoryId = categoryId,
    )

    private fun sections(
        vararg passes: Pass,
        highlightTodayPasses: Boolean = true,
    ) = deriveHomePassSections(
        passes = passes.toList(),
        highlightTodayPasses = highlightTodayPasses,
        isToday = Pass::today,
        isPinned = Pass::pinned,
    )

    private data class Pass(val id: String, val today: Boolean = false, val pinned: Boolean = false)
}
