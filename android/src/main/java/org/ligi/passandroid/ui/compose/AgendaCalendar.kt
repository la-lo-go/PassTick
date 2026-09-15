package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.R
import org.ligi.passandroid.domain.timeline.PassEvent
import org.ligi.passandroid.domain.timeline.PassTimeline
import org.ligi.passandroid.ui.theme.PassDesignSystem
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.WeekFields

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

internal fun timelineEventsForDate(timeline: PassTimeline, date: LocalDate): List<PassEvent> =
    timeline.days.firstOrNull { it.date == date }?.events.orEmpty()

private const val GridRows = 6
private const val GridColumns = 7
private const val DayCellDiameter = 40
// A fixed month backlog seeds the infinite vertical scroll in both directions;
// the initial scroll index lands on today's month.
private const val MonthsBefore = 100
private const val MonthsAfter = 100
private const val CurrentMonthIndex = MonthsBefore

@Composable
internal fun AgendaCalendar(
    timeline: PassTimeline,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    val weekdayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM d, yyyy", locale) }
    val passCounts = remember(timeline) { timeline.days.associate { it.date to it.events.size } }
    val today = remember(timeline.zoneId) { LocalDate.now(timeline.zoneId) }
    val currentMonth = remember(today) { YearMonth.from(today) }
    val weekdays = remember(firstDayOfWeek) { (0L..6L).map(firstDayOfWeek::plus) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = CurrentMonthIndex)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { dayOfWeek ->
                Text(
                    text = weekdayFormatter.format(dayOfWeek),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(
                count = MonthsBefore + MonthsAfter + 1,
                key = { it },
            ) { index ->
                AgendaMonthBlock(
                    month = currentMonth.plusMonths((index - CurrentMonthIndex).toLong()),
                    firstDayOfWeek = firstDayOfWeek,
                    monthFormatter = monthFormatter,
                    dayFormatter = dayFormatter,
                    passCounts = passCounts,
                    selectedDay = selectedDay,
                    today = today,
                    onDayClick = onDayClick,
                )
            }
        }
    }
}

@Composable
private fun AgendaMonthBlock(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    monthFormatter: DateTimeFormatter,
    dayFormatter: DateTimeFormatter,
    passCounts: Map<LocalDate, Int>,
    selectedDay: LocalDate?,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
) {
    val cells = remember(month, firstDayOfWeek) { monthGridCells(month, firstDayOfWeek) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = month.format(monthFormatter),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        // Fixed six-row grid height so every month block keeps the same size.
        Column(Modifier.height((GridRows * DayCellDiameter).dp)) {
            repeat(GridRows) { row ->
                Row(Modifier.fillMaxWidth().height(DayCellDiameter.dp)) {
                    for (column in 0 until GridColumns) {
                        val cell = cells[row * GridColumns + column]
                        AgendaDayCell(
                            date = cell,
                            count = cell?.let(passCounts::get),
                            isToday = cell == today,
                            isSelected = cell != null && cell == selectedDay,
                            isPast = cell != null && cell.isBefore(today),
                            dayFormatter = dayFormatter,
                            modifier = Modifier.weight(1f),
                            onClick = cell?.let { date -> { onDayClick(date) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaDayCell(
    date: LocalDate?,
    count: Int?,
    isToday: Boolean,
    isSelected: Boolean,
    isPast: Boolean,
    dayFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (date == null) {
        Box(modifier)
        return
    }
    val circleModifier = when {
        isSelected -> Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        isToday -> Modifier.border(2.dp, PassDesignSystem.Today, CircleShape)
        else -> Modifier
    }
    val dayColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isPast -> PassDesignSystem.Past
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dotColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
    val description = date.format(dayFormatter) +
        count?.let { label -> ", " + pluralStringResource(R.plurals.calendar_day_passes, label, label) } +
        if (isSelected) ", " + stringResource(R.string.calendar_selected_day) else ""
    Box(
        modifier
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(DayCellDiameter.dp)
                .then(circleModifier),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    color = dayColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                // The pass count is a dot under the number so the day number stays readable.
                if (count != null) {
                    Box(
                        Modifier
                            .padding(top = 1.dp)
                            .size(5.dp)
                            .background(dotColor, CircleShape),
                    )
                }
            }
        }
    }
}
