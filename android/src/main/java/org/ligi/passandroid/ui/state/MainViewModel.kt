package org.ligi.passandroid.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.repository.PassRepository
import org.ligi.passandroid.repository.SettingsRepository

class MainViewModel(
    private val passRepository: PassRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState = combine(passRepository.observePasses(), settingsRepository.settings, busy, message) {
            passes, settings, isBusy, currentMessage ->
        MainUiState(
            passes = passes.sortedWith(settings.sortOrder.toComparator()).map(PassUiModel::from),
            settings = settings,
            isBusy = isBusy,
            message = currentMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun onAction(action: AppAction) {
        when (action) {
            is AppAction.Import -> launchOperation("Pass imported") { passRepository.import(action.uri).getOrThrow() }
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
        val pass = passRepository.find(action.id) as? PassImpl ?: error("Pass not found")
        pass.description = action.draft.description
        pass.creator = action.draft.creator
        pass.barCode = action.draft.barcodeFormat?.let { format ->
            BarCode(format, action.draft.barcodeMessage).apply {
                alternativeText = action.draft.barcodeAlternativeText.ifBlank { null }
            }
        }
        passRepository.save(pass)
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
