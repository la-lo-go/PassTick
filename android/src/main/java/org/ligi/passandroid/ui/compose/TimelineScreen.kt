package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import org.ligi.passandroid.domain.timeline.EventTemporalState
import org.ligi.passandroid.domain.timeline.PassEvent
import org.ligi.passandroid.domain.timeline.PassTimeline
import org.ligi.passandroid.ui.theme.PassActionButton
import org.ligi.passandroid.ui.theme.PassActionButtonGroup
import org.ligi.passandroid.ui.theme.passActionButtonGroupWidth
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import org.ligi.passandroid.R

@Immutable
data class TimelineUiState(
    val timeline: PassTimeline = PassTimeline.empty(),
    val reminderEventIds: Set<String> = emptySet(),
)

sealed interface TimelineAction {
    data object Back : TimelineAction
    data class OpenPass(val passId: String) : TimelineAction
    data class AddToCalendar(val eventId: String) : TimelineAction
    data class ConfigureReminder(val eventId: String) : TimelineAction
}

enum class TimelineViewMode { List, Agenda }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    state: TimelineUiState,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewMode by rememberSaveable { mutableStateOf(TimelineViewMode.List) }
    var selectedDayEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val nearestIndex = remember(state.timeline.days, state.timeline.nearestEventId) {
        timelineItemIndex(state.timeline, state.timeline.nearestEventId)
    }
    LaunchedEffect(nearestIndex) {
        nearestIndex?.let { listState.scrollToItem(it) }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_timeline)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(TimelineAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.pass_detail_back))
                    }
                },
                actions = {
                    ToggleIconButton(
                        icon = Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.timeline_view_list),
                        selected = viewMode == TimelineViewMode.List,
                        onClick = { viewMode = TimelineViewMode.List },
                    )
                    ToggleIconButton(
                        icon = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.timeline_view_agenda),
                        selected = viewMode == TimelineViewMode.Agenda,
                        onClick = { viewMode = TimelineViewMode.Agenda },
                    )
                },
            )
        },
    ) { padding ->
        if (state.timeline.days.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.timeline_no_dated_passes), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.timeline_dates_from_your_passes_will_appear_here))
            }
        } else {
            when (viewMode) {
                TimelineViewMode.List -> TimelineContent(state, onAction, listState, Modifier.padding(padding))
                TimelineViewMode.Agenda -> AgendaContent(
                    state = state,
                    onAction = onAction,
                    selectedDayEpoch = selectedDayEpoch,
                    onDaySelect = { date -> selectedDayEpoch = date?.toEpochDay() },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun AgendaContent(
    state: TimelineUiState,
    onAction: (TimelineAction) -> Unit,
    selectedDayEpoch: Long?,
    onDaySelect: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var openEventId by remember { mutableStateOf<String?>(null) }
    val locale = LocalConfiguration.current.locales[0]
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale) }
    val selectedDate = selectedDayEpoch?.let(LocalDate::ofEpochDay)
    val selectedEvents = remember(state.timeline, selectedDate) {
        selectedDate?.let { timelineEventsForDate(state.timeline, it).asReversed() }.orEmpty()
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Cap the selected-day section so it never pushes the calendar off screen.
        val sectionMaxHeight = maxHeight * 0.45f
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selectedEvents.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = sectionMaxHeight),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "agendaSelectedDay") {
                        Text(
                            text = selectedDate?.format(dayFormatter).orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    items(selectedEvents, key = PassEvent::id) { event ->
                        TimelineEventRow(
                            event = event,
                            zoneId = state.timeline.zoneId,
                            reminderEnabled = event.id in state.reminderEventIds,
                            highlighted = event.id == state.timeline.nearestEventId,
                            onAction = onAction,
                            openEventId = openEventId,
                            onOpenEvent = { openEventId = it },
                        )
                    }
                }
            }
            AgendaCalendar(
                timeline = state.timeline,
                selectedDay = selectedDate,
                onDayClick = { date ->
                    onDaySelect(date.takeIf { timelineEventsForDate(state.timeline, it).isNotEmpty() })
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

@Composable
private fun ToggleIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
        )
    }
}

@Composable
private fun TimelineContent(
    state: TimelineUiState,
    onAction: (TimelineAction) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    var openEventId by remember { mutableStateOf<String?>(null) }
    val locale = LocalConfiguration.current.locales[0]
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.timeline.days.asReversed().forEach { day ->
            item(key = "day:${day.date}") {
                Text(
                    text = day.date.format(dayFormatter),
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(day.events.asReversed(), key = PassEvent::id) { event ->
                TimelineEventRow(
                    event = event,
                    zoneId = state.timeline.zoneId,
                    reminderEnabled = event.id in state.reminderEventIds,
                    highlighted = event.id == state.timeline.nearestEventId,
                    onAction = onAction,
                    openEventId = openEventId,
                    onOpenEvent = { openEventId = it },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimelineEventRow(
    event: PassEvent,
    zoneId: org.threeten.bp.ZoneId,
    reminderEnabled: Boolean,
    highlighted: Boolean,
    onAction: (TimelineAction) -> Unit,
    openEventId: String?,
    onOpenEvent: (String?) -> Unit,
) {
    var leftRevealWidthPx by remember { mutableIntStateOf(0) }
    var rightRevealWidthPx by remember { mutableIntStateOf(0) }
    val revealState = rememberTimelineRevealState(
        event.id,
        openEventId,
        leftRevealWidthPx,
        rightRevealWidthPx,
        onOpenEvent,
    )
    val containerColor = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(if (highlighted) 24.dp else 16.dp),
        onClick = { onAction(TimelineAction.OpenPass(event.pass.passId)) },
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.matchParentSize(), verticalAlignment = Alignment.CenterVertically) {
                TimelineOpenAction(event, onAction, Modifier.onSizeChanged { leftRevealWidthPx = it.width })
                Spacer(Modifier.weight(1f))
                TimelineSecondaryActions(
                    event,
                    reminderEnabled,
                    onAction,
                    Modifier.onSizeChanged { rightRevealWidthPx = it.width },
                )
            }
            Box(
                Modifier.offset { IntOffset(if (revealState.offset.isNaN()) 0 else revealState.offset.roundToInt(), 0) }
                    .anchoredDraggable(state = revealState, orientation = Orientation.Horizontal)
                    .background(containerColor)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                EventSummary(event, zoneId, Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimelineOpenAction(
    event: PassEvent,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    PassActionButtonGroup(modifier.fillMaxHeight()) {
        customItem(
            buttonGroupContent = {
                PassActionButton(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    label = stringResource(R.string.timeline_open_pass),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    index = 0,
                    count = 1,
                    fillHeight = true,
                    onClick = { onAction(TimelineAction.OpenPass(event.pass.passId)) },
                )
            },
            menuContent = { _ -> },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimelineSecondaryActions(
    event: PassEvent,
    reminderEnabled: Boolean,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    PassActionButtonGroup(modifier.fillMaxHeight()) {
        customItem(
            buttonGroupContent = {
                TimelineActionButton(
                    icon = Icons.Default.CalendarToday,
                    label = stringResource(R.string.pass_detail_add_to_calendar),
                    index = 0,
                    onClick = { onAction(TimelineAction.AddToCalendar(event.id)) },
                )
            },
            menuContent = { _ -> },
        )
        customItem(
            buttonGroupContent = {
                TimelineActionButton(
                    icon = Icons.Default.AddAlarm,
                    label = if (reminderEnabled) stringResource(R.string.timeline_turn_reminder_off) else stringResource(R.string.timeline_remind_me),
                    index = 1,
                    onClick = { onAction(TimelineAction.ConfigureReminder(event.id)) },
                )
            },
            menuContent = { _ -> },
        )
    }
}

@Composable
private fun TimelineActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    index: Int,
    onClick: () -> Unit,
) {
    PassActionButton(
        icon = icon,
        label = label,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        index = index,
        count = 2,
        fillHeight = true,
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberTimelineRevealState(
    eventId: String,
    openEventId: String?,
    measuredLeftRevealWidthPx: Int,
    measuredRightRevealWidthPx: Int,
    onOpenEvent: (String?) -> Unit,
): AnchoredDraggableState<TimelineReveal> {
    val density = LocalDensity.current
    val leftRevealWidth = passActionButtonGroupWidth(1)
    val rightRevealWidth = passActionButtonGroupWidth(2)
    val state = remember {
        AnchoredDraggableState(
            initialValue = TimelineReveal.Closed,
            anchors = DraggableAnchors {
                TimelineReveal.Closed at 0f
                TimelineReveal.Left at with(density) { leftRevealWidth.toPx() }
                TimelineReveal.Right at -with(density) { rightRevealWidth.toPx() }
            },
        )
    }
    SideEffect {
        val leftAnchor = measuredLeftRevealWidthPx.takeIf { it > 0 }?.toFloat()
            ?: with(density) { leftRevealWidth.toPx() }
        val rightAnchor = measuredRightRevealWidthPx.takeIf { it > 0 }?.toFloat()
            ?: with(density) { rightRevealWidth.toPx() }
        state.updateAnchors(
            DraggableAnchors {
                TimelineReveal.Closed at 0f
                TimelineReveal.Left at leftAnchor
                TimelineReveal.Right at -rightAnchor
            },
        )
    }
    LaunchedEffect(openEventId) {
        if (openEventId != null && openEventId != eventId) state.animateTo(TimelineReveal.Closed)
    }
    LaunchedEffect(state.currentValue) {
        onOpenEvent(eventId.takeUnless { state.currentValue == TimelineReveal.Closed })
    }
    return state
}

private enum class TimelineReveal { Closed, Left, Right }

internal fun timelineItemIndex(timeline: PassTimeline, eventId: String?): Int? {
    if (eventId == null) return null
    var itemIndex = 0
    timeline.days.asReversed().forEach { day ->
        itemIndex++
        val eventIndex = day.events.asReversed().indexOfFirst { it.id == eventId }
        if (eventIndex >= 0) return itemIndex + eventIndex
        itemIndex += day.events.size
    }
    return null
}

@Composable
private fun EventSummary(
    event: PassEvent,
    zoneId: org.threeten.bp.ZoneId,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm") }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = event.temporalState.label,
            color = event.temporalState.contentColor(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = event.startsAt.atZone(zoneId).format(timeFormatter),
            style = MaterialTheme.typography.bodyLarge,
        )
        event.location?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

private val EventTemporalState.label: String
    @Composable get() = when (this) {
        EventTemporalState.TODAY -> stringResource(R.string.timeline_today)
        EventTemporalState.UPCOMING -> stringResource(R.string.timeline_upcoming)
        EventTemporalState.PAST -> stringResource(R.string.timeline_past)
    }

@Composable
private fun EventTemporalState.contentColor() = when (this) {
    EventTemporalState.TODAY -> MaterialTheme.colorScheme.onPrimaryContainer
    EventTemporalState.UPCOMING -> MaterialTheme.colorScheme.primary
    EventTemporalState.PAST -> MaterialTheme.colorScheme.onSurfaceVariant
}
