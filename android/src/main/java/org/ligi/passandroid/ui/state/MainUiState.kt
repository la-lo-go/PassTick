package org.ligi.passandroid.ui.state

import android.net.Uri
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode

data class PassFieldUiModel(val label: String, val value: String, val hidden: Boolean)
data class PassLocationUiModel(val name: String?, val latitude: Double, val longitude: Double)

data class PassUiModel(
    val id: String,
    val description: String,
    val creator: String?,
    val type: PassType,
    val accentColor: Int,
    val barcodeFormat: PassBarCodeFormat?,
    val barcodeMessage: String?,
    val barcodeAlternativeText: String?,
    val fields: List<PassFieldUiModel>,
    val locations: List<PassLocationUiModel>,
    val hasCalendarEntry: Boolean,
) {
    companion object {
        fun from(pass: Pass) = PassUiModel(
            id = pass.id,
            description = pass.description.orEmpty(),
            creator = pass.creator,
            type = pass.type,
            accentColor = pass.accentColor,
            barcodeFormat = pass.barCode?.format,
            barcodeMessage = pass.barCode?.message,
            barcodeAlternativeText = pass.barCode?.alternativeText,
            fields = pass.fields.map { PassFieldUiModel(it.label.orEmpty(), it.value.orEmpty(), it.hide) },
            locations = pass.locations.map { PassLocationUiModel(it.name, it.lat, it.lon) },
            hasCalendarEntry = pass.calendarTimespan != null || !pass.validTimespans.isNullOrEmpty(),
        )
    }
}

data class PassDraft(
    val description: String,
    val creator: String,
    val barcodeFormat: PassBarCodeFormat?,
    val barcodeMessage: String,
    val barcodeAlternativeText: String,
)

data class MainUiState(
    val passes: List<PassUiModel> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isBusy: Boolean = false,
    val message: String? = null,
)

sealed interface AppAction {
    data class Import(val uri: Uri) : AppAction
    data class Export(val id: String, val destination: Uri) : AppAction
    data class DeletePass(val id: String) : AppAction
    data class SavePass(val id: String, val draft: PassDraft) : AppAction
    data class SetTheme(val value: ThemeMode) : AppAction
    data class SetCondensedPasses(val value: Boolean) : AppAction
    data class SetAutomaticBrightness(val value: Boolean) : AppAction
    data class SetSortOrder(val value: PassSortOrder) : AppAction
    data object ClearMessage : AppAction
}
