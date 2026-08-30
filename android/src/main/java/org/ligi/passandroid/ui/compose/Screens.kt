package org.ligi.passandroid.ui.compose

import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.R
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.PassDraft
import org.ligi.passandroid.ui.state.PassArtworkDraft
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassFinderAction
import org.ligi.passandroid.ui.state.PassListAction
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.SettingsAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassListScreen(
    state: MainUiState,
    onAction: (PassListAction) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { onAction(PassListAction.FindPassFiles) }) { Icon(Icons.Default.FolderOpen, "Find pass files") }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick = { menuOpen = false; onAction(PassListAction.OpenSettings) },
                            )
                            DropdownMenuItem(
                                text = { Text("Help") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Help, null) },
                                onClick = { menuOpen = false; onAction(PassListAction.OpenHelp) },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction(PassListAction.ImportPass) },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Import pass") },
                modifier = Modifier.testTag("import_pass"),
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            if (state.isBusy) CircularProgressIndicator(Modifier.align(Alignment.Center))
            if (!state.isBusy && state.passes.isEmpty()) {
                EmptyPassList(Modifier.align(Alignment.Center))
            } else {
                val columns = if (maxWidth >= 600.dp) 2 else 1
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.passes.chunked(columns), key = { row -> row.joinToString { it.id } }) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { pass -> PassCard(pass, state.settings.condensedPasses, Modifier.weight(1f), onAction) }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPassList(modifier: Modifier = Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Add, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("No passes", style = MaterialTheme.typography.headlineSmall)
        Text("Import a .pkpass or .espass file to begin.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PassCard(pass: PassUiModel, condensed: Boolean, modifier: Modifier, onAction: (PassListAction) -> Unit) {
    Card(modifier.clickable { onAction(PassListAction.OpenPass(pass.id)) }) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.size(10.dp, 104.dp).background(Color(pass.accentColor)))
            Column(Modifier.padding(16.dp).weight(1f)) {
                Text(pass.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!condensed) pass.creator?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Spacer(Modifier.height(if (condensed) 2.dp else 8.dp))
                Text(pass.type.name.replace('_', ' '), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            PassArtwork(pass, listOf(PassArtworkKind.LOGO, PassArtworkKind.THUMBNAIL), Modifier.size(88.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassDetailScreen(
    pass: PassUiModel?,
    automaticBrightness: Boolean,
    onAction: (PassDetailAction) -> Unit,
) {
    val activity = LocalActivity.current
    DisposableEffect(automaticBrightness, activity) {
        val attributes = activity?.window?.attributes
        val previous = attributes?.screenBrightness
        if (automaticBrightness && attributes != null) {
            attributes.screenBrightness = 1f
            activity.window.attributes = attributes
        }
        onDispose {
            if (automaticBrightness && attributes != null && previous != null) {
                attributes.screenBrightness = previous
                activity.window.attributes = attributes
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pass?.description ?: "Pass") },
                navigationIcon = { IconButton(onClick = { onAction(PassDetailAction.Back) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { onAction(PassDetailAction.Share) }, enabled = pass != null) { Icon(Icons.Default.Share, "Share") }
                    IconButton(onClick = { onAction(PassDetailAction.Edit) }, enabled = pass != null) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton(onClick = { onAction(PassDetailAction.Delete) }, enabled = pass != null) { Icon(Icons.Default.Delete, "Delete") }
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
                item { BarcodeCard(pass) }
                items(pass.fields.filterNot { it.hidden }) { field ->
                    ListItem(headlineContent = { Text(field.value) }, supportingContent = { Text(field.label) })
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onAction(PassDetailAction.Export) }, modifier = Modifier.weight(1f)) { Text("Export") }
                        OutlinedButton(onClick = { onAction(PassDetailAction.Print) }, modifier = Modifier.weight(1f)) { Text("Print") }
                    }
                }
                pass.calendarEvent?.let {
                    item { Button(onClick = { onAction(PassDetailAction.AddToCalendar) }, Modifier.fillMaxWidth()) { Text("Add to calendar") } }
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
private fun BarcodeCard(pass: PassUiModel) {
    val resources = LocalContext.current.resources
    val bitmap = remember(pass.barcodeFormat, pass.barcodeMessage) {
        if (pass.barcodeFormat == null || pass.barcodeMessage.isNullOrBlank()) null
        else BarCode(pass.barcodeFormat, pass.barcodeMessage).getBitmap(resources)?.bitmap
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (bitmap != null) Image(bitmap.asImageBitmap(), "Pass barcode", Modifier.fillMaxWidth().height(180.dp))
            else Text("No barcode", style = MaterialTheme.typography.titleMedium)
            pass.barcodeAlternativeText?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPassScreen(pass: PassUiModel?, onAction: (EditPassAction) -> Unit) {
    var description by remember(pass?.id) { mutableStateOf(pass?.description.orEmpty()) }
    var creator by remember(pass?.id) { mutableStateOf(pass?.creator.orEmpty()) }
    var passType by remember(pass?.id) { mutableStateOf(pass?.type ?: PassType.EVENT) }
    var accentColor by remember(pass?.id) { mutableStateOf(pass?.accentColor?.let(::formatColor).orEmpty()) }
    var barcodeFormat by remember(pass?.id) { mutableStateOf(pass?.barcodeFormat) }
    var barcodeMessage by remember(pass?.id) { mutableStateOf(pass?.barcodeMessage.orEmpty()) }
    var alternativeText by remember(pass?.id) { mutableStateOf(pass?.barcodeAlternativeText.orEmpty()) }
    var fields by remember(pass?.id) { mutableStateOf(pass?.fields.orEmpty()) }
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
                title = { Text("Edit pass") },
                navigationIcon = { IconButton(onClick = { onAction(EditPassAction.Back) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(creator, { creator = it }, label = { Text("Creator") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Box {
                    OutlinedButton(onClick = { typeMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Type: ${passType.displayName()}") }
                    DropdownMenu(typeMenuOpen, { typeMenuOpen = false }) {
                        PassType.entries.forEach { type ->
                            DropdownMenuItem({ Text(type.displayName()) }, onClick = { passType = type; typeMenuOpen = false })
                        }
                    }
                }
            }
            item { OutlinedTextField(accentColor, { accentColor = it }, label = { Text("Accent color (#AARRGGBB)") }, modifier = Modifier.fillMaxWidth()) }
            item {
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
            }
            item { OutlinedTextField(barcodeMessage, { barcodeMessage = it }, label = { Text("Barcode data") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(alternativeText, { alternativeText = it }, label = { Text("Barcode text") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Text("Artwork", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(PassArtworkKind.LOGO, PassArtworkKind.STRIP, PassArtworkKind.THUMBNAIL).forEach { kind ->
                        OutlinedButton(
                            onClick = {
                                pendingArtworkKind = kind
                                artworkLauncher.launch(arrayOf("image/*"))
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(kind.name.lowercase().replaceFirstChar(Char::uppercase)) }
                    }
                }
            }
            items(fields.size) { index ->
                val field = fields[index]
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
            }
            item {
                OutlinedButton(
                    onClick = { fields = fields + PassFieldUiModel("local-${fields.size + 1}", "", "", false, null) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add field") }
            }
            item {
                Button(
                    enabled = pass != null && description.isNotBlank(),
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
                            ),
                        ))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save") }
            }
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
fun ScannerScreen(onAction: (PassFinderAction) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { onAction(PassFinderAction.Import(it)) }
    Scaffold(topBar = { TopAppBar(title = { Text("Find pass files") }, navigationIcon = { IconButton(onClick = { onAction(PassFinderAction.Back) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.FolderOpen, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Select one or more pass files from this device or a document provider.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { launcher.launch(arrayOf("application/vnd.apple.pkpass", "application/vnd.espass-espass+zip", "application/zip")) }) { Text("Select pass files") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = { onAction(SettingsAction.Back) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { Text("Theme", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) }
            items(ThemeMode.entries) { mode ->
                ListItem(
                    headlineContent = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    trailingContent = { androidx.compose.material3.RadioButton(mode == settings.themeMode, { onAction(SettingsAction.SetTheme(mode)) }) },
                    modifier = Modifier.clickable { onAction(SettingsAction.SetTheme(mode)) },
                )
            }
            item { SettingSwitch("Condensed pass list", settings.condensedPasses) { onAction(SettingsAction.SetCondensedPasses(it)) } }
            item { SettingSwitch("Automatic barcode brightness", settings.automaticBrightness) { onAction(SettingsAction.SetAutomaticBrightness(it)) } }
            item { Text("Sort order", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) }
            items(PassSortOrder.entries) { order ->
                ListItem(
                    headlineContent = { Text(order.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)) },
                    trailingContent = { androidx.compose.material3.RadioButton(order == settings.sortOrder, { onAction(SettingsAction.SetSortOrder(order)) }) },
                    modifier = Modifier.clickable { onAction(SettingsAction.SetSortOrder(order)) },
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(label) }, trailingContent = { Switch(checked, onChange) }, modifier = Modifier.clickable { onChange(!checked) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Help") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("PassAndroid", style = MaterialTheme.typography.headlineMedium)
            Text("Store and use Apple Wallet pass files on Android. Import .pkpass and .espass files, inspect their data, show barcodes, edit local copies, and export them through the system document picker.")
            Text("This modernization keeps the original GPL-3.0 license and project attribution.")
        }
    }
}
