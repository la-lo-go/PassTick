package org.ligi.passandroid.ui.compose

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassDetailScreen(
    pass: PassUiModel?,
    categories: List<PassCategory> = emptyList(),
    quickCodePassId: String? = null,
    remindersEnabled: Boolean = false,
    passReminderEnabled: Boolean = false,
    onAction: (PassDetailAction) -> Unit,
) {
    var moveMenuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
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
                    IconButton(onClick = { onAction(PassDetailAction.Share) }, enabled = pass != null) { Icon(Icons.Default.Share, "Share") }
                    IconButton(onClick = { onAction(PassDetailAction.Edit) }, enabled = pass != null) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton(onClick = { confirmDelete = true }, enabled = pass != null) {
                        Icon(Icons.Default.Delete, "Delete pass")
                    }
                },
            )
        },
    ) { padding ->
        if (pass == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Pass not found") }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    PassArtwork(
                        pass,
                        listOf(PassArtworkKind.STRIP, PassArtworkKind.LOGO, PassArtworkKind.THUMBNAIL),
                        Modifier.fillMaxWidth().height(160.dp),
                    )
                }
                item { BarcodeCard(pass) { onAction(PassDetailAction.OpenCode) } }
                items(pass.fields.filterNot { it.hidden }) { field ->
                    ListItem(supportingContent = { Text(field.label) }) { Text(field.value) }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onAction(PassDetailAction.Export) }, modifier = Modifier.weight(1f)) { Text("Export") }
                        OutlinedButton(onClick = { onAction(PassDetailAction.Print) }, modifier = Modifier.weight(1f)) { Text("Print") }
                    }
                }
                item {
                    Box {
                        OutlinedButton(onClick = { moveMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Move to category")
                        }
                        DropdownMenu(expanded = moveMenuOpen, onDismissRequest = { moveMenuOpen = false }) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        moveMenuOpen = false
                                        onAction(PassDetailAction.MoveToCategory(category.id))
                                    },
                                )
                            }
                        }
                    }
                }
                pass.calendarEvent?.let {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onAction(PassDetailAction.AddToCalendar) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Add to calendar") }
                            FilledTonalButton(
                                onClick = { onAction(PassDetailAction.ToggleReminder) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    when {
                                        !remindersEnabled -> "Enable reminders"
                                        passReminderEnabled -> "Reminder on"
                                        else -> "Reminder off"
                                    },
                                )
                            }
                        }
                    }
                }
                if (!pass.barcodeMessage.isNullOrBlank()) {
                    item {
                        FilledTonalButton(
                            onClick = { onAction(PassDetailAction.UseForQuickCodeWidget) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (quickCodePassId == pass.id) "Quick code widget pass" else "Use in quick code widget")
                        }
                    }
                }
                items(pass.locations.size) { index ->
                    val location = pass.locations[index]
                    FilledTonalButton(onClick = { onAction(PassDetailAction.OpenLocation(index)) }, Modifier.fillMaxWidth()) {
                        Text(location.name ?: "Open location")
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
private fun BarcodeCard(pass: PassUiModel, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val format = pass.barcodeFormat
            val message = pass.barcodeMessage
            if (format != null && !message.isNullOrBlank()) {
                BoxWithConstraints(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val bitmap = remember(format, message, constraints.maxWidth, constraints.maxHeight) {
                        org.ligi.passandroid.ui.barcode.CrispBarcodeRenderer.renderBitmap(
                            message,
                            format,
                            constraints.maxWidth,
                            constraints.maxHeight,
                        )
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap.asImageBitmap(),
                            "Pass barcode. Tap to enlarge",
                            Modifier.size(
                                with(density) { bitmap.width.toDp() },
                                with(density) { bitmap.height.toDp() },
                            ),
                            contentScale = ContentScale.None,
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                        )
                    } else {
                        Text("Code cannot be displayed", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else Text("No barcode", style = MaterialTheme.typography.titleMedium)
            pass.barcodeAlternativeText?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPassScreen(pass: PassUiModel?, onAction: (EditPassAction) -> Unit, isNew: Boolean = false) {
    var description by remember(pass?.id) { mutableStateOf(pass?.description.orEmpty()) }
    var creator by remember(pass?.id) { mutableStateOf(pass?.creator.orEmpty()) }
    var passType by remember(pass?.id) { mutableStateOf(pass?.type ?: PassType.EVENT) }
    var accentColor by remember(pass?.id) {
        mutableStateOf(pass?.accentColor?.let(::formatColor) ?: formatColor(0xFF3D73E9.toInt()))
    }
    var barcodeFormat by remember(pass?.id) { mutableStateOf(pass?.barcodeFormat) }
    var barcodeMessage by remember(pass?.id) { mutableStateOf(pass?.barcodeMessage.orEmpty()) }
    var alternativeText by remember(pass?.id) { mutableStateOf(pass?.barcodeAlternativeText.orEmpty()) }
    var fields by remember(pass?.id) { mutableStateOf(pass?.fields.orEmpty()) }
    var calendarStart by remember(pass?.id) { mutableStateOf(pass?.calendarTimeSpan?.from?.toString().orEmpty()) }
    var calendarEnd by remember(pass?.id) { mutableStateOf(pass?.calendarTimeSpan?.to?.toString().orEmpty()) }
    var locations by remember(pass?.id) {
        mutableStateOf(
            pass?.locations.orEmpty().map {
                PassLocationDraft(it.name.orEmpty(), it.latitude.toString(), it.longitude.toString())
            },
        )
    }
    var artworkUpdates by remember(pass?.id) { mutableStateOf(emptyList<PassArtworkDraft>()) }
    var pendingArtworkKind by remember { mutableStateOf<PassArtworkKind?>(null) }
    val artworkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val kind = pendingArtworkKind
        if (uri != null && kind != null) {
            artworkUpdates = artworkUpdates.filterNot { it.kind == kind } + PassArtworkDraft(kind, uri)
        }
        pendingArtworkKind = null
    }
    var typeMenuOpen by remember { mutableStateOf(false) }
    var barcodeMenuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Create pass" else "Edit pass") },
                navigationIcon = { IconButton(onClick = { onAction(EditPassAction.Back) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).testTag("edit_pass_list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    OutlinedTextField(accentColor, { accentColor = it }, label = { Text("Accent color (#AARRGGBB)") }, modifier = Modifier.fillMaxWidth())
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
                    OutlinedTextField(calendarStart, { calendarStart = it }, label = { Text("Start (ISO 8601)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(calendarEnd, { calendarEnd = it }, label = { Text("End (ISO 8601)") }, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                EditorSection("Locations") {
                    locations.forEachIndexed { index, location ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Location ${index + 1}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { locations = locations.toMutableList().also { it.removeAt(index) } }) {
                                Icon(Icons.Default.Delete, "Delete location")
                            }
                        }
                        OutlinedTextField(location.name, { value -> locations = locations.replace(index, location.copy(name = value)) }, label = { Text("Location name") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(location.latitude, { value -> locations = locations.replace(index, location.copy(latitude = value)) }, label = { Text("Latitude") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(location.longitude, { value -> locations = locations.replace(index, location.copy(longitude = value)) }, label = { Text("Longitude") }, modifier = Modifier.weight(1f))
                        }
                    }
                    OutlinedButton(
                        onClick = { locations = locations + PassLocationDraft("", "", "") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add location") }
                }
            }
            item {
                EditorSection("Artwork") {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(PassArtworkKind.LOGO, PassArtworkKind.STRIP, PassArtworkKind.THUMBNAIL).forEach { kind ->
                        OutlinedButton(
                            onClick = {
                                pendingArtworkKind = kind
                                artworkLauncher.launch(arrayOf("image/*"))
                            },
                        ) { Text(kind.name.lowercase().replaceFirstChar(Char::uppercase)) }
                    }
                    }
                }
            }
            item {
                EditorSection("Fields") {
                    fields.forEachIndexed { index, field ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Field ${index + 1}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { fields = fields.toMutableList().also { it.removeAt(index) } }) {
                                Icon(Icons.Default.Delete, "Delete field")
                            }
                        }
                        OutlinedTextField(field.label, { value -> fields = fields.replace(index, field.copy(label = value)) }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(field.value, { value -> fields = fields.replace(index, field.copy(value = value)) }, label = { Text("Value") }, modifier = Modifier.fillMaxWidth())
                        SettingSwitch("Hide field", field.hidden) { value -> fields = fields.replace(index, field.copy(hidden = value)) }
                    }
                    OutlinedButton(
                        onClick = { fields = fields + PassFieldUiModel("local-${fields.size + 1}", "", "", false, null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add field") }
                }
            }
            item {
                Button(
                    enabled = (isNew || pass != null) && description.isNotBlank(),
                    onClick = {
                        onAction(EditPassAction.Save(
                            PassDraft(
                                description,
                                creator,
                                passType,
                                parseColor(accentColor, pass?.accentColor ?: 0),
                                barcodeFormat,
                                barcodeMessage,
                                alternativeText,
                                fields,
                                artworkUpdates,
                                calendarStart,
                                calendarEnd,
                                locations,
                            ),
                        ))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save") }
            }
        }
    }
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

private fun formatColor(color: Int) = "#%08X".format(java.util.Locale.ROOT, color)

private fun parseColor(value: String, fallback: Int) =
    runCatching { android.graphics.Color.parseColor(value) }.getOrDefault(fallback)

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
                        SettingSwitch("Condensed pass list", settings.condensedPasses) { onAction(SettingsAction.SetCondensedPasses(it)) }
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
                        if (settings.remindersEnabled) {
                            Text("Default reminder", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                            listOf(15 to "15 minutes before", 30 to "30 minutes before", 60 to "1 hour before", 1440 to "1 day before").forEach { (minutes, label) ->
                                ListItem(
                                    trailingContent = {
                                        androidx.compose.material3.RadioButton(
                                            selected = minutes == settings.defaultReminderMinutes,
                                            onClick = { onAction(SettingsAction.SetDefaultReminderMinutes(minutes)) },
                                        )
                                    },
                                    modifier = Modifier.clickable { onAction(SettingsAction.SetDefaultReminderMinutes(minutes)) },
                                ) { Text(label) }
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
    var color by remember(category.id) { mutableStateOf(formatColor(category.colorArgb.toInt())) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category.name.isBlank()) "Add category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(color, { color = it }, label = { Text("Color (#AARRGGBB)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val parsedColor = parseColor(color, category.colorArgb.toInt()).toUInt().toLong()
                    onSave(category.copy(name = name.trim(), colorArgb = parsedColor))
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
