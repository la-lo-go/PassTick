package org.ligi.passandroid.ui.state

import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

internal fun PassUiModel.occursToday(): Boolean {
    val zone = calendarTimeSpan?.from?.zone ?: calendarTimeSpan?.to?.zone ?: return false
    val date = LocalDate.now(zone)
    val from = calendarTimeSpan?.from?.withZoneSameInstant(zone)?.toLocalDate()
    // The start date defines the Today section. An event ending today is not a new Today item.
    return from == date
}

internal fun PassUiModel.todayStartTimeLabel(): String? =
    calendarTimeSpan?.from?.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

internal fun PassUiModel.dateLabel(compactForToday: Boolean = false): String? {
    val from = calendarTimeSpan?.from
    val to = calendarTimeSpan?.to
    val value = from ?: to ?: return null
    if (!compactForToday || !occursToday()) {
        return value.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy · HH:mm", Locale.getDefault()))
    }
    val timePattern = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    if (from == null || to == null) return value.format(timePattern)
    val endInStartZone = to.withZoneSameInstant(from.zone)
    val dayGap = ChronoUnit.DAYS.between(from.toLocalDate(), endInStartZone.toLocalDate())
    val daySuffix = if (dayGap >= 1) " (+$dayGap)" else ""
    return "${from.format(timePattern)} > ${endInStartZone.format(timePattern)}$daySuffix"
}

sealed interface PassCardLine {
    data class Text(val value: String, val maxLines: Int = Int.MAX_VALUE, val ellipsize: Boolean = false) : PassCardLine
    data class Metadata(val typeLabel: String?, val tags: List<PassCategory>) : PassCardLine
}

fun resolvePassCardLines(
    pass: PassUiModel,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    tagCategories: List<PassCategory>,
    hero: Boolean = false,
): List<PassCardLine> = PassCardLineResolver(pass, sectionOrder, hiddenSections, tagCategories, hero).resolve()

private class PassCardLineResolver(
    private val pass: PassUiModel,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    private val tagCategories: List<PassCategory>,
    private val hero: Boolean,
) {
    private val visibleSections = sectionOrder.filterNot { it in hiddenSections || it == HomeCardSection.ARTWORK }
    private val typeLabel = pass.type.name.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
    private val creatorTypeCombined = visibleSections.indexOf(HomeCardSection.CREATOR).let { creatorIndex ->
        creatorIndex >= 0 && visibleSections.getOrNull(creatorIndex + 1) == HomeCardSection.PASS_TYPE && !pass.creator.isNullOrBlank()
    }
    private var typeRendered = false
    private var tagsRendered = false
    private val lines = mutableListOf<PassCardLine>()

    fun resolve(): List<PassCardLine> {
        visibleSections.forEach(::appendSection)
        return lines
    }

    private fun appendSection(section: HomeCardSection) = when (section) {
        HomeCardSection.ARTWORK -> Unit
        HomeCardSection.TITLE -> lines += PassCardLine.Text(pass.description, maxLines = 2, ellipsize = true)
        HomeCardSection.PRIMARY_FIELD -> appendPrimaryField()
        HomeCardSection.DATE -> pass.dateLabel(compactForToday = hero)?.let { lines += PassCardLine.Text(it) }
        HomeCardSection.CREATOR -> appendCreator()
        HomeCardSection.CATEGORY,
        HomeCardSection.PASS_TYPE,
        -> appendMetadata()
    }

    private fun appendPrimaryField() {
        val value = pass.homeCardDetail()?.takeUnless { hero && it == pass.todayStartTimeLabel() } ?: return
        lines += PassCardLine.Text(value, maxLines = 1, ellipsize = true)
    }

    private fun appendCreator() {
        val creator = pass.creator?.takeIf(String::isNotBlank) ?: return
        val value = if (creatorTypeCombined) {
            typeRendered = true
            "$creator • $typeLabel"
        } else {
            creator
        }
        lines += PassCardLine.Text(value, maxLines = 1, ellipsize = true)
    }

    private fun appendMetadata() {
        val showType = HomeCardSection.PASS_TYPE in visibleSections && !typeRendered && !creatorTypeCombined
        val visibleTags = if (HomeCardSection.CATEGORY in visibleSections && !tagsRendered) {
            tagCategories.filter { it.role == PassCategoryRole.CUSTOM && it.id in pass.tagIds }
        } else {
            emptyList()
        }
        if (!showType && visibleTags.isEmpty()) return
        if (showType) typeRendered = true
        if (visibleTags.isNotEmpty()) tagsRendered = true
        lines += PassCardLine.Metadata(typeLabel.takeIf { showType }, visibleTags)
    }
}

fun resolvePassCardTextLines(
    pass: PassUiModel,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    tagCategories: List<PassCategory>,
    hero: Boolean = false,
): List<String> = resolvePassCardLines(pass, sectionOrder, hiddenSections, tagCategories, hero).mapNotNull { line ->
    when (line) {
        is PassCardLine.Text -> line.value.takeIf(String::isNotBlank)
        is PassCardLine.Metadata -> line.typeLabel?.takeIf(String::isNotBlank)
    }
}

fun resolvePassCardTitle(
    pass: PassUiModel,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    tagCategories: List<PassCategory>,
    hero: Boolean = false,
): String = resolvePassCardTextLines(pass, sectionOrder, hiddenSections, tagCategories, hero).firstOrNull() ?: pass.description
