package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

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
