package org.ligi.passandroid.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.repository.isUserOrganized
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.barcode.PassCodeImage
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale
import org.ligi.passandroid.ui.state.searchDocument
import org.ligi.passandroid.ui.state.searchTerms
import org.ligi.passandroid.ui.state.AppAction

sealed interface HomeAction {
    data class OpenPass(val id: String) : HomeAction
    data class SelectCategory(val categoryId: String?) : HomeAction
    data class SetSortOrder(val order: PassSortOrder) : HomeAction
    data class ReorderPass(val orderedVisibleIds: List<String>) : HomeAction
    data class Archive(val id: String) : HomeAction
    data class Restore(val id: String) : HomeAction
    data class Delete(val id: String) : HomeAction
    data class Undo(val operation: UndoOperation) : HomeAction
    data object ImportPass : HomeAction
    data object OpenTimeline : HomeAction
    data object OpenSettings : HomeAction
    data object OpenPassViewSettings : HomeAction
    data object OpenHomeCardSettings : HomeAction
}

sealed interface UndoOperation {
    val passId: String
    val originalCategoryId: String

    data class Archive(override val passId: String, override val originalCategoryId: String) : UndoOperation
    data class Restore(override val passId: String, override val originalCategoryId: String) : UndoOperation
}

internal fun UndoOperation.toAppAction() = AppAction.MovePass(passId, originalCategoryId, announce = false)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun PassHomeScreen(
    state: MainUiState,
    onAction: (HomeAction) -> Unit,
    showTodayHero: Boolean = true,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var undoSnackbarJob by remember { mutableStateOf<Job?>(null) }
    var previewPassId by remember { mutableStateOf<String?>(null) }
    var previewOpeningPassId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchHasFocus by remember { mutableStateOf(false) }
    var searchFocusClearedByBack by remember { mutableStateOf(false) }
    var searchImeWasVisible by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val previewPass = state.passes.firstOrNull { it.id == previewPassId }
    var previewContent by remember { mutableStateOf<PassUiModel?>(null) }
    LaunchedEffect(previewPass) { if (previewPass != null) previewContent = previewPass }
    val categoryPasses = remember(state.passes, state.categories, state.selectedCategoryId) {
        state.selectedCategoryId?.let { selected -> state.passes.filter { it.categoryId == selected } }
            ?: run {
                val hiddenCategoryIds = state.categories
                    .filter { it.role == PassCategoryRole.ARCHIVE || it.role == PassCategoryRole.TRASH }
                    .mapTo(mutableSetOf(), PassCategory::id)
                state.passes.filterNot { it.categoryId in hiddenCategoryIds }
            }
    }
    val categoryNames = remember(state.categories) { state.categories.associate { it.id to it.name } }
    val searchDocuments = remember(state.passes, categoryNames) {
        state.passes.associate { pass -> pass.id to pass.searchDocument(categoryNames[pass.categoryId]) }
    }
    val searchTerms = remember(searchQuery) { searchQuery.searchTerms() }
    val visiblePasses = remember(categoryPasses, searchDocuments, searchTerms) {
        if (searchTerms.isEmpty()) categoryPasses
        else categoryPasses.filter { pass ->
            val document = searchDocuments[pass.id].orEmpty()
            searchTerms.all(document::contains)
        }
    }
    val todayPasses = if (showTodayHero) visiblePasses.filter(PassUiModel::occursToday) else emptyList()
    val remainingPasses = if (showTodayHero) visiblePasses.filterNot(PassUiModel::occursToday) else visiblePasses

    BackHandler(enabled = searchExpanded) {
        if (searchHasFocus || !searchFocusClearedByBack) {
            focusManager.clearFocus()
            searchFocusClearedByBack = true
        } else {
            searchExpanded = false
            searchQuery = ""
            searchFocusClearedByBack = false
        }
    }
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) searchFocusRequester.requestFocus()
    }
    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(searchExpanded, searchHasFocus, isImeVisible) {
        if (!searchExpanded || !searchHasFocus) {
            searchImeWasVisible = false
        } else if (isImeVisible) {
            searchImeWasVisible = true
        } else if (searchImeWasVisible) {
            focusManager.clearFocus()
            searchFocusClearedByBack = true
            searchImeWasVisible = false
        }
    }

    fun dispatchReversible(action: HomeAction, operation: UndoOperation, message: String) {
        onAction(action)
        undoSnackbarJob?.cancel()
        snackbarHostState.currentSnackbarData?.dismiss()
        undoSnackbarJob = scope.launch {
            val timeout = launch {
                delay(5_000)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            if (snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Undo",
                    withDismissAction = false,
                    duration = SnackbarDuration.Indefinite,
                ) ==
                androidx.compose.material3.SnackbarResult.ActionPerformed
            ) {
                onAction(HomeAction.Undo(operation))
            }
            timeout.cancel()
        }
    }

    val visibleCategories = remember(state.categories, state.passes) {
        state.categories.filter { category ->
            category.isUserOrganized() && state.passes.any { it.categoryId == category.id }
        }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerItem(
                    label = { Text("Timeline") },
                    selected = false,
                    icon = { Icon(Icons.Default.Timeline, null) },
                    onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.OpenTimeline) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                Text(
                    "Customize",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NavigationDrawerItem(
                    label = { Text("Pass view") },
                    selected = false,
                    icon = { Icon(Icons.Default.Visibility, null) },
                    onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.OpenPassViewSettings) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("Home cards") },
                    selected = false,
                    icon = { Icon(Icons.Default.ViewAgenda, null) },
                    onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.OpenHomeCardSettings) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    icon = { Icon(Icons.Default.Settings, null) },
                    onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.OpenSettings) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
    Scaffold(
        topBar = {
            HomeToolbar(
                categories = visibleCategories,
                selectedCategoryId = state.selectedCategoryId,
                onSelectCategory = { onAction(HomeAction.SelectCategory(it)) },
                onOpenDrawer = { scope.launch { drawerState.open() } },
                searchExpanded = searchExpanded,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onOpenSearch = { searchExpanded = true },
                searchFocusRequester = searchFocusRequester,
                onSearchFocusChanged = {
                    searchHasFocus = it
                    if (it) searchFocusClearedByBack = false
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { scaffoldPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(scaffoldPadding)) {
            val expanded = maxWidth >= 840.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (!state.isContentLoading && !searchExpanded) {
                    item(key = "sort") {
                        HomeSortSelector(
                            selectedSort = state.settings.sortOrder,
                            onSort = { onAction(HomeAction.SetSortOrder(it)) },
                        )
                    }
                }
                if (state.isContentLoading || state.isBusy) {
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator(Modifier.semantics { contentDescription = "Loading passes" })
                        }
                    }
                }
                if (visiblePasses.isEmpty() && !state.isContentLoading && !state.isBusy) {
                    item(key = "empty") {
                        if (searchTerms.isEmpty()) EmptyHome() else EmptySearch(searchQuery)
                    }
                }
                if (todayPasses.isNotEmpty()) {
                    item(key = "today-heading") { SectionHeading("Today") }
                    item(key = "today-feed") {
                        TicketFeed(
                            passes = todayPasses,
                            categories = state.categories,
                            columns = if (expanded) 2 else 1,
                            hero = true,
                            sectionOrder = state.settings.homeCardSectionOrder,
                            hiddenSections = state.settings.hiddenHomeCardSections,
                            onOpen = { onAction(HomeAction.OpenPass(it)) },
                            onArchive = { id, restoring, originalCategoryId ->
                                if (restoring) {
                                    dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id, originalCategoryId), "Pass restored")
                                } else {
                                    dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id, originalCategoryId), "Pass archived")
                                }
                            },
                            onDelete = { id, _ -> onAction(HomeAction.Delete(id)) },
                            onReorder = { onAction(HomeAction.ReorderPass(it)) },
                            onPreviewChanged = {
                                previewPassId = it
                                if (it == null) previewOpeningPassId = null
                            },
                            onPreviewOpeningChanged = { previewOpeningPassId = it },
                        )
                    }
                }
                if (remainingPasses.isNotEmpty()) {
                    if (todayPasses.isNotEmpty()) {
                        item(key = "passes-heading") { SectionHeading("Other passes") }
                    }
                    item(key = "pass-feed") {
                        TicketFeed(
                            passes = remainingPasses,
                            categories = state.categories,
                            columns = if (expanded) 2 else 1,
                            hero = false,
                            sectionOrder = state.settings.homeCardSectionOrder,
                            hiddenSections = state.settings.hiddenHomeCardSections,
                            onOpen = { onAction(HomeAction.OpenPass(it)) },
                            onArchive = { id, restoring, originalCategoryId ->
                                if (restoring) {
                                    dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id, originalCategoryId), "Pass restored")
                                } else {
                                    dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id, originalCategoryId), "Pass archived")
                                }
                            },
                            onDelete = { id, _ -> onAction(HomeAction.Delete(id)) },
                            onReorder = { onAction(HomeAction.ReorderPass(it)) },
                            onPreviewChanged = {
                                previewPassId = it
                                if (it == null) previewOpeningPassId = null
                            },
                            onPreviewOpeningChanged = { previewOpeningPassId = it },
                        )
                    }
                }
            }
            LargeFloatingActionButton(
                onClick = { onAction(HomeAction.ImportPass) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    .semantics { contentDescription = "Import passes" },
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(36.dp))
            }
            if (previewPass != null) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.48f))
                        .semantics { contentDescription = "Pass preview scrim" },
                )
            }
            AnimatedVisibility(
                visible = previewPass != null,
                modifier = if (expanded) Modifier.align(Alignment.CenterEnd) else Modifier.align(Alignment.BottomCenter),
                enter = if (expanded) slideInHorizontally { it } else slideInVertically { it },
                exit = if (expanded) slideOutHorizontally { it } else slideOutVertically { it },
            ) {
                previewContent?.let { pass ->
                    PassHoldPreview(
                        pass = pass,
                        opening = previewOpeningPassId == pass.id,
                        modifier = if (expanded) {
                            Modifier.fillMaxHeight().widthIn(max = 440.dp).padding(16.dp)
                        } else {
                            Modifier.fillMaxWidth().padding(12.dp)
                        },
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
    categories: List<PassCategory>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    onOpenDrawer: () -> Unit,
    searchExpanded: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    searchFocusRequester: FocusRequester,
    onSearchFocusChanged: (Boolean) -> Unit,
) {
    TopAppBar(
        title = {
            if (searchExpanded) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).padding(start = 16.dp), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search passes",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester)
                                    .onFocusChanged { onSearchFocusChanged(it.hasFocus) }
                                    .semantics { contentDescription = "Pass search" },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Close, "Clear search") }
                        }
                    }
                }
            } else if (categories.isNotEmpty()) {
                CategorySelector(categories, selectedCategoryId, onSelectCategory, Modifier.fillMaxWidth())
            }
        },
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "Navigation menu") }
        },
        actions = {
            if (!searchExpanded) {
                IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, "Search passes") }
            }
        },
    )
}

@Composable
private fun CategorySelector(
    categories: List<PassCategory>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun HomeSortSelector(selectedSort: PassSortOrder, onSort: (PassSortOrder) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selectedSort == PassSortOrder.MANUAL) {
            item { FilterChip(selected = true, onClick = {}, label = { Text("Manual order") }) }
        }
        item {
            val ascending = selectedSort == PassSortOrder.DATE_ASC
            val dateSelected = ascending || selectedSort == PassSortOrder.DATE_DESC
            val label = if (ascending) "Oldest first" else "Newest first"
            FilterChip(
                selected = dateSelected,
                onClick = {
                    onSort(
                        when {
                            !dateSelected -> PassSortOrder.DATE_DESC
                            ascending -> PassSortOrder.DATE_DESC
                            else -> PassSortOrder.DATE_ASC
                        },
                    )
                },
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        label,
                    )
                },
            )
        }
        item {
            FilterChip(
                selected = selectedSort == PassSortOrder.DATE_DIFF,
                onClick = { onSort(PassSortOrder.DATE_DIFF) },
                label = { Text("Nearest date") },
            )
        }
        item {
            FilterChip(
                selected = selectedSort == PassSortOrder.TYPE,
                onClick = { onSort(PassSortOrder.TYPE) },
                label = { Text("Pass type") },
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
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    onOpen: (String) -> Unit,
    onArchive: (String, Boolean, String) -> Unit,
    onDelete: (String, String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onPreviewChanged: (String?) -> Unit,
    onPreviewOpeningChanged: (String?) -> Unit,
) {
    val feeds = remember(passes, columns) {
        List(columns) { column -> passes.filterIndexed { index, _ -> index % columns == column } }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        feeds.forEach { feed ->
            if (feed.isNotEmpty()) {
                ReorderableTicketColumn(
                    passes = feed,
                    categories = categories,
                    hero = hero,
                    sectionOrder = sectionOrder,
                    hiddenSections = hiddenSections,
                    modifier = Modifier.weight(1f),
                    onOpen = onOpen,
                    onArchive = onArchive,
                    onDelete = onDelete,
                    onReorder = onReorder,
                    onPreviewChanged = onPreviewChanged,
                    onPreviewOpeningChanged = onPreviewOpeningChanged,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class FeedItemBounds(val top: Float, val height: Int) {
    val center: Float get() = top + height / 2f
}

@Composable
private fun ReorderableTicketColumn(
    passes: List<PassUiModel>,
    categories: List<PassCategory>,
    hero: Boolean,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    modifier: Modifier,
    onOpen: (String) -> Unit,
    onArchive: (String, Boolean, String) -> Unit,
    onDelete: (String, String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onPreviewChanged: (String?) -> Unit,
    onPreviewOpeningChanged: (String?) -> Unit,
) {
    var visualPasses by remember { mutableStateOf(passes) }
    var measuredBounds by remember { mutableStateOf<Map<String, FeedItemBounds>>(emptyMap()) }
    var frozenBounds by remember { mutableStateOf<Map<String, FeedItemBounds>>(emptyMap()) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var draggedFromIndex by remember { mutableStateOf(-1) }
    var draggedTargetIndex by remember { mutableStateOf(-1) }
    var draggedOffset by remember { mutableStateOf(0f) }
    var pendingCommitIds by remember { mutableStateOf<List<String>?>(null) }
    var settlingId by remember { mutableStateOf<String?>(null) }
    val settlingOffset = remember { Animatable(0f) }
    val hapticFeedback = LocalHapticFeedback.current
    val reorderScope = rememberCoroutineScope()
    val targetHysteresis = with(LocalDensity.current) { 8.dp.toPx() }

    LaunchedEffect(passes.map(PassUiModel::id), draggedId) {
        if (draggedId == null) {
            val incomingIds = passes.map(PassUiModel::id)
            if (pendingCommitIds == incomingIds) pendingCommitIds = null
            if (pendingCommitIds == null || incomingIds.toSet() != visualPasses.map(PassUiModel::id).toSet()) {
                visualPasses = passes
            }
        }
    }

    fun clearDrag() {
        draggedId = null
        draggedFromIndex = -1
        draggedTargetIndex = -1
        draggedOffset = 0f
        frozenBounds = emptyMap()
    }

    val simulatedOrder = if (
        draggedFromIndex in visualPasses.indices && draggedTargetIndex in visualPasses.indices
    ) {
        visualPasses.toMutableList().apply {
            add(draggedTargetIndex, removeAt(draggedFromIndex))
        }
    } else {
        visualPasses
    }
    val itemGap = visualPasses.zipWithNext().firstNotNullOfOrNull { (current, next) ->
        val currentBounds = frozenBounds[current.id] ?: return@firstNotNullOfOrNull null
        val nextBounds = frozenBounds[next.id] ?: return@firstNotNullOfOrNull null
        nextBounds.top - currentBounds.top - currentBounds.height
    } ?: 0f
    var nextSimulatedTop = frozenBounds[visualPasses.firstOrNull()?.id]?.top ?: 0f
    val simulatedTops = buildMap {
        simulatedOrder.forEach { item ->
            put(item.id, nextSimulatedTop)
            nextSimulatedTop += (frozenBounds[item.id]?.height ?: 0) + itemGap
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        visualPasses.forEachIndexed { index, pass ->
            key(pass.id) {
                val category = categories.firstOrNull { it.id == pass.categoryId }
                val radius = if (hero) 28.dp else 20.dp
                val shape = RoundedCornerShape(
                    topStart = if (index == 0) radius else 0.dp,
                    topEnd = if (index == 0) radius else 0.dp,
                    bottomStart = if (index == visualPasses.lastIndex) radius else 0.dp,
                    bottomEnd = if (index == visualPasses.lastIndex) radius else 0.dp,
                )
                val siblingTargetOffset = if (pass.id != draggedId) {
                    val original = frozenBounds[pass.id]?.top
                    val target = simulatedTops[pass.id]
                    if (original != null && target != null) target - original else 0f
                } else {
                    0f
                }
                val siblingOffset by animateFloatAsState(
                    targetValue = siblingTargetOffset,
                    animationSpec = tween(120),
                    label = "reorderSiblingOffset",
                )
                val isDragged = pass.id == draggedId
                val isSettling = pass.id == settlingId
                val visualOffset = when {
                    isDragged -> draggedOffset
                    isSettling -> settlingOffset.value
                    else -> siblingOffset
                }
                val isLifted = isDragged || isSettling

                TicketSwipeContainer(
                    pass = pass,
                    category = category,
                    hero = hero,
                    sectionOrder = sectionOrder,
                    hiddenSections = hiddenSections,
                    modifier = Modifier.fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            if (draggedId == null) {
                                measuredBounds = measuredBounds + (
                                    pass.id to FeedItemBounds(coordinates.positionInParent().y, coordinates.size.height)
                                )
                            }
                        }
                        .zIndex(if (isLifted) 2f else 0f)
                        .graphicsLayer {
                            translationY = visualOffset
                            scaleX = if (isLifted) 1.025f else 1f
                            scaleY = if (isLifted) 1.025f else 1f
                            shadowElevation = if (isLifted) 12.dp.toPx() else 0f
                        },
                    shape = shape,
                    reorderingActive = draggedId != null,
                    onOpen = onOpen,
                    onArchive = onArchive,
                    onDelete = onDelete,
                    onReorderStart = {
                        draggedId = pass.id
                        draggedFromIndex = index
                        draggedTargetIndex = index
                        draggedOffset = 0f
                        frozenBounds = measuredBounds
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onReorderDrag = { delta ->
                        draggedOffset += delta
                        val origin = frozenBounds[pass.id]
                        if (origin != null) {
                            val draggedCenter = origin.center + draggedOffset
                            var nextTarget = draggedTargetIndex
                            while (nextTarget < visualPasses.lastIndex) {
                                val currentCenter = frozenBounds[visualPasses[nextTarget].id]?.center ?: break
                                val followingCenter = frozenBounds[visualPasses[nextTarget + 1].id]?.center ?: break
                                if (draggedCenter > (currentCenter + followingCenter) / 2f + targetHysteresis) {
                                    nextTarget++
                                } else {
                                    break
                                }
                            }
                            while (nextTarget > 0) {
                                val currentCenter = frozenBounds[visualPasses[nextTarget].id]?.center ?: break
                                val precedingCenter = frozenBounds[visualPasses[nextTarget - 1].id]?.center ?: break
                                if (draggedCenter < (currentCenter + precedingCenter) / 2f - targetHysteresis) {
                                    nextTarget--
                                } else {
                                    break
                                }
                            }
                            if (nextTarget != draggedTargetIndex) {
                                draggedTargetIndex = nextTarget
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    onReorderEnd = {
                        val from = draggedFromIndex
                        val to = draggedTargetIndex
                        val id = draggedId
                        if (id != null && from in visualPasses.indices && to in visualPasses.indices && from != to) {
                            val reorderedPasses = visualPasses.toMutableList().apply { add(to, removeAt(from)) }
                            val reorderedIds = reorderedPasses.map(PassUiModel::id)
                            val originTop = frozenBounds[id]?.top ?: 0f
                            val targetTop = simulatedTops[id] ?: originTop
                            val releaseOffset = originTop + draggedOffset - targetTop
                            reorderScope.launch {
                                settlingOffset.snapTo(releaseOffset)
                                settlingId = id
                                pendingCommitIds = reorderedIds
                                visualPasses = reorderedPasses
                                clearDrag()
                                onReorder(reorderedIds)
                                settlingOffset.animateTo(0f, tween(180))
                                settlingId = null
                            }
                        } else {
                            clearDrag()
                        }
                    },
                    onReorderCancel = ::clearDrag,
                    onPreviewChanged = onPreviewChanged,
                    onPreviewOpeningChanged = onPreviewOpeningChanged,
                )
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
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    modifier: Modifier,
    shape: Shape,
    reorderingActive: Boolean,
    onOpen: (String) -> Unit,
    onArchive: (String, Boolean, String) -> Unit,
    onDelete: (String, String) -> Unit,
    onReorderStart: () -> Unit,
    onReorderDrag: (Float) -> Unit,
    onReorderEnd: () -> Unit,
    onReorderCancel: () -> Unit,
    onPreviewChanged: (String?) -> Unit,
    onPreviewOpeningChanged: (String?) -> Unit,
) {
    val restoring = category?.role == PassCategoryRole.ARCHIVE
    val archiveLabel = if (restoring) "Restore" else "Archive"
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(reorderingActive) {
        if (reorderingActive) dismissState.reset()
    }
    LaunchedEffect(dismissState.settledValue) {
        when (dismissState.settledValue) {
            SwipeToDismissBoxValue.StartToEnd -> onArchive(pass.id, restoring, pass.categoryId)
            SwipeToDismissBoxValue.EndToStart -> onDelete(pass.id, pass.categoryId)
            SwipeToDismissBoxValue.Settled -> return@LaunchedEffect
        }
        dismissState.reset()
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(shape),
        gesturesEnabled = !reorderingActive,
        backgroundContent = {
            if (reorderingActive) {
                Box(
                    Modifier.fillMaxSize().background(
                        if (hero) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            } else {
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
            }
        },
        content = {
            TicketRow(
                pass = pass,
                category = category,
                hero = hero,
                sectionOrder = sectionOrder,
                hiddenSections = hiddenSections,
                modifier = Modifier.semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(archiveLabel) { onArchive(pass.id, restoring, pass.categoryId); true },
                        CustomAccessibilityAction("Delete") { onDelete(pass.id, pass.categoryId); true },
                    )
                },
                shape = shape,
                onOpen = onOpen,
                onReorderStart = onReorderStart,
                onReorderDrag = onReorderDrag,
                onReorderEnd = onReorderEnd,
                onReorderCancel = onReorderCancel,
                onPreviewChanged = { visible -> onPreviewChanged(pass.id.takeIf { visible }) },
                onPreviewOpeningChanged = { opening -> onPreviewOpeningChanged(pass.id.takeIf { opening }) },
            )
        },
    )
}

@Composable
private fun TicketRow(
    pass: PassUiModel,
    category: PassCategory?,
    hero: Boolean,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    modifier: Modifier,
    shape: Shape,
    onOpen: (String) -> Unit,
    onReorderStart: () -> Unit,
    onReorderDrag: (Float) -> Unit,
    onReorderEnd: () -> Unit,
    onReorderCancel: () -> Unit,
    onPreviewChanged: (Boolean) -> Unit,
    onPreviewOpeningChanged: (Boolean) -> Unit,
) {
    val interactionSource = remember(pass.id) { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    Surface(
        color = if (hero) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (hero) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = shape,
        modifier = modifier.fillMaxWidth().animateContentSize()
            .indication(interactionSource, LocalIndication.current),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(if (hero) 20.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        onClick(label = "Open ${pass.description}") {
                            onOpen(pass.id)
                            true
                        }
                    }
                    .pointerInput(pass.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val press = PressInteraction.Press(down.position)
                            interactionSource.tryEmit(press)
                            var releasedBeforeLongPress = false
                            var movedBeforeLongPress = false
                            val held = withTimeoutOrNull(750) {
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        releasedBeforeLongPress = true
                                        return@withTimeoutOrNull false
                                    }
                                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                        movedBeforeLongPress = true
                                        return@withTimeoutOrNull false
                                    }
                                }
                                @Suppress("UNREACHABLE_CODE") false
                            } ?: true
                            if (held) {
                                onPreviewChanged(true)
                                var opening = false
                                var releasedAfterPreview = false
                                try {
                                    while (true) {
                                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                        val aboveOpenThreshold = change != null && down.position.y - change.position.y > 72.dp.toPx()
                                        if (!opening && aboveOpenThreshold) {
                                            opening = true
                                            interactionSource.tryEmit(PressInteraction.Cancel(press))
                                            onPreviewOpeningChanged(true)
                                        } else if (opening && !aboveOpenThreshold) {
                                            opening = false
                                            onPreviewOpeningChanged(false)
                                        }
                                        change?.consume()
                                        if (change == null || !change.pressed) {
                                            releasedAfterPreview = change != null
                                            break
                                        }
                                    }
                                } finally {
                                    if (opening && releasedAfterPreview) {
                                        scope.launch {
                                            delay(180)
                                            onPreviewChanged(false)
                                            onOpen(pass.id)
                                        }
                                    } else {
                                        interactionSource.tryEmit(PressInteraction.Release(press))
                                        onPreviewChanged(false)
                                    }
                                }
                            } else if (releasedBeforeLongPress && !movedBeforeLongPress) {
                                interactionSource.tryEmit(PressInteraction.Release(press))
                                onOpen(pass.id)
                            } else {
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                            }
                        }
                },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
            if (HomeCardSection.ARTWORK !in hiddenSections) {
                PassThumbnail(pass, Modifier.size(if (hero) 44.dp else 32.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                var metadataShown = false
                sectionOrder.filterNot { it in hiddenSections || it == HomeCardSection.ARTWORK }.forEach { section ->
                    when (section) {
                        HomeCardSection.ARTWORK -> Unit
                        HomeCardSection.TITLE -> {
                            Text(
                                pass.description,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        HomeCardSection.PRIMARY_FIELD -> pass.homeCardDetail()?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        HomeCardSection.DATE -> pass.dateLabel(compactForToday = hero)?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        HomeCardSection.CREATOR -> pass.creator?.takeIf(String::isNotBlank)?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        HomeCardSection.CATEGORY,
                        HomeCardSection.PASS_TYPE,
                        -> if (!metadataShown) {
                            val showType = HomeCardSection.PASS_TYPE !in hiddenSections
                            val visibleTag = category
                                ?.takeIf(PassCategory::isUserOrganized)
                                ?.takeIf { HomeCardSection.CATEGORY !in hiddenSections }
                            if (showType || visibleTag != null) {
                                metadataShown = true
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (showType) {
                                        Text(
                                            pass.type.name.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase),
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                    visibleTag?.let { CategoryBadge(it) }
                                }
                            }
                        }
                    }
                }
            }
            }
            Icon(
                Icons.Default.DragHandle,
                "Reorder ${pass.description}",
                Modifier.size(40.dp).pointerInput(pass.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            onReorderStart()
                        },
                        onDragEnd = onReorderEnd,
                        onDragCancel = onReorderCancel,
                        onDrag = { change, amount ->
                            change.consume()
                            onReorderDrag(amount.y)
                        },
                    )
                }.padding(8.dp),
            )
        }
    }
}

@Composable
private fun PassHoldPreview(pass: PassUiModel, opening: Boolean, modifier: Modifier) {
    val openingProgress by animateFloatAsState(
        targetValue = if (opening) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "passPreviewOpening",
    )
    Surface(
        modifier = modifier.graphicsLayer {
            translationY = -96.dp.toPx() * openingProgress
            scaleX = 1f + (0.04f * openingProgress)
            scaleY = 1f + (0.04f * openingProgress)
        },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(pass.description, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            pass.creator?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (pass.barcodeFormat != null && !pass.barcodeMessage.isNullOrBlank()) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                    PassCodeImage(
                        format = pass.barcodeFormat,
                        message = pass.barcodeMessage,
                        modifier = Modifier.fillMaxWidth().height(if (pass.barcodeFormat.isQuadratic()) 280.dp else 160.dp)
                            .padding(12.dp),
                        contentDescription = "Preview pass code",
                    )
                }
            }
            pass.dateLabel()?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            pass.fields.filterNot { it.hidden }.take(4).forEach { field ->
                Text("${field.label}: ${field.value}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PassThumbnail(pass: PassUiModel, modifier: Modifier) {
    val artwork = pass.artwork.firstOrNull { it.kind == PassArtworkKind.LOGO }
        ?: pass.artwork.firstOrNull { it.kind == PassArtworkKind.THUMBNAIL }
    if (artwork != null) {
        AdaptivePassArtwork(
            bytes = artwork.bytes,
            accentColor = pass.accentColor,
            contentDescription = "Pass artwork",
            modifier = modifier,
            contentPadding = 2.dp,
            cropNearlySquare = true,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(16),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = pass.homeCardInitial(),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: PassCategory) {
    Surface(color = Color(category.colorArgb.toInt()).copy(alpha = 0.18f), shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun SectionHeading(title: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
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
        Text("Import one or more pass files.", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptySearch(query: String) {
    Column(
        Modifier.fillMaxWidth().widthIn(max = 520.dp).padding(horizontal = 24.dp, vertical = 56.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No matching passes", style = MaterialTheme.typography.headlineSmall)
        Text("Try fewer or different words for “${query.trim()}”.", style = MaterialTheme.typography.bodyLarge)
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

private fun PassUiModel.dateLabel(compactForToday: Boolean = false): String? {
    val value = calendarTimeSpan?.from ?: calendarTimeSpan?.to ?: return null
    if (compactForToday && occursToday()) {
        return value.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    }
    return value.format(DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm", Locale.getDefault()))
}
