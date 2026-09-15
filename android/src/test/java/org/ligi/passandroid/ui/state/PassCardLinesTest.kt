package org.ligi.passandroid.ui.state

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.defaultHiddenHomeCardSections
import org.ligi.passandroid.repository.defaultHomeCardSectionOrder
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class PassCardLinesTest {
    private val eventStart = ZonedDateTime.of(2026, 1, 10, 15, 30, 0, 0, ZoneId.of("Europe/Berlin"))

    @Test
    fun `default customization puts the description first and the primary field and date after it`() {
        val lines = resolvePassCardTextLines(
            pass(
                description = "Concert",
                primaryField = "Standing area",
                from = eventStart,
            ),
            defaultHomeCardSectionOrder,
            defaultHiddenHomeCardSections,
            emptyList(),
        )

        assertThat(lines.first()).isEqualTo("Concert")
        assertThat(lines).contains("Standing area")
        assertThat(lines).anyMatch { it.contains("15:30") }
    }

    @Test
    fun `hiding the title promotes the primary field to the card title`() {
        val lines = resolvePassCardTextLines(
            pass(
                description = "Concert",
                primaryField = "Standing area",
                from = eventStart,
            ),
            defaultHomeCardSectionOrder,
            defaultHiddenHomeCardSections + HomeCardSection.TITLE,
            emptyList(),
        )

        assertThat(lines.first()).isEqualTo("Standing area")
        assertThat(lines).noneMatch { it == "Concert" }
    }

    @Test
    fun `moving the date before the title makes the date the card title`() {
        val lines = resolvePassCardTextLines(
            pass(
                description = "Concert",
                from = eventStart,
            ),
            listOf(HomeCardSection.ARTWORK, HomeCardSection.DATE, HomeCardSection.TITLE, HomeCardSection.PRIMARY_FIELD),
            emptySet(),
            emptyList(),
        )

        assertThat(lines.first()).contains("15:30")
        assertThat(lines.drop(1)).containsExactly("Concert")
    }

    @Test
    fun `hiding the title and primary field falls back to the date`() {
        val lines = resolvePassCardTextLines(
            pass(
                description = "Concert",
                primaryField = "Standing area",
                from = eventStart,
            ),
            defaultHomeCardSectionOrder,
            defaultHiddenHomeCardSections + HomeCardSection.TITLE + HomeCardSection.PRIMARY_FIELD,
            emptyList(),
        )

        assertThat(lines.first()).contains("15:30")
    }

    @Test
    fun `hero cards drop a primary field that repeats the today start time`() {
        val pass = pass(
            description = "Concert",
            primaryField = "15:30",
            from = eventStart,
        )

        assertThat(resolvePassCardTextLines(pass, defaultHomeCardSectionOrder, defaultHiddenHomeCardSections, emptyList(), hero = true))
            .doesNotContain("15:30")
        assertThat(resolvePassCardTextLines(pass, defaultHomeCardSectionOrder, defaultHiddenHomeCardSections, emptyList(), hero = false))
            .contains("15:30")
    }

    @Test
    fun `a blank pass with every section hidden has no text lines`() {
        val lines = resolvePassCardTextLines(
            pass(description = "  ", primaryField = null, from = null, creator = "  "),
            defaultHomeCardSectionOrder,
            HomeCardSection.entries.toSet(),
            emptyList(),
        )

        assertThat(lines).isEmpty()
        assertThat(resolvePassCardTitle(pass(description = "  "), defaultHomeCardSectionOrder, HomeCardSection.entries.toSet(), emptyList()))
            .isEqualTo("  ")
    }

    @Test
    fun `an adjacent creator and pass type render as one combined line`() {
        val lines = resolvePassCardTextLines(
            pass(description = "Concert", creator = "Example issuer"),
            defaultHomeCardSectionOrder,
            defaultHiddenHomeCardSections - HomeCardSection.CREATOR - HomeCardSection.PASS_TYPE,
            emptyList(),
        )

        assertThat(lines).contains("Example issuer • Event")
        assertThat(lines.filter { it.contains("Event") }).hasSize(1)
    }

    @Test
    fun `a separated pass type keeps its own metadata line`() {
        val lines = resolvePassCardLines(
            pass(description = "Concert", creator = null),
            defaultHomeCardSectionOrder,
            defaultHiddenHomeCardSections - HomeCardSection.PASS_TYPE,
            emptyList(),
        )

        assertThat(lines.filterIsInstance<PassCardLine.Metadata>().single().typeLabel).isEqualTo("Event")
    }

    @Test
    fun `category metadata carries only the custom tags of the pass`() {
        val travel = PassCategory("travel", "Travel", 0xFF1565C0)
        val builtIn = PassCategory("new", "Inbox", 0xFF3F51B5, org.ligi.passandroid.repository.PassCategoryRole.INBOX)
        val lines = resolvePassCardLines(
            pass(description = "Concert", tagIds = setOf("travel", "new")),
            defaultHomeCardSectionOrder,
            defaultHiddenHomeCardSections,
            listOf(travel, builtIn),
        )

        val metadata = lines.filterIsInstance<PassCardLine.Metadata>().single()
        assertThat(metadata.tags).containsExactly(travel)
    }

    private fun pass(
        description: String = "Pass",
        primaryField: String? = null,
        from: ZonedDateTime? = null,
        creator: String? = null,
        tagIds: Set<String> = emptySet(),
    ) = PassUiModel(
        id = "pass",
        description = description,
        creator = creator,
        type = PassType.EVENT,
        accentColor = 0,
        barcodeFormat = null,
        barcodeMessage = null,
        barcodeAlternativeText = null,
        fields = primaryField?.let {
            listOf(PassFieldUiModel("event", "Event", it, false, "primaryFields"))
        } ?: emptyList(),
        locations = emptyList(),
        calendarEvent = null,
        calendarTimeSpan = from?.let { PassTimeSpanUiModel(it, it.plusHours(2)) },
        tagIds = tagIds,
    )
}
