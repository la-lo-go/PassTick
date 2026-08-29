package org.ligi.passandroid.ui.compose

import android.net.Uri
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
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.R
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.platform.PlatformActions
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.repository.ThemeMode
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.PassDraft
import org.ligi.passandroid.ui.state.PassUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassListScreen(
    state: MainUiState,
    onOpen: (String) -> Unit,
    onImport: () -> Unit,
    onScan: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onScan) { Icon(Icons.Default.QrCodeScanner, "Scan") }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick = { menuOpen = false; onSettings() },
                            )
                            DropdownMenuItem(
                                text = { Text("Help") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Help, null) },
                                onClick = { menuOpen = false; onHelp() },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onImport, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Import pass") })
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
                            row.forEach { pass -> PassCard(pass, state.settings.condensedPasses, Modifier.weight(1f), onOpen) }
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
private fun PassCard(pass: PassUiModel, condensed: Boolean, modifier: Modifier, onOpen: (String) -> Unit) {
    Card(modifier.clickable { onOpen(pass.id) }) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.size(10.dp, 104.dp).background(Color(pass.accentColor)))
            Column(Modifier.padding(16.dp).weight(1f)) {
                Text(pass.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!condensed) pass.creator?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Spacer(Modifier.height(if (condensed) 2.dp else 8.dp))
                Text(pass.type.name.replace('_', ' '), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassDetailScreen(
    pass: PassUiModel?,
    source: Pass?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    automaticBrightness: Boolean,
    platformActions: PlatformActions,
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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onShare, enabled = pass != null) { Icon(Icons.Default.Share, "Share") }
                    IconButton(onClick = onEdit, enabled = pass != null) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton(onClick = onDelete, enabled = pass != null) { Icon(Icons.Default.Delete, "Delete") }
                },
            )
        },
    ) { padding ->
        if (pass == null || source == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Pass not found") }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { BarcodeCard(pass) }
                items(pass.fields.filterNot { it.hidden }) { field ->
                    ListItem(headlineContent = { Text(field.value) }, supportingContent = { Text(field.label) })
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Export") }
                        OutlinedButton(onClick = { platformActions.print(source) }, modifier = Modifier.weight(1f)) { Text("Print") }
                    }
                }
                source.calendarTimespan?.let { span ->
                    item { Button(onClick = { platformActions.addToCalendar(source, span) }, Modifier.fillMaxWidth()) { Text("Add to calendar") } }
                }
                items(source.locations) { location ->
                    FilledTonalButton(onClick = { platformActions.openLocation(location) }, Modifier.fillMaxWidth()) {
                        Text(location.name ?: "Open location")
                    }
                }
            }
        }
    }
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
fun EditPassScreen(pass: PassUiModel?, onBack: () -> Unit, onSave: (PassDraft) -> Unit) {
    var description by remember(pass?.id) { mutableStateOf(pass?.description.orEmpty()) }
    var creator by remember(pass?.id) { mutableStateOf(pass?.creator.orEmpty()) }
    var barcodeMessage by remember(pass?.id) { mutableStateOf(pass?.barcodeMessage.orEmpty()) }
    var alternativeText by remember(pass?.id) { mutableStateOf(pass?.barcodeAlternativeText.orEmpty()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit pass") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(creator, { creator = it }, label = { Text("Creator") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(barcodeMessage, { barcodeMessage = it }, label = { Text("Barcode data") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(alternativeText, { alternativeText = it }, label = { Text("Barcode text") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    enabled = pass != null && description.isNotBlank(),
                    onClick = { onSave(PassDraft(description, creator, pass?.barcodeFormat, barcodeMessage, alternativeText)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(onBack: () -> Unit, onFileSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onFileSelected) }
    Scaffold(topBar = { TopAppBar(title = { Text("Scan or select") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.QrCodeScanner, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Import a pass file from this device.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { launcher.launch(arrayOf("application/vnd.apple.pkpass", "application/zip")) }) { Text("Select pass file") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: AppSettings, onAction: (AppAction) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { Text("Theme", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) }
            items(ThemeMode.entries) { mode ->
                ListItem(
                    headlineContent = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    trailingContent = { androidx.compose.material3.RadioButton(mode == settings.themeMode, { onAction(AppAction.SetTheme(mode)) }) },
                    modifier = Modifier.clickable { onAction(AppAction.SetTheme(mode)) },
                )
            }
            item { SettingSwitch("Condensed pass list", settings.condensedPasses) { onAction(AppAction.SetCondensedPasses(it)) } }
            item { SettingSwitch("Automatic barcode brightness", settings.automaticBrightness) { onAction(AppAction.SetAutomaticBrightness(it)) } }
            item { Text("Sort order", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) }
            items(PassSortOrder.entries) { order ->
                ListItem(
                    headlineContent = { Text(order.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)) },
                    trailingContent = { androidx.compose.material3.RadioButton(order == settings.sortOrder, { onAction(AppAction.SetSortOrder(order)) }) },
                    modifier = Modifier.clickable { onAction(AppAction.SetSortOrder(order)) },
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
