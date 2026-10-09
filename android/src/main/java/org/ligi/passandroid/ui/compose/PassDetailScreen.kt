package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.graphics.Shape
import androidx.annotation.StringRes
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.semantics.Role
import org.ligi.passandroid.ui.state.PassTicketLayout
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.pluralStringResource
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.backFields
import org.ligi.passandroid.ui.state.ticketLayout
import org.ligi.passandroid.ui.state.ticketShowsCalendarDate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.navigation.PassDateField
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.repository.PassDetailSection
import org.ligi.passandroid.reminder.NotificationAction
import org.ligi.passandroid.repository.defaultPassDetailSectionOrder
import org.ligi.passandroid.ui.state.PassBarcodeUiModel
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.displayArtwork
import org.ligi.passandroid.ui.state.hasDisplayableCode
import org.ligi.passandroid.ui.barcode.CodeViewOptions
import org.ligi.passandroid.ui.barcode.ExpandedPassCodeDialog
import org.ligi.passandroid.ui.barcode.PassCodePreview
import org.ligi.passandroid.ui.barcode.codeSizeStepFactor
import org.threeten.bp.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import org.ligi.passandroid.R
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
    enhanceCodeBrightness: Boolean = true,
    codeViewOptions: CodeViewOptions = CodeViewOptions(),
    calendarEventPresent: Boolean = false,
    passDetailSectionOrder: List<PassDetailSection> = defaultPassDetailSectionOrder,
    hiddenPassDetailSections: Set<PassDetailSection> = emptySet(),
    documentPages: PassDocumentPages? = null,
    onAction: (PassDetailAction) -> Unit,
) {
    var overflowOpen by remember { mutableStateOf(false) }
    var tagMenuOpen by remember { mutableStateOf(false) }
    var configureReminder by remember { mutableStateOf(false) }
    var editDateDialog by remember { mutableStateOf(false) }
    var editNotesDialog by remember { mutableStateOf(false) }
    var advancedReminderActions by remember(pass?.id) { mutableStateOf(false) }
    var codeHeld by remember(pass?.id) { mutableStateOf(false) }
    var codePinned by remember(pass?.id) { mutableStateOf(initialCodeExpanded) }
    val codeExpanded = codeHeld || codePinned
    LaunchedEffect(initialCodeExpanded) {
        if (initialCodeExpanded) onInitialCodeShown()
    }
    // One count per displayed pass; the pass id key stops recomposition from counting again.
    LaunchedEffect(pass?.id) {
        if (pass != null && pass.hasDisplayableCode()) onAction(PassDetailAction.RecordUse)
    }
    if (codeExpanded && pass?.barcodeFormat != null && !pass.barcodeMessage.isNullOrBlank()) {
        ExpandedPassCodeDialog(
            format = pass.barcodeFormat,
            message = pass.barcodeMessage,
            alternativeText = pass.barcodeAlternativeText,
            options = codeViewOptions,
            enhanceBrightness = enhanceCodeBrightness,
            onDismiss = {
                codeHeld = false
                codePinned = false
            },
        )
    }
    if (configureReminder) {
        AlertDialog(
            onDismissRequest = { configureReminder = false },
            title = { Text(stringResource(R.string.pass_detail_pass_reminder)) },
            text = {
                Column {
                    ReminderChoice(
                        label = stringResource(R.string.pass_detail_use_default_reminder_times),
                        selected = passReminderEnabled && reminderLeadMinutes == null && !reminderExactAtEvent,
                    ) {
                        configureReminder = false
                        onAction(PassDetailAction.ConfigureReminder(true, null))
                    }
                    ReminderChoice(stringResource(R.string.pass_detail_at_event_time), passReminderEnabled && reminderExactAtEvent) {
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
                    ReminderChoice(stringResource(R.string.pass_detail_off_for_this_pass), !passReminderEnabled) {
                        configureReminder = false
                        onAction(PassDetailAction.ConfigureReminder(false, null))
                    }
                    TextButton(onClick = { advancedReminderActions = !advancedReminderActions }) {
                        Text(if (advancedReminderActions) stringResource(R.string.pass_detail_hide_actions) else stringResource(R.string.settings_notification_actions))
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
                                Text(stringResource(R.string.pass_detail_use_default_actions))
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
                TextButton(onClick = { configureReminder = false }) { Text(stringResource(R.string.pass_detail_cancel)) }
            },
        )
    }
    if (editDateDialog) {
        AlertDialog(
            onDismissRequest = { editDateDialog = false },
            title = { Text(stringResource(R.string.pass_detail_edit_date)) },
            text = { Text(stringResource(R.string.pass_detail_select_the_date_to_edit)) },
            confirmButton = {
                TextButton(onClick = { editDateDialog = false; onAction(PassDetailAction.EditDate(PassDateField.START)) }) {
                    Text(stringResource(R.string.pass_detail_start))
                }
            },
            dismissButton = {
                TextButton(onClick = { editDateDialog = false; onAction(PassDetailAction.EditDate(PassDateField.END)) }) {
                    Text(stringResource(R.string.pass_detail_end))
                }
            },
        )
    }
    if (editNotesDialog) {
        var noteDraft by remember(pass?.id) { mutableStateOf(pass?.notes.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editNotesDialog = false },
            title = { Text(stringResource(R.string.pass_detail_note)) },
            text = {
                OutlinedTextField(
                    noteDraft,
                    { noteDraft = it },
                    label = { Text(stringResource(R.string.layout_notes)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("note_draft"),
                )
            },
            confirmButton = {
                TextButton(onClick = { editNotesDialog = false; onAction(PassDetailAction.SetNotes(noteDraft)) }) {
                    Text(stringResource(R.string.pass_detail_save_note))
                }
            },
            dismissButton = {
                TextButton(onClick = { editNotesDialog = false }) { Text(stringResource(R.string.pass_detail_cancel)) }
            },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                // The ticket shows the pass name, so the bar only names the screen when there is no pass.
                title = { if (pass == null) Text(stringResource(R.string.pass_detail_fallback_title)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { onAction(PassDetailAction.Back) }, shape = CircleShape) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pass_detail_back))
                    }
                },
                actions = {
                    Box {
                        FilledTonalIconButton(onClick = { overflowOpen = true }, enabled = pass != null, shape = CircleShape) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.pass_detail_pass_actions))
                        }
                        DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pass_detail_export_as_image)) },
                                leadingIcon = { Icon(Icons.Default.Image, null) },
                                onClick = { overflowOpen = false; onAction(PassDetailAction.OpenImageExport) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pass_detail_save_pass_file)) },
                                leadingIcon = { Icon(Icons.Default.SaveAlt, null) },
                                onClick = { overflowOpen = false; onAction(PassDetailAction.Export) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pass_detail_save_code)) },
                                leadingIcon = { Icon(Icons.Default.QrCode, null) },
                                enabled = pass?.hasDisplayableCode() == true,
                                onClick = { overflowOpen = false; onAction(PassDetailAction.SaveBarcodeImage) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pass_detail_duplicate)) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                enabled = pass != null,
                                onClick = { overflowOpen = false; onAction(PassDetailAction.Duplicate) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pass_detail_customize_pass)) },
                                leadingIcon = { Icon(Icons.Default.Visibility, null) },
                                onClick = {
                                    overflowOpen = false
                                    onAction(PassDetailAction.OpenPassCustomization)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pass_detail_configure_reminder)) },
                                leadingIcon = { Icon(Icons.Default.Notifications, null) },
                                enabled = pass != null,
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
                                text = { Text(stringResource(R.string.pass_detail_manage_tags)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) },
                                onClick = {
                                    overflowOpen = false
                                    tagMenuOpen = true
                                },
                            )
                            if (pass?.notes.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.pass_detail_add_note)) },
                                    leadingIcon = { Icon(Icons.Default.EditNote, null) },
                                    onClick = {
                                        overflowOpen = false
                                        editNotesDialog = true
                                    },
                                )
                            }
                            if (!pass?.notes.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.pass_detail_remove_note)) },
                                    leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                                    onClick = {
                                        overflowOpen = false
                                        onAction(PassDetailAction.SetNotes(""))
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when {
                                            allPassesProtected -> stringResource(R.string.pass_detail_protected_by_privacy_settings)
                                            pass?.isProtected == true -> stringResource(R.string.home_remove_protection)
                                            else -> stringResource(R.string.home_protect_pass)
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
                                text = {
                                    Text(
                                        stringResource(
                                            if (pass?.isArchived == true) R.string.home_restore else R.string.home_archive,
                                        ),
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (pass?.isArchived == true) Icons.Default.Restore else Icons.Default.Archive,
                                        null,
                                    )
                                },
                                enabled = pass != null,
                                onClick = {
                                    overflowOpen = false
                                    onAction(PassDetailAction.SetArchived(pass?.isArchived != true))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pass_detail_delete_pass)) },
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
                                text = { Text(stringResource(R.string.pass_detail_add_new_tag)) },
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
                FloatingActionButton(onClick = { onAction(PassDetailAction.Edit) }) {
                    Icon(Icons.Default.Edit, stringResource(R.string.pass_detail_edit_pass))
                }
            }
        },
    ) { padding ->
        if (pass == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(stringResource(R.string.pass_detail_pass_not_found)) }
        } else {
            Box(Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    Modifier.fillMaxHeight().widthIn(max = 760.dp).align(Alignment.TopCenter),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 136.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                if (pass.isProtected || allPassesProtected) item {
                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                        ListItem(
                            leadingContent = { Icon(Icons.Default.Lock, null) },
                            supportingContent = {
                                Text(
                                    if (allPassesProtected) stringResource(R.string.pass_detail_locked_by_app_protection) else stringResource(R.string.pass_detail_unlocked_with_fingerprint_or_screen_lock),
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        ) { Text(stringResource(R.string.pass_detail_protected_pass)) }
                    }
                }
                item {
                    PassDetailSectionList(
                        pass = pass,
                        sectionOrder = passDetailSectionOrder,
                        hiddenSections = hiddenPassDetailSections,
                        documentPages = documentPages,
                        calendarEventPresent = calendarEventPresent,
                        codeViewOptions = codeViewOptions,
                        onAction = onAction,
                        onBarcodeHoldChanged = { codeHeld = it },
                        onBarcodePin = { codePinned = true },
                        onEditDateRequested = { editDateDialog = true },
                        onEditNotesRequested = { editNotesDialog = true },
                        quickActions = {
                            PassQuickActions(
                                pass = pass,
                                calendarEventPresent = calendarEventPresent,
                                onAction = onAction,
                                onEditDate = { editDateDialog = true },
                                onConfigureReminder = {
                                    if (remindersGloballyEnabled) configureReminder = true else onAction(PassDetailAction.OpenReminderSettings)
                                },
                            )
                        },
                    )
                }
                }
            }
        }
    }
}

@Composable
internal fun PassDetailSectionList(
    pass: PassUiModel,
    sectionOrder: List<PassDetailSection>,
    hiddenSections: Set<PassDetailSection>,
    documentPages: PassDocumentPages? = null,
    calendarEventPresent: Boolean,
    codeViewOptions: CodeViewOptions = CodeViewOptions(),
    onAction: (PassDetailAction) -> Unit,
    onBarcodeHoldChanged: (Boolean) -> Unit,
    onBarcodePin: () -> Unit,
    onEditDateRequested: () -> Unit,
    onEditNotesRequested: () -> Unit = {},
    modifier: Modifier = Modifier,
    quickActions: (@Composable () -> Unit)? = null,
) {
    val layout = remember(pass) { pass.ticketLayout() }
    val visibleSections = sectionOrder.filterNot(hiddenSections::contains)
    // A pass without a code gets no empty code area.
    val showCode = PassDetailSection.BARCODE in visibleSections && pass.hasDisplayableCode()
    val showTicket = PassDetailSection.FIELDS in visibleSections
    // The ticket joins the details and the code, so it takes the place of whichever comes first.
    val ticketAnchor = visibleSections.firstOrNull { it == PassDetailSection.FIELDS || it == PassDetailSection.BARCODE }
    // Without a banner above it, the code is the main visual and uses the larger size.
    val emphasized = !pass.isDocumentPass || PassDetailSection.ARTWORK in hiddenSections
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!showTicket) quickActions?.invoke()
        visibleSections.forEach { section ->
                when (section) {
                    // Issuer strips and logos crop badly as a banner. The ticket header shows the icon instead.
                    PassDetailSection.ARTWORK -> if (pass.isDocumentPass) {
                        DocumentViewer(
                            pass = pass,
                            documentPages = documentPages,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    PassDetailSection.BARCODE, PassDetailSection.FIELDS -> when {
                        showTicket && section == ticketAnchor -> {
                            PassTicket(
                                pass = pass,
                                layout = layout,
                                code = if (showCode) {
                                    { PassCodes(pass, emphasized, codeViewOptions, onBarcodeHoldChanged, onBarcodePin) }
                                } else {
                                    null
                                },
                            )
                            quickActions?.invoke()
                            BackFields(pass.backFields)
                        }
                        !showTicket && showCode -> BarcodeCard(pass, emphasized, codeViewOptions, onBarcodeHoldChanged, onBarcodePin)
                    }
                    // With one location the Directions quick action already opens it.
                    PassDetailSection.LOCATIONS -> if (pass.locations.size > if (quickActions != null) 1 else 0) {
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
                                        supportingContent = { Text(stringResource(R.string.pass_detail_open_in_maps)) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    ) { Text(label) }
                                }
                            }
                        }
                    }
                    // When the ticket already shows the pass date, a separate date card would repeat it.
                    PassDetailSection.CALENDAR -> if (!(showTicket && pass.ticketShowsCalendarDate(layout))) pass.calendarEvent?.let {
                        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                            ListItem(
                                leadingContent = { Icon(Icons.Default.CalendarMonth, null) },
                                supportingContent = {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        pass.calendarDateTimeLines().forEach { Text(it) }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(if (calendarEventPresent) stringResource(R.string.pass_detail_already_in_calendar) else stringResource(R.string.pass_detail_add_to_calendar))
                                            if (calendarEventPresent) Icon(Icons.Default.Check, stringResource(R.string.pass_detail_in_calendar), Modifier.size(18.dp))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                    enabled = true,
                                    onClick = {
                                        if (!calendarEventPresent) onAction(PassDetailAction.AddToCalendar)
                                    },
                                    onLongClick = { onEditDateRequested() },
                                ),
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            ) { Text(stringResource(R.string.pass_detail_date_and_time)) }
                        }
                    }
                    PassDetailSection.NOTES -> if (pass.notes.isNotBlank()) {
                        val noteShape = RoundedCornerShape(28.dp)
                        Surface(
                            modifier = Modifier.fillMaxWidth().clip(noteShape).clickable { onEditNotesRequested() },
                            shape = noteShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            ListItem(
                                leadingContent = { Icon(Icons.Default.EditNote, null) },
                                supportingContent = { Text(pass.notes) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            ) { Text(stringResource(R.string.pass_detail_note)) }
                        }
                    }
                }
            }
    }
}

@Composable
private fun ReminderActionSetting(action: NotificationAction, enabled: Boolean, onEnabled: (Boolean) -> Unit) {
    val label = when (action) {
        NotificationAction.OPEN_CODE -> stringResource(R.string.pass_detail_open_code)
        NotificationAction.DIRECTIONS -> stringResource(R.string.pass_detail_directions)
        NotificationAction.SNOOZE -> stringResource(R.string.reminder_snooze)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(enabled, onCheckedChange = onEnabled)
        Text(label)
    }
}


@Composable
private fun BarcodeCard(
    pass: PassUiModel,
    emphasized: Boolean,
    codeViewOptions: CodeViewOptions,
    onHoldChanged: (Boolean) -> Unit,
    onPin: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Color.White, contentColor = Color.Black) {
        Box(Modifier.padding(8.dp)) { PassCodes(pass, emphasized, codeViewOptions, onHoldChanged, onPin) }
    }
}

@Composable
private fun PassCodes(
    pass: PassUiModel,
    emphasized: Boolean,
    codeViewOptions: CodeViewOptions,
    onHoldChanged: (Boolean) -> Unit,
    onPin: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val codes = pass.displayableCodes()
        when {
            codes.isEmpty() -> Text(
                stringResource(R.string.pass_detail_no_barcode),
                Modifier.padding(vertical = 8.dp),
                color = Color.Black,
                style = MaterialTheme.typography.titleMedium,
            )
            codes.size == 1 -> SinglePassCode(codes.first(), emphasized, codeViewOptions, onHoldChanged, onPin)
            else -> BarcodePager(codes, emphasized, codeViewOptions, onHoldChanged, onPin)
        }
    }
}

@Composable
private fun PassQuickActions(
    pass: PassUiModel,
    calendarEventPresent: Boolean,
    onAction: (PassDetailAction) -> Unit,
    onEditDate: () -> Unit,
    onConfigureReminder: () -> Unit,
) {
    val hasDate = pass.calendarEvent != null
    val hasLocation = pass.locations.isNotEmpty()
    // Unavailable actions are left out, so a pass without a date or a place shows no inactive buttons.
    val actions = buildList {
        if (hasDate) {
            add(
                QuickActionSpec(
                    icon = if (calendarEventPresent) Icons.Default.EventAvailable else Icons.Default.EditCalendar,
                    label = if (calendarEventPresent) R.string.pass_detail_in_calendar else R.string.pass_detail_action_calendar,
                    selected = calendarEventPresent,
                    onClick = { if (!calendarEventPresent) onAction(PassDetailAction.AddToCalendar) },
                    onLongClick = onEditDate,
                ),
            )
            add(QuickActionSpec(Icons.Default.AddAlert, R.string.pass_detail_action_reminder, onClick = onConfigureReminder))
        }
        if (hasLocation) {
            add(QuickActionSpec(Icons.Default.Directions, R.string.pass_detail_directions, onClick = { onAction(PassDetailAction.OpenLocation(0)) }))
        }
        add(QuickActionSpec(Icons.Default.Share, R.string.pass_detail_action_share, onClick = { onAction(PassDetailAction.Share) }))
    }
    // A connected button group: outer corners are round, inner corners are tight, like the Expressive button group.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        actions.forEachIndexed { index, action ->
            val start = if (index == 0) QuickActionOuterCorner else QuickActionInnerCorner
            val end = if (index == actions.lastIndex) QuickActionOuterCorner else QuickActionInnerCorner
            QuickAction(action, RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end))
        }
    }
}

private class QuickActionSpec(
    val icon: ImageVector,
    @StringRes val label: Int,
    val selected: Boolean = false,
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit,
)

private val QuickActionOuterCorner = 24.dp
private val QuickActionInnerCorner = 6.dp

@Composable
private fun RowScope.QuickAction(action: QuickActionSpec, shape: Shape) {
    val colors = MaterialTheme.colorScheme
    val content = if (action.selected) colors.onPrimary else colors.onSecondaryContainer
    Column(
        Modifier.weight(1f).fillMaxHeight().clip(shape)
            .background(if (action.selected) colors.primary else colors.secondaryContainer)
            .combinedClickable(role = Role.Button, onClick = action.onClick, onLongClick = action.onLongClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(action.icon, null, tint = content)
        Text(
            stringResource(action.label),
            color = content,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun BackFields(fields: List<PassFieldUiModel>) {
    if (fields.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(28.dp)
    Surface(
        Modifier.fillMaxWidth().clip(shape).clickable { expanded = !expanded },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            ListItem(
                leadingContent = { Icon(Icons.Default.Info, null) },
                supportingContent = { Text(pluralStringResource(R.plurals.pass_detail_back_fields, fields.size, fields.size)) },
                trailingContent = { Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            ) { Text(stringResource(R.string.pass_detail_more_information)) }
            if (expanded) {
                fields.forEach { field ->
                    ListItem(
                        overlineContent = { Text(field.label) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { SelectionContainer { Text(field.value) } }
                }
            }
        }
    }
}

private data class DisplayableCode(val format: PassBarCodeFormat, val message: String)

private fun PassUiModel.displayableCodes(): List<DisplayableCode> {
    val candidates = if (barcodes.isNotEmpty()) {
        barcodes
    } else {
        listOf(PassBarcodeUiModel(barcodeFormat, barcodeMessage, barcodeAlternativeText))
    }
    return candidates.mapNotNull { barcode ->
        val format = barcode.format ?: return@mapNotNull null
        val message = barcode.message?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        DisplayableCode(format, message)
    }
}

private fun codeHeight(format: PassBarCodeFormat, cardWidth: Dp, emphasized: Boolean, sizeStep: Int): Dp {
    val base = if (format.isQuadratic()) {
        (cardWidth * 0.78f).coerceIn(196.dp, if (emphasized) 300.dp else 264.dp)
    } else {
        (cardWidth / 2.6f).coerceIn(144.dp, if (emphasized) 240.dp else 208.dp)
    }
    return base * codeSizeStepFactor(sizeStep)
}

@Composable
private fun SinglePassCode(
    code: DisplayableCode,
    emphasized: Boolean,
    options: CodeViewOptions,
    onHoldChanged: (Boolean) -> Unit,
    onPin: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val height = codeHeight(code.format, maxWidth, emphasized, options.sizeStep)
        PassCodePreview(code.format, code.message, options, onHoldChanged, onPin, Modifier.fillMaxWidth().height(height))
    }
}

// Matches the default IconButton touch target reserved on each side of the pager.
private val BarcodePagerArrowWidth = 48.dp

@Composable
private fun BarcodePager(
    codes: List<DisplayableCode>,
    emphasized: Boolean,
    options: CodeViewOptions,
    onHoldChanged: (Boolean) -> Unit,
    onPin: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val showArrows = codes.size > 1
        val pagerWidth = if (showArrows) maxWidth - BarcodePagerArrowWidth * 2 else maxWidth
        val height = codes.maxOf { codeHeight(it.format, pagerWidth, emphasized, options.sizeStep) }
        val pagerState = rememberPagerState(pageCount = { codes.size })
        val scope = rememberCoroutineScope()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showArrows) {
                    BarcodePagerArrow(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous code",
                        enabled = pagerState.currentPage > 0,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                            }
                        },
                    )
                }
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).height(height)) { page ->
                    val code = codes[page]
                    PassCodePreview(code.format, code.message, options, onHoldChanged, onPin, Modifier.fillMaxWidth().height(height))
                }
                if (showArrows) {
                    BarcodePagerArrow(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next code",
                        enabled = pagerState.currentPage < codes.lastIndex,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(codes.lastIndex))
                            }
                        },
                    )
                }
            }
            Surface(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    text = stringResource(R.string.pass_detail_code_page, pagerState.currentPage + 1, codes.size),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun BarcodePagerArrow(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    ) {
        Icon(icon, contentDescription)
    }
}

internal fun PassUiModel.calendarDateTimeLines(): List<String> {
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · HH:mm z")
    val span = calendarTimeSpan ?: return emptyList()
    return buildList {
        span.from?.let { add("Starts: ${it.format(formatter)}") }
        span.to?.let { add("Ends: ${it.format(formatter)}") }
    }
}
