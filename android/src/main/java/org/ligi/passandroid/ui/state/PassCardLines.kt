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
): List<PassCardLine> {
    val visibleSections = sectionOrder.filterNot { it in hiddenSections || it == HomeCardSection.ARTWORK }
    val typeLabel = pass.type.name.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
    val creatorTypeCombined = visibleSections.indexOf(HomeCardSection.CREATOR).let { creatorIndex ->
        creatorIndex >= 0 && visibleSections.getOrNull(creatorIndex + 1) == HomeCardSection.PASS_TYPE && !pass.creator.isNullOrBlank()
    }
    var typeRendered = false
    var tagsRendered = false
    val lines = mutableListOf<PassCardLine>()
    visibleSections.forEach { section ->
        when (section) {
            HomeCardSection.ARTWORK -> Unit
            HomeCardSection.TITLE -> lines += PassCardLine.Text(pass.description, maxLines = 2, ellipsize = true)
            HomeCardSection.PRIMARY_FIELD -> pass.homeCardDetail()
                ?.takeUnless { hero && it == pass.todayStartTimeLabel() }
                ?.let { lines += PassCardLine.Text(it, maxLines = 1, ellipsize = true) }
            HomeCardSection.DATE -> pass.dateLabel(compactForToday = hero)?.let { lines += PassCardLine.Text(it) }
            HomeCardSection.CREATOR -> pass.creator?.takeIf(String::isNotBlank)?.let { creator ->
                val value = if (creatorTypeCombined) {
                    typeRendered = true
                    "$creator • $typeLabel"
                } else {
                    creator
                }
                lines += PassCardLine.Text(value, maxLines = 1, ellipsize = true)
            }
            HomeCardSection.CATEGORY,
            HomeCardSection.PASS_TYPE,
            -> {
                val showType = HomeCardSection.PASS_TYPE in visibleSections && !typeRendered && !creatorTypeCombined
                val visibleTags = if (HomeCardSection.CATEGORY in visibleSections && !tagsRendered) {
                    tagCategories.filter { it.role == PassCategoryRole.CUSTOM && it.id in pass.tagIds }
                } else {
                    emptyList()
                }
                if (showType || visibleTags.isNotEmpty()) {
                    if (showType) typeRendered = true
                    if (visibleTags.isNotEmpty()) tagsRendered = true
                    lines += PassCardLine.Metadata(typeLabel.takeIf { showType }, visibleTags)
                }
            }
        }
    }
    return lines
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
