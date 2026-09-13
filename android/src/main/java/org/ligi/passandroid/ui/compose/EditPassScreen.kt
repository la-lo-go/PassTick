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
        val error = validatePassDraft(draft)
        if (error == null) {
            val initialDraft = pass?.toEditableDraft()
            if (draft == initialDraft) {
                onAction(EditPassAction.Back)
            } else {
                onAction(EditPassAction.Save(draft))
            }
        } else {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(error, withDismissAction = true)
            }
        }
    }

    BackHandler(onBack = ::saveAndClose)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit pass") },
                navigationIcon = {
                    IconButton(onClick = ::saveAndClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Save and go back") }
                },
                actions = {
                    Box {
                        IconButton(onClick = { editorMenuOpen = true }) { Icon(Icons.Default.MoreVert, "Editor actions") }
                        DropdownMenu(editorMenuOpen, { editorMenuOpen = false }) {
                            DropdownMenuItem(text = { Text("Discard changes") }, onClick = {
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
                EditorSection("Pass") {
                    OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(creator, { creator = it }, label = { Text("Creator") }, modifier = Modifier.fillMaxWidth())
                    Box {
                        OutlinedButton(onClick = { typeMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Type: ${passType.displayName()}") }
                        DropdownMenu(typeMenuOpen, { typeMenuOpen = false }) {
                            PassType.entries.forEach { type ->
                                DropdownMenuItem({ Text(type.displayName()) }, onClick = { passType = type; typeMenuOpen = false })
                            }
                        }
                    }
                    ColorPickerField("Accent color", accentColor, { accentColor = it }, Modifier.fillMaxWidth())
                }
            }
            item {
                EditorSection("Code", initiallyExpanded = false) {
                    Box {
                        OutlinedButton(onClick = { barcodeMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Barcode: ${barcodeFormat?.name?.replace('_', ' ') ?: "None"}")
                        }
                        DropdownMenu(barcodeMenuOpen, { barcodeMenuOpen = false }) {
                            DropdownMenuItem({ Text("None") }, onClick = { barcodeFormat = null; barcodeMenuOpen = false })
                            PassBarCodeFormat.entries.forEach { format ->
                                DropdownMenuItem({ Text(format.name.replace('_', ' ')) }, onClick = { barcodeFormat = format; barcodeMenuOpen = false })
                            }
                        }
                    }
                    OutlinedTextField(barcodeMessage, { barcodeMessage = it }, label = { Text("Barcode data") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(alternativeText, { alternativeText = it }, label = { Text("Barcode text") }, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                EditorSection("Calendar") {
                    DatePickerField("Start date", calendarStart, {
                        calendarStart = it
                        if (it != null && calendarEnd == null) calendarEnd = it.plusHours(2)
                    }, Modifier.fillMaxWidth(), initiallyOpen = initialDateField == PassDateField.START)
                    DatePickerField(
                        "End date",
                        calendarEnd,
                        { calendarEnd = it },
                        Modifier.fillMaxWidth(),
                        initiallyOpen = initialDateField == PassDateField.END,
                    )
                }
            }
            item {
                EditorSection("Locations") {
                    locations.forEachIndexed { index, location ->
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        location.name,
                                        { value -> locations = locations.replace(index, location.copy(name = value)) },
                                        label = { Text("Address or place") },
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { locations = locations.toMutableList().also { it.removeAt(index) } }) {
                                        Icon(Icons.Default.Delete, "Delete location")
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(location.latitude, { value -> locations = locations.replace(index, location.copy(latitude = value)) }, label = { Text("Latitude (optional)") }, modifier = Modifier.weight(1f))
                                    OutlinedTextField(location.longitude, { value -> locations = locations.replace(index, location.copy(longitude = value)) }, label = { Text("Longitude (optional)") }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { locations = locations + PassLocationDraft("", "", "") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Add location")
                    }
                }
            }
            item {
                EditorSection("Fields") {
                    fields.forEachIndexed { index, field ->
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        field.label,
                                        { value -> fields = fields.replace(index, field.copy(label = value)) },
                                        label = { Text("Label") },
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        enabled = index > 0,
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            fields = fields.moveItem(index, -1)
                                        },
                                    ) { Icon(Icons.Default.ArrowUpward, "Move field up") }
                                    IconButton(
                                        enabled = index < fields.lastIndex,
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            fields = fields.moveItem(index, 1)
                                        },
                                    ) { Icon(Icons.Default.ArrowDownward, "Move field down") }
                                    IconToggleButton(
                                        checked = field.hidden,
                                        onCheckedChange = { hidden -> fields = fields.replace(index, field.copy(hidden = hidden)) },
                                    ) {
                                        Icon(
                                            if (field.hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            if (field.hidden) "Show field" else "Hide field",
                                        )
                                    }
                                    IconButton(onClick = { fields = fields.toMutableList().also { it.removeAt(index) } }) {
                                        Icon(Icons.Default.Delete, "Delete field")
                                    }
                                }
                                OutlinedTextField(field.value, { value -> fields = fields.replace(index, field.copy(value = value)) }, label = { Text("Value") }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { fields = fields + PassFieldUiModel("local-${fields.size + 1}", "", "", false, null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add field") }
                }
            }
            item {
                Button(onClick = ::saveAndClose, enabled = pass != null, modifier = Modifier.fillMaxWidth()) {
                    Text("Save and close")
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

internal fun validatePassDraft(draft: PassDraft): String? {
    validateDescription(draft)?.let { return it }
    validateBarcode(draft)?.let { return it }
    validateCalendar(draft)?.let { return it }
    validateLocations(draft.locations)?.let { return it }
    return validateFields(draft)
}

private fun validateDescription(draft: PassDraft): String? =
    if (draft.description.isBlank()) "Add a description before leaving." else null

private fun validateBarcode(draft: PassDraft): String? =
    if (draft.barcodeFormat != null && draft.barcodeMessage.isBlank()) "Add barcode data or remove the barcode." else null

private fun validateCalendar(draft: PassDraft): String? {
    val start = parseDraftDate(draft.calendarStart)
    val end = parseDraftDate(draft.calendarEnd)
    if (draft.calendarStart.isNotBlank() && start == null) return "Select a valid start date or clear it."
    if (draft.calendarEnd.isNotBlank() && end == null) return "Select a valid end date or clear it."
    if (start != null && end != null && end.isBefore(start)) return "The end date must be after the start date."
    return null
}

private fun parseDraftDate(value: String): ZonedDateTime? =
    value.takeIf(String::isNotBlank)?.let { runCatching { ZonedDateTime.parse(it) }.getOrNull() }

private fun validateLocations(locations: List<PassLocationDraft>): String? {
    locations.forEach { location ->
        validateLocation(location)?.let { return it }
    }
    return null
}

private fun validateLocation(location: PassLocationDraft): String? {
    val hasLatitude = location.latitude.isNotBlank()
    val hasLongitude = location.longitude.isNotBlank()
    if (hasLatitude != hasLongitude) return "Enter both coordinates or clear both."
    val latitude = location.latitude.takeIf(String::isNotBlank)?.toDoubleOrNull()
    val longitude = location.longitude.takeIf(String::isNotBlank)?.toDoubleOrNull()
    if (hasLatitude && (latitude == null || longitude == null)) return "Fix the location coordinates or clear them."
    if (latitude != null && latitude !in -90.0..90.0) return "Latitude must be between -90 and 90."
    if (longitude != null && longitude !in -180.0..180.0) return "Longitude must be between -180 and 180."
    if (location.name.isBlank() && !hasLatitude) return "Add an address or delete the empty location."
    return null
}

private fun validateFields(draft: PassDraft): String? =
    if (draft.fields.any { it.label.isBlank() && it.value.isBlank() }) "Complete or delete the empty field." else null

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
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "Collapse $title" else "Expand $title")
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
