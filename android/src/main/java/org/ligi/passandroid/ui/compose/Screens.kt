package org.ligi.passandroid.ui.compose

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.CategorySettingsAction
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.PassDraft
import org.ligi.passandroid.ui.state.PassArtworkDraft
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationDraft
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.SettingsAction
import org.ligi.passandroid.ui.barcode.ExpandedPassCodeDialog
import org.ligi.passandroid.ui.barcode.PassCodePreview
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PassDetailScreen(
    pass: PassUiModel?,
    categories: List<PassCategory> = emptyList(),
    passReminderEnabled: Boolean = false,
    initialCodeExpanded: Boolean = false,
    onInitialCodeShown: () -> Unit = {},
    flashlightAvailable: Boolean = false,
    flashlightEnabled: Boolean = false,
    onAction: (PassDetailAction) -> Unit,
) {
    var overflowOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var codeHeld by remember(pass?.id) { mutableStateOf(false) }
    var codePinned by remember(pass?.id) { mutableStateOf(initialCodeExpanded) }
    val codeExpanded = codeHeld || codePinned
    LaunchedEffect(initialCodeExpanded) {
        if (initialCodeExpanded) onInitialCodeShown()
    }
    if (codeExpanded && pass?.barcodeFormat != null && !pass.barcodeMessage.isNullOrBlank()) {
        ExpandedPassCodeDialog(
            format = pass.barcodeFormat,
            message = pass.barcodeMessage,
            alternativeText = pass.barcodeAlternativeText,
            onDismiss = {
                codeHeld = false
                codePinned = false
            },
        )
    }
    DisposableEffect(Unit) {
        onDispose { onAction(PassDetailAction.SetFlashlightEnabled(false)) }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete pass permanently?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onAction(PassDetailAction.Delete)
                    },
                ) { Text("Delete permanently") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pass?.description ?: "Pass") },
                navigationIcon = { IconButton(onClick = { onAction(PassDetailAction.Back) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    Box {
                        IconButton(onClick = { overflowOpen = true }, enabled = pass != null) {
                            Icon(Icons.Default.MoreVert, "Pass actions")
                        }
                        DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                            DropdownMenuItem(text = { Text("Print") }, onClick = {
                                overflowOpen = false
                                onAction(PassDetailAction.Print)
                            })
                            DropdownMenuItem(
                                text = { Text(if (passReminderEnabled) "Turn reminder off" else "Turn reminder on") },
                                enabled = pass?.calendarEvent != null,
                                onClick = {
                                    overflowOpen = false
                                    onAction(PassDetailAction.ToggleReminder)
                                },
                            )
                            categories.forEach { category ->
                                DropdownMenuItem(text = { Text("Move to ${category.name}") }, onClick = {
                                    overflowOpen = false
                                    onAction(PassDetailAction.MoveToCategory(category.id))
                                })
                            }
                            pass?.locations?.forEachIndexed { index, location ->
                                DropdownMenuItem(text = { Text(location.name?.takeIf(String::isNotBlank) ?: "Open location") }, onClick = {
                                    overflowOpen = false
                                    onAction(PassDetailAction.OpenLocation(index))
                                })
                            }
                            DropdownMenuItem(text = { Text("Delete permanently") }, onClick = {
                                overflowOpen = false
                                confirmDelete = true
                            })
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (pass != null) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = {
                        FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = { onAction(PassDetailAction.Edit) }) {
                            Icon(Icons.Default.Edit, "Edit pass")
                        }
                    },
                ) {
                    IconButton(onClick = { onAction(PassDetailAction.Share) }) {
                        Icon(Icons.Default.Share, "Share pass")
                    }
                    if (!pass.barcodeMessage.isNullOrBlank()) {
                        IconButton(
                            onClick = { onAction(PassDetailAction.SetFlashlightEnabled(!flashlightEnabled)) },
                            enabled = flashlightAvailable || flashlightEnabled,
                        ) {
                            Icon(
                                if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                if (flashlightEnabled) "Turn flashlight off" else "Turn flashlight on",
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (pass == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Pass not found") }
        } else {
            Box(Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    Modifier.fillMaxHeight().widthIn(max = 760.dp).align(Alignment.TopCenter),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                item {
                    PassArtwork(
                        pass,
                        listOf(PassArtworkKind.STRIP, PassArtworkKind.LOGO, PassArtworkKind.THUMBNAIL),
                        Modifier.fillMaxWidth().height(160.dp),
                    )
                }
                item {
                    BarcodeCard(
                        pass = pass,
                        onHoldChanged = { codeHeld = it },
                        onPin = { codePinned = true },
                    )
                }
                val visibleFields = pass.fields.filterNot { it.hidden }
                if (visibleFields.isNotEmpty()) {
                    item {
                        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                            Column(Modifier.padding(vertical = 8.dp)) {
                                visibleFields.forEach { field ->
                                    ListItem(supportingContent = { Text(field.label) }) { Text(field.value) }
                                }
                            }
                        }
                    }
                }
                pass.calendarEvent?.let {
                    item {
                        Button(onClick = { onAction(PassDetailAction.AddToCalendar) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Add to calendar")
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun PassArtwork(pass: PassUiModel, preferredKinds: List<PassArtworkKind>, modifier: Modifier) {
    val artwork = preferredKinds.firstNotNullOfOrNull { kind -> pass.artwork.firstOrNull { it.kind == kind } }
        ?: return
    val bitmap = remember(artwork.bytes) {
        BitmapFactory.decodeByteArray(artwork.bytes, 0, artwork.bytes.size)?.asImageBitmap()
    } ?: return
    Image(bitmap, "Pass artwork", modifier, contentScale = ContentScale.Fit)
}

@Composable
private fun BarcodeCard(pass: PassUiModel, onHoldChanged: (Boolean) -> Unit, onPin: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val format = pass.barcodeFormat
            val message = pass.barcodeMessage
            if (format != null && !message.isNullOrBlank()) {
                BoxWithConstraints(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    PassCodePreview(format, message, onHoldChanged, onPin, Modifier.fillMaxSize())
                }
            } else Text("No barcode", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPassScreen(pass: PassUiModel?, onAction: (EditPassAction) -> Unit) {
    var description by remember(pass?.id) { mutableStateOf(pass?.description.orEmpty()) }
    var creator by remember(pass?.id) { mutableStateOf(pass?.creator.orEmpty()) }
    var passType by remember(pass?.id) { mutableStateOf(pass?.type ?: PassType.EVENT) }
    var accentColor by remember(pass?.id) { mutableStateOf(pass?.accentColor ?: 0xFF3D73E9.toInt()) }
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
    var artworkUpdates by remember(pass?.id) { mutableStateOf(emptyList<PassArtworkDraft>()) }
    var pendingArtworkKind by remember { mutableStateOf<PassArtworkKind?>(null) }
    var typeMenuOpen by remember { mutableStateOf(false) }
    var barcodeMenuOpen by remember { mutableStateOf(false) }
    var editorMenuOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val artworkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val kind = pendingArtworkKind
        if (uri != null && kind != null) artworkUpdates = artworkUpdates.filterNot { it.kind == kind } + PassArtworkDraft(kind, uri)
        pendingArtworkKind = null
    }

    fun currentDraft() = PassDraft(
        description = description,
        creator = creator,
        type = passType,
        accentColor = accentColor,
        barcodeFormat = barcodeFormat,
        barcodeMessage = barcodeMessage,
        barcodeAlternativeText = alternativeText,
        fields = fields,
        artworkUpdates = artworkUpdates,
        calendarStart = calendarStart?.toString().orEmpty(),
        calendarEnd = calendarEnd?.toString().orEmpty(),
        locations = locations,
    )

    fun saveAndClose() {
        val draft = currentDraft()
        val error = validatePassDraft(draft)
        if (error == null) {
            onAction(EditPassAction.Save(draft))
        } else {
            scope.launch { snackbarHostState.showSnackbar(error, withDismissAction = true) }
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
                EditorSection("Code") {
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
                    }, Modifier.fillMaxWidth())
                    DatePickerField("End date", calendarEnd, { calendarEnd = it }, Modifier.fillMaxWidth())
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
                EditorSection("Artwork") {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(PassArtworkKind.LOGO, PassArtworkKind.STRIP, PassArtworkKind.THUMBNAIL).forEach { kind ->
                            OutlinedButton(onClick = {
                                pendingArtworkKind = kind
                                artworkLauncher.launch(arrayOf("image/*"))
                            }) { Text(kind.name.lowercase().replaceFirstChar(Char::uppercase)) }
                        }
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

internal fun validatePassDraft(draft: PassDraft): String? {
    if (draft.description.isBlank()) return "Add a description before leaving."
    if (draft.barcodeFormat != null && draft.barcodeMessage.isBlank()) return "Add barcode data or remove the barcode."
    val start = draft.calendarStart.takeIf(String::isNotBlank)?.let { runCatching { org.threeten.bp.ZonedDateTime.parse(it) }.getOrNull() }
    val end = draft.calendarEnd.takeIf(String::isNotBlank)?.let { runCatching { org.threeten.bp.ZonedDateTime.parse(it) }.getOrNull() }
    if (draft.calendarStart.isNotBlank() && start == null) return "Select a valid start date or clear it."
    if (draft.calendarEnd.isNotBlank() && end == null) return "Select a valid end date or clear it."
    if (start != null && end != null && end.isBefore(start)) return "The end date must be after the start date."
    draft.locations.forEach { location ->
        val hasLatitude = location.latitude.isNotBlank()
        val hasLongitude = location.longitude.isNotBlank()
        if (hasLatitude != hasLongitude) return "Enter both coordinates or clear both."
        val latitude = location.latitude.takeIf(String::isNotBlank)?.toDoubleOrNull()
        val longitude = location.longitude.takeIf(String::isNotBlank)?.toDoubleOrNull()
        if (hasLatitude && (latitude == null || longitude == null)) return "Fix the location coordinates or clear them."
        if (latitude != null && latitude !in -90.0..90.0) return "Latitude must be between -90 and 90."
        if (longitude != null && longitude !in -180.0..180.0) return "Longitude must be between -180 and 180."
        if (location.name.isBlank() && !hasLatitude) return "Add an address or delete the empty location."
    }
    if (draft.fields.any { it.label.isBlank() && it.value.isBlank() }) return "Complete or delete the empty field."
    return null
}

@Composable
private fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

private fun <T> List<T>.replace(index: Int, value: T) = toMutableList().also { it[index] = value }

private fun PassType.displayName() = name.lowercase().replaceFirstChar(Char::uppercase)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = { onAction(SettingsAction.Back) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.fillMaxHeight().widthIn(max = 760.dp).align(Alignment.TopCenter),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    SettingsGroup("Appearance") {
                        Text("Theme", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                        ThemeMode.entries.forEach { mode ->
                            ListItem(
                                trailingContent = { androidx.compose.material3.RadioButton(mode == settings.themeMode, { onAction(SettingsAction.SetTheme(mode)) }) },
                                modifier = Modifier.clickable { onAction(SettingsAction.SetTheme(mode)) },
                            ) { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        SettingSwitch("Automatic barcode brightness", settings.automaticBrightness) { onAction(SettingsAction.SetAutomaticBrightness(it)) }
                    }
                }
                item {
                    SettingsGroup("Pass list") {
                        SettingSwitch("Show today's passes prominently", settings.highlightTodayPasses) {
                            onAction(SettingsAction.SetHighlightTodayPasses(it))
                        }
                        SettingSwitch("Automatically move past passes", settings.automaticallyMarkPast) {
                            onAction(SettingsAction.SetAutomaticallyMarkPast(it))
                        }
                        ListItem(
                            supportingContent = { Text("Manage names, colors, and order") },
                            modifier = Modifier.clickable { onAction(SettingsAction.OpenCategories) },
                        ) { Text("Categories") }
                    }
                }
                item {
                    SettingsGroup("Calendar") {
                        SettingSwitch("Open calendar after import", settings.offerCalendarAfterImport) {
                            onAction(SettingsAction.SetOfferCalendarAfterImport(it))
                        }
                    }
                }
                item {
                    SettingsGroup("Notifications") {
                        SettingSwitch("Pass reminders", settings.remindersEnabled) {
                            onAction(SettingsAction.SetRemindersEnabled(it))
                        }
                        Text("Reminder times", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                        listOf(15 to "15 minutes before", 30 to "30 minutes before", 60 to "1 hour before", 1440 to "1 day before").forEach { (minutes, label) ->
                            val selected = minutes in settings.reminderMinutes
                            fun toggle() {
                                val updated = settings.reminderMinutes.toMutableSet()
                                if (!updated.add(minutes)) updated.remove(minutes)
                                onAction(SettingsAction.SetReminderMinutes(updated))
                            }
                            ListItem(
                                trailingContent = {
                                    Checkbox(
                                        checked = selected,
                                        enabled = settings.remindersEnabled,
                                        onCheckedChange = { toggle() },
                                    )
                                },
                                modifier = Modifier.clickable(enabled = settings.remindersEnabled, onClick = ::toggle),
                            ) {
                                Text(
                                    label,
                                    color = if (settings.remindersEnabled) Color.Unspecified
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.padding(vertical = 8.dp), content = content)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(
    categories: List<PassCategory>,
    onAction: (CategorySettingsAction) -> Unit,
) {
    var editing by remember { mutableStateOf<PassCategory?>(null) }
    var deleting by remember { mutableStateOf<PassCategory?>(null) }
    editing?.let { category ->
        CategoryEditorDialog(
            category = category,
            onDismiss = { editing = null },
            onSave = {
                onAction(CategorySettingsAction.Save(it))
                editing = null
            },
        )
    }
    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete category?") },
            text = { Text("Passes in ${category.name} will move to Inbox.") },
            confirmButton = {
                TextButton(onClick = {
                    onAction(CategorySettingsAction.Delete(category.id))
                    deleting = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = { onAction(CategorySettingsAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = PassCategory(
                        id = "custom-${UUID.randomUUID()}",
                        name = "",
                        colorArgb = 0xFF6750A4,
                    )
                },
                modifier = Modifier.semantics { contentDescription = "Add category" },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add category") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
        ) {
            items(categories, key = PassCategory::id) { category ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    ListItem(
                    supportingContent = {
                        Text(category.role.name.lowercase().replaceFirstChar(Char::uppercase))
                    },
                    leadingContent = {
                        Box(
                            Modifier.size(32.dp)
                                .background(Color(category.colorArgb.toInt()), RoundedCornerShape(12.dp)),
                        )
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { onAction(CategorySettingsAction.Move(category.id, -1)) }) {
                                Icon(Icons.Default.ArrowUpward, "Move ${category.name} up")
                            }
                            IconButton(onClick = { onAction(CategorySettingsAction.Move(category.id, 1)) }) {
                                Icon(Icons.Default.ArrowDownward, "Move ${category.name} down")
                            }
                            if (category.role == PassCategoryRole.CUSTOM) {
                                IconButton(onClick = { deleting = category }) {
                                    Icon(Icons.Default.Delete, "Delete ${category.name}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.clickable { editing = category },
                    ) { Text(category.name) }
                }
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    category: PassCategory,
    onDismiss: () -> Unit,
    onSave: (PassCategory) -> Unit,
) {
    var name by remember(category.id) { mutableStateOf(category.name) }
    var color by remember(category.id) { mutableStateOf(category.colorArgb.toInt()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category.name.isBlank()) "Add category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                ColorPickerField("Category color", color, { color = it }, Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(category.copy(name = name.trim(), colorArgb = color.toUInt().toLong()))
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(trailingContent = { Switch(checked, onChange) }, modifier = Modifier.clickable { onChange(!checked) }) {
        Text(label)
    }
}
