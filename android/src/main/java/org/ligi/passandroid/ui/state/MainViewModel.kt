package org.ligi.passandroid.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.ligi.passandroid.platform.PlatformActions
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.repository.PassUpdate
import org.ligi.passandroid.repository.PassRepository
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.repository.PassArtworkUpdate
import org.ligi.passandroid.repository.PassLocationSnapshot
import org.ligi.passandroid.repository.PassTimeSpanSnapshot
import org.ligi.passandroid.repository.SettingsRepository
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime

class MainViewModel(
    private val passRepository: PassRepository,
    private val settingsRepository: SettingsRepository,
    private val platformActions: PlatformActions,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState = combine(passRepository.observePasses(), settingsRepository.settings, busy, message) {
            passes, settings, isBusy, currentMessage ->
        MainUiState(
            passes = passes.sortedWith(settings.sortOrder.snapshotComparator()).map(PassUiModel::from),
            settings = settings,
            isBusy = isBusy,
            message = currentMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun onAction(action: AppAction) {
        when (action) {
            is AppAction.Import -> launchOperation("Pass imported") { passRepository.import(action.uri).getOrThrow() }
            is AppAction.ImportFiles -> launchOperation("Passes imported") {
                action.uris.forEach { passRepository.import(it).getOrThrow() }
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
            is AppAction.SetTheme -> viewModelScope.launch { settingsRepository.setThemeMode(action.value) }
            is AppAction.SetCondensedPasses -> viewModelScope.launch { settingsRepository.setCondensedPasses(action.value) }
            is AppAction.SetAutomaticBrightness -> viewModelScope.launch {
                settingsRepository.setAutomaticBrightness(action.value)
            }
            is AppAction.SetSortOrder -> viewModelScope.launch { settingsRepository.setSortOrder(action.value) }
            AppAction.ClearMessage -> message.value = null
        }
    }

    private suspend fun save(action: AppAction.SavePass) {
        passRepository.update(
            action.id,
            PassUpdate(
                description = action.draft.description,
                creator = action.draft.creator,
                type = action.draft.type,
                accentColor = action.draft.accentColor,
                barcodeFormat = action.draft.barcodeFormat,
                barcodeMessage = action.draft.barcodeMessage,
                barcodeAlternativeText = action.draft.barcodeAlternativeText,
                fields = action.draft.fields.map {
                    org.ligi.passandroid.repository.PassFieldSnapshot(
                        it.key,
                        it.label,
                        it.value,
                        it.hidden,
                        it.hint,
                    )
                },
                artworkUpdates = action.draft.artworkUpdates.map { PassArtworkUpdate(it.kind, it.uri) },
                calendarTimeSpan = action.draft.toTimeSpan(),
                locations = action.draft.locations.map { location ->
                    PassLocationSnapshot(
                        name = location.name.ifBlank { null },
                        latitude = location.latitude.toDoubleOrNull() ?: error("Invalid location latitude"),
                        longitude = location.longitude.toDoubleOrNull() ?: error("Invalid location longitude"),
                    )
                },
            ),
        )
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
    }
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
