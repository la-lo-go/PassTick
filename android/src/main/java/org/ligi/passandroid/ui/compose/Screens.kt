package org.ligi.passandroid.ui.compose

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import org.ligi.passandroid.repository.PassDetailSection
import org.ligi.passandroid.repository.HomeCardSection
import org.ligi.passandroid.reminder.NotificationLockScreenDetail
import org.ligi.passandroid.reminder.NotificationAction
import org.ligi.passandroid.repository.defaultPassDetailSectionOrder
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.CategorySettingsAction
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.PassDraft
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationDraft
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.displayArtwork
import org.ligi.passandroid.platform.PassImageExportMode
import org.ligi.passandroid.platform.PassImageExportSelection
import org.ligi.passandroid.ui.state.SettingsAction
import org.ligi.passandroid.ui.barcode.ExpandedPassCodeDialog
import org.ligi.passandroid.ui.barcode.PassCodePreview
import org.threeten.bp.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PassDetailScreen(
    pass: PassUiModel?,
    allPassesProtected: Boolean = false,
    categories: List<PassCategory> = emptyList(),
    passReminderEnabled: Boolean = false,
    remindersGloballyEnabled: Boolean = false,
    reminderLeadMinutes: Int? = null,
    reminderExactAtEvent: Boolean = false,
    reminderActionOverride: Set<NotificationAction>? = null,
    initialCodeExpanded: Boolean = false,
    onInitialCodeShown: () -> Unit = {},
    flashlightAvailable: Boolean = false,
    flashlightEnabled: Boolean = false,
    enhanceCodeBrightness: Boolean = true,
    calendarEventPresent: Boolean = false,
    passDetailSectionOrder: List<PassDetailSection> = defaultPassDetailSectionOrder,
    hiddenPassDetailSections: Set<PassDetailSection> = emptySet(),
    onAction: (PassDetailAction) -> Unit,
) {
    var overflowOpen by remember { mutableStateOf(false) }
    var tagMenuOpen by remember { mutableStateOf(false) }
    var imageExportOpen by remember { mutableStateOf(false) }
    var customExportOpen by remember { mutableStateOf(false) }
    var customArtwork by remember { mutableStateOf(true) }
    var customText by remember { mutableStateOf(true) }
    var customBarcode by remember { mutableStateOf(true) }
    var configureReminder by remember { mutableStateOf(false) }
    var advancedReminderActions by remember(pass?.id) { mutableStateOf(false) }
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
            enhanceBrightness = enhanceCodeBrightness,
            onDismiss = {
                codeHeld = false
                codePinned = false
            },
        )
    }
    DisposableEffect(Unit) {
        onDispose { onAction(PassDetailAction.SetFlashlightEnabled(false)) }
    }
    if (configureReminder) {
        AlertDialog(
            onDismissRequest = { configureReminder = false },
            title = { Text("Pass reminder") },
            text = {
                Column {
                    ReminderChoice(
                        label = "Use default reminder times",
                        selected = passReminderEnabled && reminderLeadMinutes == null && !reminderExactAtEvent,
                    ) {
                        configureReminder = false
                        onAction(PassDetailAction.ConfigureReminder(true, null))
                    }
                    ReminderChoice("At event time", passReminderEnabled && reminderExactAtEvent) {
                        configureReminder = false
                        onAction(PassDetailAction.ConfigureReminder(true, 0, exactAtEvent = true))
                    }
                    listOf(15 to "15 minutes before", 30 to "30 minutes before", 60 to "1 hour before", 1440 to "1 day before")
                        .forEach { (minutes, label) ->
                            ReminderChoice(label, passReminderEnabled && reminderLeadMinutes == minutes) {
                                configureReminder = false
                                onAction(PassDetailAction.ConfigureReminder(true, minutes))
                            }
                        }
                    ReminderChoice("Off for this pass", !passReminderEnabled) {
                        configureReminder = false
                        onAction(PassDetailAction.ConfigureReminder(false, null))
                    }
                    TextButton(onClick = { advancedReminderActions = !advancedReminderActions }) {
                        Text(if (advancedReminderActions) "Hide actions" else "Notification actions")
                    }
                    if (advancedReminderActions) {
                        val available = buildSet {
                            if (pass?.barcodeFormat != null && !pass.barcodeMessage.isNullOrBlank()) add(NotificationAction.OPEN_CODE)
                            if (pass?.locations?.isNotEmpty() == true) add(NotificationAction.DIRECTIONS)
                            add(NotificationAction.SNOOZE)
                        }
                        val selected = reminderActionOverride ?: available
                        if (reminderActionOverride != null) {
                            TextButton(onClick = { onAction(PassDetailAction.SetReminderActions(null)) }) {
                                Text("Use default actions")
                            }
                        }
                        available.forEach { action ->
                            ReminderActionSetting(action, action in selected) { enabled ->
                                onAction(
                                    PassDetailAction.SetReminderActions(
                                        selected.toMutableSet().apply { if (enabled) add(action) else remove(action) },
                                    ),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { configureReminder = false }) { Text("Cancel") }
            },
        )
    }
    if (imageExportOpen) {
        AlertDialog(
            onDismissRequest = { imageExportOpen = false },
            title = { Text("Export as image") },
            text = { Text("Choose the content to include in the PNG image.") },
            confirmButton = {
                Column {
                    TextButton(onClick = { imageExportOpen = false; onAction(PassDetailAction.ExportImage(PassImageExportMode.FULL)) }) { Text("Full pass") }
                    TextButton(onClick = { imageExportOpen = false; onAction(PassDetailAction.ExportImage(PassImageExportMode.BARCODE)) }) { Text("Barcode only") }
                    TextButton(onClick = { imageExportOpen = false; customExportOpen = true }) { Text("Custom") }
                }
            },
            dismissButton = { TextButton(onClick = { imageExportOpen = false }) { Text("Cancel") } },
        )
    }
    if (customExportOpen) {
        AlertDialog(
            onDismissRequest = { customExportOpen = false },
            title = { Text("Custom image") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(customArtwork, { customArtwork = it }); Text("Artwork") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(customText, { customText = it }); Text("Pass details") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(customBarcode, { customBarcode = it }); Text("Barcode") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    customExportOpen = false
                    onAction(PassDetailAction.ExportImage(PassImageExportMode.CUSTOM, PassImageExportSelection(customArtwork, customText, customBarcode)))
                }, enabled = customArtwork || customText || customBarcode) { Text("Export") }
            },
            dismissButton = { TextButton(onClick = { customExportOpen = false }) { Text("Cancel") } },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pass?.description ?: "Pass") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { onAction(PassDetailAction.Back) }, shape = CircleShape) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Box {
                        FilledTonalIconButton(onClick = { overflowOpen = true }, enabled = pass != null, shape = CircleShape) {
                            Icon(Icons.Default.MoreVert, "Pass actions")
                        }
                        DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Print") },
                                leadingIcon = { Icon(Icons.Default.Print, null) },
                                onClick = {
                                overflowOpen = false
                                onAction(PassDetailAction.Print)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Export as image") },
                                leadingIcon = { Icon(Icons.Default.Image, null) },
                                onClick = { overflowOpen = false; imageExportOpen = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Customize pass") },
                                leadingIcon = { Icon(Icons.Default.Visibility, null) },
                                onClick = {
                                    overflowOpen = false
                                    onAction(PassDetailAction.OpenPassCustomization)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Configure reminder") },
                                leadingIcon = { Icon(Icons.Default.Notifications, null) },
                                enabled = pass?.calendarEvent != null,
                                onClick = {
                                    overflowOpen = false
                                    if (remindersGloballyEnabled) {
                                        configureReminder = true
                                    } else {
                                        onAction(PassDetailAction.OpenReminderSettings)
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Manage tags") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) },
                                onClick = {
                                    overflowOpen = false
                                    tagMenuOpen = true
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when {
                                            allPassesProtected -> "Protected by privacy settings"
                                            pass?.isProtected == true -> "Remove protection"
                                            else -> "Protect pass"
                                        },
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (pass?.isProtected == true && !allPassesProtected) Icons.Default.LockOpen
                                        else Icons.Default.Lock,
                                        null,
                                    )
                                },
                                enabled = !allPassesProtected,
                                onClick = {
                                    overflowOpen = false
                                    onAction(PassDetailAction.SetProtected(pass?.isProtected != true))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete pass") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = {
                                    overflowOpen = false
                                    onAction(PassDetailAction.Delete)
                                },
                            )
                        }
                        DropdownMenu(expanded = tagMenuOpen, onDismissRequest = { tagMenuOpen = false }) {
                            val tags = categories.filter { it.role == PassCategoryRole.CUSTOM }
                            tags.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    leadingIcon = {
                                        Icon(
                                            categoryIcon(category.icon),
                                            null,
                                            tint = Color(category.colorArgb.toInt()),
                                        )
                                    },
                                    trailingIcon = { Checkbox(category.id in (pass?.tagIds ?: emptySet()), null) },
                                    onClick = {
                                        val selected = pass?.tagIds ?: emptySet()
                                        onAction(PassDetailAction.SetTags(if (category.id in selected) selected - category.id else selected + category.id))
                                    },
                                )
                            }
                            if (tags.isNotEmpty()) HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Add new tag") },
                                leadingIcon = { Icon(Icons.Default.Add, null) },
                                onClick = {
                                    tagMenuOpen = false
                                    onAction(PassDetailAction.OpenTagSettings)
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (pass != null) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    floatingActionButton = {
                        FloatingToolbarDefaults.StandardFloatingActionButton(onClick = { onAction(PassDetailAction.Edit) }) {
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
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 136.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                val artwork = pass.artwork.firstOrNull { it.kind == PassArtworkKind.STRIP }
                    ?: pass.artwork.firstOrNull { it.kind == PassArtworkKind.LOGO }
                    ?: pass.artwork.firstOrNull { it.kind == PassArtworkKind.THUMBNAIL }
                val visibleFields = pass.fields.filterNot { field ->
                    field.hidden || (pass.calendarEvent != null && field.value.containsDateAndTime())
                }
                if (pass.isProtected || allPassesProtected) item {
                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                        ListItem(
                            leadingContent = { Icon(Icons.Default.Lock, null) },
                            supportingContent = {
                                Text(
                                    if (allPassesProtected) "Locked by app protection"
                                    else "Unlocked with fingerprint or screen lock",
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        ) { Text("Protected pass") }
                    }
                }
                passDetailSectionOrder
                    .filterNot(hiddenPassDetailSections::contains)
                    .forEach { section ->
                        when (section) {
                            PassDetailSection.ARTWORK -> if (artwork != null) item {
                                Box(Modifier.fillMaxWidth().height(160.dp)) {
                                    PassArtwork(pass, listOf(artwork.kind), Modifier.fillMaxSize())
                                }
                            }
                            PassDetailSection.BARCODE -> item {
                                BarcodeCard(
                                    pass = pass,
                                    emphasized = artwork == null || PassDetailSection.ARTWORK in hiddenPassDetailSections,
                                    onHoldChanged = { codeHeld = it },
                                    onPin = { codePinned = true },
                                )
                            }
                            PassDetailSection.FIELDS -> if (visibleFields.isNotEmpty()) item {
                                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                                    Column(Modifier.padding(vertical = 8.dp)) {
                                        visibleFields.forEach { field ->
                                            ListItem(
                                                supportingContent = { Text(field.label) },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                            ) { Text(field.value) }
                                        }
                                    }
                                }
                            }
                            PassDetailSection.LOCATIONS -> if (pass.locations.isNotEmpty()) item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        pass.locations.forEachIndexed { index, location ->
                                            val label = location.name?.takeIf(String::isNotBlank)
                                                ?: "${location.latitude}, ${location.longitude}"
                                            val locationShape = RoundedCornerShape(28.dp)
                                            Surface(
                                                modifier = Modifier.fillMaxWidth().clip(locationShape).clickable {
                                                    onAction(PassDetailAction.OpenLocation(index))
                                                },
                                                shape = locationShape,
                                                color = MaterialTheme.colorScheme.surfaceContainer,
                                            ) {
                                                ListItem(
                                                    leadingContent = { Icon(Icons.Default.LocationOn, null) },
                                                    supportingContent = { Text("Open in Maps") },
                                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                                ) { Text(label) }
                                            }
                                        }
                                }
                            }
                            PassDetailSection.CALENDAR -> pass.calendarEvent?.let {
                                item {
                                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                                        ListItem(
                                            leadingContent = { Icon(Icons.Default.CalendarMonth, null) },
                                            supportingContent = {
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    pass.calendarDateTimeLines().forEach { Text(it) }
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(if (calendarEventPresent) "Already in calendar" else "Add to calendar")
                                                        if (calendarEventPresent) Icon(Icons.Default.Check, "In calendar", Modifier.size(18.dp))
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().clickable(enabled = !calendarEventPresent) { onAction(PassDetailAction.AddToCalendar) },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        ) { Text("Date and time") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderActionSetting(action: NotificationAction, enabled: Boolean, onEnabled: (Boolean) -> Unit) {
    val label = when (action) {
        NotificationAction.OPEN_CODE -> "Open code"
        NotificationAction.DIRECTIONS -> "Directions"
        NotificationAction.SNOOZE -> "Snooze"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(enabled, onCheckedChange = onEnabled)
        Text(label)
    }
}

@Composable
private fun ReminderChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        trailingContent = { RadioButton(selected, onClick) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) { Text(label) }
}

@Composable
private fun PassArtwork(pass: PassUiModel, preferredKinds: List<PassArtworkKind>, modifier: Modifier) {
    val artwork = pass.displayArtwork(preferredKinds) ?: return
    AdaptivePassArtwork(
        bytes = artwork.bytes,
        kind = artwork.kind,
        accentColor = pass.accentColor,
        contentDescription = "Pass artwork",
        modifier = modifier,
    )
}

@Composable
private fun BarcodeCard(
    pass: PassUiModel,
    emphasized: Boolean,
    onHoldChanged: (Boolean) -> Unit,
    onPin: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Color.White, contentColor = Color.Black) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val format = pass.barcodeFormat
            val message = pass.barcodeMessage
            if (format != null && !message.isNullOrBlank()) {
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val height = if (format.isQuadratic()) {
                        (maxWidth * 0.78f).coerceIn(196.dp, if (emphasized) 300.dp else 264.dp)
                    } else {
                        (maxWidth / 2.6f).coerceIn(144.dp, if (emphasized) 240.dp else 208.dp)
                    }
                    PassCodePreview(format, message, onHoldChanged, onPin, Modifier.fillMaxWidth().height(height))
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

private fun <T> List<T>.moveItem(index: Int, offset: Int): List<T> {
    val to = (index + offset).coerceIn(indices)
    if (to == index) return this
    return toMutableList().apply { add(to, removeAt(index)) }
}

private fun PassType.displayName() = name.lowercase().replaceFirstChar(Char::uppercase)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = { onAction(SettingsAction.Back) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.fillMaxHeight().widthIn(max = 760.dp).align(Alignment.TopCenter).testTag("settings_list"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item { AppearanceSettings(settings, onAction) }
                item { HomeSettings(settings, onAction) }
                item { PassListSettings(onAction) }
                item { PrivacySettings(settings, onAction) }
                item { CalendarSettings(settings, onAction) }
                item { NotificationSettings(settings, onAction) }
            }
        }
    }
}

@Composable
private fun AppearanceSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup("Appearance") {
        Text("Theme", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
        ThemeMode.entries.forEach { mode -> ThemeSetting(mode, settings.themeMode, onAction) }
        if (settings.themeMode == ThemeMode.DARK) {
            SettingSwitch("Use AMOLED black background", settings.amoledBlackBackground) {
                onAction(SettingsAction.SetAmoledBlackBackground(it))
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        SettingSwitch("Use HDR and maximum code brightness", settings.automaticBrightness) {
            onAction(SettingsAction.SetAutomaticBrightness(it))
        }
    }
}

@Composable
private fun ThemeSetting(mode: ThemeMode, selectedMode: ThemeMode, onAction: (SettingsAction) -> Unit) {
    ListItem(
        trailingContent = { RadioButton(mode == selectedMode, { onAction(SettingsAction.SetTheme(mode)) }) },
        modifier = Modifier.clickable { onAction(SettingsAction.SetTheme(mode)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }
}

@Composable
private fun HomeSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup("Home") {
        SettingSwitch(
            "Highlight today's passes",
            settings.highlightTodayPasses,
            supportingText = "Show today's passes before other passes",
        ) { onAction(SettingsAction.SetHighlightTodayPasses(it)) }
    }
}

@Composable
private fun PassListSettings(onAction: (SettingsAction) -> Unit) {
    SettingsGroup("Customize pass list") {
        PassListSetting(Icons.Default.ViewAgenda, "Home cards") {
            onAction(SettingsAction.OpenHomeCardSettings)
        }
        PassListSetting(Icons.Default.Visibility, "Pass view") {
            onAction(SettingsAction.OpenPassViewSettings)
        }
        PassListSetting(Icons.AutoMirrored.Filled.Label, "Tags") {
            onAction(SettingsAction.OpenCategories)
        }
    }
}

@Composable
private fun PassListSetting(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    ListItem(
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) { Text(title) }
}

@Composable
private fun PrivacySettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup("Privacy") {
        SettingSwitch(
            "Protect the app",
            settings.lockAllPasses,
            supportingText = "Require fingerprint or screen lock to open the app",
        ) { onAction(SettingsAction.SetLockAllPasses(it)) }
        SettingSwitch("Show a lock icon on protected passes", settings.showProtectedPassLockIcon) {
            onAction(SettingsAction.SetShowProtectedPassLockIcon(it))
        }
        SettingSwitch("Blur protected pass information", settings.blurProtectedPassCards) {
            onAction(SettingsAction.SetBlurProtectedPassCards(it))
        }
        SettingSwitch("Keep protected passes in a locked section", settings.separateProtectedPasses) {
            onAction(SettingsAction.SetSeparateProtectedPasses(it))
        }
        SettingSwitch(
            "Block screenshots",
            settings.blockScreenshots,
            supportingText = "Prevent screenshots on protected content",
        ) { onAction(SettingsAction.SetBlockScreenshots(it)) }
    }
}

@Composable
private fun CalendarSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup("Calendar") {
        SettingSwitch("Automatically add imported passes", settings.offerCalendarAfterImport) {
            onAction(SettingsAction.SetOfferCalendarAfterImport(it))
        }
    }
}

@Composable
private fun NotificationSettings(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    SettingsGroup("Notifications") {
        SettingSwitch("Pass reminders", settings.remindersEnabled) {
            onAction(SettingsAction.SetRemindersEnabled(it))
        }
        Text("Reminder times", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
        reminderOptions.forEach { (minutes, label) ->
            ReminderSetting(minutes, label, settings, onAction)
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        Text("Event access", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
        listOf(15 to "15 minutes", 30 to "30 minutes", 60 to "1 hour").forEach { (minutes, label) ->
            ReminderChoice(label, settings.notificationAccessWindowMinutes == minutes) {
                onAction(SettingsAction.SetNotificationAccessWindow(minutes))
            }
        }
        SettingSwitch(
            "Exact reminders",
            settings.notificationExactTiming,
            supportingText = "Use exact alarms when Android allows them",
        ) { onAction(SettingsAction.SetNotificationExactTiming(it)) }
        SettingSwitch("Notification actions", settings.notificationActionsEnabled) {
            onAction(SettingsAction.SetNotificationActionsEnabled(it))
        }
        if (settings.notificationActionsEnabled) {
            SettingSwitch("Snooze action", settings.notificationSnoozeEnabled) {
                onAction(SettingsAction.SetNotificationSnoozeEnabled(it))
            }
        }
        SettingSwitch("Update when event starts", settings.updateNotificationAtEventStart) {
            onAction(SettingsAction.SetUpdateNotificationAtEventStart(it))
        }
        Text("Lock screen", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
        NotificationLockScreenDetail.entries.forEach { detail ->
            ReminderChoice(detail.displayName(), settings.notificationLockScreenDetail == detail) {
                onAction(SettingsAction.SetNotificationLockScreenDetail(detail))
            }
        }
    }
}

private fun NotificationLockScreenDetail.displayName() = when (this) {
    NotificationLockScreenDetail.FULL -> "Show all"
    NotificationLockScreenDetail.HIDE_SENSITIVE -> "Hide protected details"
    NotificationLockScreenDetail.HIDDEN -> "Hide on lock screen"
}

private val reminderOptions = listOf(
    15 to "15 minutes before",
    30 to "30 minutes before",
    60 to "1 hour before",
    1440 to "1 day before",
)

@Composable
private fun ReminderSetting(
    minutes: Int,
    label: String,
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit,
) {
    val toggle = { onAction(SettingsAction.SetReminderMinutes(settings.reminderMinutes.toggle(minutes))) }
    ListItem(
        trailingContent = {
            Checkbox(
                checked = minutes in settings.reminderMinutes,
                enabled = settings.remindersEnabled,
                onCheckedChange = { toggle() },
            )
        },
        modifier = Modifier.clickable(enabled = settings.remindersEnabled, onClick = toggle),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(
            label,
            color = if (settings.remindersEnabled) Color.Unspecified
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}

private fun Set<Int>.toggle(value: Int) = toMutableSet().apply {
    if (!add(value)) remove(value)
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
fun PassDetailLayoutSettingsScreen(
    order: List<PassDetailSection>,
    hidden: Set<PassDetailSection>,
    onAction: (org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pass view") },
                navigationIcon = {
                    IconButton(onClick = { onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Show or hide sections, then arrange their order.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            itemsIndexed(order, key = { _, section -> section.name }) { index, section ->
                Surface(
                    modifier = Modifier.animateItem(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    ListItem(
                        trailingContent = {
                            Row {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.Move(section, -1)) },
                                ) { Icon(Icons.Default.ArrowUpward, "Move ${section.displayName()} up") }
                                IconButton(
                                    enabled = index < order.lastIndex,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.Move(section, 1)) },
                                ) { Icon(Icons.Default.ArrowDownward, "Move ${section.displayName()} down") }
                                Switch(
                                    checked = section !in hidden,
                                    onCheckedChange = { visible ->
                                        onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.SetVisible(section, visible))
                                    },
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { Text(section.displayName()) }
                }
            }
        }
    }
}

private fun PassDetailSection.displayName() = when (this) {
    PassDetailSection.ARTWORK -> "Pass image"
    PassDetailSection.BARCODE -> "Barcode"
    PassDetailSection.FIELDS -> "Pass details"
    PassDetailSection.LOCATIONS -> "Locations"
    PassDetailSection.CALENDAR -> "Date and time"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCardLayoutSettingsScreen(
    order: List<HomeCardSection>,
    hidden: Set<HomeCardSection>,
    onAction: (org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home cards") },
                navigationIcon = {
                    IconButton(onClick = { onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Choose the card image and arrange the text lines.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            item(key = "artwork") {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    ListItem(
                        supportingContent = { Text("The image stays beside the text") },
                        trailingContent = {
                            Switch(
                                checked = HomeCardSection.ARTWORK !in hidden,
                                onCheckedChange = { visible ->
                                    onAction(
                                        org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.SetVisible(
                                            HomeCardSection.ARTWORK,
                                            visible,
                                        ),
                                    )
                                },
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { Text("Pass image") }
                }
            }
            val textSections = order.filterNot { it == HomeCardSection.ARTWORK }
            itemsIndexed(textSections, key = { _, section -> section.name }) { index, section ->
                Surface(
                    modifier = Modifier.animateItem(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    ListItem(
                        trailingContent = {
                            Row {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.Move(section, -1)) },
                                ) { Icon(Icons.Default.ArrowUpward, "Move ${section.displayName()} up") }
                                IconButton(
                                    enabled = index < textSections.lastIndex,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.Move(section, 1)) },
                                ) { Icon(Icons.Default.ArrowDownward, "Move ${section.displayName()} down") }
                                Switch(
                                    checked = section !in hidden,
                                    onCheckedChange = { visible ->
                                        onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.SetVisible(section, visible))
                                    },
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { Text(section.displayName()) }
                }
            }
        }
    }
}

private fun HomeCardSection.displayName() = when (this) {
    HomeCardSection.ARTWORK -> "Pass image"
    HomeCardSection.TITLE -> "Title"
    HomeCardSection.PRIMARY_FIELD -> "Primary field"
    HomeCardSection.DATE -> "Date and time"
    HomeCardSection.CREATOR -> "Creator"
    HomeCardSection.CATEGORY -> "Tag"
    HomeCardSection.PASS_TYPE -> "Pass type"
}

internal fun PassUiModel.calendarDateTimeLines(): List<String> {
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · HH:mm z")
    val span = calendarTimeSpan ?: return emptyList()
    return buildList {
        span.from?.let { add("Starts: ${it.format(formatter)}") }
        span.to?.let { add("Ends: ${it.format(formatter)}") }
    }
}

private fun String.containsDateAndTime(): Boolean = DATE_AND_TIME_PATTERN.containsMatchIn(this)

private val DATE_AND_TIME_PATTERN = Regex(
    "\\d{1,4}[-/.]\\d{1,2}[-/.]\\d{1,4}.*?\\d{1,2}:\\d{2}",
)

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
            title = { Text("Delete tag?") },
            text = { Text("The tag will be removed from passes in ${category.name}.") },
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
                title = { Text("Tags") },
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
                modifier = Modifier.semantics { contentDescription = "Add tag" },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add tag") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
        ) {
            items(categories.filter { it.role == PassCategoryRole.CUSTOM }, key = PassCategory::id) { category ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    ListItem(
                    supportingContent = null,
                    leadingContent = {
                        Box(
                            Modifier.size(32.dp)
                                .background(Color(category.colorArgb.toInt()), RoundedCornerShape(12.dp)),
                        ) { Icon(categoryIcon(category.icon), category.icon, tint = Color.White, modifier = Modifier.padding(7.dp)) }
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
                                    Icon(Icons.Default.Delete, "Delete ${category.name} tag")
                                }
                            }
                        }
                    },
                    modifier = Modifier.clickable { editing = category },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
    var icon by remember(category.id) { mutableStateOf(category.icon) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category.name.isBlank()) "Add tag" else "Edit tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                ColorPickerField("Tag color", color, { color = it }, Modifier.fillMaxWidth())
                Text("Tag icon", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("label", "star", "event", "flight").forEach { option ->
                        IconButton(onClick = { icon = option }) {
                            Icon(categoryIcon(option), option, tint = if (icon == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(category.copy(name = name.trim(), colorArgb = color.toUInt().toLong(), icon = icon))
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

internal fun categoryIcon(name: String) = when (name) {
    "star" -> Icons.Default.Star
    "event" -> Icons.Default.Event
    "flight" -> Icons.Default.FlightTakeoff
    else -> Icons.AutoMirrored.Filled.Label
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    supportingText: String? = null,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        supportingContent = supportingText?.let { text -> { Text(text) } },
        trailingContent = { Switch(checked, onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(label)
    }
}
