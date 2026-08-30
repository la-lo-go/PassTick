package org.ligi.passandroid.ui.state

import android.net.Uri
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.functions.CalendarEvent
import org.ligi.passandroid.functions.DEFAULT_EVENT_LENGTH_IN_HOURS
import org.ligi.passandroid.platform.PlatformLocation
import org.ligi.passandroid.platform.PrintableField
import org.ligi.passandroid.platform.PrintablePass

data class PassFieldUiModel(
    val key: String?,
    val label: String,
    val value: String,
    val hidden: Boolean,
    val hint: String?,
)
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
    val calendarEvent: CalendarEvent?,
) {
    companion object {
        fun from(pass: PassSnapshot) = PassUiModel(
            id = pass.id,
            description = pass.description.orEmpty(),
            creator = pass.creator,
            type = pass.type,
            accentColor = pass.accentColor,
            barcodeFormat = pass.barcodeFormat,
            barcodeMessage = pass.barcodeMessage,
            barcodeAlternativeText = pass.barcodeAlternativeText,
            fields = pass.fields.map { PassFieldUiModel(it.key, it.label, it.value, it.hidden, it.hint) },
            locations = pass.locations.map { PassLocationUiModel(it.name, it.latitude, it.longitude) },
            calendarEvent = pass.calendarTimeSpan?.let { span ->
                val from = span.from ?: span.to?.minusHours(DEFAULT_EVENT_LENGTH_IN_HOURS)
                val to = span.to ?: span.from?.plusHours(DEFAULT_EVENT_LENGTH_IN_HOURS)
                if (from == null || to == null) null else CalendarEvent(
                    title = pass.description,
                    beginTimeMillis = from.toEpochSecond() * 1000,
                    endTimeMillis = to.toEpochSecond() * 1000,
                    location = pass.locations.firstOrNull()?.name,
                )
            },
        )
    }

    fun toPrintablePass() = PrintablePass(
        description = description,
        barcodeFormat = barcodeFormat,
        barcodeMessage = barcodeMessage,
        barcodeAlternativeText = barcodeAlternativeText,
        fields = fields.filterNot { it.hidden }.map { PrintableField(it.label, it.value) },
    )
}

fun PassLocationUiModel.toPlatformLocation() = PlatformLocation(latitude, longitude)

data class PassDraft(
    val description: String,
    val creator: String,
    val type: PassType,
    val accentColor: Int,
    val barcodeFormat: PassBarCodeFormat?,
    val barcodeMessage: String,
    val barcodeAlternativeText: String,
    val fields: List<PassFieldUiModel>,
)

data class MainUiState(
    val passes: List<PassUiModel> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isBusy: Boolean = false,
    val message: String? = null,
)

sealed interface AppAction {
    data class Import(val uri: Uri) : AppAction
    data class ImportFiles(val uris: List<Uri>) : AppAction
    data class Export(val id: String, val destination: Uri) : AppAction
    data class SharePass(val id: String) : AppAction
    data class PrintPass(val id: String) : AppAction
    data class AddToCalendar(val id: String) : AppAction
    data class OpenLocation(val id: String, val locationIndex: Int) : AppAction
    data class DeletePass(val id: String) : AppAction
    data class SavePass(val id: String, val draft: PassDraft) : AppAction
    data class SetTheme(val value: ThemeMode) : AppAction
    data class SetCondensedPasses(val value: Boolean) : AppAction
    data class SetAutomaticBrightness(val value: Boolean) : AppAction
    data class SetSortOrder(val value: PassSortOrder) : AppAction
    data object ClearMessage : AppAction
}
