package org.ligi.passandroid.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.withContext
import org.ligi.passandroid.platform.PlatformActions
import org.ligi.passandroid.platform.PassImageExporter
import org.ligi.passandroid.R
import org.ligi.passandroid.imports.DocumentImportProcessor
import org.ligi.passandroid.imports.ImportDraft
import org.ligi.passandroid.imports.ImportEdits
import org.ligi.passandroid.imports.ImportSource
import org.ligi.passandroid.imports.NormalizedRect
import org.ligi.passandroid.ui.compose.PassDocumentPages
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.repository.PassUpdate
import org.ligi.passandroid.repository.PassRepository
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.repository.PassArtworkUpdate
import org.ligi.passandroid.repository.PassLocationSnapshot
import org.ligi.passandroid.repository.PassTimeSpanSnapshot
import org.ligi.passandroid.repository.DEFAULT_PASS_CATEGORY_ID
import org.ligi.passandroid.repository.TRASH_RETENTION
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.repository.SettingsRepository
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.ligi.passandroid.domain.timeline.buildPassTimeline
import org.ligi.passandroid.domain.timeline.normalizedTimeSpan
import org.ligi.passandroid.reminder.ReminderScheduler
import org.ligi.passandroid.reminder.buildPassReminders
import org.ligi.passandroid.reminder.PassReminderOverride
import org.ligi.passandroid.widget.WidgetSnapshotPublisher
import org.ligi.passandroid.widget.WidgetRefresher
import org.ligi.passandroid.shortcuts.ShortcutsPublisher

class MainViewModel(
    private val passRepository: PassRepository,
    private val settingsRepository: SettingsRepository,
    private val platformActions: PlatformActions,
    private val reminderScheduler: ReminderScheduler = ReminderScheduler.None,
    private val widgetPublisher: WidgetSnapshotPublisher? = null,
    private val shortcutsPublisher: ShortcutsPublisher? = null,
    private val widgetRefresher: WidgetRefresher? = null,
    private val strings: StringResolver = StringResolver { _, _ -> "" },
) : ViewModel() {
    private var reminderActionOverrides: Map<String, Set<org.ligi.passandroid.reminder.NotificationAction>>? = null
    suspend fun isCalendarEventPresent(pass: PassUiModel): Boolean = withContext(Dispatchers.IO) {
        pass.calendarEvent?.let(platformActions::isCalendarEventPresent) == true
    }

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val pendingDeletionIds = MutableStateFlow<Set<String>>(emptySet())
    private val importReview = MutableStateFlow<ImportReviewUiState?>(null)
    private val preparingImport = MutableStateFlow(false)
    private val recentlyImportedIds = MutableStateFlow<Set<String>>(emptySet())
    private var recentlyImportedClearJob: Job? = null
    private var pendingImportDraft: ImportDraft? = null
    private val systemAccentColor = platformActions.systemAccentColor()
    private val categoryMoves = Channel<AppAction.MovePass>(Channel.UNLIMITED)
    private val passes = passRepository.observePasses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val trashedPasses = passRepository.observeTrashedPasses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val visiblePasses = combine(passes, pendingDeletionIds) { currentPasses, pendingIds ->
        currentPasses.filterNot { it.id in pendingIds }
    }
    private val visibleTrashedPasses = combine(trashedPasses, pendingDeletionIds) { currentTrashed, pendingIds ->
        currentTrashed.filterNot { it.id in pendingIds }
    }
    private data class PassCollections(
        val passes: List<PassSnapshot>,
        val trashedPasses: List<PassSnapshot>,
    )
    private val passCollections = combine(visiblePasses, visibleTrashedPasses) { passes, trashed ->
        PassCollections(passes, trashed)
    }

    val uiState = combine(passCollections, settingsRepository.settings, busy, message, selectedCategoryId) {
            collections, settings, isBusy, currentMessage, requestedCategoryId ->
        val categories = settings.categories.withLegacyCategories(collections.passes)
        val timeline = buildPassTimeline(collections.passes, Instant.now(), ZoneId.systemDefault())
        MainUiState(
            passes = collections.passes.sortedForDisplay(settings.sortOrder, settings.passOrder).map(PassUiModel::from),
            trashedPasses = collections.trashedPasses
                .sortedByDescending { it.trashedAtEpochMillis ?: 0L }
                .map(PassUiModel::from),
            settings = settings,
            isContentLoading = false,
            isBusy = isBusy,
            message = currentMessage,
            categories = categories,
            selectedCategoryId = requestedCategoryId?.takeIf { requested ->
                requested in setOf(
                    PROTECTED_PASSES_CATEGORY_ID,
                    PINNED_PASSES_CATEGORY_ID,
                    ARCHIVED_PASSES_CATEGORY_ID,
                    TRASHED_PASSES_CATEGORY_ID,
                ) || categories.any { it.id == requested }
            },
            timeline = timeline,
            systemAccentColor = systemAccentColor,
            codePassPickerRows = codePassPickerRows(collections.passes, settings),
        )
    }.combine(importReview) { currentState, review ->
        currentState.copy(importReview = review)
    }.combine(preparingImport) { currentState, preparing ->
        currentState.copy(isPreparingImport = preparing)
    }.combine(recentlyImportedIds) { currentState, importedIds ->
        currentState.copy(recentlyImportedIds = importedIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        // Seed once: later settings emissions must not overwrite a selection made in this session.
        viewModelScope.launch {
            selectedCategoryId.value = settingsRepository.settings.first().selectedCategoryId
        }
        // Trash has no background scheduler; expiry is enforced on every app open.
        viewModelScope.launch {
            runCatching { passRepository.purgeExpiredTrash(TRASH_RETENTION) }
        }
        viewModelScope.launch {
            for (move in categoryMoves) {
                runCatching { passRepository.moveToCategory(move.id, move.categoryId) }
                    .onSuccess { if (move.announce) message.value = strings.resolve(R.string.message_pass_moved) }
                    .onFailure { message.value = it.message ?: strings.resolve(R.string.message_operation_failed) }
            }
        }
        viewModelScope.launch {
            combine(passes, settingsRepository.settings) { currentPasses, settings -> currentPasses to settings }
                .collectLatest { (currentPasses, settings) ->
                    val now = Instant.now()
                    val timeline = buildPassTimeline(currentPasses, now, ZoneId.systemDefault())
                    reminderScheduler.sync(
                        if (settings.remindersEnabled) {
                            val overrides = buildMap {
                                settings.reminderExcludedPassIds.forEach { put(it, PassReminderOverride.Disabled) }
                                settings.reminderLeadMinutesByPass.forEach { (passId, minutes) ->
                                    put(passId, PassReminderOverride.LeadTime(minutes))
                                }
                                settings.reminderExactPassIds.forEach { put(it, PassReminderOverride.ExactAtEvent) }
                            }
                            buildPassReminders(
                                timeline,
                                now,
                                settings.reminderMinutes,
                                overrides,
                                settings.reminderActionsByPass,
                            )
                        } else {
                            emptyList()
                        },
                        settings.notificationPolicySettings,
                    )
                    val widgetExcludedIds = settings.categories
                        .filter { it.role == PassCategoryRole.ARCHIVE || it.role == PassCategoryRole.TRASH }
                        .mapTo(mutableSetOf()) { it.id }
                    runCatching {
                        widgetPublisher?.publish(
                            currentPasses.filterNot(PassSnapshot::isTrashed),
                            widgetExcludedIds,
                            settings.lockAllPasses,
                            settings.homeCardSectionOrder,
                            settings.hiddenHomeCardSections,
                            settings.categories,
                        )
                    }
                    runCatching {
                        shortcutsPublisher?.publish(
                            currentPasses.filterNot(PassSnapshot::isTrashed),
                            settings.lockAllPasses,
                            settings.homeCardSectionOrder,
                            settings.hiddenHomeCardSections,
                            settings.categories,
                        )
                    }
                    if (settings.automaticallyMarkPast) {
                        // The archived check keeps the effect idempotent across store notifications.
                        currentPasses.filter { it.hasEndedWithoutArchive(now) }.forEach { pass ->
                            viewModelScope.launch { runCatching { passRepository.setArchived(pass.id, true) } }
                        }
                    }
                }
        }
    }

    fun onAction(action: AppAction) {
        if (handlePassMetadataAction(action)) return
        if (action is AppAction.ClearMessage) {
            message.value = null
            return
        }
        if (handleImportExportAction(action)) return
        if (handlePassPresentationAction(action)) return
        if (handlePassMutationAction(action)) return
        if (handleTrashAction(action)) return
        if (handleCategoryAction(action)) return
        if (handleHomeAction(action)) return
        if (handleCodeSettingsAction(action)) return
        if (handleAppearanceSettingsAction(action)) return
        if (handlePassListSettingsAction(action)) return
        if (handlePrivacySettingsAction(action)) return
        if (handleNotificationSettingsAction(action)) return
        handleReminderAction(action)
    }

    private fun handleImportExportAction(action: AppAction): Boolean = when (action) {
        is AppAction.Import -> {
            launchOperation(strings.resolve(R.string.message_pass_imported)) {
                val imported = passRepository.import(action.uri).getOrThrow()
                addCalendarEventsAfterImport(listOf(imported))
                highlightImported(listOf(imported.id))
            }
            true
        }
        is AppAction.ImportFiles -> {
            importFiles(action.uris)
            true
        }
        is AppAction.Export -> {
            launchOperation(strings.resolve(R.string.message_pass_exported)) {
                passRepository.export(action.id, action.destination).getOrThrow()
            }
            true
        }
        is AppAction.SharePass -> {
            launchOperation(strings.resolve(R.string.message_pass_ready_to_share)) {
                val uri = passRepository.prepareShare(action.id).getOrThrow()
                platformActions.share(uri, shareMimeType(uri))
            }
            true
        }
        is AppAction.ShareImage -> {
            launchOperation(strings.resolve(R.string.message_image_ready_to_share)) {
                val pass = uiState.value.passes.firstOrNull { it.id == action.id } ?: error("Pass not found")
                withContext(Dispatchers.IO) { platformActions.shareImage(pass, action.options) }
            }
            true
        }
        is AppAction.PrintImage -> {
            launchOperation(null) {
                val pass = uiState.value.passes.firstOrNull { it.id == action.id } ?: error("Pass not found")
                val bitmap = withContext(Dispatchers.Default) { PassImageExporter.renderBitmap(pass, action.options) }
                platformActions.printImage(pass.description.ifBlank { "Pass" }, bitmap)
            }
            true
        }
        else -> handleArchiveAction(action)
    }

    private fun importFiles(uris: List<Uri>) {
        launchOperation(null) {
            val results = uris.map { uri -> passRepository.import(uri) }
            val imported = results.mapNotNull { it.getOrNull() }
            val failedCount = results.count { it.isFailure }
            addCalendarEventsAfterImport(imported)
            highlightImported(imported.map(PassSnapshot::id))
            message.value = when {
                failedCount == 0 -> strings.resolve(R.string.message_passes_imported)
                imported.isEmpty() -> results.firstNotNullOfOrNull { it.exceptionOrNull()?.message }
                    ?: strings.resolve(R.string.message_operation_failed)
                else -> strings.resolve(
                    R.string.message_passes_imported_summary,
                    imported.size,
                    failedCount,
                )
            }
        }
    }

    private fun handleArchiveAction(action: AppAction): Boolean = when (action) {
        is AppAction.ExportArchive -> {
            launchOperation(strings.resolve(R.string.message_backup_saved)) {
                passRepository.exportArchive(action.destination).getOrThrow()
            }
            true
        }
        is AppAction.ImportArchive -> {
            launchOperation(null) {
                val summary = passRepository.importArchive(action.source).getOrThrow()
                message.value = if (summary.skipped == 0 && summary.failed == 0) {
                    strings.resolve(R.string.message_passes_restored, summary.restored)
                } else {
                    strings.resolve(
                        R.string.message_passes_restored_summary,
                        summary.restored,
                        summary.skipped,
                        summary.failed,
                    )
                }
            }
            true
        }
        else -> false
    }

    val documentPages = PassDocumentPages { passId, pageIndex, targetWidthPx ->
        passRepository.renderDocumentPage(passId, pageIndex, targetWidthPx).getOrNull()
    }

    suspend fun createPass(draft: PassDraft): Result<PassSnapshot> {
        busy.value = true
        return try {
            val created = passRepository.create(draft.toPassUpdate())
            passRepository.setNotes(created.id, draft.notes)
            message.value = strings.resolve(R.string.message_pass_created)
            highlightImported(listOf(created.id))
            Result.success(created)
        } catch (error: Throwable) {
            message.value = error.message ?: strings.resolve(R.string.message_operation_failed)
            Result.failure(error)
        } finally {
            busy.value = false
        }
    }

    suspend fun prepareDocumentImport(uri: Uri, source: ImportSource): Result<Unit> {
        discardPendingDraft()
        preparingImport.value = true
        return try {
            passRepository.prepareDocumentImport(uri, source)
                .onSuccess { draft ->
                    pendingImportDraft = draft
                    importReview.value = ImportReviewUiState(
                        draftId = draft.id,
                        source = draft.source,
                        title = draft.suggestedTitle,
                        accentColor = draft.suggestedAccentColor,
                        suggestedAccentColor = draft.suggestedAccentColor,
                        pageCount = draft.pageCount,
                        previewPng = draft.previewPng,
                        detectedCodes = draft.detectedCodes.map { DetectedCodeUiModel(it.format, it.message) },
                        selectedCodeIndices = draft.detectedCodes.indices.toSet(),
                    )
                }
                .onFailure { error ->
                    message.value = error.message ?: strings.resolve(R.string.import_review_unreadable)
                }
                .map { }
        } finally {
            preparingImport.value = false
        }
    }

    fun onImportReviewAction(action: ImportReviewAction) {
        val review = importReview.value ?: return
        when (action) {
            is ImportReviewAction.SetTitle -> importReview.value = review.copy(title = action.title)
            is ImportReviewAction.SetAccentColor -> importReview.value = review.copy(accentColor = action.color)
            is ImportReviewAction.SelectCode -> importReview.value = review.withToggledCode(action.index)
            ImportReviewAction.Rotate -> rotateImportPreview(review)
            is ImportReviewAction.SetCrop -> importReview.value = review.copy(crop = action.crop)
            is ImportReviewAction.SetCropEditing -> setImportReviewCropEditing(review, action.editing)
            ImportReviewAction.Confirm -> confirmImportReview(review)
            ImportReviewAction.Discard -> discardPendingDraft()
        }
    }

    private fun ImportReviewUiState.withToggledCode(index: Int?): ImportReviewUiState {
        if (index == null) return copy(selectedCodeIndices = emptySet())
        val updated = if (index in selectedCodeIndices) {
            selectedCodeIndices - index
        } else {
            selectedCodeIndices + index
        }
        return copy(selectedCodeIndices = updated)
    }

    private fun setImportReviewCropEditing(review: ImportReviewUiState, editing: Boolean) {
        importReview.value = review.copy(cropEditing = editing)
        if (editing) return
        viewModelScope.launch {
            val croppedPng = withContext(Dispatchers.Default) {
                DocumentImportProcessor.decodePng(review.previewPng)
                    ?.let { bitmap -> DocumentImportProcessor.crop(bitmap, review.crop) }
                    ?.let(DocumentImportProcessor::encodePng)
            } ?: return@launch
            val latest = importReview.value ?: return@launch
            if (!latest.cropEditing) importReview.value = latest.copy(displayPng = croppedPng)
        }
    }

    private fun rotateImportPreview(review: ImportReviewUiState) {
        if (review.isBusy) return
        importReview.value = review.copy(isBusy = true)
        viewModelScope.launch {
            val rotatedPng = withContext(Dispatchers.Default) {
                DocumentImportProcessor.decodePng(review.previewPng)
                    ?.let { DocumentImportProcessor.rotate(it, 90) }
                    ?.let(DocumentImportProcessor::encodePng)
            }
            val latest = importReview.value ?: return@launch
            importReview.value = if (rotatedPng == null) {
                latest.copy(isBusy = false)
            } else {
                latest.copy(
                    previewPng = rotatedPng,
                    displayPng = rotatedPng,
                    rotationDegrees = (review.rotationDegrees + 90) % 360,
                    crop = NormalizedRect.Full,
                    isBusy = false,
                )
            }
        }
    }

    private fun confirmImportReview(review: ImportReviewUiState) {
        val draft = pendingImportDraft ?: return
        if (review.isBusy) return
        importReview.value = review.copy(isBusy = true)
        viewModelScope.launch {
            val edits = ImportEdits(
                title = review.title,
                accentColor = review.accentColor,
                rotationDegrees = review.rotationDegrees,
                crop = review.crop,
                selectedCodeIndices = review.selectedCodeIndices,
            )
            passRepository.commitDocumentImport(draft, edits)
                .onSuccess { imported ->
                    pendingImportDraft = null
                    importReview.value = null
                    message.value = strings.resolve(R.string.message_pass_imported)
                    addCalendarEventsAfterImport(listOf(imported))
                    highlightImported(listOf(imported.id))
                }
                .onFailure { error ->
                    importReview.value = review.copy(isBusy = false)
                    message.value = error.message ?: strings.resolve(R.string.message_operation_failed)
                }
        }
    }

    private fun discardPendingDraft() {
        val draft = pendingImportDraft ?: return
        pendingImportDraft = null
        importReview.value = null
        viewModelScope.launch { passRepository.discardDocumentImport(draft.id) }
    }

    private fun handlePassPresentationAction(action: AppAction): Boolean = when (action) {
        is AppAction.AddToCalendar -> {
            withPass(action.id) { pass ->
                pass.calendarEvent?.let(platformActions::addToCalendar) ?: error("Pass has no date")
            }
            true
        }
        is AppAction.OpenLocation -> {
            withPass(action.id) { pass ->
                val location = pass.locations.getOrNull(action.locationIndex) ?: error("Location not found")
                platformActions.openLocation(location.toPlatformLocation())
            }
            true
        }
        is AppAction.OpenUrl -> {
            runCatching { platformActions.openUrl(action.url) }
                .onFailure { message.value = it.message ?: strings.resolve(R.string.message_operation_failed) }
            true
        }
        else -> false
    }

    private fun handlePassMutationAction(action: AppAction): Boolean = when (action) {
        is AppAction.SetPassProtected -> {
            launchOperation(
                if (action.isProtected) strings.resolve(R.string.message_pass_protected)
                else strings.resolve(R.string.message_protection_removed),
            ) {
                passRepository.setProtected(action.id, action.isProtected)
            }
            true
        }
        is AppAction.SavePass -> {
            launchOperation(strings.resolve(R.string.message_pass_saved)) { save(action) }
            true
        }
        is AppAction.DuplicatePass -> {
            launchOperation(strings.resolve(R.string.message_pass_duplicated)) {
                highlightImported(listOf(passRepository.duplicate(action.id).id))
            }
            true
        }
        else -> false
    }

    private fun handleTrashAction(action: AppAction): Boolean = when (action) {
        is AppAction.DeletePass -> {
            deletePass(action.id)
            true
        }
        is AppAction.DeleteForever -> {
            deletePass(action.id)
            true
        }
        is AppAction.TrashPass -> {
            viewModelScope.launch {
                busy.value = true
                runCatching { passRepository.trashPass(action.id) }
                    .onFailure { message.value = it.message ?: strings.resolve(R.string.message_operation_failed) }
                pendingDeletionIds.update { it - action.id }
                busy.value = false
            }
            true
        }
        is AppAction.RestoreFromTrash -> {
            launchOperation(strings.resolve(R.string.message_pass_restored)) {
                passRepository.restoreFromTrash(action.id)
            }
            true
        }
        is AppAction.EmptyTrash -> {
            launchOperation(null) {
                passRepository.emptyTrash()
            }
            true
        }
        is AppAction.SetPassPendingDeletion -> {
            pendingDeletionIds.update { pendingIds ->
                if (action.pending) pendingIds + action.id else pendingIds - action.id
            }
            true
        }
        else -> false
    }

    private fun deletePass(id: String) {
        viewModelScope.launch {
            busy.value = true
            runCatching { check(passRepository.delete(id)) }
                .onFailure { message.value = it.message ?: strings.resolve(R.string.message_operation_failed) }
            pendingDeletionIds.update { it - id }
            busy.value = false
        }
    }

    private fun handleCategoryAction(action: AppAction): Boolean = when (action) {
        is AppAction.SelectCategory -> {
            selectedCategoryId.value = action.categoryId
            viewModelScope.launch { settingsRepository.setSelectedCategoryId(action.categoryId) }
            true
        }
        is AppAction.SaveCategory -> {
            viewModelScope.launch {
                val categories = uiState.value.settings.categories
                settingsRepository.setCategories(
                    categories.filterNot { it.id == action.category.id } + action.category,
                )
            }
            true
        }
        is AppAction.DeleteCategory -> {
            launchOperation(strings.resolve(R.string.message_category_deleted)) {
                val category = uiState.value.categories.firstOrNull { it.id == action.categoryId }
                    ?: error("Category not found")
                require(category.role == PassCategoryRole.CUSTOM) { "Built-in categories cannot be deleted" }
                uiState.value.passes.filter { action.categoryId in it.tagIds }.forEach { pass ->
                    passRepository.setTags(pass.id, pass.tagIds - action.categoryId)
                }
                settingsRepository.setCategories(
                    uiState.value.settings.categories.filterNot { it.id == action.categoryId },
                )
            }
            true
        }
        is AppAction.MoveCategory -> {
            viewModelScope.launch {
                val categories = uiState.value.categories.toMutableList()
                val from = categories.indexOfFirst { it.id == action.categoryId }
                val to = (from + action.offset).coerceIn(categories.indices)
                if (from >= 0 && from != to) {
                    val category = categories.removeAt(from)
                    categories.add(to, category)
                    settingsRepository.setCategories(categories)
                }
            }
            true
        }
        is AppAction.MovePass -> {
            check(categoryMoves.trySend(action).isSuccess) { "Pass move queue is closed" }
            true
        }
        else -> false
    }

    private fun handleHomeAction(action: AppAction): Boolean = when (action) {
        is AppAction.ReorderPass -> {
            viewModelScope.launch {
                val currentIds = uiState.value.passes.map(PassUiModel::id).toMutableList()
                val visibleIds = action.orderedVisibleIds.distinct().filter(currentIds::contains)
                val visibleIdSet = visibleIds.toSet()
                val reorderedIds = visibleIds.iterator()
                val mergedIds = currentIds.map { id ->
                    if (id in visibleIdSet) reorderedIds.next() else id
                }
                if (mergedIds != currentIds) {
                    settingsRepository.setPassOrder(mergedIds)
                    settingsRepository.setSortOrder(PassSortOrder.MANUAL)
                }
            }
            true
        }
        is AppAction.SetHighlightTodayPasses -> {
            viewModelScope.launch {
                settingsRepository.setHighlightTodayPasses(action.value)
            }
            true
        }
        is AppAction.SetAutomaticallyMarkPast -> {
            viewModelScope.launch {
                settingsRepository.setAutomaticallyMarkPast(action.value)
            }
            true
        }
        is AppAction.SetOfferCalendarAfterImport -> {
            viewModelScope.launch {
                settingsRepository.setOfferCalendarAfterImport(action.value)
            }
            true
        }
        is AppAction.MoveHomeCardSection -> {
            viewModelScope.launch {
                settingsRepository.moveHomeCardSection(action.section, action.offset)
            }
            true
        }
        is AppAction.SetHomeCardSectionVisible -> {
            viewModelScope.launch {
                settingsRepository.setHomeCardSectionVisible(action.section, action.visible)
            }
            true
        }
        else -> false
    }

    private fun handleCodeSettingsAction(action: AppAction): Boolean = when (action) {
        is AppAction.SetAutomaticBrightness -> {
            viewModelScope.launch {
                settingsRepository.setAutomaticBrightness(action.value)
            }
            true
        }
        is AppAction.SetCodePassId -> {
            viewModelScope.launch {
                settingsRepository.setCodePassId(action.value)
                runCatching { widgetRefresher?.refresh() }
            }
            true
        }
        is AppAction.SetCodeSizeStep -> {
            viewModelScope.launch {
                settingsRepository.setCodeSizeStep(action.value)
            }
            true
        }
        is AppAction.SetCodeWhiteSurround -> {
            viewModelScope.launch {
                settingsRepository.setCodeWhiteSurround(action.value)
            }
            true
        }
        is AppAction.SetCodeExtraQuietZone -> {
            viewModelScope.launch {
                settingsRepository.setCodeExtraQuietZone(action.value)
            }
            true
        }
        is AppAction.SetCodeRotateQuarterTurn -> {
            viewModelScope.launch {
                settingsRepository.setCodeRotateQuarterTurn(action.value)
            }
            true
        }
        is AppAction.SetCodeKeepScreenOn -> {
            viewModelScope.launch {
                settingsRepository.setCodeKeepScreenOn(action.value)
            }
            true
        }
        else -> false
    }

    private fun handleAppearanceSettingsAction(action: AppAction): Boolean = when (action) {
        is AppAction.SetTheme -> {
            viewModelScope.launch { settingsRepository.setThemeMode(action.value) }
            true
        }
        is AppAction.SetAmoledBlackBackground -> {
            viewModelScope.launch {
                settingsRepository.setAmoledBlackBackground(action.value)
            }
            true
        }
        is AppAction.SetDynamicColors -> {
            viewModelScope.launch {
                // Keep the wallpaper accent as the seed the first time dynamic colors turn off,
                // so the scheme does not jump to the default color.
                if (!action.value && settingsRepository.settings.first().accentColor == null) {
                    systemAccentColor?.let { settingsRepository.setAccentColor(it) }
                }
                settingsRepository.setDynamicColors(action.value)
            }
            true
        }
        is AppAction.SetAccentColor -> {
            viewModelScope.launch {
                settingsRepository.setAccentColor(action.value)
            }
            true
        }
        is AppAction.SetColorStyle -> {
            viewModelScope.launch {
                settingsRepository.setColorStyle(action.value)
            }
            true
        }
        is AppAction.SetBlockScreenshots -> {
            viewModelScope.launch {
                settingsRepository.setBlockScreenshots(action.value)
            }
            true
        }
        is AppAction.SetTrashEnabled -> {
            viewModelScope.launch {
                settingsRepository.setTrashEnabled(action.value)
            }
            true
        }
        is AppAction.SetImageExportOptions -> {
            viewModelScope.launch {
                settingsRepository.setImageExportOptions(action.value)
            }
            true
        }
        else -> false
    }

    private fun handlePassListSettingsAction(action: AppAction): Boolean = when (action) {
        is AppAction.SetSortOrder -> {
            viewModelScope.launch { settingsRepository.setSortOrder(action.value) }
            true
        }
        is AppAction.MovePassDetailSection -> {
            viewModelScope.launch {
                settingsRepository.movePassDetailSection(action.section, action.offset)
            }
            true
        }
        is AppAction.SetPassDetailSectionVisible -> {
            viewModelScope.launch {
                settingsRepository.setPassDetailSectionVisible(action.section, action.visible)
            }
            true
        }
        else -> false
    }

    private fun handlePrivacySettingsAction(action: AppAction): Boolean = when (action) {
        is AppAction.SetLockAllPasses -> {
            viewModelScope.launch {
                settingsRepository.setLockAllPasses(action.value)
            }
            true
        }
        is AppAction.SetShowProtectedPassLockIcon -> {
            viewModelScope.launch {
                settingsRepository.setShowProtectedPassLockIcon(action.value)
            }
            true
        }
        is AppAction.SetBlurProtectedPassCards -> {
            viewModelScope.launch {
                settingsRepository.setBlurProtectedPassCards(action.value)
            }
            true
        }
        is AppAction.SetSeparateProtectedPasses -> {
            viewModelScope.launch {
                settingsRepository.setSeparateProtectedPasses(action.value)
            }
            true
        }
        else -> false
    }

    private fun handleNotificationSettingsAction(action: AppAction): Boolean = when (action) {
        is AppAction.SetRemindersEnabled -> {
            viewModelScope.launch {
                settingsRepository.setRemindersEnabled(action.value)
            }
            true
        }
        is AppAction.SetReminderMinutes -> {
            viewModelScope.launch {
                settingsRepository.setReminderMinutes(action.value)
            }
            true
        }
        is AppAction.SetNotificationAccessWindow -> {
            viewModelScope.launch {
                settingsRepository.setNotificationAccessWindowMinutes(action.minutes)
            }
            true
        }
        is AppAction.SetNotificationExactTiming -> {
            viewModelScope.launch {
                settingsRepository.setNotificationExactTiming(action.value)
            }
            true
        }
        is AppAction.SetNotificationActionsEnabled -> {
            viewModelScope.launch {
                settingsRepository.setNotificationActionsEnabled(action.value)
            }
            true
        }
        else -> false
    }

    private fun handleReminderAction(action: AppAction): Boolean = when (action) {
        is AppAction.SetPassReminderActions -> {
            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { setPassReminderActions(action) }
            true
        }
        is AppAction.TogglePassReminder -> {
            togglePassReminder(action)
            true
        }
        is AppAction.ConfigurePassReminder -> {
            configurePassReminder(action)
            true
        }
        else -> false
    }

    private fun togglePassReminder(action: AppAction.TogglePassReminder) {
        viewModelScope.launch {
            val excluded = uiState.value.settings.reminderExcludedPassIds.toMutableSet()
            if (!excluded.add(action.passId)) excluded.remove(action.passId)
            settingsRepository.setReminderExcludedPassIds(excluded)
        }
    }

    private fun configurePassReminder(action: AppAction.ConfigurePassReminder) {
        viewModelScope.launch {
            val settings = uiState.value.settings
            val excluded = settings.reminderExcludedPassIds.toMutableSet().apply {
                if (action.enabled) remove(action.passId) else add(action.passId)
            }
            val leads = settings.reminderLeadMinutesByPass.toMutableMap().apply {
                val minutes = action.leadMinutes
                if (action.enabled && minutes != null) {
                    put(action.passId, minutes.coerceIn(0, 10_080))
                } else {
                    remove(action.passId)
                }
            }
            settingsRepository.setReminderExcludedPassIds(excluded)
            settingsRepository.setReminderLeadMinutesByPass(leads)
            settingsRepository.setReminderExactPassIds(
                settings.reminderExactPassIds.toMutableSet().apply {
                    if (action.enabled && action.exactAtEvent) add(action.passId) else remove(action.passId)
                },
            )
        }
    }

    internal suspend fun setPassReminderActions(action: AppAction.SetPassReminderActions) {
        val current = reminderActionOverrides ?: uiState.value.settings.reminderActionsByPass
        val updated = action.actions?.let { current + (action.passId to it) } ?: (current - action.passId)
        reminderActionOverrides = updated
        settingsRepository.setReminderActionsByPass(updated)
    }

    private suspend fun save(action: AppAction.SavePass) {
        passRepository.update(action.id, action.draft.toPassUpdate())
        passRepository.setNotes(action.id, action.draft.notes)
    }

    private fun handlePassMetadataAction(action: AppAction): Boolean = when (action) {
        is AppAction.SetPassFavorite -> {
            launchOperation(
                if (action.isFavorite) strings.resolve(R.string.message_pass_pinned)
                else strings.resolve(R.string.message_pass_unpinned),
            ) {
                passRepository.setPinned(action.id, action.isFavorite)
            }
            true
        }
        is AppAction.SetPassPinned -> {
            launchOperation(
                if (action.isPinned) strings.resolve(R.string.message_pass_pinned)
                else strings.resolve(R.string.message_pass_unpinned),
            ) {
                passRepository.setPinned(action.id, action.isPinned)
            }
            true
        }
        is AppAction.SetPassTags -> {
            launchOperation(strings.resolve(R.string.message_tags_updated)) { passRepository.setTags(action.id, action.tagIds) }
            true
        }
        is AppAction.SetPassNotes -> {
            launchOperation(strings.resolve(R.string.message_notes_updated)) { passRepository.setNotes(action.id, action.text) }
            true
        }
        is AppAction.RecordPassUse -> {
            // A display hook must stay silent and must not flash the busy indicator.
            viewModelScope.launch { runCatching { passRepository.recordUse(action.id) } }
            true
        }
        is AppAction.SetPassArchived -> {
            launchOperation(
                if (action.announce) {
                    if (action.isArchived) strings.resolve(R.string.message_pass_archived)
                    else strings.resolve(R.string.message_pass_restored)
                } else {
                    null
                },
            ) {
                passRepository.setArchived(action.id, action.isArchived)
            }
            true
        }
        is AppAction.SetPreferredArtwork -> {
            launchOperation(strings.resolve(R.string.message_pass_image_updated)) {
                passRepository.setPreferredArtwork(action.id, action.kind)
            }
            true
        }
        else -> false
    }

    private fun addCalendarEventsAfterImport(imported: List<PassSnapshot>) {
        if (!uiState.value.settings.offerCalendarAfterImport) return
        imported.asSequence().map(PassUiModel::from).mapNotNull(PassUiModel::calendarEvent).forEach { event ->
            check(platformActions.addToCalendarAutomatically(event)) { "No writable calendar is available" }
        }
    }

    private fun highlightImported(ids: List<String>) {
        if (ids.isEmpty()) return
        recentlyImportedIds.value = recentlyImportedIds.value + ids
        recentlyImportedClearJob?.cancel()
        recentlyImportedClearJob = viewModelScope.launch {
            delay(RECENT_IMPORT_HIGHLIGHT_MILLIS)
            recentlyImportedIds.value = emptySet()
        }
    }

    private fun withPass(id: String, action: (PassUiModel) -> Unit) {
        runCatching { action(uiState.value.passes.firstOrNull { it.id == id } ?: error("Pass not found")) }
            .onFailure { message.value = it.message ?: "Operation failed" }
    }

    private fun launchOperation(successMessage: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            runCatching { block() }
                .onSuccess { if (successMessage != null) message.value = successMessage }
                .onFailure { message.value = it.message ?: strings.resolve(R.string.message_operation_failed) }
            busy.value = false
        }
    }
}

private const val RECENT_IMPORT_HIGHLIGHT_MILLIS = 2_600L

private const val ESPASS_SHARE_MIME_TYPE = "application/vnd.espass-espass+zip"
private const val PKPASS_SHARE_MIME_TYPE = "application/vnd.apple.pkpass"

// prepareShare returns the retained original when the import wrote one, so the share keeps its file type.
internal fun shareMimeType(uri: Uri): String =
    if (uri.lastPathSegment.orEmpty().endsWith(".pkpass", ignoreCase = true)) {
        PKPASS_SHARE_MIME_TYPE
    } else {
        ESPASS_SHARE_MIME_TYPE
    }

private fun PassSortOrder.snapshotComparator(): Comparator<PassSnapshot> {
    val ascendingByDate = Comparator<PassSnapshot> { left, right ->
        compareNullable(left.sortDate(), right.sortDate())
    }
    return when (this) {
        PassSortOrder.DATE_ASC -> ascendingByDate
        PassSortOrder.DATE_DESC -> Comparator { left, right ->
            compareNullable(left.sortDate(), right.sortDate()) { first, second -> second.compareTo(first) }
        }
        PassSortOrder.TYPE -> compareBy<PassSnapshot> { it.type }.then(ascendingByDate)
        PassSortOrder.MOST_USED -> compareByDescending<PassSnapshot> { it.useCount }.thenBy(PassSnapshot::id)
        PassSortOrder.DATE_DIFF -> Comparator { left, right ->
            val now = LocalDateTime.now()
            val leftDistance = left.sortDate()?.let { Duration.between(now, it.toLocalDateTime()).abs() }
            val rightDistance = right.sortDate()?.let { Duration.between(now, it.toLocalDateTime()).abs() }
            compareNullable(leftDistance, rightDistance)
        }
        PassSortOrder.MANUAL -> compareBy(PassSnapshot::id)
    }
}

private fun List<PassSnapshot>.sortedForDisplay(
    sortOrder: PassSortOrder,
    manualOrder: List<String>,
): List<PassSnapshot> {
    if (sortOrder != PassSortOrder.MANUAL) return sortedWith(sortOrder.snapshotComparator())
    val positions = manualOrder.withIndex().associate { it.value to it.index }
    return sortedWith(compareBy<PassSnapshot> { positions[it.id] ?: Int.MAX_VALUE }.thenBy(PassSnapshot::id))
}

private fun List<PassCategory>.withLegacyCategories(passes: List<PassSnapshot>): List<PassCategory> {
    val knownIds = mapTo(mutableSetOf(), PassCategory::id)
    val legacy = (passes.flatMap { it.tagIds } + passes.map(PassSnapshot::categoryId))
        .distinct()
        .filter { it.isNotBlank() && it != DEFAULT_PASS_CATEGORY_ID && it != "trash" }
        .filterNot(knownIds::contains)
        .map { id ->
        PassCategory(
            id = id,
            name = id.replace('_', ' ').replaceFirstChar(Char::uppercase),
            colorArgb = legacyCategoryColor(id),
            role = PassCategoryRole.CUSTOM,
        )
    }
    return this + legacy
}

private fun legacyCategoryColor(id: String): Long {
    val palette = longArrayOf(0xFF006C4C, 0xFF6750A4, 0xFF8C4A60, 0xFF00658A, 0xFF765B00)
    return palette[(id.hashCode() and Int.MAX_VALUE) % palette.size]
}

private fun PassSnapshot.sortDate() = calendarTimeSpan?.from

private fun PassSnapshot.hasEndedWithoutArchive(now: Instant): Boolean =
    !isArchived && !isTrashed && normalizedTimeSpan()?.endsAt?.isBefore(now) == true

private fun PassDraft.toTimeSpan(): PassTimeSpanSnapshot? {
    val from = calendarStart.takeIf(String::isNotBlank)?.let {
        runCatching { org.threeten.bp.ZonedDateTime.parse(it) }.getOrElse { error("Invalid calendar start") }
    }
    val to = calendarEnd.takeIf(String::isNotBlank)?.let {
        runCatching { org.threeten.bp.ZonedDateTime.parse(it) }.getOrElse { error("Invalid calendar end") }
    }
    return if (from == null && to == null) null else PassTimeSpanSnapshot(from, to)
}

private fun PassDraft.toPassUpdate(): PassUpdate {
    org.ligi.passandroid.ui.compose.validatePassDraft(this)?.let { error("Invalid pass draft: ${it.name}") }
    return PassUpdate(
    description = description,
    creator = creator,
    type = type,
    accentColor = accentColor,
    barcodeFormat = barcodeFormat,
    barcodeMessage = barcodeMessage,
    barcodeAlternativeText = barcodeAlternativeText,
    fields = fields.map {
        org.ligi.passandroid.repository.PassFieldSnapshot(it.key, it.label, it.value, it.hidden, it.hint)
    },
    artworkUpdates = artworkUpdates.map { PassArtworkUpdate(it.kind, it.uri) },
    calendarTimeSpan = toTimeSpan(),
    locations = locations.map { location ->
        PassLocationSnapshot(
            name = location.name.ifBlank { null },
            latitude = location.latitude.toDoubleOrNull() ?: 0.0,
            longitude = location.longitude.toDoubleOrNull() ?: 0.0,
        )
    },
    )
}

private fun <T : Comparable<T>> compareNullable(
    left: T?,
    right: T?,
    comparePresent: (T, T) -> Int = Comparable<T>::compareTo,
): Int = when {
    left === right -> 0
    left == null -> 1
    right == null -> -1
    else -> comparePresent(left, right)
}
