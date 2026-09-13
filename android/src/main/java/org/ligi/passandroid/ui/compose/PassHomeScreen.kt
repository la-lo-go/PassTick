package org.ligi.passandroid.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PROTECTED_PASSES_CATEGORY_ID
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.searchDocument
import org.ligi.passandroid.ui.state.searchTerms
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.theme.PassIcons
import androidx.compose.ui.res.stringResource
import org.ligi.passandroid.R

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
    data class OpenUrl(val url: String) : HomeAction
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
    val separatedProtectedPassesShown = state.settings.separateProtectedPasses &&
        protectedPassesUnlocked && !searchExpanded
    val separatedProtectedPasses = if (
        separatedProtectedPassesShown && state.selectedCategoryId != PROTECTED_PASSES_CATEGORY_ID
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

    val undoLabel = stringResource(R.string.home_undo)
    val unlockPreviewMessage = stringResource(R.string.home_unlock_the_pass_to_preview)
    val passRestoredMessage = stringResource(R.string.home_pass_restored)
    val passArchivedMessage = stringResource(R.string.home_pass_archived)

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
                    actionLabel = undoLabel,
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
                Column(Modifier.fillMaxSize()) {
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.home_timeline)) },
                        selected = false,
                        icon = { Icon(Icons.Default.Timeline, null) },
                        onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.OpenTimeline) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.home_all)) },
                        selected = state.selectedCategoryId == null,
                        icon = { Icon(Icons.Default.ViewAgenda, null) },
                        onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.SelectCategory(null)) },
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_filter_all"),
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.home_protected)) },
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
                        label = { Text(stringResource(R.string.home_pinned)) },
                        selected = state.selectedCategoryId == "pinned",
                        icon = { Icon(Icons.Default.PushPin, null) },
                        onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.SelectCategory("pinned")) },
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_filter_pinned"),
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.home_archived)) },
                        selected = state.selectedCategoryId == "archived",
                        icon = { Icon(Icons.Default.Archive, null) },
                        onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.SelectCategory("archived")) },
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_filter_archived"),
                    )
                    if (visibleCategories.isNotEmpty()) {
                        Text(
                            stringResource(R.string.category_tags),
                            modifier = Modifier.padding(start = 28.dp, top = 16.dp, end = 28.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        visibleCategories.forEach { category ->
                            NavigationDrawerItem(
                                label = { Text(category.name) },
                                selected = state.selectedCategoryId == category.id,
                                icon = { Icon(categoryIcon(category.icon), category.icon) },
                                onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.SelectCategory(category.id)) },
                                modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_filter_${category.id}"),
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.home_github_repo)) },
                        selected = false,
                        icon = { Icon(PassIcons.GitHub, null) },
                        onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.OpenUrl(PROJECT_REPOSITORY_URL)) },
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_open_repository"),
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.settings_settings)) },
                        selected = false,
                        icon = { Icon(Icons.Default.Settings, null) },
                        onClick = { scope.launch { drawerState.close() }; onAction(HomeAction.OpenSettings) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
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
                val hasNoVisiblePasses = visiblePasses.isEmpty() && separatedProtectedPasses.isEmpty() &&
                    !showLockedSection
                if (hasNoVisiblePasses && !state.isContentLoading && !state.isBusy) {
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
                    item(key = "today-heading") { SectionHeading(stringResource(R.string.home_today), todayExpanded) { todayExpanded = !todayExpanded } }
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
                                if (restoring) dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id, originalCategoryId), passRestoredMessage)
                                else dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id, originalCategoryId), passArchivedMessage)
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
                    item(key = "pinned-heading") { SectionHeading(stringResource(R.string.home_pinned), pinnedExpanded) { pinnedExpanded = !pinnedExpanded } }
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
                                    dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id, originalCategoryId), passRestoredMessage)
                                } else {
                                    dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id, originalCategoryId), passArchivedMessage)
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
                                scope.launch { snackbarHostState.showSnackbar(unlockPreviewMessage) }
                            },
                            openSwipePassId = openSwipePassId,
                            tagCategories = state.categories,
                        )
                    }
                }
                if (remainingPasses.isNotEmpty()) {
                    // The heading only labels the leftover group; without other sections it has no contrast.
                    val showOtherPassesHeading = todayPasses.isNotEmpty() || pinnedPasses.isNotEmpty() ||
                        separatedProtectedPasses.isNotEmpty() || showLockedSection
                    if (showOtherPassesHeading) {
                        item(key = "passes-heading") { SectionHeading(stringResource(R.string.home_other_passes), otherExpanded) { otherExpanded = !otherExpanded } }
                    }
                    if (otherExpanded || !showOtherPassesHeading) item(key = "pass-feed") {
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
                                    dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id, originalCategoryId), passRestoredMessage)
                                } else {
                                    dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id, originalCategoryId), passArchivedMessage)
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
                                scope.launch { snackbarHostState.showSnackbar(unlockPreviewMessage) }
                            },
                            openSwipePassId = openSwipePassId,
                            tagCategories = state.categories,
                        )
                    }
                }
                if (separatedProtectedPasses.isNotEmpty()) {
                    item(key = "protected-heading") {
                        SectionHeading(stringResource(R.string.home_protected_passes), protectedExpanded) { protectedExpanded = !protectedExpanded }
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
                                    dispatchReversible(HomeAction.Restore(id), UndoOperation.Restore(id, originalCategoryId), passRestoredMessage)
                                } else {
                                    dispatchReversible(HomeAction.Archive(id), UndoOperation.Archive(id, originalCategoryId), passArchivedMessage)
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
                                    stringResource(R.string.home_search_passes),
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
                            IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Close, stringResource(R.string.home_clear_search)) }
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, stringResource(R.string.home_navigation_menu)) }
        },
        actions = {
            if (!searchExpanded) {
                IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, stringResource(R.string.home_search_passes)) }
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

@Composable
private fun homeSortOptions(selectedSort: PassSortOrder, showManual: Boolean) = buildList {
    if (showManual) add(HomeSortOption(PassSortOrder.MANUAL, stringResource(R.string.home_manual)))
    add(HomeSortOption(PassSortOrder.DATE_DESC, if (selectedSort == PassSortOrder.DATE_ASC) stringResource(R.string.home_oldest) else stringResource(R.string.home_newest)))
    add(HomeSortOption(PassSortOrder.DATE_DIFF, stringResource(R.string.home_nearest)))
    add(HomeSortOption(PassSortOrder.TYPE, stringResource(R.string.home_type)))
}

@Composable
private fun LockedPassSection(passCount: Int, onUnlock: () -> Unit) {
    val unlockDescription = stringResource(R.string.home_unlock_protected_passes)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onUnlock)
            .semantics { contentDescription = unlockDescription },
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
                Text(stringResource(R.string.home_protected_passes), style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.home_locked_passes, passCount, passCount),
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
        Modifier.fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            )
            .padding(vertical = 4.dp),
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
        Text(stringResource(R.string.home_your_passes_live_here), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.home_import_one_or_more_pass_files), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptySearch(query: String) {
    Column(
        Modifier.fillMaxWidth().widthIn(max = 520.dp).padding(horizontal = 24.dp, vertical = 56.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.home_no_matching_passes), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.home_no_matches_for_query, query.trim()), style = MaterialTheme.typography.bodyLarge)
    }
}
