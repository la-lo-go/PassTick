package org.ligi.passandroid.ui.compose

import androidx.annotation.StringRes
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.ui.state.PassCodeDraft
import org.ligi.passandroid.ui.state.PassDraft
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationDraft
import org.ligi.passandroid.ui.state.PassLocationUiModel
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.displayableCodes
import org.threeten.bp.ZonedDateTime

/** The editor sections, in screen order. */
internal enum class EditorPart { BASICS, DATE, FIELDS, CODES, PLACES, IMAGES, NOTE }

internal enum class PassDraftIssue(@param:StringRes val messageRes: Int, val part: EditorPart) {
    DESCRIPTION_MISSING(R.string.edit_pass_validation_description, EditorPart.BASICS),
    BARCODE_MISSING(R.string.edit_pass_validation_barcode, EditorPart.CODES),
    CALENDAR_START_INVALID(R.string.edit_pass_validation_calendar_start, EditorPart.DATE),
    CALENDAR_END_INVALID(R.string.edit_pass_validation_calendar_end, EditorPart.DATE),
    CALENDAR_END_BEFORE_START(R.string.edit_pass_validation_calendar_order, EditorPart.DATE),
    LOCATION_COORDINATES_PARTIAL(R.string.edit_pass_validation_location_partial, EditorPart.PLACES),
    LOCATION_COORDINATES_INVALID(R.string.edit_pass_validation_location_invalid, EditorPart.PLACES),
    LATITUDE_RANGE(R.string.edit_pass_validation_latitude, EditorPart.PLACES),
    LONGITUDE_RANGE(R.string.edit_pass_validation_longitude, EditorPart.PLACES),
    LOCATION_EMPTY(R.string.edit_pass_validation_location_empty, EditorPart.PLACES),
    FIELD_EMPTY(R.string.edit_pass_validation_field_empty, EditorPart.FIELDS),
}

internal fun validatePassDraft(draft: PassDraft): PassDraftIssue? {
    validateDescription(draft)?.let { return it }
    validateBarcode(draft)?.let { return it }
    validateCalendar(draft)?.let { return it }
    validateLocations(draft.locations)?.let { return it }
    return validateFields(draft)
}

private fun validateDescription(draft: PassDraft): PassDraftIssue? =
    if (draft.description.isBlank()) PassDraftIssue.DESCRIPTION_MISSING else null

private fun validateBarcode(draft: PassDraft): PassDraftIssue? =
    if (draft.codes.any { it.message.isBlank() }) PassDraftIssue.BARCODE_MISSING else null

private fun validateCalendar(draft: PassDraft): PassDraftIssue? {
    val start = parseDraftDate(draft.calendarStart)
    val end = parseDraftDate(draft.calendarEnd)
    if (draft.calendarStart.isNotBlank() && start == null) return PassDraftIssue.CALENDAR_START_INVALID
    if (draft.calendarEnd.isNotBlank() && end == null) return PassDraftIssue.CALENDAR_END_INVALID
    if (start != null && end != null && end.isBefore(start)) return PassDraftIssue.CALENDAR_END_BEFORE_START
    return null
}

internal fun parseDraftDate(value: String): ZonedDateTime? =
    value.takeIf(String::isNotBlank)?.let { runCatching { ZonedDateTime.parse(it) }.getOrNull() }

private fun validateLocations(locations: List<PassLocationDraft>): PassDraftIssue? {
    locations.forEach { location ->
        validateLocation(location)?.let { return it }
    }
    return null
}

private fun validateLocation(location: PassLocationDraft): PassDraftIssue? {
    val hasLatitude = location.latitude.isNotBlank()
    val hasLongitude = location.longitude.isNotBlank()
    if (hasLatitude != hasLongitude) return PassDraftIssue.LOCATION_COORDINATES_PARTIAL
    val latitude = location.latitude.takeIf(String::isNotBlank)?.toDoubleOrNull()
    val longitude = location.longitude.takeIf(String::isNotBlank)?.toDoubleOrNull()
    if (hasLatitude && (latitude == null || longitude == null)) return PassDraftIssue.LOCATION_COORDINATES_INVALID
    if (latitude != null && latitude !in -90.0..90.0) return PassDraftIssue.LATITUDE_RANGE
    if (longitude != null && longitude !in -180.0..180.0) return PassDraftIssue.LONGITUDE_RANGE
    if (location.name.isBlank() && !hasLatitude) return PassDraftIssue.LOCATION_EMPTY
    return null
}

private fun validateFields(draft: PassDraft): PassDraftIssue? =
    if (draft.fields.any { it.label.isBlank() && it.value.isBlank() }) PassDraftIssue.FIELD_EMPTY else null

/** Where a field shows on the ticket. Each slot stores as the pass field hint that the ticket layout reads. */
internal enum class FieldSlot(@param:StringRes val label: Int) {
    HEADER(R.string.edit_pass_slot_header),
    MAIN(R.string.edit_pass_slot_main),
    DETAILS(R.string.edit_pass_slot_details),
    BACK(R.string.edit_pass_slot_back),
}

private const val HEADER_HINT = "headerFields"
private const val PRIMARY_HINT = "primaryFields"
private const val SECONDARY_HINT = "secondaryFields"
private const val AUXILIARY_HINT = "auxiliaryFields"
private const val BACK_HINT = "backFields"

internal val PassFieldUiModel.slot: FieldSlot
    get() = when {
        hidden -> FieldSlot.BACK
        hint == HEADER_HINT -> FieldSlot.HEADER
        hint == PRIMARY_HINT -> FieldSlot.MAIN
        else -> FieldSlot.DETAILS
    }

internal fun PassFieldUiModel.withSlot(slot: FieldSlot): PassFieldUiModel = when (slot) {
    FieldSlot.HEADER -> copy(hidden = false, hint = HEADER_HINT)
    FieldSlot.MAIN -> copy(hidden = false, hint = PRIMARY_HINT)
    // Details keeps the secondary or auxiliary row that an imported field already has.
    FieldSlot.DETAILS -> copy(hidden = false, hint = hint.takeIf { it == SECONDARY_HINT || it == AUXILIARY_HINT } ?: SECONDARY_HINT)
    FieldSlot.BACK -> copy(hidden = true, hint = BACK_HINT)
}

/** The types the editor offers. The legacy boarding type stays as it is and shows as boarding. */
internal val EditorPassTypes = listOf(
    PassType.BOARDING,
    PassType.EVENT,
    PassType.COUPON,
    PassType.VOUCHER,
    PassType.LOYALTY,
    PassType.GENERIC,
)

internal fun PassType.editorChoice(): PassType = if (this == PassType.PKBOARDING) PassType.BOARDING else this

/** Code format names as their standards write them. They are product names, so they are not translated. */
internal fun PassBarCodeFormat.displayName(): String = when (this) {
    PassBarCodeFormat.QR_CODE -> "QR code"
    PassBarCodeFormat.AZTEC -> "Aztec"
    PassBarCodeFormat.DATA_MATRIX -> "Data Matrix"
    PassBarCodeFormat.PDF_417 -> "PDF417"
    PassBarCodeFormat.CODE_128 -> "Code 128"
    PassBarCodeFormat.CODE_39 -> "Code 39"
    PassBarCodeFormat.CODE_93 -> "Code 93"
    PassBarCodeFormat.EAN_13 -> "EAN-13"
    PassBarCodeFormat.EAN_8 -> "EAN-8"
    PassBarCodeFormat.UPC_A -> "UPC-A"
    PassBarCodeFormat.UPC_E -> "UPC-E"
    PassBarCodeFormat.ITF -> "ITF"
    PassBarCodeFormat.CODABAR -> "Codabar"
}

/** Square codes first: they are the common ticket codes. */
internal val EditorCodeFormats = PassBarCodeFormat.entries.sortedBy { if (it.isQuadratic()) 0 else 1 }

// An address-only place stores 0, 0 as its coordinates. The editor shows no coordinates for it.
private fun PassLocationUiModel.isAddressOnly() = latitude == 0.0 && longitude == 0.0 && !name.isNullOrBlank()

internal fun PassUiModel.toEditableDraft(): PassDraft = PassDraft(
    description = description,
    creator = creator.orEmpty(),
    type = type,
    accentColor = accentColor,
    fields = fields,
    codes = displayableCodes().map { PassCodeDraft(it.format, it.message, it.alternativeText.orEmpty()) },
    calendarStart = calendarTimeSpan?.from?.toString().orEmpty(),
    calendarEnd = calendarTimeSpan?.to?.toString().orEmpty(),
    locations = locations.map {
        PassLocationDraft(
            name = it.name.orEmpty(),
            latitude = if (it.isAddressOnly()) "" else it.latitude.toString(),
            longitude = if (it.isAddressOnly()) "" else it.longitude.toString(),
        )
    },
    notes = notes,
)

internal val DEFAULT_PASS_ACCENT_COLOR = 0xFF3D73E9.toInt()

internal fun emptyPassDraft() = PassDraft(
    description = "",
    creator = "",
    type = PassType.EVENT,
    accentColor = DEFAULT_PASS_ACCENT_COLOR,
    fields = emptyList(),
)
