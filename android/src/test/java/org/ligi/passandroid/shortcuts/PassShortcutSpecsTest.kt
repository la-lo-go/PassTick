package org.ligi.passandroid.shortcuts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.repository.PassFieldSnapshot
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.repository.defaultHiddenHomeCardSections
import org.ligi.passandroid.repository.defaultHomeCardSectionOrder
import org.ligi.passandroid.repository.defaultPassCategories

class PassShortcutSpecsTest {

    @Test
    fun `four pinned passes become four shortcuts`() {
        val passes = (1..5).map { pinned("pass-$it", "Pass $it") }

        val specs = specs(passes)

        assertThat(specs).hasSize(4)
        assertThat(specs.map(PassShortcutSpec::passId)).containsExactly("pass-1", "pass-2", "pass-3", "pass-4")
    }

    @Test
    fun `only pinned passes become shortcuts`() {
        val passes = listOf(
            pinned("pinned", "Pinned"),
            plain("plain", "Not pinned"),
        )

        val specs = specs(passes)

        assertThat(specs.map(PassShortcutSpec::passId)).containsExactly("pinned")
    }

    @Test
    fun `protected passes never become shortcuts`() {
        val passes = listOf(
            pinned("public", "Public").copy(isProtected = true),
            pinned("visible", "Visible"),
        )

        val specs = specs(passes)

        assertThat(specs.map(PassShortcutSpec::passId)).containsExactly("visible")
    }

    @Test
    fun `lock all clears every shortcut`() {
        val passes = listOf(pinned("pinned", "Pinned"))

        val specs = specs(passes, lockAllPasses = true)

        assertThat(specs).isEmpty()
    }

    @Test
    fun `shortcut titles fall back to a neutral label`() {
        val passes = listOf(pinned("pinned", ""))

        val specs = specs(passes)

        assertThat(specs.single().title).isEqualTo("Pass")
    }

    @Test
    fun `shortcut title is the first visible home card line`() {
        val passes = listOf(
            pinned("pinned", "Train ticket").copy(
                fields = listOf(PassFieldSnapshot("seat", "Seat", "12A", hidden = false, hint = "primaryFields")),
            ),
        )

        val specs = specs(passes, hiddenSections = setOf(HomeCardSection.TITLE))

        assertThat(specs.single().title).isEqualTo("12A")
    }

    private fun specs(
        passes: List<PassSnapshot>,
        lockAllPasses: Boolean = false,
        hiddenSections: Set<HomeCardSection> = defaultHiddenHomeCardSections,
    ) = shortcutSpecs(
        passes,
        lockAllPasses,
        defaultHomeCardSectionOrder,
        hiddenSections,
        defaultPassCategories,
    )

    private fun pinned(id: String, description: String) = plain(id, description).copy(isFavorite = true)

    private fun plain(id: String, description: String) = PassSnapshot(
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
        calendarTimeSpan = null,
    )
}