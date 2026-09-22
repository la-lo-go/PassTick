package org.ligi.passandroid.ui.state

import android.net.Uri
import org.ligi.passandroid.imports.ImportSource
import org.ligi.passandroid.imports.NormalizedRect
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ColorStyle
import org.ligi.passandroid.repository.PassImageExportOptions
import org.ligi.passandroid.repository.PassDetailSection
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.DEFAULT_PASS_CATEGORY_ID
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.functions.CalendarEvent
import org.ligi.passandroid.functions.DEFAULT_EVENT_LENGTH_IN_HOURS
import org.ligi.passandroid.platform.PlatformLocation
import org.threeten.bp.ZonedDateTime
import org.ligi.passandroid.domain.timeline.PassTimeline
import org.ligi.passandroid.navigation.passDeepLink

data class PassFieldUiModel(
    val key: String?,
    val label: String,
    val value: String,
    val hidden: Boolean,
    val hint: String?,
)
data class PassLocationUiModel(val name: String?, val latitude: Double, val longitude: Double)
data class PassLocationDraft(val name: String, val latitude: String, val longitude: String)
data class PassTimeSpanUiModel(val from: ZonedDateTime?, val to: ZonedDateTime?)
data class PassArtworkUiModel(val kind: PassArtworkKind, val bytes: ByteArray)
data class PassArtworkDraft(val kind: PassArtworkKind, val uri: Uri)
data class PassBarcodeUiModel(
    val format: PassBarCodeFormat?,
    val message: String?,
    val alternativeText: String?,
)
data class DetectedCodeUiModel(val format: PassBarCodeFormat, val message: String)

data class ImportReviewUiState(
    val draftId: String,
    val source: ImportSource,
    val title: String,
    val accentColor: Int,
    val suggestedAccentColor: Int,
    val pageCount: Int,
    val previewPng: ByteArray,
    val displayPng: ByteArray = previewPng,
    val detectedCodes: List<DetectedCodeUiModel>,
    val selectedCodeIndices: Set<Int>,
    val rotationDegrees: Int = 0,
    val crop: NormalizedRect = NormalizedRect.Full,
    val cropEditing: Boolean = false,
    val isBusy: Boolean = false,
)

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
    val artwork: List<PassArtworkUiModel> = emptyList(),
    val calendarTimeSpan: PassTimeSpanUiModel? = null,
    val categoryId: String = DEFAULT_PASS_CATEGORY_ID,
    val isProtected: Boolean = false,
    val isFavorite: Boolean = false,
    val tagIds: Set<String> = emptySet(),
    val isArchived: Boolean = false,
    val preferredArtworkKind: PassArtworkKind? = null,
    val trashedAtEpochMillis: Long? = null,
    val notes: String = "",
    val importSource: ImportSource? = null,
    val hasDocument: Boolean = false,
    val documentPageCount: Int = 0,
    val barcodes: List<PassBarcodeUiModel> = emptyList(),
) {
    val isPinned: Boolean get() = isFavorite

    val isDocumentPass: Boolean get() = importSource != null || hasDocument

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
                    description = "Open pass: ${passDeepLink(pass.id)}",
                )
            },
            artwork = pass.artwork.map { PassArtworkUiModel(it.kind, it.bytes) },
            calendarTimeSpan = pass.calendarTimeSpan?.let { PassTimeSpanUiModel(it.from, it.to) },
            categoryId = pass.categoryId,
            isProtected = pass.isProtected,
            isFavorite = pass.isFavorite,
            tagIds = pass.tagIds,
            isArchived = pass.isArchived,
            preferredArtworkKind = pass.preferredArtworkKind,
            trashedAtEpochMillis = pass.trashedAtEpochMillis,
            notes = pass.notes,
            importSource = pass.importSource,
            hasDocument = pass.hasDocument,
            documentPageCount = pass.documentPageCount,
            barcodes = pass.barcodes.map { PassBarcodeUiModel(it.format, it.message, it.alternativeText) },
        )
    }

    fun homeCardDetail(): String? = fields.asSequence()
        .filter { !it.hidden && it.hint == "primaryFields" }
        .map { it.value.trim() }
        .firstOrNull { it.isNotEmpty() && !it.equals(description.trim(), ignoreCase = true) }

    fun homeCardInitial(): String = fields.asSequence()
        .filter { !it.hidden && it.hint == "primaryFields" }
        .map { it.value.trim() }
        .firstOrNull { it.isNotEmpty() }
        ?.first()
        ?.uppercaseChar()
        ?.toString()
        ?: description.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
}

fun PassUiModel.displayArtwork(defaultKinds: List<PassArtworkKind>): PassArtworkUiModel? =
    preferredArtworkKind?.let { selected -> artwork.firstOrNull { it.kind == selected } }
        ?: defaultKinds.firstNotNullOfOrNull { kind -> artwork.firstOrNull { it.kind == kind } }

fun PassLocationUiModel.toPlatformLocation() = PlatformLocation(
    address = name,
    latitude = latitude.takeUnless { it == 0.0 && longitude == 0.0 && !name.isNullOrBlank() },
    longitude = longitude.takeUnless { latitude == 0.0 && it == 0.0 && !name.isNullOrBlank() },
)

data class PassDraft(
    val description: String,
    val creator: String,
    val type: PassType,
    val accentColor: Int,
    val barcodeFormat: PassBarCodeFormat?,
    val barcodeMessage: String,
    val barcodeAlternativeText: String,
    val fields: List<PassFieldUiModel>,
    val artworkUpdates: List<PassArtworkDraft> = emptyList(),
    val calendarStart: String = "",
    val calendarEnd: String = "",
    val locations: List<PassLocationDraft> = emptyList(),
    val notes: String = "",
)

const val PROTECTED_PASSES_CATEGORY_ID = "protected"
const val PINNED_PASSES_CATEGORY_ID = "pinned"
const val ARCHIVED_PASSES_CATEGORY_ID = "archived"
const val TRASHED_PASSES_CATEGORY_ID = "trashed"

data class MainUiState(
    val passes: List<PassUiModel> = emptyList(),
    val trashedPasses: List<PassUiModel> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isContentLoading: Boolean = true,
    val isBusy: Boolean = false,
    val message: String? = null,
    val categories: List<PassCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val timeline: PassTimeline = PassTimeline.empty(),
    val systemAccentColor: Long? = null,
    val importReview: ImportReviewUiState? = null,
    val isPreparingImport: Boolean = false,
    /** Passes imported in the last moments; the home list animates them into view. */
    val recentlyImportedIds: Set<String> = emptySet(),
)

sealed interface AppAction {
    data class Import(val uri: Uri) : AppAction
    data class ImportFiles(val uris: List<Uri>) : AppAction
    data class Export(val id: String, val destination: Uri) : AppAction
    data class ExportArchive(val destination: Uri) : AppAction
    data class ImportArchive(val source: Uri) : AppAction
    data class SharePass(val id: String) : AppAction
    data class ShareImage(val id: String, val options: PassImageExportOptions) : AppAction
    data class PrintImage(val id: String, val options: PassImageExportOptions) : AppAction
    data class AddToCalendar(val id: String) : AppAction
    data class OpenLocation(val id: String, val locationIndex: Int) : AppAction
    data class OpenUrl(val url: String) : AppAction
    data class DeletePass(val id: String) : AppAction
    data class TrashPass(val id: String) : AppAction
    data class RestoreFromTrash(val id: String) : AppAction
    data class DeleteForever(val id: String) : AppAction
    data object EmptyTrash : AppAction
    data class SetPassPendingDeletion(val id: String, val pending: Boolean) : AppAction
    data class SetPassProtected(val id: String, val isProtected: Boolean) : AppAction
    data class SetPassFavorite(val id: String, val isFavorite: Boolean) : AppAction
    data class SetPassPinned(val id: String, val isPinned: Boolean) : AppAction
    data class SetPassTags(val id: String, val tagIds: Set<String>) : AppAction
    data class SetPassNotes(val id: String, val text: String) : AppAction
    data class SetPassArchived(val id: String, val isArchived: Boolean, val announce: Boolean = true) : AppAction
    data class SetPreferredArtwork(val id: String, val kind: PassArtworkKind?) : AppAction
    data class SavePass(val id: String, val draft: PassDraft) : AppAction
    data class DuplicatePass(val id: String) : AppAction
    data class MovePass(val id: String, val categoryId: String, val announce: Boolean = true) : AppAction
    data class SelectCategory(val categoryId: String?) : AppAction
    data class SaveCategory(val category: PassCategory) : AppAction
    data class DeleteCategory(val categoryId: String) : AppAction
    data class MoveCategory(val categoryId: String, val offset: Int) : AppAction
    data class SetTheme(val value: ThemeMode) : AppAction
    data class SetAmoledBlackBackground(val value: Boolean) : AppAction
    data class SetDynamicColors(val value: Boolean) : AppAction
    data class SetAccentColor(val value: Long?) : AppAction
    data class SetColorStyle(val value: ColorStyle) : AppAction
    data class SetAutomaticBrightness(val value: Boolean) : AppAction
    data class SetSortOrder(val value: PassSortOrder) : AppAction
    data class ReorderPass(val orderedVisibleIds: List<String>) : AppAction
    data class SetHighlightTodayPasses(val value: Boolean) : AppAction
    data class SetAutomaticallyMarkPast(val value: Boolean) : AppAction
    data class SetOfferCalendarAfterImport(val value: Boolean) : AppAction
    data class SetRemindersEnabled(val value: Boolean) : AppAction
    data class SetReminderMinutes(val value: Set<Int>) : AppAction
    data class SetNotificationAccessWindow(val minutes: Int) : AppAction
    data class SetNotificationExactTiming(val value: Boolean) : AppAction
    data class SetNotificationActionsEnabled(val value: Boolean) : AppAction
    data class SetLockAllPasses(val value: Boolean) : AppAction
    data class SetShowProtectedPassLockIcon(val value: Boolean) : AppAction
    data class SetBlurProtectedPassCards(val value: Boolean) : AppAction
    data class SetSeparateProtectedPasses(val value: Boolean) : AppAction
    data class SetBlockScreenshots(val value: Boolean) : AppAction
    data class SetTrashEnabled(val value: Boolean) : AppAction
    data class SetImageExportOptions(val value: PassImageExportOptions) : AppAction
    data class MovePassDetailSection(val section: PassDetailSection, val offset: Int) : AppAction
    data class SetPassDetailSectionVisible(val section: PassDetailSection, val visible: Boolean) : AppAction
    data class MoveHomeCardSection(val section: HomeCardSection, val offset: Int) : AppAction
    data class SetHomeCardSectionVisible(val section: HomeCardSection, val visible: Boolean) : AppAction
    data class TogglePassReminder(val passId: String) : AppAction
    data class ConfigurePassReminder(
        val passId: String,
        val enabled: Boolean,
        val leadMinutes: Int?,
        val exactAtEvent: Boolean = false,
    ) : AppAction
    data class SetPassReminderActions(
        val passId: String,
        val actions: Set<org.ligi.passandroid.reminder.NotificationAction>?,
    ) : AppAction
    data object ClearMessage : AppAction
}
