package org.ligi.passandroid.ui.compose

import org.ligi.passandroid.domain.timeline.PassTimeline
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

internal fun monthGridCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val cells = MutableList<LocalDate?>(leading) { null }
    for (day in 1..month.lengthOfMonth()) {
        cells.add(month.atDay(day))
    }
    while (cells.size < 42) {
        cells.add(null)
    }
    return cells
}

internal fun agendaBadgeLabel(count: Int): String = if (count > 9) "9+" else count.toString()

internal fun timelineDayIndex(timeline: PassTimeline, date: LocalDate): Int? {
    var itemIndex = 0
    timeline.days.asReversed().forEach { day ->
        if (day.date == date) return itemIndex
        itemIndex += 1 + day.events.size
    }
    return null
}
