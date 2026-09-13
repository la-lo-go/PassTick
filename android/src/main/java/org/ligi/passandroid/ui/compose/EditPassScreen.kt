package org.ligi.passandroid.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.navigation.PassDateField
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.PassDraft
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationDraft
import org.ligi.passandroid.ui.state.PassUiModel
import org.threeten.bp.ZonedDateTime
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import org.ligi.passandroid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPassScreen(pass: PassUiModel?, initialDateField: PassDateField? = null, onAction: (EditPassAction) -> Unit) {
    var description by remember(pass?.id) { mutableStateOf(pass?.description.orEmpty()) }
    var creator by remember(pass?.id) { mutableStateOf(pass?.creator.orEmpty()) }
    var passType by remember(pass?.id) { mutableStateOf(pass?.type ?: PassType.EVENT) }
    var accentColor by remember(pass?.id) { mutableIntStateOf(pass?.accentColor ?: 0xFF3D73E9.toInt()) }
    var barcodeFormat by remember(pass?.id) { mutableStateOf(pass?.barcodeFormat) }
    var barcodeMessage by remember(pass?.id) { mutableStateOf(pass?.barcodeMessage.orEmpty()) }
    var alternativeText by remember(pass?.id) { mutableStateOf(pass?.barcodeAlternativeText.orEmpty()) }
    var fields by remember(pass?.id) { mutableStateOf(pass?.fields.orEmpty()) }
    var calendarStart by remember(pass?.id) { mutableStateOf(pass?.calendarTimeSpan?.from) }
    var calendarEnd by remember(pass?.id) { mutableStateOf(pass?.calendarTimeSpan?.to) }
    var locations by remember(pass?.id) {
        mutableStateOf(
            pass?.locations.orEmpty().map {
                val coordinatesAreAddressSentinel = it.latitude == 0.0 && it.longitude == 0.0 && !it.name.isNullOrBlank()
                PassLocationDraft(
                    it.name.orEmpty(),
                    if (coordinatesAreAddressSentinel) "" else it.latitude.toString(),
                    if (coordinatesAreAddressSentinel) "" else it.longitude.toString(),
                )
            },
        )
    }
    var typeMenuOpen by remember { mutableStateOf(false) }
    var barcodeMenuOpen by remember { mutableStateOf(false) }
    var editorMenuOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val hapticFeedback = LocalHapticFeedback.current
    fun currentDraft() = PassDraft(
        description = description,
        creator = creator,
        type = passType,
        accentColor = accentColor,
        barcodeFormat = barcodeFormat,
        barcodeMessage = barcodeMessage,
        barcodeAlternativeText = alternativeText,
        fields = fields,
        calendarStart = calendarStart?.toString().orEmpty(),
        calendarEnd = calendarEnd?.toString().orEmpty(),
        locations = locations,
    )

    fun saveAndClose() {
        val draft = currentDraft()
        val issue = validatePassDraft(draft)
        if (issue == null) {
            val initialDraft = pass?.toEditableDraft()
            if (draft == initialDraft) {
                onAction(EditPassAction.Back)
            } else {
                onAction(EditPassAction.Save(draft))
            }
        } else {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(resources.getString(issue.messageRes), withDismissAction = true)
            }
        }
    }

    BackHandler(onBack = ::saveAndClose)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pass_detail_edit_pass)) },
                navigationIcon = {
                    IconButton(onClick = ::saveAndClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.edit_pass_save_and_go_back)) }
                },
                actions = {
                    Box {
                        IconButton(onClick = { editorMenuOpen = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.edit_pass_editor_actions)) }
                        DropdownMenu(editorMenuOpen, { editorMenuOpen = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.edit_pass_discard_changes)) }, onClick = {
                                editorMenuOpen = false
                                onAction(EditPassAction.Back)
                            })
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).testTag("edit_pass_list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                EditorSection(stringResource(R.string.edit_pass_pass)) {
                    OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.edit_pass_description)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(creator, { creator = it }, label = { Text(stringResource(R.string.edit_pass_creator)) }, modifier = Modifier.fillMaxWidth())
                    Box {
                        OutlinedButton(onClick = { typeMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.edit_pass_type_value, passType.displayName()))
                        }
                        DropdownMenu(typeMenuOpen, { typeMenuOpen = false }) {
                            PassType.entries.forEach { type ->
                                DropdownMenuItem({ Text(type.displayName()) }, onClick = { passType = type; typeMenuOpen = false })
                            }
                        }
                    }
                    ColorPickerField(stringResource(R.string.edit_pass_accent_color), accentColor, { accentColor = it }, Modifier.fillMaxWidth())
                }
            }
            item {
                EditorSection(stringResource(R.string.edit_pass_code), initiallyExpanded = false) {
                    Box {
                        OutlinedButton(onClick = { barcodeMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(
                                    R.string.edit_pass_barcode_value,
                                    barcodeFormat?.name?.replace('_', ' ') ?: stringResource(R.string.edit_pass_none),
                                ),
                            )
                        }
                        DropdownMenu(barcodeMenuOpen, { barcodeMenuOpen = false }) {
                            DropdownMenuItem({ Text(stringResource(R.string.edit_pass_none)) }, onClick = { barcodeFormat = null; barcodeMenuOpen = false })
                            PassBarCodeFormat.entries.forEach { format ->
                                DropdownMenuItem({ Text(format.name.replace('_', ' ')) }, onClick = { barcodeFormat = format; barcodeMenuOpen = false })
                            }
                        }
                    }
                    OutlinedTextField(barcodeMessage, { barcodeMessage = it }, label = { Text(stringResource(R.string.edit_pass_barcode_data)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(alternativeText, { alternativeText = it }, label = { Text(stringResource(R.string.edit_pass_barcode_text)) }, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                EditorSection(stringResource(R.string.edit_pass_calendar)) {
                    DatePickerField(stringResource(R.string.edit_pass_start_date), calendarStart, {
                        calendarStart = it
                        if (it != null && calendarEnd == null) calendarEnd = it.plusHours(2)
                    }, Modifier.fillMaxWidth(), initiallyOpen = initialDateField == PassDateField.START)
                    DatePickerField(
                        stringResource(R.string.edit_pass_end_date),
                        calendarEnd,
                        { calendarEnd = it },
                        Modifier.fillMaxWidth(),
                        initiallyOpen = initialDateField == PassDateField.END,
                    )
                }
            }
            item {
                EditorSection(stringResource(R.string.edit_pass_locations)) {
                    locations.forEachIndexed { index, location ->
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        location.name,
                                        { value -> locations = locations.replace(index, location.copy(name = value)) },
                                        label = { Text(stringResource(R.string.edit_pass_address_or_place)) },
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { locations = locations.toMutableList().also { it.removeAt(index) } }) {
                                        Icon(Icons.Default.Delete, stringResource(R.string.edit_pass_delete_location))
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(location.latitude, { value -> locations = locations.replace(index, location.copy(latitude = value)) }, label = { Text(stringResource(R.string.edit_pass_latitude_optional)) }, modifier = Modifier.weight(1f))
                                    OutlinedTextField(location.longitude, { value -> locations = locations.replace(index, location.copy(longitude = value)) }, label = { Text(stringResource(R.string.edit_pass_longitude_optional)) }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { locations = locations + PassLocationDraft("", "", "") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.edit_pass_add_location))
                    }
                }
            }
            item {
                EditorSection(stringResource(R.string.edit_pass_fields)) {
                    fields.forEachIndexed { index, field ->
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        field.label,
                                        { value -> fields = fields.replace(index, field.copy(label = value)) },
                                        label = { Text(stringResource(R.string.edit_pass_label)) },
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        enabled = index > 0,
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            fields = fields.moveItem(index, -1)
                                        },
                                    ) { Icon(Icons.Default.ArrowUpward, stringResource(R.string.edit_pass_move_field_up)) }
                                    IconButton(
                                        enabled = index < fields.lastIndex,
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            fields = fields.moveItem(index, 1)
                                        },
                                    ) { Icon(Icons.Default.ArrowDownward, stringResource(R.string.edit_pass_move_field_down)) }
                                    IconToggleButton(
                                        checked = field.hidden,
                                        onCheckedChange = { hidden -> fields = fields.replace(index, field.copy(hidden = hidden)) },
                                    ) {
                                        Icon(
                                            if (field.hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            if (field.hidden) stringResource(R.string.edit_pass_show_field) else stringResource(R.string.edit_pass_hide_field),
                                        )
                                    }
                                    IconButton(onClick = { fields = fields.toMutableList().also { it.removeAt(index) } }) {
                                        Icon(Icons.Default.Delete, stringResource(R.string.edit_pass_delete_field))
                                    }
                                }
                                OutlinedTextField(field.value, { value -> fields = fields.replace(index, field.copy(value = value)) }, label = { Text(stringResource(R.string.edit_pass_value)) }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { fields = fields + PassFieldUiModel("local-${fields.size + 1}", "", "", false, null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.edit_pass_add_field)) }
                }
            }
            item {
                Button(onClick = ::saveAndClose, enabled = pass != null, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.edit_pass_save_and_close))
                }
            }
        }
    }
}

private fun PassUiModel.toEditableDraft(): PassDraft = PassDraft(
    description = description,
    creator = creator.orEmpty(),
    type = type,
    accentColor = accentColor,
    barcodeFormat = barcodeFormat,
    barcodeMessage = barcodeMessage.orEmpty(),
    barcodeAlternativeText = barcodeAlternativeText.orEmpty(),
    fields = fields,
    calendarStart = calendarTimeSpan?.from?.toString().orEmpty(),
    calendarEnd = calendarTimeSpan?.to?.toString().orEmpty(),
    locations = locations.map {
        val addressOnly = it.latitude == 0.0 && it.longitude == 0.0 && !it.name.isNullOrBlank()
        PassLocationDraft(
            name = it.name.orEmpty(),
            latitude = if (addressOnly) "" else it.latitude.toString(),
            longitude = if (addressOnly) "" else it.longitude.toString(),
        )
    },
)

internal enum class PassDraftIssue(@StringRes val messageRes: Int) {
    DESCRIPTION_MISSING(R.string.edit_pass_validation_description),
    BARCODE_MISSING(R.string.edit_pass_validation_barcode),
    CALENDAR_START_INVALID(R.string.edit_pass_validation_calendar_start),
    CALENDAR_END_INVALID(R.string.edit_pass_validation_calendar_end),
    CALENDAR_END_BEFORE_START(R.string.edit_pass_validation_calendar_order),
    LOCATION_COORDINATES_PARTIAL(R.string.edit_pass_validation_location_partial),
    LOCATION_COORDINATES_INVALID(R.string.edit_pass_validation_location_invalid),
    LATITUDE_RANGE(R.string.edit_pass_validation_latitude),
    LONGITUDE_RANGE(R.string.edit_pass_validation_longitude),
    LOCATION_EMPTY(R.string.edit_pass_validation_location_empty),
    FIELD_EMPTY(R.string.edit_pass_validation_field_empty),
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
    if (draft.barcodeFormat != null && draft.barcodeMessage.isBlank()) PassDraftIssue.BARCODE_MISSING else null

private fun validateCalendar(draft: PassDraft): PassDraftIssue? {
    val start = parseDraftDate(draft.calendarStart)
    val end = parseDraftDate(draft.calendarEnd)
    if (draft.calendarStart.isNotBlank() && start == null) return PassDraftIssue.CALENDAR_START_INVALID
    if (draft.calendarEnd.isNotBlank() && end == null) return PassDraftIssue.CALENDAR_END_INVALID
    if (start != null && end != null && end.isBefore(start)) return PassDraftIssue.CALENDAR_END_BEFORE_START
    return null
}

private fun parseDraftDate(value: String): ZonedDateTime? =
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

@Composable
private fun EditorSection(
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                stringResource(
                    if (expanded) R.string.edit_pass_collapse_section else R.string.edit_pass_expand_section,
                    title,
                ),
            )
        }
        if (expanded) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
            }
        }
    }
}

private fun <T> List<T>.replace(index: Int, value: T) = toMutableList().also { it[index] = value }

private fun <T> List<T>.moveItem(index: Int, offset: Int): List<T> {
    val to = (index + offset).coerceIn(indices)
    if (to == index) return this
    return toMutableList().apply { add(to, removeAt(index)) }
}

private fun PassType.displayName() = name.lowercase().replaceFirstChar(Char::uppercase)
