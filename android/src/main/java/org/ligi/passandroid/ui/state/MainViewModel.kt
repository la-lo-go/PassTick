package org.ligi.passandroid.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ligi.passandroid.platform.PlatformActions
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.repository.PassUpdate
import org.ligi.passandroid.repository.PassRepository
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.repository.PassArtworkUpdate
import org.ligi.passandroid.repository.PassLocationSnapshot
import org.ligi.passandroid.repository.PassTimeSpanSnapshot
import org.ligi.passandroid.repository.DEFAULT_PASS_CATEGORY_ID
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.repository.SettingsRepository
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.ligi.passandroid.domain.timeline.EventTemporalState
import org.ligi.passandroid.domain.timeline.buildPassTimeline
import org.ligi.passandroid.reminder.ReminderScheduler
import org.ligi.passandroid.reminder.buildPassReminders
import org.ligi.passandroid.reminder.PassReminderOverride
import org.ligi.passandroid.widget.PassWidgetSnapshotPublisher

class MainViewModel(
    private val passRepository: PassRepository,
    private val settingsRepository: SettingsRepository,
    private val platformActions: PlatformActions,
    private val reminderScheduler: ReminderScheduler = ReminderScheduler.None,
    private val widgetPublisher: PassWidgetSnapshotPublisher? = null,
) : ViewModel() {
    suspend fun isCalendarEventPresent(pass: PassUiModel): Boolean = withContext(Dispatchers.IO) {
        pass.calendarEvent?.let(platformActions::isCalendarEventPresent) == true
    }

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val categoryMoves = Channel<AppAction.MovePass>(Channel.UNLIMITED)
    private val passes = passRepository.observePasses()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState = combine(passes, settingsRepository.settings, busy, message, selectedCategoryId) {
            passes, settings, isBusy, currentMessage, requestedCategoryId ->
        val categories = settings.categories.withLegacyCategories(passes)
        val timeline = buildPassTimeline(passes, Instant.now(), ZoneId.systemDefault())
        MainUiState(
            passes = passes.sortedForDisplay(settings.sortOrder, settings.passOrder).map(PassUiModel::from),
            settings = settings,
            isContentLoading = false,
            isBusy = isBusy,
            message = currentMessage,
            categories = categories,
            selectedCategoryId = requestedCategoryId?.takeIf { requested -> categories.any { it.id == requested } },
            timeline = timeline,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            for (move in categoryMoves) {
                runCatching { passRepository.moveToCategory(move.id, move.categoryId) }
                    .onSuccess { if (move.announce) message.value = "Pass moved" }
                    .onFailure { message.value = it.message ?: "Operation failed" }
            }
        }
        viewModelScope.launch {
            combine(passes, settingsRepository.settings) { currentPasses, settings -> currentPasses to settings }
                .collectLatest { (currentPasses, settings) ->
                    val now = Instant.now()
                    val timeline = buildPassTimeline(currentPasses, now, ZoneId.systemDefault())
                    if (settings.automaticallyMarkPast) {
                        val pastCategoryId = settings.categories.firstOrNull { it.role == PassCategoryRole.PAST }?.id
                        if (pastCategoryId != null) {
                            val excludedRoles = setOf(
                                PassCategoryRole.PAST,
                                PassCategoryRole.ARCHIVE,
                                PassCategoryRole.TRASH,
                            )
                            val excludedIds = settings.categories.filter { it.role in excludedRoles }
                                .mapTo(mutableSetOf()) { it.id }
                            val pastPassIds = timeline.days.flatMap { it.events }
                                .filter { it.temporalState == EventTemporalState.PAST }
                                .mapTo(mutableSetOf()) { it.pass.passId }
                            currentPasses.filter { it.id in pastPassIds && it.categoryId !in excludedIds }
                                .forEach { pass -> passRepository.moveToCategory(pass.id, pastCategoryId) }
                        }
                    }
                    reminderScheduler.sync(
                        if (settings.remindersEnabled) {
                            val overrides = buildMap {
                                settings.reminderExcludedPassIds.forEach { put(it, PassReminderOverride.Disabled) }
                                settings.reminderLeadMinutesByPass.forEach { (passId, minutes) ->
                                    put(passId, PassReminderOverride.LeadTime(minutes))
                                }
                            }
                            buildPassReminders(timeline, now, settings.reminderMinutes, overrides)
                        } else {
                            emptyList()
                        },
                    )
                    val widgetExcludedIds = settings.categories
                        .filter { it.role == PassCategoryRole.ARCHIVE || it.role == PassCategoryRole.TRASH }
                        .mapTo(mutableSetOf()) { it.id }
                    runCatching {
                        widgetPublisher?.publish(currentPasses, widgetExcludedIds)
                    }
                }
        }
    }

    fun onAction(action: AppAction) {
        when (action) {
            is AppAction.Import -> launchOperation("Pass imported") {
                val imported = passRepository.import(action.uri).getOrThrow()
                addCalendarEventsAfterImport(listOf(imported))
            }
            is AppAction.ImportFiles -> launchOperation("Passes imported") {
                val imported = action.uris.map { passRepository.import(it).getOrThrow() }
                addCalendarEventsAfterImport(imported)
            }
            is AppAction.Export -> launchOperation("Pass exported") {
                passRepository.export(action.id, action.destination).getOrThrow()
            }
            is AppAction.SharePass -> launchOperation("Pass ready to share") {
                val uri = passRepository.prepareShare(action.id).getOrThrow()
                platformActions.share(uri, "application/vnd.espass-espass+zip")
            }
            is AppAction.PrintPass -> withPass(action.id) { platformActions.print(it.toPrintablePass()) }
            is AppAction.AddToCalendar -> withPass(action.id) { pass ->
                pass.calendarEvent?.let(platformActions::addToCalendar) ?: error("Pass has no date")
            }
            is AppAction.OpenLocation -> withPass(action.id) { pass ->
                val location = pass.locations.getOrNull(action.locationIndex) ?: error("Location not found")
                platformActions.openLocation(location.toPlatformLocation())
            }
            is AppAction.DeletePass -> launchOperation("Pass deleted") { check(passRepository.delete(action.id)) }
            is AppAction.SavePass -> launchOperation("Pass saved") { save(action) }
            is AppAction.MovePass -> check(categoryMoves.trySend(action).isSuccess) { "Pass move queue is closed" }
            is AppAction.SelectCategory -> selectedCategoryId.value = action.categoryId
            is AppAction.SaveCategory -> viewModelScope.launch {
                val categories = uiState.value.settings.categories
                settingsRepository.setCategories(
                    categories.filterNot { it.id == action.category.id } + action.category,
                )
            }
            is AppAction.DeleteCategory -> launchOperation("Category deleted") {
                val category = uiState.value.settings.categories.firstOrNull { it.id == action.categoryId }
                    ?: error("Category not found")
                require(category.role == PassCategoryRole.CUSTOM) { "Built-in categories cannot be deleted" }
                val inboxId = uiState.value.settings.categories.first { it.role == PassCategoryRole.INBOX }.id
                uiState.value.passes.filter { it.categoryId == action.categoryId }.forEach { pass ->
                    passRepository.moveToCategory(pass.id, inboxId)
                }
                settingsRepository.setCategories(
                    uiState.value.settings.categories.filterNot { it.id == action.categoryId },
                )
            }
            is AppAction.MoveCategory -> viewModelScope.launch {
                val categories = uiState.value.settings.categories.toMutableList()
                val from = categories.indexOfFirst { it.id == action.categoryId }
                val to = (from + action.offset).coerceIn(categories.indices)
                if (from >= 0 && from != to) {
                    val category = categories.removeAt(from)
                    categories.add(to, category)
                    settingsRepository.setCategories(categories)
                }
            }
            is AppAction.SetTheme -> viewModelScope.launch { settingsRepository.setThemeMode(action.value) }
            is AppAction.SetAmoledBlackBackground -> viewModelScope.launch {
                settingsRepository.setAmoledBlackBackground(action.value)
            }
            is AppAction.SetAutomaticBrightness -> viewModelScope.launch {
                settingsRepository.setAutomaticBrightness(action.value)
            }
            is AppAction.SetSortOrder -> viewModelScope.launch { settingsRepository.setSortOrder(action.value) }
            is AppAction.ReorderPass -> viewModelScope.launch {
                val currentIds = uiState.value.passes.map(PassUiModel::id).toMutableList()
                val from = currentIds.indexOf(action.passId)
                val to = (from + action.offset).coerceIn(currentIds.indices)
                if (from >= 0 && from != to) {
                    currentIds.add(to, currentIds.removeAt(from))
                    settingsRepository.setPassOrder(currentIds)
                    settingsRepository.setSortOrder(PassSortOrder.MANUAL)
                }
            }
            is AppAction.SetHighlightTodayPasses -> viewModelScope.launch {
                settingsRepository.setHighlightTodayPasses(action.value)
            }
            is AppAction.SetAutomaticallyMarkPast -> viewModelScope.launch {
                settingsRepository.setAutomaticallyMarkPast(action.value)
            }
            is AppAction.SetOfferCalendarAfterImport -> viewModelScope.launch {
                settingsRepository.setOfferCalendarAfterImport(action.value)
            }
            is AppAction.SetRemindersEnabled -> viewModelScope.launch {
                settingsRepository.setRemindersEnabled(action.value)
            }
            is AppAction.SetReminderMinutes -> viewModelScope.launch {
                settingsRepository.setReminderMinutes(action.value)
            }
            is AppAction.MovePassDetailSection -> viewModelScope.launch {
                settingsRepository.movePassDetailSection(action.section, action.offset)
            }
            is AppAction.SetPassDetailSectionVisible -> viewModelScope.launch {
                settingsRepository.setPassDetailSectionVisible(action.section, action.visible)
            }
            is AppAction.MoveHomeCardSection -> viewModelScope.launch {
                settingsRepository.moveHomeCardSection(action.section, action.offset)
            }
            is AppAction.SetHomeCardSectionVisible -> viewModelScope.launch {
                settingsRepository.setHomeCardSectionVisible(action.section, action.visible)
            }
            is AppAction.TogglePassReminder -> viewModelScope.launch {
                val excluded = uiState.value.settings.reminderExcludedPassIds.toMutableSet()
                if (!excluded.add(action.passId)) excluded.remove(action.passId)
                settingsRepository.setReminderExcludedPassIds(excluded)
            }
            is AppAction.ConfigurePassReminder -> viewModelScope.launch {
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
            }
            AppAction.ClearMessage -> message.value = null
        }
    }

    private suspend fun save(action: AppAction.SavePass) {
        passRepository.update(action.id, action.draft.toPassUpdate())
    }

    private fun addCalendarEventsAfterImport(imported: List<PassSnapshot>) {
        if (!uiState.value.settings.offerCalendarAfterImport) return
        imported.asSequence().map(PassUiModel::from).mapNotNull(PassUiModel::calendarEvent).forEach { event ->
            check(platformActions.addToCalendarAutomatically(event)) { "No writable calendar is available" }
        }
    }

    private fun withPass(id: String, action: (PassUiModel) -> Unit) {
        runCatching { action(uiState.value.passes.firstOrNull { it.id == id } ?: error("Pass not found")) }
            .onFailure { message.value = it.message ?: "Operation failed" }
    }

    private fun launchOperation(successMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            runCatching { block() }
                .onSuccess { message.value = successMessage }
                .onFailure { message.value = it.message ?: "Operation failed" }
            busy.value = false
        }
    }
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
    val legacy = passes.map(PassSnapshot::categoryId).distinct().filterNot(knownIds::contains).map { id ->
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
    org.ligi.passandroid.ui.compose.validatePassDraft(this)?.let(::error)
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
