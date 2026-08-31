package org.ligi.passandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.ligi.passandroid.navigation.AppDestination
import org.ligi.passandroid.navigation.PassDeepLinkRequest
import org.ligi.passandroid.navigation.passDeepLinkRequestOrNull
import org.ligi.passandroid.repository.supportedPassImportMimeTypes
import org.ligi.passandroid.ui.compose.EditPassScreen
import org.ligi.passandroid.ui.compose.CategorySettingsScreen
import org.ligi.passandroid.ui.compose.PassDetailLayoutSettingsScreen
import org.ligi.passandroid.ui.compose.HomeCardLayoutSettingsScreen
import org.ligi.passandroid.ui.compose.HomeAction
import org.ligi.passandroid.ui.compose.PassDetailScreen
import org.ligi.passandroid.ui.compose.PassHomeScreen
import org.ligi.passandroid.ui.compose.SettingsScreen
import org.ligi.passandroid.ui.compose.TimelineAction
import org.ligi.passandroid.ui.compose.TimelineScreen
import org.ligi.passandroid.ui.compose.TimelineUiState
import org.ligi.passandroid.ui.compose.UndoOperation
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.CategorySettingsAction
import org.ligi.passandroid.ui.state.MainViewModel
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.SettingsAction
import org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction
import org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction
import org.ligi.passandroid.ui.theme.PassTheme
import org.ligi.passandroid.platform.AndroidFlashlightController
import org.ligi.passandroid.platform.FlashlightState
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.reminder.reminderNotificationsAvailable
import org.ligi.passandroid.ui.adaptive.AdaptivePassListDetailShell

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()
    private val deepLinkRequest = MutableStateFlow<PassDeepLinkRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkRequest.value = intent.data?.passDeepLinkRequestOrNull()
        if (savedInstanceState == null) importFrom(intent)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val requestedPass by deepLinkRequest.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val backStack = rememberNavBackStack(AppDestination.PassList)
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            var showCalendarPermissionWarning by remember { mutableStateOf(false) }
            val undoCategories = remember { mutableMapOf<String, String>() }
            var expandedCodePassId by remember { mutableStateOf<String?>(null) }
            val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                if (uris.isNotEmpty()) viewModel.onAction(AppAction.ImportFiles(uris))
            }
            var hasCameraPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED,
                )
            }
            var enableFlashAfterPermission by remember { mutableStateOf(false) }
            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                hasCameraPermission = granted
                enableFlashAfterPermission = granted
            }
            val flashlightController = remember(hasCameraPermission) {
                if (hasCameraPermission) AndroidFlashlightController(context.applicationContext) else null
            }
            LaunchedEffect(flashlightController, enableFlashAfterPermission) {
                if (enableFlashAfterPermission && flashlightController != null) {
                    flashlightController.setEnabled(true)
                    enableFlashAfterPermission = false
                }
            }
            DisposableEffect(flashlightController) {
                onDispose { flashlightController?.close() }
            }
            val flashlightFlow = remember(flashlightController) {
                flashlightController?.state ?: MutableStateFlow(FlashlightState())
            }
            val flashlight by flashlightFlow.collectAsStateWithLifecycle()
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                if (granted && reminderNotificationsAvailable(context)) {
                    viewModel.onAction(AppAction.SetRemindersEnabled(true))
                } else {
                    viewModel.onAction(AppAction.SetRemindersEnabled(false))
                }
            }
            val calendarPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { permissions ->
                val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                    permissions[Manifest.permission.WRITE_CALENDAR] == true
                viewModel.onAction(AppAction.SetOfferCalendarAfterImport(granted))
                if (!granted) coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar("Calendar permission is required for automatic events")
                }
            }

            fun handleHomeAction(action: HomeAction) {
                when (action) {
                    is HomeAction.OpenPass -> backStack.add(AppDestination.PassDetail(action.id))
                    is HomeAction.SelectCategory -> viewModel.onAction(AppAction.SelectCategory(action.categoryId))
                    is HomeAction.SetSortOrder -> viewModel.onAction(AppAction.SetSortOrder(action.order))
                    is HomeAction.ReorderPass -> viewModel.onAction(AppAction.ReorderPass(action.id, action.offset))
                    is HomeAction.Archive -> {
                        state.passes.firstOrNull { it.id == action.id }?.let {
                            undoCategories["archive:${action.id}"] = it.categoryId
                        }
                        state.categories.firstOrNull { it.role == PassCategoryRole.ARCHIVE }?.let {
                            viewModel.onAction(AppAction.MovePass(action.id, it.id, announce = false))
                        }
                    }
                    is HomeAction.Restore -> {
                        state.passes.firstOrNull { it.id == action.id }?.let {
                            undoCategories["restore:${action.id}"] = it.categoryId
                        }
                        state.categories.firstOrNull { it.role == PassCategoryRole.INBOX }?.let {
                            viewModel.onAction(AppAction.MovePass(action.id, it.id, announce = false))
                        }
                    }
                    is HomeAction.Delete -> {
                        state.passes.firstOrNull { it.id == action.id }?.let {
                            undoCategories["delete:${action.id}"] = it.categoryId
                        }
                        state.categories.firstOrNull { it.role == PassCategoryRole.TRASH }?.let {
                            viewModel.onAction(AppAction.MovePass(action.id, it.id, announce = false))
                        }
                    }
                    is HomeAction.Undo -> {
                        val key = when (action.operation) {
                            is UndoOperation.Archive -> "archive:${action.operation.passId}"
                            is UndoOperation.Restore -> "restore:${action.operation.passId}"
                            is UndoOperation.Delete -> "delete:${action.operation.passId}"
                        }
                        undoCategories.remove(key)?.let { categoryId ->
                            viewModel.onAction(
                                AppAction.MovePass(action.operation.passId, categoryId, announce = false),
                            )
                        }
                    }
                    HomeAction.ImportPass -> importLauncher.launch(supportedPassImportMimeTypes.toTypedArray())
                    HomeAction.OpenSettings -> backStack.add(AppDestination.Settings)
                    HomeAction.OpenTimeline -> backStack.add(AppDestination.Timeline)
                }
            }

            fun handlePassDetailAction(passId: String, action: PassDetailAction) {
                when (action) {
                    PassDetailAction.Back -> backStack.removeLastOrNull()
                    PassDetailAction.Edit -> backStack.add(AppDestination.EditPass(passId))
                    PassDetailAction.Delete -> {
                        viewModel.onAction(AppAction.DeletePass(passId))
                        backStack.removeLastOrNull()
                    }
                    PassDetailAction.Share -> viewModel.onAction(AppAction.SharePass(passId))
                    PassDetailAction.Print -> viewModel.onAction(AppAction.PrintPass(passId))
                    PassDetailAction.AddToCalendar -> viewModel.onAction(AppAction.AddToCalendar(passId))
                    is PassDetailAction.SetFlashlightEnabled -> {
                        if (action.enabled && !hasCameraPermission) {
                            enableFlashAfterPermission = true
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            flashlightController?.setEnabled(action.enabled)
                        }
                    }
                    PassDetailAction.OpenReminderSettings -> backStack.add(AppDestination.Settings)
                    is PassDetailAction.ConfigureReminder -> viewModel.onAction(
                        AppAction.ConfigurePassReminder(passId, action.enabled, action.leadMinutes),
                    )
                    is PassDetailAction.OpenLocation -> viewModel.onAction(AppAction.OpenLocation(passId, action.index))
                    is PassDetailAction.MoveToCategory -> viewModel.onAction(
                        AppAction.MovePass(passId, action.categoryId),
                    )
                }
            }

            LaunchedEffect(state.message) {
                state.message?.let {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(it)
                    viewModel.onAction(AppAction.ClearMessage)
                }
            }
            LaunchedEffect(requestedPass, state.passes) {
                val request = requestedPass ?: return@LaunchedEffect
                if (state.passes.any { it.id == request.passId }) {
                    if (request.showCode) expandedCodePassId = request.passId
                    backStack.add(AppDestination.PassDetail(request.passId))
                    deepLinkRequest.value = null
                }
            }
            LaunchedEffect(state.settings.remindersEnabled) {
                if (
                    state.settings.remindersEnabled &&
                    (
                        (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                            ) ||
                            !reminderNotificationsAvailable(this@MainActivity)
                        )
                ) {
                    viewModel.onAction(AppAction.SetRemindersEnabled(false))
                }
            }
            LaunchedEffect(state.settings.offerCalendarAfterImport) {
                if (
                    state.settings.offerCalendarAfterImport &&
                    (
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CALENDAR) !=
                            PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_CALENDAR) !=
                            PackageManager.PERMISSION_GRANTED
                        )
                ) {
                    viewModel.onAction(AppAction.SetOfferCalendarAfterImport(false))
                }
            }

            PassTheme(state.settings.themeMode, state.settings.amoledBlackBackground) {
                if (showCalendarPermissionWarning) {
                    AlertDialog(
                        onDismissRequest = { showCalendarPermissionWarning = false },
                        title = { Text("Add events automatically?") },
                        text = {
                            Text(
                                "Dated passes will be added directly to your primary writable calendar after import. " +
                                    "You can turn this off at any time.",
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showCalendarPermissionWarning = false
                                    val permissions = arrayOf(
                                        Manifest.permission.READ_CALENDAR,
                                        Manifest.permission.WRITE_CALENDAR,
                                    )
                                    if (permissions.all {
                                            ContextCompat.checkSelfPermission(this@MainActivity, it) ==
                                                PackageManager.PERMISSION_GRANTED
                                        }
                                    ) {
                                        viewModel.onAction(AppAction.SetOfferCalendarAfterImport(true))
                                    } else {
                                        calendarPermissionLauncher.launch(permissions)
                                    }
                                },
                            ) { Text("Allow") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCalendarPermissionWarning = false }) { Text("Cancel") }
                        },
                    )
                }
                Surface {
                    Box(Modifier.fillMaxSize()) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            entry<AppDestination.PassList> {
                                PassHomeScreen(
                                    state = state,
                                    showTodayHero = state.settings.highlightTodayPasses,
                                    onAction = ::handleHomeAction,
                                )
                            }
                            entry<AppDestination.PassDetail> { destination ->
                                AdaptivePassListDetailShell(
                                    selectedDestination = destination,
                                    listPane = {
                                        PassHomeScreen(
                                            state = state,
                                            showTodayHero = state.settings.highlightTodayPasses,
                                            onAction = { action ->
                                                if (action is HomeAction.OpenPass) {
                                                    backStack.removeLastOrNull()
                                                    backStack.add(AppDestination.PassDetail(action.id))
                                                } else {
                                                    handleHomeAction(action)
                                                }
                                            },
                                        )
                                    },
                                    detailPane = { selected ->
                                        val pass = state.passes.firstOrNull { it.id == selected.passId }
                                        var calendarEventPresent by remember(selected.passId) { mutableStateOf(false) }
                                        LaunchedEffect(pass?.calendarEvent) {
                                            calendarEventPresent = pass?.let { viewModel.isCalendarEventPresent(it) } == true
                                        }
                                        val lifecycleOwner = LocalLifecycleOwner.current
                                        DisposableEffect(lifecycleOwner, pass?.id, pass?.calendarEvent) {
                                            val observer = LifecycleEventObserver { _, event ->
                                                if (event == Lifecycle.Event.ON_RESUME && pass != null) {
                                                    coroutineScope.launch {
                                                        calendarEventPresent = viewModel.isCalendarEventPresent(pass)
                                                    }
                                                }
                                            }
                                            lifecycleOwner.lifecycle.addObserver(observer)
                                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                                        }
                                        PassDetailScreen(
                                            pass = pass,
                                            categories = state.categories,
                                            passReminderEnabled = state.settings.remindersEnabled &&
                                                selected.passId !in state.settings.reminderExcludedPassIds,
                                            remindersGloballyEnabled = state.settings.remindersEnabled,
                                            reminderLeadMinutes = state.settings.reminderLeadMinutesByPass[selected.passId],
                                            initialCodeExpanded = expandedCodePassId == selected.passId,
                                            onInitialCodeShown = { expandedCodePassId = null },
                                            flashlightAvailable = flashlight.isAvailable || !hasCameraPermission,
                                            flashlightEnabled = flashlight.isEnabled,
                                            enhanceCodeBrightness = state.settings.automaticBrightness,
                                            calendarEventPresent = calendarEventPresent,
                                            passDetailSectionOrder = state.settings.passDetailSectionOrder,
                                            hiddenPassDetailSections = state.settings.hiddenPassDetailSections,
                                            onAction = { action ->
                                                handlePassDetailAction(selected.passId, action)
                                            },
                                        )
                                    },
                                )
                            }
                            entry<AppDestination.Timeline> {
                                TimelineScreen(
                                    state = TimelineUiState(
                                        timeline = state.timeline,
                                        reminderEventIds = if (state.settings.remindersEnabled) {
                                            state.timeline.days.flatMap { it.events }
                                                .filterNot { it.pass.passId in state.settings.reminderExcludedPassIds }
                                                .mapTo(mutableSetOf()) { it.id }
                                        } else {
                                            emptySet()
                                        },
                                    ),
                                    onAction = { action ->
                                    when (action) {
                                        TimelineAction.Back -> backStack.removeLastOrNull()
                                        is TimelineAction.OpenPass -> backStack.add(AppDestination.PassDetail(action.passId))
                                        is TimelineAction.AddToCalendar -> state.timeline.days
                                            .flatMap { it.events }
                                            .firstOrNull { it.id == action.eventId }
                                            ?.let { viewModel.onAction(AppAction.AddToCalendar(it.pass.passId)) }
                                        is TimelineAction.ConfigureReminder -> {
                                            val event = state.timeline.days.flatMap { it.events }
                                                .firstOrNull { it.id == action.eventId }
                                            if (!state.settings.remindersEnabled) {
                                                backStack.add(AppDestination.Settings)
                                            } else if (event != null) {
                                                viewModel.onAction(AppAction.TogglePassReminder(event.pass.passId))
                                            }
                                        }
                                    }
                                    },
                                )
                            }
                            entry<AppDestination.EditPass> { destination ->
                                EditPassScreen(
                                    pass = state.passes.firstOrNull { it.id == destination.passId },
                                    onAction = { action ->
                                        when (action) {
                                            EditPassAction.Back -> backStack.removeLastOrNull()
                                            is EditPassAction.Save -> {
                                                viewModel.onAction(AppAction.SavePass(destination.passId, action.draft))
                                                backStack.removeLastOrNull()
                                            }
                                        }
                                    },
                                )
                            }
                            entry<AppDestination.Settings> {
                                SettingsScreen(state.settings) { action ->
                                    when (action) {
                                        SettingsAction.Back -> backStack.removeLastOrNull()
                                        is SettingsAction.SetTheme -> viewModel.onAction(AppAction.SetTheme(action.value))
                                        is SettingsAction.SetAmoledBlackBackground -> viewModel.onAction(
                                            AppAction.SetAmoledBlackBackground(action.value),
                                        )
                                        is SettingsAction.SetAutomaticBrightness -> viewModel.onAction(AppAction.SetAutomaticBrightness(action.value))
                                        is SettingsAction.SetSortOrder -> viewModel.onAction(AppAction.SetSortOrder(action.value))
                                        SettingsAction.OpenCategories -> backStack.add(AppDestination.CategorySettings)
                                        SettingsAction.OpenPassDetailLayout -> backStack.add(AppDestination.PassDetailLayoutSettings)
                                        SettingsAction.OpenHomeCardLayout -> backStack.add(AppDestination.HomeCardLayoutSettings)
                                        is SettingsAction.SetHighlightTodayPasses -> viewModel.onAction(
                                            AppAction.SetHighlightTodayPasses(action.value),
                                        )
                                        is SettingsAction.SetAutomaticallyMarkPast -> viewModel.onAction(
                                            AppAction.SetAutomaticallyMarkPast(action.value),
                                        )
                                        is SettingsAction.SetOfferCalendarAfterImport -> {
                                            if (action.value) {
                                                showCalendarPermissionWarning = true
                                            } else {
                                                viewModel.onAction(AppAction.SetOfferCalendarAfterImport(false))
                                            }
                                        }
                                        is SettingsAction.SetRemindersEnabled -> {
                                            if (!action.value) {
                                                viewModel.onAction(AppAction.SetRemindersEnabled(false))
                                            } else if (
                                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                ContextCompat.checkSelfPermission(
                                                    this@MainActivity,
                                                    Manifest.permission.POST_NOTIFICATIONS,
                                                ) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else if (!reminderNotificationsAvailable(this@MainActivity)) {
                                                viewModel.onAction(AppAction.SetRemindersEnabled(false))
                                                startActivity(
                                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                                    },
                                                )
                                                coroutineScope.launch {
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    snackbarHostState.showSnackbar(
                                                        "Allow pass reminders in Android notification settings",
                                                    )
                                                }
                                            } else {
                                                viewModel.onAction(AppAction.SetRemindersEnabled(true))
                                            }
                                        }
                                        is SettingsAction.SetReminderMinutes -> viewModel.onAction(
                                            AppAction.SetReminderMinutes(action.value),
                                        )
                                    }
                                }
                            }
                            entry<AppDestination.CategorySettings> {
                                CategorySettingsScreen(state.settings.categories) { action ->
                                    when (action) {
                                        CategorySettingsAction.Back -> backStack.removeLastOrNull()
                                        is CategorySettingsAction.Save -> viewModel.onAction(
                                            AppAction.SaveCategory(action.category),
                                        )
                                        is CategorySettingsAction.Delete -> viewModel.onAction(
                                            AppAction.DeleteCategory(action.categoryId),
                                        )
                                        is CategorySettingsAction.Move -> viewModel.onAction(
                                            AppAction.MoveCategory(action.categoryId, action.offset),
                                        )
                                    }
                                }
                            }
                            entry<AppDestination.PassDetailLayoutSettings> {
                                PassDetailLayoutSettingsScreen(
                                    order = state.settings.passDetailSectionOrder,
                                    hidden = state.settings.hiddenPassDetailSections,
                                ) { action ->
                                    val order = state.settings.passDetailSectionOrder
                                    val hidden = state.settings.hiddenPassDetailSections
                                    when (action) {
                                        PassDetailLayoutSettingsAction.Back -> backStack.removeLastOrNull()
                                        is PassDetailLayoutSettingsAction.Move -> {
                                            val from = order.indexOf(action.section)
                                            val to = (from + action.offset).coerceIn(order.indices)
                                            if (from >= 0 && from != to) {
                                                val updated = order.toMutableList().apply { add(to, removeAt(from)) }
                                                viewModel.onAction(AppAction.SetPassDetailLayout(updated, hidden))
                                            }
                                        }
                                        is PassDetailLayoutSettingsAction.SetVisible -> {
                                            val updated = hidden.toMutableSet().apply {
                                                if (action.visible) remove(action.section) else add(action.section)
                                            }
                                            viewModel.onAction(AppAction.SetPassDetailLayout(order, updated))
                                        }
                                    }
                                }
                            }
                            entry<AppDestination.HomeCardLayoutSettings> {
                                HomeCardLayoutSettingsScreen(
                                    order = state.settings.homeCardSectionOrder,
                                    hidden = state.settings.hiddenHomeCardSections,
                                ) { action ->
                                    val order = state.settings.homeCardSectionOrder
                                    val hidden = state.settings.hiddenHomeCardSections
                                    when (action) {
                                        HomeCardLayoutSettingsAction.Back -> backStack.removeLastOrNull()
                                        is HomeCardLayoutSettingsAction.Move -> {
                                            val textOrder = order.filterNot { it == org.ligi.passandroid.repository.HomeCardSection.ARTWORK }
                                            val from = textOrder.indexOf(action.section)
                                            val to = (from + action.offset).coerceIn(textOrder.indices)
                                            if (from >= 0 && from != to) {
                                                val updated = listOf(org.ligi.passandroid.repository.HomeCardSection.ARTWORK) +
                                                    textOrder.toMutableList().apply { add(to, removeAt(from)) }
                                                viewModel.onAction(AppAction.SetHomeCardLayout(updated, hidden))
                                            }
                                        }
                                        is HomeCardLayoutSettingsAction.SetVisible -> {
                                            val updated = hidden.toMutableSet().apply {
                                                if (action.visible) remove(action.section) else add(action.section)
                                            }
                                            viewModel.onAction(AppAction.SetHomeCardLayout(order, updated))
                                        }
                                    }
                                }
                            }
                        },
                    )
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(16.dp),
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkRequest.value = intent.data?.passDeepLinkRequestOrNull()
        importFrom(intent)
    }

    private fun importFrom(intent: Intent) {
        val uris = intent.importUris()
        if (uris.isNotEmpty()) viewModel.onAction(AppAction.ImportFiles(uris))
    }
}

@Suppress("DEPRECATION")
private fun Intent.importUris(): List<android.net.Uri> = buildList {
    data?.let(::add)
    clipData?.let { clip ->
        repeat(clip.itemCount) { index -> clip.getItemAt(index).uri?.let(::add) }
    }
    if (action == Intent.ACTION_SEND_MULTIPLE) {
        getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let(::addAll)
    } else {
        getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let(::add)
    }
}.filter { it.scheme == "content" }.distinct()
