package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.domain.timeline.EventTemporalState
import org.ligi.passandroid.domain.timeline.PassEvent
import org.ligi.passandroid.domain.timeline.PassTimeline
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    state: TimelineUiState,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Timeline") },
                navigationIcon = {
                    IconButton(onClick = { onAction(TimelineAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
                Text("No dated passes", style = MaterialTheme.typography.headlineSmall)
                Text("Dates from your passes will appear here.")
            }
        } else {
            TimelineContent(state, onAction, Modifier.padding(padding))
        }
    }
}

@Composable
private fun TimelineContent(
    state: TimelineUiState,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val nearestIndex = remember(state.timeline.days, state.timeline.nearestEventId) {
        state.timeline.nearestEventId?.let { nearestId ->
            var itemIndex = 0
            state.timeline.days.forEach { day ->
                itemIndex += 1
                val eventIndex = day.events.indexOfFirst { it.id == nearestId }
                if (eventIndex >= 0) return@let itemIndex + eventIndex
                itemIndex += day.events.size
            }
            null
        }
    }
    LaunchedEffect(nearestIndex) {
        nearestIndex?.let { listState.scrollToItem(it) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.timeline.days.forEach { day ->
            item(key = "day:${day.date}") {
                Text(
                    text = day.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(day.events, key = PassEvent::id) { event ->
                TimelineEventRow(
                    event = event,
                    zoneId = state.timeline.zoneId,
                    reminderEnabled = event.id in state.reminderEventIds,
                    highlighted = event.id == state.timeline.nearestEventId,
                    onAction = onAction,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineEventRow(
    event: PassEvent,
    zoneId: org.threeten.bp.ZoneId,
    reminderEnabled: Boolean,
    highlighted: Boolean,
    onAction: (TimelineAction) -> Unit,
) {
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
        BoxWithConstraints(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            val expanded = maxWidth >= 720.dp
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    EventSummary(event, zoneId, Modifier.weight(1f))
                    EventActions(event, reminderEnabled, onAction)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventSummary(event, zoneId, Modifier.fillMaxWidth())
                    EventActions(event, reminderEnabled, onAction)
                }
            }
        }
    }
}

@Composable
private fun EventSummary(
    event: PassEvent,
    zoneId: org.threeten.bp.ZoneId,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventActions(
    event: PassEvent,
    reminderEnabled: Boolean,
    onAction: (TimelineAction) -> Unit,
) {
    FlowRow(
        modifier = Modifier.widthIn(max = 480.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextButton(onClick = { onAction(TimelineAction.OpenPass(event.pass.passId)) }) { Text("Open pass") }
        TextButton(onClick = { onAction(TimelineAction.AddToCalendar(event.id)) }) { Text("Calendar") }
        TextButton(onClick = { onAction(TimelineAction.ConfigureReminder(event.id)) }) {
            Text(if (reminderEnabled) "Edit reminder" else "Remind me")
        }
    }
}

private val EventTemporalState.label: String
    get() = when (this) {
        EventTemporalState.TODAY -> "Today"
        EventTemporalState.UPCOMING -> "Upcoming"
        EventTemporalState.PAST -> "Past"
    }

@Composable
private fun EventTemporalState.contentColor() = when (this) {
    EventTemporalState.TODAY -> MaterialTheme.colorScheme.onPrimaryContainer
    EventTemporalState.UPCOMING -> MaterialTheme.colorScheme.primary
    EventTemporalState.PAST -> MaterialTheme.colorScheme.onSurfaceVariant
}
