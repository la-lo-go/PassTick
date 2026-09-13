package org.ligi.passandroid.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.ligi.passandroid.ui.state.PassUiModel
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

internal data class HomePassSections<T>(
    val today: List<T>,
    val pinned: List<T>,
    val other: List<T>,
) {
    val showOtherHeading: Boolean get() = other.isNotEmpty() && (today.isNotEmpty() || pinned.isNotEmpty())
}

internal fun <T> deriveHomePassSections(
    passes: List<T>,
    highlightTodayPasses: Boolean,
    isToday: (T) -> Boolean,
    isPinned: (T) -> Boolean,
): HomePassSections<T> {
    val today = passes.filter { highlightTodayPasses && isToday(it) }
    val pinned = passes.filter { !(highlightTodayPasses && isToday(it)) && isPinned(it) }
    val other = passes.filter { !(highlightTodayPasses && isToday(it)) && !isPinned(it) }
    return HomePassSections(today, pinned, other)
}

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
