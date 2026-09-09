package org.ligi.passandroid.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
import org.ligi.passandroid.ui.state.PROTECTED_PASSES_CATEGORY_ID
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.displayArtwork
import org.ligi.passandroid.ui.barcode.PassCodeImage
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale
import org.ligi.passandroid.ui.state.searchDocument
import org.ligi.passandroid.ui.state.searchTerms
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.theme.PassActionButtonGroup
import org.ligi.passandroid.ui.theme.PassActionButton
import org.ligi.passandroid.ui.theme.PassActionButtonGap
import org.ligi.passandroid.ui.theme.passActionButtonGroupWidth

sealed interface HomeAction {
    data class OpenPass(val id: String) : HomeAction
    data class SelectCategory(val categoryId: String?) : HomeAction
    data class SetSortOrder(val order: PassSortOrder) : HomeAction
    data class ReorderPass(val orderedVisibleIds: List<String>) : HomeAction
    data class Archive(val id: String) : HomeAction
    data class Restore(val id: String) : HomeAction
    data class Delete(val id: String) : HomeAction
    data class ToggleFavorite(val id: String) : HomeAction
    data class ToggleProtected(val id: String) : HomeAction
    data class Undo(val operation: UndoOperation) : HomeAction
    data object ImportPass : HomeAction
    data object OpenTimeline : HomeAction
    data object OpenSettings : HomeAction
    data object OpenPassViewSettings : HomeAction
    data object OpenHomeCardSettings : HomeAction
    data object UnlockProtectedPasses : HomeAction
}

sealed interface UndoOperation {
    val passId: String
    val originalCategoryId: String

    data class Archive(override val passId: String, override val originalCategoryId: String) : UndoOperation
    data class Restore(override val passId: String, override val originalCategoryId: String) : UndoOperation
}

internal fun UndoOperation.toAppAction() = when (this) {
    is UndoOperation.Archive -> AppAction.SetPassArchived(passId, false)
    is UndoOperation.Restore -> AppAction.SetPassArchived(passId, true)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun PassHomeScreen(
    state: MainUiState,
    onAction: (HomeAction) -> Unit,
    showTodayHero: Boolean = true,
    protectedPassesUnlocked: Boolean = false,
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
    val openSwipePassId = remember { mutableStateOf<String?>(null) }
    var todayExpanded by rememberSaveable(state.selectedCategoryId) { mutableStateOf(true) }
    var pinnedExpanded by rememberSaveable(state.selectedCategoryId) { mutableStateOf(true) }
    var otherExpanded by rememberSaveable(state.selectedCategoryId) { mutableStateOf(true) }
    var protectedExpanded by rememberSaveable(state.selectedCategoryId) { mutableStateOf(true) }
    val protectedPassIds = remember(state.passes, state.settings.lockAllPasses) {
        state.passes.filter { state.settings.lockAllPasses || it.isProtected }.mapTo(mutableSetOf(), PassUiModel::id)
    }
    val previewPass = state.passes.firstOrNull { it.id == previewPassId }?.let { pass ->
        pass.copy(isProtected = pass.id in protectedPassIds && !protectedPassesUnlocked)
    }
    var previewContent by remember { mutableStateOf<PassUiModel?>(null) }
    LaunchedEffect(previewPass) { if (previewPass != null) previewContent = previewPass }
    val categoryPasses = remember(
        state.passes,
        state.categories,
        state.selectedCategoryId,
        protectedPassIds,
        protectedPassesUnlocked,
    ) {
        val selectedPasses = when (state.selectedCategoryId) {
            PROTECTED_PASSES_CATEGORY_ID -> state.passes.filter { it.id in protectedPassIds }
            "pinned" -> state.passes.filter(PassUiModel::isFavorite)
            "archived" -> state.passes.filter(PassUiModel::isArchived)
            null -> run {
                val hiddenCategoryIds = state.categories
                    .filter { it.role == PassCategoryRole.ARCHIVE || it.role == PassCategoryRole.TRASH }
                    .mapTo(mutableSetOf(), PassCategory::id)
                state.passes.filterNot { it.categoryId in hiddenCategoryIds || it.isArchived }
            }
            else -> state.passes.filter {
                it.categoryId == state.selectedCategoryId || state.selectedCategoryId in it.tagIds
            }
        }
        selectedPasses.map { pass ->
            pass.copy(isProtected = pass.id in protectedPassIds && !protectedPassesUnlocked)
        }
    }
    val categoryNames = remember(state.categories) { state.categories.associate { it.id to it.name } }
    val searchDocuments = remember(state.passes, categoryNames, protectedPassIds, protectedPassesUnlocked) {
        state.passes.filter { protectedPassesUnlocked || it.id !in protectedPassIds }
            .associate { pass ->
                val tags = pass.tagIds.mapNotNull(categoryNames::get).joinToString(" ")
                pass.id to "${pass.searchDocument(categoryNames[pass.categoryId])} $tags".trim()
            }
    }
    val searchTerms = remember(searchQuery) { searchQuery.searchTerms() }
    val visiblePasses = remember(
        categoryPasses,
        searchDocuments,
        searchTerms,
        searchExpanded,
        protectedPassIds,
        protectedPassesUnlocked,
        state.settings.separateProtectedPasses,
        state.selectedCategoryId,
    ) {
        if (!searchExpanded) {
            if (state.selectedCategoryId == PROTECTED_PASSES_CATEGORY_ID && !protectedPassesUnlocked) {
                emptyList()
            } else if (
                state.settings.separateProtectedPasses && state.selectedCategoryId != PROTECTED_PASSES_CATEGORY_ID
            ) {
                categoryPasses.filterNot { it.id in protectedPassIds }
            } else categoryPasses
        } else categoryPasses.filter { protectedPassesUnlocked || it.id !in protectedPassIds }.filter { pass ->
            val document = searchDocuments[pass.id].orEmpty()
            searchTerms.all(document::contains)
        }
    }
    val separatedProtectedPasses = if (
        state.settings.separateProtectedPasses && protectedPassesUnlocked && !searchExpanded &&
        state.selectedCategoryId != PROTECTED_PASSES_CATEGORY_ID
    ) {
        categoryPasses.filter { it.id in protectedPassIds }
    } else {
        emptyList()
    }
    val showLockedSection = state.settings.separateProtectedPasses &&
        !protectedPassesUnlocked && protectedPassIds.isNotEmpty() && !searchExpanded
    val homeSections = deriveHomePassSections(
        passes = visiblePasses,
        highlightTodayPasses = showTodayHero,
        isToday = PassUiModel::occursToday,
        isPinned = PassUiModel::isFavorite,
    )
    val todayPasses = homeSections.today
    val pinnedPasses = homeSections.pinned
    val remainingPasses = homeSections.other

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
            category.role == PassCategoryRole.CUSTOM && state.passes.any { category.id in it.tagIds }
        }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = openSwipePassId.value == null && previewPass == null,
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
                NavigationDrawerItem(
                    label = { Text("All") },
                    selected = state.selectedCategoryId == null,
                    icon = { Icon(Icons.Default.ViewAgenda, null) },
                    onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.SelectCategory(null)) },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_filter_all"),
                )
                NavigationDrawerItem(
                    label = { Text("Protected") },
                    selected = state.selectedCategoryId == PROTECTED_PASSES_CATEGORY_ID,
                    icon = { Icon(Icons.Default.Lock, null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onAction(
                            if (protectedPassesUnlocked) HomeAction.SelectCategory(PROTECTED_PASSES_CATEGORY_ID)
                            else HomeAction.UnlockProtectedPasses,
                        )
                    },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_filter_protected"),
                )
                NavigationDrawerItem(
                    label = { Text("Pinned") },
                    selected = state.selectedCategoryId == "pinned",
                    icon = { Icon(Icons.Default.PushPin, null) },
                    onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.SelectCategory("pinned")) },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_filter_pinned"),
                )
                NavigationDrawerItem(
                    label = { Text("Archived") },
                    selected = state.selectedCategoryId == "archived",
                    icon = { Icon(Icons.Default.Archive, null) },
                    onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.SelectCategory("archived")) },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_filter_archived"),
                )
                if (visibleCategories.isNotEmpty()) {
                    Text(
                        "Tags",
                        modifier = Modifier.padding(start = 28.dp, top = 16.dp, end = 28.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                visibleCategories.forEach { category ->
                    NavigationDrawerItem(
                        label = { Text(category.name) },
                        selected = state.selectedCategoryId == category.id,
                        icon = { Icon(categoryIcon(category.icon), category.icon) },
                        onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.SelectCategory(category.id)) },
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_filter_${category.id}"),
                    )
                }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
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
            Box {
            HomeToolbar(
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
            if (previewPass != null) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.48f)))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { scaffoldPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(scaffoldPadding)) {
            val expanded = maxWidth >= 840.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                if (visiblePasses.isEmpty() && separatedProtectedPasses.isEmpty() && !showLockedSection &&
                    !state.isContentLoading && !state.isBusy
                ) {
                    item(key = "empty") {
                        if (searchTerms.isEmpty()) EmptyHome() else EmptySearch(searchQuery)
                    }
                }
                if (showLockedSection) {
                    item(key = "locked-passes") {
                        LockedPassSection(protectedPassIds.size) { onAction(HomeAction.UnlockProtectedPasses) }
                    }
                }
                if (todayPasses.isNotEmpty()) {
                    item(key = "today-heading") { SectionHeading("Today", todayExpanded) { todayExpanded = !todayExpanded } }
                    if (todayExpanded) item(key = "today-feed") {
                        TicketFeed(
                            passes = todayPasses,
                            categories = state.categories,
                            columns = if (expanded) 2 else 1,
                            hero = true,
                            sectionOrder = state.settings.homeCardSectionOrder,
                            hiddenSections = state.settings.hiddenHomeCardSections,
                            showProtectedPassLockIcon = state.settings.showProtectedPassLockIcon,
                            blurProtectedPassCards = state.settings.blurProtectedPassCards,
                            onOpen = { onAction(HomeAction.OpenPass(it)) },
                            onArchive = { id, restoring, originalCategoryId ->
                                if (restoring) dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id, originalCategoryId), "Pass restored")
                                else dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id, originalCategoryId), "Pass archived")
                            },
                            onDelete = { id, _ -> onAction(HomeAction.Delete(id)) },
                            onToggleFavorite = { onAction(HomeAction.ToggleFavorite(it)) },
                            onToggleProtected = { onAction(HomeAction.ToggleProtected(it)) },
                            onReorder = { onAction(HomeAction.ReorderPass(it)) },
                            onPreviewChanged = { previewPassId = it },
                            onPreviewOpeningChanged = { previewOpeningPassId = it },
                            onProtectedPreviewRequested = {},
                            openSwipePassId = openSwipePassId,
                            tagCategories = state.categories,
                        )
                    }
                }
                if (pinnedPasses.isNotEmpty()) {
                    item(key = "pinned-heading") { SectionHeading("Pinned", pinnedExpanded) { pinnedExpanded = !pinnedExpanded } }
                    if (pinnedExpanded) item(key = "pinned-feed") {
                        TicketFeed(
                            passes = pinnedPasses,
                            categories = state.categories,
                            columns = if (expanded) 2 else 1,
                            hero = false,
                            sectionOrder = state.settings.homeCardSectionOrder,
                            hiddenSections = state.settings.hiddenHomeCardSections,
                            showProtectedPassLockIcon = state.settings.showProtectedPassLockIcon,
                            blurProtectedPassCards = state.settings.blurProtectedPassCards,
                            onOpen = { onAction(HomeAction.OpenPass(it)) },
                            onArchive = { id, restoring, originalCategoryId ->
                                if (restoring) {
                                    dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id, originalCategoryId), "Pass restored")
                                } else {
                                    dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id, originalCategoryId), "Pass archived")
                                }
                            },
                            onDelete = { id, _ -> onAction(HomeAction.Delete(id)) },
                            onToggleFavorite = { onAction(HomeAction.ToggleFavorite(it)) },
                            onToggleProtected = { onAction(HomeAction.ToggleProtected(it)) },
                            onReorder = { onAction(HomeAction.ReorderPass(it)) },
                            onPreviewChanged = {
                                previewPassId = it
                                if (it == null) previewOpeningPassId = null
                            },
                            onPreviewOpeningChanged = { previewOpeningPassId = it },
                            onProtectedPreviewRequested = {
                                scope.launch { snackbarHostState.showSnackbar("Unlock the pass to preview it") }
                            },
                            openSwipePassId = openSwipePassId,
                            tagCategories = state.categories,
                        )
                    }
                }
                if (remainingPasses.isNotEmpty()) {
                    item(key = "passes-heading") { SectionHeading("Other passes", otherExpanded) { otherExpanded = !otherExpanded } }
                    if (otherExpanded) item(key = "pass-feed") {
                        TicketFeed(
                            passes = remainingPasses,
                            categories = state.categories,
                            columns = if (expanded) 2 else 1,
                            hero = false,
                            sectionOrder = state.settings.homeCardSectionOrder,
                            hiddenSections = state.settings.hiddenHomeCardSections,
                            showProtectedPassLockIcon = state.settings.showProtectedPassLockIcon,
                            blurProtectedPassCards = state.settings.blurProtectedPassCards,
                            onOpen = { onAction(HomeAction.OpenPass(it)) },
                            onArchive = { id, restoring, originalCategoryId ->
                                if (restoring) {
                                    dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id, originalCategoryId), "Pass restored")
                                } else {
                                    dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id, originalCategoryId), "Pass archived")
                                }
                            },
                            onDelete = { id, _ -> onAction(HomeAction.Delete(id)) },
                            onToggleFavorite = { onAction(HomeAction.ToggleFavorite(it)) },
                            onToggleProtected = { onAction(HomeAction.ToggleProtected(it)) },
                            onReorder = { onAction(HomeAction.ReorderPass(it)) },
                            onPreviewChanged = {
                                previewPassId = it
                                if (it == null) previewOpeningPassId = null
                            },
                            onPreviewOpeningChanged = { previewOpeningPassId = it },
                            onProtectedPreviewRequested = {
                                scope.launch { snackbarHostState.showSnackbar("Unlock the pass to preview it") }
                            },
                            openSwipePassId = openSwipePassId,
                            tagCategories = state.categories,
                        )
                    }
                }
                if (separatedProtectedPasses.isNotEmpty()) {
                    item(key = "protected-heading") {
                        SectionHeading("Protected passes", protectedExpanded) { protectedExpanded = !protectedExpanded }
                    }
                    if (protectedExpanded) item(key = "protected-feed") {
                        TicketFeed(
                            passes = separatedProtectedPasses,
                            categories = state.categories,
                            columns = if (expanded) 2 else 1,
                            hero = false,
                            sectionOrder = state.settings.homeCardSectionOrder,
                            hiddenSections = state.settings.hiddenHomeCardSections,
                            showProtectedPassLockIcon = state.settings.showProtectedPassLockIcon,
                            blurProtectedPassCards = false,
                            onOpen = { onAction(HomeAction.OpenPass(it)) },
                            onArchive = { id, restoring, originalCategoryId ->
                                if (restoring) {
                                    dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id, originalCategoryId), "Pass restored")
                                } else {
                                    dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id, originalCategoryId), "Pass archived")
                                }
                            },
                            onDelete = { id, _ -> onAction(HomeAction.Delete(id)) },
                            onToggleFavorite = { onAction(HomeAction.ToggleFavorite(it)) },
                            onToggleProtected = { onAction(HomeAction.ToggleProtected(it)) },
                            onReorder = { onAction(HomeAction.ReorderPass(it)) },
                            onPreviewChanged = {
                                previewPassId = it
                                if (it == null) previewOpeningPassId = null
                            },
                            onPreviewOpeningChanged = { previewOpeningPassId = it },
                            onProtectedPreviewRequested = {},
                            openSwipePassId = openSwipePassId,
                            tagCategories = state.categories,
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun HomeSortSelector(selectedSort: PassSortOrder, onSort: (PassSortOrder) -> Unit) {
    AnimatedContent(
        targetState = selectedSort == PassSortOrder.MANUAL,
        modifier = Modifier.fillMaxWidth(),
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
        label = "Manual sort option",
    ) { showManual ->
        val options = homeSortOptions(selectedSort, showManual)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            options.forEachIndexed { index, option ->
                ToggleButton(
                    checked = option.matches(selectedSort),
                    onCheckedChange = { onSort(option.nextOrder(selectedSort)) },
                    modifier = Modifier.weight(1f).height(40.dp).semantics { role = Role.RadioButton },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    colors = ToggleButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text(option.label, maxLines = 1)
                }
            }
        }
    }
}

private data class HomeSortOption(val order: PassSortOrder, val label: String) {
    fun matches(selectedSort: PassSortOrder) = if (order == PassSortOrder.DATE_DESC) {
        selectedSort == PassSortOrder.DATE_DESC || selectedSort == PassSortOrder.DATE_ASC
    } else {
        selectedSort == order
    }

    fun nextOrder(selectedSort: PassSortOrder) = if (order == PassSortOrder.DATE_DESC && selectedSort == PassSortOrder.DATE_DESC) {
        PassSortOrder.DATE_ASC
    } else {
        order
    }
}

private fun homeSortOptions(selectedSort: PassSortOrder, showManual: Boolean) = buildList {
    if (showManual) add(HomeSortOption(PassSortOrder.MANUAL, "Manual"))
    add(HomeSortOption(PassSortOrder.DATE_DESC, if (selectedSort == PassSortOrder.DATE_ASC) "Oldest" else "Newest"))
    add(HomeSortOption(PassSortOrder.DATE_DIFF, "Nearest"))
    add(HomeSortOption(PassSortOrder.TYPE, "Type"))
}

@Composable
private fun TicketFeed(
    passes: List<PassUiModel>,
    categories: List<PassCategory>,
    columns: Int,
    hero: Boolean,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    showProtectedPassLockIcon: Boolean,
    blurProtectedPassCards: Boolean,
    onOpen: (String) -> Unit,
    onArchive: (String, Boolean, String) -> Unit,
    onDelete: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleProtected: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onPreviewChanged: (String?) -> Unit,
    onPreviewOpeningChanged: (String?) -> Unit,
    onProtectedPreviewRequested: () -> Unit,
    openSwipePassId: androidx.compose.runtime.MutableState<String?>,
    tagCategories: List<PassCategory>,
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
                    showProtectedPassLockIcon = showProtectedPassLockIcon,
                    blurProtectedPassCards = blurProtectedPassCards,
                    modifier = Modifier.weight(1f),
                    onOpen = onOpen,
                    onArchive = onArchive,
                    onDelete = onDelete,
                    onToggleFavorite = onToggleFavorite,
                    onToggleProtected = onToggleProtected,
                    onReorder = onReorder,
                    onPreviewChanged = onPreviewChanged,
                    onPreviewOpeningChanged = onPreviewOpeningChanged,
                    onProtectedPreviewRequested = onProtectedPreviewRequested,
                    openSwipePassId = openSwipePassId,
                    tagCategories = tagCategories,
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
    showProtectedPassLockIcon: Boolean,
    blurProtectedPassCards: Boolean,
    modifier: Modifier,
    onOpen: (String) -> Unit,
    onArchive: (String, Boolean, String) -> Unit,
    onDelete: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleProtected: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onPreviewChanged: (String?) -> Unit,
    onPreviewOpeningChanged: (String?) -> Unit,
    onProtectedPreviewRequested: () -> Unit,
    openSwipePassId: androidx.compose.runtime.MutableState<String?>,
    tagCategories: List<PassCategory>,
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

    LaunchedEffect(passes, draggedId, pendingCommitIds) {
        if (draggedId == null) {
            val incomingIds = passes.map(PassUiModel::id)
            val latestById = passes.associateBy(PassUiModel::id)
            if (pendingCommitIds == incomingIds) {
                pendingCommitIds = null
                visualPasses = passes
            } else if (pendingCommitIds != null && pendingCommitIds!!.toSet() == incomingIds.toSet()) {
                // Keep a locally committed reorder until the repository emits that order.
                visualPasses = pendingCommitIds!!.mapNotNull(latestById::get)
            } else {
                // A new sort or metadata update owns the incoming order.
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
                    showProtectedPassLockIcon = showProtectedPassLockIcon,
                    blurProtectedPassCards = blurProtectedPassCards,
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
                    onToggleFavorite = onToggleFavorite,
                    onToggleProtected = onToggleProtected,
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
                    onProtectedPreviewRequested = onProtectedPreviewRequested,
                    openSwipePassId = openSwipePassId,
                    tagCategories = tagCategories,
                )
            }
        }
    }
}

private enum class SwipeRevealAnchor { Closed, StartActions, StartCommit, EndActions }

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TicketSwipeContainer(
    pass: PassUiModel,
    category: PassCategory?,
    hero: Boolean,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    showProtectedPassLockIcon: Boolean,
    blurProtectedPassCards: Boolean,
    modifier: Modifier,
    shape: Shape,
    reorderingActive: Boolean,
    onOpen: (String) -> Unit,
    onArchive: (String, Boolean, String) -> Unit,
    onDelete: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleProtected: (String) -> Unit,
    onReorderStart: () -> Unit,
    onReorderDrag: (Float) -> Unit,
    onReorderEnd: () -> Unit,
    onReorderCancel: () -> Unit,
    onPreviewChanged: (String?) -> Unit,
    onPreviewOpeningChanged: (String?) -> Unit,
    onProtectedPreviewRequested: () -> Unit,
    openSwipePassId: androidx.compose.runtime.MutableState<String?>,
    tagCategories: List<PassCategory>,
) {
    val restoring = pass.isArchived
    val archiveLabel = if (restoring) "Restore" else "Archive"
    val pinnedLabel = if (pass.isPinned) "Unpin pass" else "Pin pass"
    val protectLabel = if (pass.isProtected) "Remove protection" else "Protect pass"
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val startRevealWidth = passActionButtonGroupWidth(1)
    val endRevealWidth = passActionButtonGroupWidth(3)
    val cardActionGap = PassActionButtonGap
    var measuredEndRevealWidthPx by remember { mutableIntStateOf(0) }
    var measuredCardWidthPx by remember { mutableIntStateOf(0) }
    val revealState = remember {
        AnchoredDraggableState(
            initialValue = SwipeRevealAnchor.Closed,
            anchors = DraggableAnchors {
                SwipeRevealAnchor.StartActions at with(density) { (startRevealWidth + cardActionGap).toPx() }
                SwipeRevealAnchor.Closed at 0f
                SwipeRevealAnchor.EndActions at -with(density) { (endRevealWidth + cardActionGap).toPx() }
            },
        )
    }
    SideEffect {
        val gapPx = with(density) { cardActionGap.toPx() }
        val startAnchor = with(density) { startRevealWidth.toPx() } + gapPx
        val endAnchor = (measuredEndRevealWidthPx.takeIf { it > 0 }?.toFloat()
            ?: with(density) { endRevealWidth.toPx() }) + gapPx
        val commitAnchor = maxOf(
            startAnchor + 1f,
            2f * measuredCardWidthPx * FULL_SWIPE_COMMIT_FRACTION - startAnchor,
        )
        revealState.updateAnchors(
            DraggableAnchors {
                SwipeRevealAnchor.StartActions at startAnchor
                SwipeRevealAnchor.StartCommit at commitAnchor
                SwipeRevealAnchor.Closed at 0f
                SwipeRevealAnchor.EndActions at -endAnchor
            },
        )
    }
    SyncSwipeReveal(pass.id, reorderingActive, openSwipePassId, revealState)
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(revealState, measuredCardWidthPx) {
        if (measuredCardWidthPx == 0) return@LaunchedEffect
        val commitThreshold = measuredCardWidthPx * FULL_SWIPE_COMMIT_FRACTION
        var wasActionable = false
        snapshotFlow {
            val offset = revealState.offset
            !offset.isNaN() && offset >= commitThreshold
        }.collect { isActionable ->
            if (isActionable && !wasActionable) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            }
            wasActionable = isActionable
        }
    }
    fun runSwipeAction(action: () -> Unit) {
        action()
        if (openSwipePassId.value == pass.id) openSwipePassId.value = null
        scope.launch { revealState.animateTo(SwipeRevealAnchor.Closed) }
    }

    Box(modifier.clip(shape).onSizeChanged { measuredCardWidthPx = it.width }) {
        Box(Modifier.matchParentSize()) {
            val gapPx = with(density) { cardActionGap.toPx() }
            val minimumActionWidthPx = with(density) { startRevealWidth.toPx() }
            val rawOffset = revealState.offset.let { if (it.isNaN()) 0f else it.coerceAtLeast(0f) }
            val actionWidthPx = maxOf(minimumActionWidthPx, rawOffset - gapPx)
                .coerceAtMost(measuredCardWidthPx.takeIf { it > 0 }?.toFloat() ?: minimumActionWidthPx)
            val commitThresholdPx = measuredCardWidthPx * FULL_SWIPE_COMMIT_FRACTION
            val iconScale = if (commitThresholdPx > 0f) {
                1f + 0.16f * (rawOffset / commitThresholdPx).coerceIn(0f, 1f)
            } else {
                1f
            }
            PassActionButtonGroup(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight()
                    .graphicsLayer { alpha = if (revealState.offset.let { !it.isNaN() && it > 0f }) 1f else 0f }
                    .then(
                        if (revealState.currentValue == SwipeRevealAnchor.StartActions ||
                            revealState.currentValue == SwipeRevealAnchor.StartCommit
                        ) Modifier
                        else Modifier.clearAndSetSemantics {},
                    ),
            ) {
                customItem(
                    buttonGroupContent = {
                        PassActionButton(
                            icon = if (restoring) Icons.Default.Restore else Icons.Default.Archive,
                            label = archiveLabel,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            index = 0,
                            count = 1,
                            fillHeight = true,
                            width = with(density) { actionWidthPx.toDp() },
                            iconScale = iconScale,
                            onClick = { runSwipeAction { onArchive(pass.id, restoring, pass.categoryId) } },
                        )
                    },
                    menuContent = { _ -> },
                )
            }
            PassActionButtonGroup(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().wrapContentWidth()
                    .graphicsLayer { alpha = if (revealState.offset.let { !it.isNaN() && it < 0f }) 1f else 0f }
                    .onSizeChanged { measuredEndRevealWidthPx = it.width }
                    .testTag("pass_end_actions_${pass.id}")
                    .then(
                        if (revealState.currentValue == SwipeRevealAnchor.EndActions) Modifier
                        else Modifier.clearAndSetSemantics {},
                    ),
            ) {
                customItem(
                    buttonGroupContent = {
                        PassActionButton(
                            icon = Icons.Default.PushPin,
                            label = pinnedLabel,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            index = 0,
                            count = 3,
                            fillHeight = true,
                            onClick = { runSwipeAction { onToggleFavorite(pass.id) } },
                        )
                    },
                    menuContent = { _ -> },
                )
                customItem(
                    buttonGroupContent = {
                        PassActionButton(
                            icon = if (pass.isProtected) Icons.Default.LockOpen else Icons.Default.Lock,
                            label = protectLabel,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            index = 1,
                            count = 3,
                            fillHeight = true,
                            onClick = { runSwipeAction { onToggleProtected(pass.id) } },
                        )
                    },
                    menuContent = { _ -> },
                )
                customItem(
                    buttonGroupContent = {
                        PassActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            index = 2,
                            count = 3,
                            fillHeight = true,
                            onClick = { runSwipeAction { onDelete(pass.id, pass.categoryId) } },
                        )
                    },
                    menuContent = { _ -> },
                )
            }
        }
        Box(
            Modifier.testTag("pass_card_${pass.id}").offset {
                val swipeOffset = revealState.offset
                IntOffset(if (swipeOffset.isNaN()) 0 else swipeOffset.roundToInt(), 0)
            }.pointerInput(pass.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    openSwipePassId.value = pass.id
                    var pressed: Boolean
                    do {
                        pressed = awaitPointerEvent().changes.any { it.pressed }
                    } while (pressed)
                    if (revealState.offset >= measuredCardWidthPx * FULL_SWIPE_COMMIT_FRACTION) {
                        onArchive(pass.id, restoring, pass.categoryId)
                        if (openSwipePassId.value == pass.id) openSwipePassId.value = null
                        scope.launch { revealState.snapTo(SwipeRevealAnchor.Closed) }
                    }
                }
            }.anchoredDraggable(
                state = revealState,
                orientation = Orientation.Horizontal,
                enabled = !reorderingActive,
            ),
        ) {
            TicketRow(
                pass = pass,
                category = category,
                hero = hero,
                sectionOrder = sectionOrder,
                hiddenSections = hiddenSections,
                showProtectedPassLockIcon = showProtectedPassLockIcon,
                blurProtectedPassCards = blurProtectedPassCards,
                modifier = Modifier.semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(archiveLabel) {
                            runSwipeAction { onArchive(pass.id, restoring, pass.categoryId) }
                            true
                        },
                        CustomAccessibilityAction("Delete") {
                            runSwipeAction { onDelete(pass.id, pass.categoryId) }
                            true
                        },
                        CustomAccessibilityAction(pinnedLabel) {
                            runSwipeAction { onToggleFavorite(pass.id) }
                            true
                        },
                        CustomAccessibilityAction(protectLabel) {
                            runSwipeAction { onToggleProtected(pass.id) }
                            true
                        },
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
                onProtectedPreviewRequested = onProtectedPreviewRequested,
                tagCategories = tagCategories,
            )
        }
    }
}

@Composable
private fun SyncSwipeReveal(
    passId: String,
    reorderingActive: Boolean,
    openPassId: androidx.compose.runtime.MutableState<String?>,
    state: AnchoredDraggableState<SwipeRevealAnchor>,
) {
    LaunchedEffect(reorderingActive) {
        if (reorderingActive) {
            state.animateTo(SwipeRevealAnchor.Closed)
            if (openPassId.value == passId) openPassId.value = null
        }
    }
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeRevealAnchor.Closed) {
            if (openPassId.value == passId) openPassId.value = null
        } else {
            openPassId.value = passId
        }
    }
    LaunchedEffect(openPassId.value) {
        if (openPassId.value != null && openPassId.value != passId) state.animateTo(SwipeRevealAnchor.Closed)
    }
}

@Composable
private fun TicketRow(
    pass: PassUiModel,
    category: PassCategory?,
    hero: Boolean,
    sectionOrder: List<HomeCardSection>,
    hiddenSections: Set<HomeCardSection>,
    showProtectedPassLockIcon: Boolean,
    blurProtectedPassCards: Boolean,
    modifier: Modifier,
    shape: Shape,
    onOpen: (String) -> Unit,
    onReorderStart: () -> Unit,
    onReorderDrag: (Float) -> Unit,
    onReorderEnd: () -> Unit,
    onReorderCancel: () -> Unit,
    onPreviewChanged: (Boolean) -> Unit,
    onPreviewOpeningChanged: (Boolean) -> Unit,
    onProtectedPreviewRequested: () -> Unit,
    tagCategories: List<PassCategory>,
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
        Box {
        Row(
            Modifier.fillMaxWidth().padding(if (hero) 20.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val contentBlurred = pass.isProtected && blurProtectedPassCards
            Box(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth()
                    .softProtectedBlur(contentBlurred)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        if (pass.isProtected && blurProtectedPassCards) {
                            contentDescription = "Protected pass information blurred"
                        }
                        onClick(
                            label = if (pass.isProtected && blurProtectedPassCards) {
                                "Open protected pass"
                            } else {
                                "Open ${pass.description}"
                            },
                        ) {
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
                            if (held && pass.isProtected) {
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                                onProtectedPreviewRequested()
                            } else if (held) {
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
                            } else if (!held && releasedBeforeLongPress && !movedBeforeLongPress) {
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
                        HomeCardSection.PRIMARY_FIELD -> pass.homeCardDetail()
                            ?.takeUnless { hero && it == pass.todayStartTimeLabel() }
                            ?.let {
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
                            val visibleTags = if (HomeCardSection.CATEGORY !in hiddenSections) {
                                tagCategories.filter { it.role == PassCategoryRole.CUSTOM && it.id in pass.tagIds }
                            } else {
                                emptyList()
                            }
                            if (showType || visibleTags.isNotEmpty()) {
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
                                    visibleTags.forEach { CategoryBadge(it) }
                                }
                            }
                        }
                    }
                }
            }
            }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.DragHandle,
                    "Reorder ${pass.description}",
                    Modifier.size(40.dp).pointerInput(pass.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onReorderStart() },
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
        if (pass.isProtected && showProtectedPassLockIcon) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(bottomStart = 18.dp),
            ) {
                Icon(Icons.Default.Lock, "Protected pass", Modifier.padding(8.dp).size(18.dp))
            }
        }
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
    val artwork = pass.displayArtwork(listOf(PassArtworkKind.ICON, PassArtworkKind.THUMBNAIL, PassArtworkKind.LOGO))
    if (artwork != null) {
        AdaptivePassArtwork(
            bytes = artwork.bytes,
            kind = artwork.kind,
            accentColor = pass.accentColor,
            contentDescription = "Pass artwork",
            modifier = modifier,
            context = PassArtworkContext.HOME_THUMBNAIL,
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
private fun LockedPassSection(passCount: Int, onUnlock: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onUnlock)
            .semantics { contentDescription = "Unlock protected passes" },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("Protected passes", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$passCount ${if (passCount == 1) "pass" else "passes"} · Tap to unlock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Collapse $title" else "Expand $title")
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

private fun PassUiModel.occursToday(): Boolean {
    val zone = calendarTimeSpan?.from?.zone ?: calendarTimeSpan?.to?.zone ?: return false
    val date = LocalDate.now(zone)
    val from = calendarTimeSpan?.from?.withZoneSameInstant(zone)?.toLocalDate()
    // The start date defines the Today section. An event ending today is not a new Today item.
    return from == date
}

private fun PassUiModel.todayStartTimeLabel(): String? =
    calendarTimeSpan?.from?.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

private fun PassUiModel.dateLabel(compactForToday: Boolean = false): String? {
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

private const val FULL_SWIPE_COMMIT_FRACTION = 0.62f
