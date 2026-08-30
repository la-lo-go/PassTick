package org.ligi.passandroid.ui.compose

import android.graphics.BitmapFactory
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PassUiModel
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

sealed interface HomeAction {
    data class OpenPass(val id: String) : HomeAction
    data class SelectCategory(val categoryId: String?) : HomeAction
    data class SetSortOrder(val order: PassSortOrder) : HomeAction
    data class Archive(val id: String) : HomeAction
    data class Restore(val id: String) : HomeAction
    data class Delete(val id: String) : HomeAction
    data class Undo(val operation: UndoOperation) : HomeAction
    data object ImportPass : HomeAction
    data object CreatePass : HomeAction
    data object OpenTimeline : HomeAction
    data object OpenSettings : HomeAction
}

sealed interface UndoOperation {
    val passId: String

    data class Archive(override val passId: String) : UndoOperation
    data class Restore(override val passId: String) : UndoOperation
    data class Delete(override val passId: String) : UndoOperation
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PassHomeScreen(
    state: MainUiState,
    onAction: (HomeAction) -> Unit,
    showTodayHero: Boolean = true,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val visiblePasses = remember(state.passes, state.categories, state.selectedCategoryId) {
        state.selectedCategoryId?.let { selected -> state.passes.filter { it.categoryId == selected } }
            ?: run {
                val hiddenCategoryIds = state.categories
                    .filter { it.role == PassCategoryRole.ARCHIVE || it.role == PassCategoryRole.TRASH }
                    .mapTo(mutableSetOf(), PassCategory::id)
                state.passes.filterNot { it.categoryId in hiddenCategoryIds }
            }
    }
    val todayPasses = if (showTodayHero) visiblePasses.filter(PassUiModel::occursToday) else emptyList()
    val remainingPasses = if (showTodayHero) visiblePasses.filterNot(PassUiModel::occursToday) else visiblePasses

    fun dispatchReversible(action: HomeAction, operation: UndoOperation, message: String) {
        onAction(action)
        scope.launch {
            if (snackbarHostState.showSnackbar(message = message, actionLabel = "Undo", withDismissAction = true) ==
                androidx.compose.material3.SnackbarResult.ActionPerformed
            ) {
                onAction(HomeAction.Undo(operation))
            }
        }
    }

    Scaffold(
        topBar = {
            HomeToolbar(
                selectedSort = state.settings.sortOrder,
                onSort = { onAction(HomeAction.SetSortOrder(it)) },
                onTimeline = { onAction(HomeAction.OpenTimeline) },
                onSettings = { onAction(HomeAction.OpenSettings) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FloatingActionButton(onClick = { onAction(HomeAction.CreatePass) }) {
                    Icon(Icons.Default.Add, "Create pass")
                }
                ExtendedFloatingActionButton(
                    onClick = { onAction(HomeAction.ImportPass) },
                    modifier = Modifier.semantics { contentDescription = "Import pass" },
                    icon = { Icon(Icons.Default.UploadFile, null) },
                    text = { Text("Import pass") },
                )
            }
        },
    ) { scaffoldPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(scaffoldPadding)) {
            val expanded = maxWidth >= 840.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (state.categories.isNotEmpty()) {
                    item(key = "categories") {
                        CategorySelector(
                            categories = state.categories,
                            selectedId = state.selectedCategoryId,
                            onSelect = { onAction(HomeAction.SelectCategory(it)) },
                        )
                    }
                }
                if (state.isBusy) {
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator(Modifier.semantics { contentDescription = "Loading passes" })
                        }
                    }
                }
                if (visiblePasses.isEmpty() && !state.isBusy) {
                    item(key = "empty") { EmptyHome() }
                }
                if (todayPasses.isNotEmpty()) {
                    item(key = "today-heading") { SectionHeading("Today", "${todayPasses.size} current") }
                    item(key = "today-feed") {
                        TicketFeed(
                            passes = todayPasses,
                            categories = state.categories,
                            columns = if (expanded) 2 else 1,
                            hero = true,
                            onOpen = { onAction(HomeAction.OpenPass(it)) },
                            onArchive = { id, restoring ->
                                if (restoring) dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id), "Pass restored")
                                else dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id), "Pass archived")
                            },
                            onDelete = { id -> dispatchReversible(HomeAction.Delete(id), UndoOperation.Delete(id), "Pass deleted") },
                        )
                    }
                }
                if (remainingPasses.isNotEmpty()) {
                    item(key = "passes-heading") { SectionHeading(if (todayPasses.isEmpty()) "Passes" else "Later", "${remainingPasses.size} passes") }
                    item(key = "pass-feed") {
                        TicketFeed(
                            passes = remainingPasses,
                            categories = state.categories,
                            columns = if (expanded) 2 else 1,
                            hero = false,
                            onOpen = { onAction(HomeAction.OpenPass(it)) },
                            onArchive = { id, restoring ->
                                if (restoring) dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id), "Pass restored")
                                else dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id), "Pass archived")
                            },
                            onDelete = { id -> dispatchReversible(HomeAction.Delete(id), UndoOperation.Delete(id), "Pass deleted") },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeToolbar(
    selectedSort: PassSortOrder,
    onSort: (PassSortOrder) -> Unit,
    onTimeline: () -> Unit,
    onSettings: () -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Box {
                TextButton(onClick = { sortMenuOpen = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, null)
                    Spacer(Modifier.size(8.dp))
                    Text(selectedSort.displayName())
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    PassSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.displayName()) },
                            trailingIcon = { if (order == selectedSort) Text("Selected", style = MaterialTheme.typography.labelSmall) },
                            onClick = {
                                sortMenuOpen = false
                                onSort(order)
                            },
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onTimeline) { Icon(Icons.Default.Timeline, "Timeline") }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
        },
    )
}

@Composable
private fun CategorySelector(categories: List<PassCategory>, selectedId: String?, onSelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(selected = selectedId == null, onClick = { onSelect(null) }, label = { Text("All") })
        }
        items(categories, key = PassCategory::id) { category ->
            FilterChip(
                selected = category.id == selectedId,
                onClick = { onSelect(category.id) },
                label = { Text(category.name) },
                leadingIcon = { CategoryDot(category.colorArgb) },
            )
        }
    }
}

@Composable
private fun TicketFeed(
    passes: List<PassUiModel>,
    categories: List<PassCategory>,
    columns: Int,
    hero: Boolean,
    onOpen: (String) -> Unit,
    onArchive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    val feeds = remember(passes, columns) {
        List(columns) { column -> passes.filterIndexed { index, _ -> index % columns == column } }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        feeds.forEach { feed ->
            if (feed.isNotEmpty()) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(if (hero) 28.dp else 20.dp),
                    color = if (hero) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column {
                        feed.forEachIndexed { index, pass ->
                            val category = categories.firstOrNull { it.id == pass.categoryId }
                            TicketSwipeContainer(
                                pass = pass,
                                category = category,
                                hero = hero,
                                modifier = Modifier.fillMaxWidth(),
                                onOpen = onOpen,
                                onArchive = onArchive,
                                onDelete = onDelete,
                            )
                            if (index < feed.lastIndex) {
                                HorizontalDivider(Modifier.padding(horizontal = if (hero) 20.dp else 16.dp))
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TicketSwipeContainer(
    pass: PassUiModel,
    category: PassCategory?,
    hero: Boolean,
    modifier: Modifier,
    onOpen: (String) -> Unit,
    onArchive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    val restoring = category?.role == PassCategoryRole.ARCHIVE
    val archiveLabel = if (restoring) "Restore" else "Archive"
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> onArchive(pass.id, restoring)
            SwipeToDismissBoxValue.EndToStart -> onDelete(pass.id)
            SwipeToDismissBoxValue.Settled -> return@LaunchedEffect
        }
        dismissState.reset()
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val deleting = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val background = if (deleting) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
            val foreground = if (deleting) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
            Row(
                Modifier.fillMaxSize().background(background).padding(horizontal = 20.dp),
                horizontalArrangement = if (deleting) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (deleting) Icons.Default.Delete else if (restoring) Icons.Default.Restore else Icons.Default.Archive, null, tint = foreground)
                Spacer(Modifier.size(8.dp))
                Text(if (deleting) "Delete" else archiveLabel, color = foreground, fontWeight = FontWeight.SemiBold)
            }
        },
        content = {
            TicketRow(
                pass = pass,
                category = category,
                hero = hero,
                archiveLabel = archiveLabel,
                modifier = Modifier.semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(archiveLabel) { onArchive(pass.id, restoring); true },
                        CustomAccessibilityAction("Delete") { onDelete(pass.id); true },
                    )
                },
                onOpen = onOpen,
                onArchive = { onArchive(pass.id, restoring) },
                onDelete = { onDelete(pass.id) },
            )
        },
    )
}

@Composable
private fun TicketRow(
    pass: PassUiModel,
    category: PassCategory?,
    hero: Boolean,
    archiveLabel: String,
    modifier: Modifier,
    onOpen: (String) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        color = if (hero) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (hero) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(ZeroCornerSize),
        modifier = modifier.fillMaxWidth().animateContentSize().clickable { onOpen(pass.id) },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(if (hero) 20.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PassThumbnail(pass, Modifier.size(if (hero) 88.dp else 64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hero) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Event, null, Modifier.size(16.dp))
                        Text("Today", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text(
                    pass.description,
                    style = if (hero) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (hero) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                pass.dateLabel()?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                pass.creator?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    category?.let { CategoryBadge(it) }
                    Text(pass.type.name.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelMedium)
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Pass actions") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(archiveLabel) },
                        leadingIcon = { Icon(if (archiveLabel == "Restore") Icons.Default.Restore else Icons.Default.Archive, null) },
                        onClick = { menuOpen = false; onArchive() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun PassThumbnail(pass: PassUiModel, modifier: Modifier) {
    val artwork = pass.artwork.firstOrNull { it.kind == PassArtworkKind.LOGO }
        ?: pass.artwork.firstOrNull { it.kind == PassArtworkKind.THUMBNAIL }
    val bitmap = remember(artwork?.bytes) {
        artwork?.bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        if (bitmap != null) {
            Image(bitmap, "Pass artwork", Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(pass.description.take(1).uppercase(Locale.ROOT), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: PassCategory) {
    Surface(color = Color(category.colorArgb.toInt()).copy(alpha = 0.18f), shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryDot(category.colorArgb)
            Spacer(Modifier.size(6.dp))
            Text(category.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CategoryDot(colorArgb: Long) {
    Box(Modifier.size(10.dp).background(Color(colorArgb.toInt()), RoundedCornerShape(50)))
}

@Composable
private fun SectionHeading(title: String, supporting: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(supporting, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyHome() {
    Column(
        Modifier.fillMaxWidth().widthIn(max = 520.dp).padding(horizontal = 24.dp, vertical = 56.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Your passes live here", style = MaterialTheme.typography.headlineSmall)
        Text("Import an existing pass or create one.", style = MaterialTheme.typography.bodyLarge)
    }
}

private fun PassUiModel.occursToday(): Boolean {
    val zone = calendarTimeSpan?.from?.zone ?: calendarTimeSpan?.to?.zone ?: return false
    val date = LocalDate.now(zone)
    val from = calendarTimeSpan?.from?.withZoneSameInstant(zone)?.toLocalDate()
    val to = calendarTimeSpan?.to?.withZoneSameInstant(zone)?.toLocalDate()
    return when {
        from != null && to != null -> !from.isAfter(date) && !to.isBefore(date)
        from != null -> from == date
        to != null -> to == date
        else -> false
    }
}

private fun PassUiModel.dateLabel(): String? {
    val value = calendarTimeSpan?.from ?: calendarTimeSpan?.to ?: return null
    return value.format(DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm", Locale.getDefault()))
}

private fun PassSortOrder.displayName() = when (this) {
    PassSortOrder.DATE_DESC -> "Newest first"
    PassSortOrder.DATE_ASC -> "Oldest first"
    PassSortOrder.DATE_DIFF -> "Nearest date"
    PassSortOrder.TYPE -> "Pass type"
}
