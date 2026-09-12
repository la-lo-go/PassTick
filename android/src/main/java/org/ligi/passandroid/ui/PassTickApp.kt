package org.ligi.passandroid.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.ligi.passandroid.navigation.AppDestination
import org.ligi.passandroid.navigation.passCustomizationDestination
import org.ligi.passandroid.navigation.PassDeepLinkRequest
import org.ligi.passandroid.repository.supportedPassImportMimeTypes
import org.ligi.passandroid.ui.compose.EditPassScreen
import org.ligi.passandroid.ui.compose.CategorySettingsScreen
import org.ligi.passandroid.ui.compose.ExportImageScreen
import org.ligi.passandroid.ui.compose.PassDetailLayoutSettingsScreen
import org.ligi.passandroid.ui.compose.HomeCardLayoutSettingsScreen
import org.ligi.passandroid.ui.compose.HomeAction
import org.ligi.passandroid.ui.compose.PassDetailScreen
import org.ligi.passandroid.ui.compose.PassCustomizationScreen
import org.ligi.passandroid.ui.compose.PRIVACY_POLICY_URL
import org.ligi.passandroid.ui.compose.PROJECT_REPOSITORY_URL
import org.ligi.passandroid.ui.compose.PassHomeScreen
import org.ligi.passandroid.ui.compose.SettingsScreen
import org.ligi.passandroid.ui.compose.TimelineAction
import org.ligi.passandroid.ui.compose.TimelineScreen
import org.ligi.passandroid.ui.compose.TimelineUiState
import org.ligi.passandroid.ui.compose.toAppAction
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.CategorySettingsAction
import org.ligi.passandroid.ui.state.MainViewModel
import org.ligi.passandroid.ui.state.PROTECTED_PASSES_CATEGORY_ID
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.PassImageExportAction
import org.ligi.passandroid.ui.state.PassCustomizationAction
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.SettingsAction
import org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction
import org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction
import org.ligi.passandroid.ui.theme.PassTheme
import org.ligi.passandroid.repository.StartupAppearanceStore
import org.ligi.passandroid.platform.AndroidFlashlightController
import org.ligi.passandroid.platform.PassAuthenticator
import org.ligi.passandroid.platform.PassImageExporter
import org.ligi.passandroid.repository.PassImageContent
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.reminder.reminderNotificationsAvailable
import org.ligi.passandroid.ui.adaptive.AdaptivePassListDetailShell
import androidx.compose.runtime.Composable
import org.ligi.passandroid.MainActivity
import org.ligi.passandroid.R
import org.ligi.passandroid.repository.StartupAppearance

@Composable
fun PassTickApp(
    activity: MainActivity,
    viewModel: MainViewModel,
    deepLinkRequest: MutableStateFlow<PassDeepLinkRequest?>,
    startupAppearance: StartupAppearance,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val displayedThemeMode = if (state.isContentLoading) startupAppearance.themeMode else state.settings.themeMode
    val displayedAmoled = if (state.isContentLoading) {
        startupAppearance.amoledBlackBackground
    } else {
        state.settings.amoledBlackBackground
    }
    val darkSystemBars = displayedThemeMode == org.ligi.passandroid.repository.ThemeMode.DARK ||
        displayedThemeMode == org.ligi.passandroid.repository.ThemeMode.SYSTEM && isSystemInDarkTheme()
    SideEffect {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars = !darkSystemBars
    }
    LaunchedEffect(state.isContentLoading, state.settings.themeMode, state.settings.amoledBlackBackground) {
        if (!state.isContentLoading) {
            StartupAppearanceStore.write(
                activity,
                state.settings.themeMode,
                state.settings.amoledBlackBackground,
            )
        }
    }
    val requestedPass by deepLinkRequest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backStack = rememberNavBackStack(AppDestination.PassList)
    fun popBackStack() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }
    var protectedPassesUnlocked by remember { mutableStateOf(false) }
    fun requiresProtection(pass: PassUiModel): Boolean = state.settings.lockAllPasses || pass.isProtected
    val protectedContentVisible = when (val destination = backStack.lastOrNull()) {
        is AppDestination.PassDetail -> state.passes.any { it.id == destination.passId && requiresProtection(it) }
        is AppDestination.EditPass -> state.passes.any { it.id == destination.passId && requiresProtection(it) }
        is AppDestination.PassCustomization -> state.passes.any { it.id == destination.passId && requiresProtection(it) }
        AppDestination.PassList, AppDestination.Timeline -> state.passes.any(::requiresProtection) &&
            (!state.settings.separateProtectedPasses || protectedPassesUnlocked)
        else -> false
    }
    val secureContentVisible = protectedContentVisible && state.settings.blockScreenshots
    DisposableEffect(secureContentVisible) {
        if (secureContentVisible) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (secureContentVisible) activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val passAuthenticator = remember { PassAuthenticator(activity) }
    var showCalendarPermissionWarning by remember { mutableStateOf(false) }
    var expandedCodePassId by remember { mutableStateOf<String?>(null) }
    var scrollSettingsToNotifications by rememberSaveable { mutableStateOf(false) }
    var authenticatedPassIds by remember { mutableStateOf(emptySet<String>()) }
    val appLocked = state.settings.lockAllPasses && !protectedPassesUnlocked
    var startupUnlockRequested by remember { mutableStateOf(false) }
    fun requestAppUnlock() {
        if (!passAuthenticator.canAuthenticate()) {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Set a screen lock before protecting every pass")
            }
        } else {
            passAuthenticator.authenticate { authenticated ->
                if (authenticated) {
                    protectedPassesUnlocked = true
                    authenticatedPassIds = authenticatedPassIds +
                        state.passes.filter(::requiresProtection).map(PassUiModel::id)
                }
            }
        }
    }
    LaunchedEffect(appLocked) {
        if (appLocked && !startupUnlockRequested) {
            startupUnlockRequested = true
            requestAppUnlock()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.onAction(AppAction.ImportFiles(uris))
    }
    var imageExporting by remember { mutableStateOf(false) }
    fun exportImageToGallery(pass: PassUiModel, barcodeOnly: Boolean, returnToPass: Boolean) {
        val options = if (barcodeOnly) {
            state.settings.imageExportOptions.copy(
                content = PassImageContent(
                    artwork = false,
                    details = false,
                    barcode = true,
                    dateTime = false,
                    location = false,
                ),
            )
        } else {
            state.settings.imageExportOptions
        }
        coroutineScope.launch {
            imageExporting = true
            try {
                runCatching {
                    withContext(Dispatchers.IO) {
                        PassImageExporter.writeToGallery(
                            activity.contentResolver,
                            imageExportFileName(pass.description),
                            pass,
                            options,
                        )
                    }
                }.onSuccess {
                    if (returnToPass) popBackStack()
                    snackbarHostState.showSnackbar("Image saved to gallery")
                }.onFailure { error ->
                    snackbarHostState.showSnackbar("Image export failed: ${error.message.orEmpty()}")
                }
            } finally {
                imageExporting = false
            }
        }
    }
    val flashlightController = remember { AndroidFlashlightController(context.applicationContext) }
    DisposableEffect(flashlightController) {
        onDispose { flashlightController.close() }
    }
    val flashlightFlow = remember(flashlightController) { flashlightController.state }
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

    fun requestDelete(passId: String) {
        viewModel.onAction(AppAction.SetPassPendingDeletion(passId, true))
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val timeout = launch {
                delay(5_000)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
                message = "Pass deleted",
                actionLabel = "Undo",
                withDismissAction = false,
                duration = SnackbarDuration.Indefinite,
            )
            timeout.cancel()
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onAction(AppAction.SetPassPendingDeletion(passId, false))
            } else {
                viewModel.onAction(AppAction.DeletePass(passId))
            }
        }
    }

    fun openPass(passId: String, showCode: Boolean = false, replaceCurrent: Boolean = false) {
        val pass = state.passes.firstOrNull { it.id == passId } ?: return
        fun navigate() {
            if (showCode) expandedCodePassId = passId
            if (replaceCurrent) popBackStack()
            backStack.add(AppDestination.PassDetail(passId))
        }
        if (!requiresProtection(pass) || protectedPassesUnlocked) {
            navigate()
        } else if (!passAuthenticator.canAuthenticate()) {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Set a screen lock before opening protected passes")
            }
        } else {
            passAuthenticator.authenticate { authenticated ->
                if (authenticated) {
                    authenticatedPassIds = authenticatedPassIds + passId
                    navigate()
                }
            }
        }
    }

    fun setProtectedWithAuthentication(passId: String, protect: Boolean) {
        if (!passAuthenticator.canAuthenticate()) {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Set a screen lock before protecting passes")
            }
            return
        }
        passAuthenticator.authenticate { authenticated ->
            if (authenticated) {
                authenticatedPassIds = if (protect) {
                    authenticatedPassIds + passId
                } else {
                    authenticatedPassIds - passId
                }
                viewModel.onAction(AppAction.SetPassProtected(passId, protect))
            }
        }
    }

    fun handleHomeAction(action: HomeAction) {
        when (action) {
            is HomeAction.OpenPass -> openPass(action.id)
            HomeAction.UnlockProtectedPasses -> {
                if (!passAuthenticator.canAuthenticate()) {
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("Set a screen lock before opening protected passes")
                    }
                } else {
                    passAuthenticator.authenticate { authenticated ->
                        if (authenticated) {
                            protectedPassesUnlocked = true
                            authenticatedPassIds = authenticatedPassIds +
                                state.passes.filter(::requiresProtection).map(PassUiModel::id)
                            viewModel.onAction(AppAction.SelectCategory(PROTECTED_PASSES_CATEGORY_ID))
                        }
                    }
                }
            }
            is HomeAction.ToggleFavorite -> {
                val pass = state.passes.firstOrNull { it.id == action.id } ?: return
                viewModel.onAction(AppAction.SetPassPinned(action.id, !pass.isPinned))
            }
            is HomeAction.ToggleProtected -> {
                val pass = state.passes.firstOrNull { it.id == action.id } ?: return
                setProtectedWithAuthentication(action.id, !pass.isProtected)
            }
            is HomeAction.SelectCategory -> viewModel.onAction(AppAction.SelectCategory(action.categoryId))
            is HomeAction.SetSortOrder -> viewModel.onAction(AppAction.SetSortOrder(action.order))
            is HomeAction.ReorderPass -> viewModel.onAction(AppAction.ReorderPass(action.orderedVisibleIds))
            is HomeAction.Archive -> {
                viewModel.onAction(AppAction.SetPassArchived(action.id, true, announce = false))
            }
            is HomeAction.Restore -> {
                viewModel.onAction(AppAction.SetPassArchived(action.id, false, announce = false))
            }
            is HomeAction.Delete -> {
                requestDelete(action.id)
            }
            is HomeAction.Undo -> viewModel.onAction(action.operation.toAppAction().copy(announce = false))
            HomeAction.ImportPass -> importLauncher.launch(supportedPassImportMimeTypes.toTypedArray())
            HomeAction.OpenSettings -> backStack.add(AppDestination.Settings)
            HomeAction.OpenPassViewSettings -> backStack.add(AppDestination.PassDetailLayoutSettings)
            HomeAction.OpenHomeCardSettings -> backStack.add(AppDestination.HomeCardLayoutSettings)
            HomeAction.OpenTimeline -> backStack.add(AppDestination.Timeline)
            is HomeAction.OpenUrl -> viewModel.onAction(AppAction.OpenUrl(action.url))
        }
    }

    fun handlePassDetailAction(passId: String, action: PassDetailAction) {
        when (action) {
            PassDetailAction.Back -> popBackStack()
            PassDetailAction.Edit -> backStack.add(AppDestination.EditPass(passId))
            is PassDetailAction.EditDate -> backStack.add(AppDestination.EditPass(passId, action.field))
            PassDetailAction.Share -> viewModel.onAction(AppAction.SharePass(passId))
            PassDetailAction.OpenImageExport -> backStack.add(AppDestination.ExportImage(passId))
            PassDetailAction.SaveBarcodeImage -> {
                state.passes.firstOrNull { it.id == passId }?.let { pass ->
                    exportImageToGallery(pass, barcodeOnly = true, returnToPass = false)
                }
            }
            PassDetailAction.AddToCalendar -> viewModel.onAction(AppAction.AddToCalendar(passId))
            is PassDetailAction.SetFlashlightEnabled -> flashlightController.setEnabled(action.enabled)
            PassDetailAction.OpenReminderSettings -> {
                scrollSettingsToNotifications = true
                backStack.add(AppDestination.Settings)
            }
            PassDetailAction.OpenPassViewSettings -> backStack.add(AppDestination.PassDetailLayoutSettings)
            PassDetailAction.OpenPassCustomization -> {
                val artworkKinds = state.passes.firstOrNull { it.id == passId }?.artwork.orEmpty().map { it.kind }
                backStack.add(passCustomizationDestination(passId, artworkKinds))
            }
            PassDetailAction.OpenTagSettings -> backStack.add(AppDestination.CategorySettings)
            is PassDetailAction.ConfigureReminder -> {
                val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    activity.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                if (!action.exactAtEvent || exactAllowed) {
                    viewModel.onAction(
                        AppAction.ConfigurePassReminder(
                            passId,
                            action.enabled,
                            action.leadMinutes,
                            action.exactAtEvent,
                        ),
                    )
                } else {
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${activity.packageName}"),
                        ),
                    )
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Allow exact alarms, then select this option again")
                    }
                }
            }
            is PassDetailAction.SetReminderActions -> viewModel.onAction(
                AppAction.SetPassReminderActions(passId, action.actions),
            )
            is PassDetailAction.OpenLocation -> viewModel.onAction(AppAction.OpenLocation(passId, action.index))
            is PassDetailAction.MoveToCategory -> viewModel.onAction(
                AppAction.MovePass(passId, action.categoryId),
            )
            is PassDetailAction.SetTags -> viewModel.onAction(AppAction.SetPassTags(passId, action.tagIds))
            is PassDetailAction.SetProtected -> setProtectedWithAuthentication(passId, action.isProtected)
            PassDetailAction.Delete -> {
                requestDelete(passId)
                popBackStack()
            }
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
            deepLinkRequest.value = null
            openPass(request.passId, request.showCode)
        }
    }
    LaunchedEffect(state.settings.remindersEnabled) {
        if (state.settings.remindersEnabled) {
            val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            val remindersUnavailable = !reminderNotificationsAvailable(activity)
            if (needsNotificationPermission || remindersUnavailable) {
                viewModel.onAction(AppAction.SetRemindersEnabled(false))
            }
        }
    }
    LaunchedEffect(state.settings.offerCalendarAfterImport) {
        if (
            state.settings.offerCalendarAfterImport &&
            (
                ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CALENDAR) !=
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CALENDAR) !=
                    PackageManager.PERMISSION_GRANTED
                )
        ) {
            viewModel.onAction(AppAction.SetOfferCalendarAfterImport(false))
        }
    }

    PassTheme(displayedThemeMode, displayedAmoled) {
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
                                    ContextCompat.checkSelfPermission(activity, it) ==
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
            if (appLocked) {
                Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Default.Lock, null)
                    Text(stringResource(R.string.protected_pass_message, stringResource(R.string.app_name)))
                    Button(onClick = ::requestAppUnlock) { Text("Unlock") }
                }
            } else {
            NavDisplay(
                backStack = backStack,
                onBack = { popBackStack() },
                entryProvider = entryProvider {
                    entry<AppDestination.PassList> {
                        PassHomeScreen(
                            state = state,
                            showTodayHero = state.settings.highlightTodayPasses,
                            protectedPassesUnlocked = protectedPassesUnlocked,
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
                                    protectedPassesUnlocked = protectedPassesUnlocked,
                                    onAction = { action ->
                                        if (action is HomeAction.OpenPass) {
                                            openPass(action.id, replaceCurrent = true)
                                        } else {
                                            handleHomeAction(action)
                                        }
                                    },
                                )
                            },
                            detailPane = { selected ->
                                val pass = state.passes.firstOrNull { it.id == selected.passId }
                                val requiresUnlock = pass?.let(::requiresProtection) == true &&
                                    selected.passId !in authenticatedPassIds
                                if (requiresUnlock) {
                                    LaunchedEffect(selected.passId) {
                                        if (!passAuthenticator.canAuthenticate()) {
                                            popBackStack()
                                            snackbarHostState.showSnackbar(
                                                "Set a screen lock before opening protected passes",
                                            )
                                        } else {
                                            passAuthenticator.authenticate { authenticated ->
                                                if (authenticated) {
                                                    authenticatedPassIds = authenticatedPassIds + selected.passId
                                                } else if (backStack.lastOrNull() == selected) {
                                                    popBackStack()
                                                }
                                            }
                                        }
                                    }
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Unlocking protected pass...")
                                    }
                                } else {
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
                                    allPassesProtected = state.settings.lockAllPasses,
                                    categories = state.categories,
                                    passReminderEnabled = state.settings.remindersEnabled &&
                                        selected.passId !in state.settings.reminderExcludedPassIds,
                                    remindersGloballyEnabled = state.settings.remindersEnabled,
                                    reminderLeadMinutes = state.settings.reminderLeadMinutesByPass[selected.passId],
                                    reminderExactAtEvent = selected.passId in state.settings.reminderExactPassIds,
                                    reminderActionOverride = state.settings.reminderActionsByPass[selected.passId],
                                    initialCodeExpanded = expandedCodePassId == selected.passId,
                                    onInitialCodeShown = { expandedCodePassId = null },
                                    flashlightAvailable = flashlight.isAvailable,
                                    flashlightEnabled = flashlight.isEnabled,
                                    enhanceCodeBrightness = state.settings.automaticBrightness,
                                    calendarEventPresent = calendarEventPresent,
                                    passDetailSectionOrder = state.settings.passDetailSectionOrder,
                                    hiddenPassDetailSections = state.settings.hiddenPassDetailSections,
                                    onAction = { action ->
                                        handlePassDetailAction(selected.passId, action)
                                    },
                                )
                                }
                            },
                        )
                    }
                    entry<AppDestination.Timeline> {
                        TimelineScreen(
                            state = TimelineUiState(
                                timeline = if (state.settings.separateProtectedPasses && !protectedPassesUnlocked) {
                                    val protectedIds = state.passes.filter(::requiresProtection).mapTo(mutableSetOf(), PassUiModel::id)
                                    state.timeline.copy(
                                        days = state.timeline.days.mapNotNull { day ->
                                            day.copy(events = day.events.filterNot { it.pass.passId in protectedIds })
                                                .takeIf { it.events.isNotEmpty() }
                                        },
                                        nearestEventId = state.timeline.nearestEventId?.takeUnless { eventId ->
                                            state.timeline.days.flatMap { it.events }.any {
                                                it.id == eventId && it.pass.passId in protectedIds
                                            }
                                        },
                                    )
                                } else state.timeline,
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
                                TimelineAction.Back -> popBackStack()
                                is TimelineAction.OpenPass -> openPass(action.passId)
                                is TimelineAction.AddToCalendar -> state.timeline.days
                                    .flatMap { it.events }
                                    .firstOrNull { it.id == action.eventId }
                                    ?.let { viewModel.onAction(AppAction.AddToCalendar(it.pass.passId)) }
                                is TimelineAction.ConfigureReminder -> {
                                    val event = state.timeline.days.flatMap { it.events }
                                        .firstOrNull { it.id == action.eventId }
                                    if (!state.settings.remindersEnabled) {
                                        scrollSettingsToNotifications = true
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
                        val pass = state.passes.firstOrNull { it.id == destination.passId }
                        if (pass?.let(::requiresProtection) == true && destination.passId !in authenticatedPassIds) {
                            LaunchedEffect(destination.passId) {
                                popBackStack()
                                backStack.add(AppDestination.PassDetail(destination.passId))
                            }
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Unlocking protected pass...")
                            }
                        } else EditPassScreen(
                            pass = pass,
                            initialDateField = destination.dateField,
                            onAction = { action ->
                                when (action) {
                                    EditPassAction.Back -> popBackStack()
                                    is EditPassAction.Save -> {
                                        viewModel.onAction(AppAction.SavePass(destination.passId, action.draft))
                                        popBackStack()
                                    }
                                }
                            },
                        )
                    }
                    entry<AppDestination.PassCustomization> { destination ->
                        PassCustomizationScreen(
                            pass = state.passes.firstOrNull { it.id == destination.passId },
                            onAction = { action ->
                                when (action) {
                                    PassCustomizationAction.Back -> popBackStack()
                                    PassCustomizationAction.OpenLayout -> backStack.add(AppDestination.PassDetailLayoutSettings)
                                    is PassCustomizationAction.SelectArtwork -> viewModel.onAction(
                                        AppAction.SetPreferredArtwork(destination.passId, action.kind),
                                    )
                                }
                            },
                        )
                    }
                    entry<AppDestination.ExportImage> { destination ->
                        ExportImageScreen(
                            pass = state.passes.firstOrNull { it.id == destination.passId },
                            options = state.settings.imageExportOptions,
                            isBusy = state.isBusy || imageExporting,
                            onAction = { action ->
                                when (action) {
                                    PassImageExportAction.Back -> popBackStack()
                                    is PassImageExportAction.SetOptions -> viewModel.onAction(
                                        AppAction.SetImageExportOptions(action.value),
                                    )
                                    PassImageExportAction.Save -> {
                                        state.passes.firstOrNull { it.id == destination.passId }?.let { pass ->
                                            exportImageToGallery(pass, barcodeOnly = false, returnToPass = true)
                                        }
                                    }
                                    PassImageExportAction.Share -> {
                                        viewModel.onAction(
                                            AppAction.ShareImage(destination.passId, state.settings.imageExportOptions),
                                        )
                                        popBackStack()
                                    }
                                    PassImageExportAction.Print -> viewModel.onAction(
                                        AppAction.PrintImage(destination.passId, state.settings.imageExportOptions),
                                    )
                                }
                            },
                        )
                    }
                    entry<AppDestination.Settings> {
                        SettingsScreen(
                            settings = state.settings,
                            scrollToNotifications = scrollSettingsToNotifications,
                            onNotificationScrollConsumed = { scrollSettingsToNotifications = false },
                        ) { action ->
                            when (action) {
                                SettingsAction.Back -> popBackStack()
                                is SettingsAction.SetTheme -> viewModel.onAction(AppAction.SetTheme(action.value))
                                is SettingsAction.SetAmoledBlackBackground -> viewModel.onAction(
                                    AppAction.SetAmoledBlackBackground(action.value),
                                )
                                is SettingsAction.SetAutomaticBrightness -> viewModel.onAction(AppAction.SetAutomaticBrightness(action.value))
                                is SettingsAction.SetSortOrder -> viewModel.onAction(AppAction.SetSortOrder(action.value))
                                SettingsAction.OpenCategories -> backStack.add(AppDestination.CategorySettings)
                                SettingsAction.OpenPassViewSettings -> backStack.add(AppDestination.PassDetailLayoutSettings)
                                SettingsAction.OpenHomeCardSettings -> backStack.add(AppDestination.HomeCardLayoutSettings)
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
                                            activity,
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else if (!reminderNotificationsAvailable(activity)) {
                                        viewModel.onAction(AppAction.SetRemindersEnabled(false))
                                        activity.startActivity(
                                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
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
                                is SettingsAction.SetNotificationAccessWindow -> viewModel.onAction(
                                    AppAction.SetNotificationAccessWindow(action.minutes),
                                )
                                is SettingsAction.SetNotificationExactTiming -> {
                                    val alarmManager = activity.getSystemService(AlarmManager::class.java)
                                    val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                                        alarmManager.canScheduleExactAlarms()
                                    if (!action.value || exactAllowed) {
                                        viewModel.onAction(AppAction.SetNotificationExactTiming(action.value))
                                    } else {
                                        viewModel.onAction(AppAction.SetNotificationExactTiming(false))
                                        activity.startActivity(
                                            Intent(
                                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                Uri.parse("package:${activity.packageName}"),
                                            ),
                                        )
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Allow exact alarms, then enable this option again")
                                        }
                                    }
                                }
                                is SettingsAction.SetNotificationActionsEnabled -> viewModel.onAction(
                                    AppAction.SetNotificationActionsEnabled(action.value),
                                )
                                is SettingsAction.SetNotificationLockScreenDetail -> viewModel.onAction(
                                    AppAction.SetNotificationLockScreenDetail(action.value),
                                )
                                is SettingsAction.SetLockAllPasses -> {
                                    if (action.value && !passAuthenticator.canAuthenticate()) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Set a screen lock before protecting every pass",
                                            )
                                        }
                                    } else {
                                        protectedPassesUnlocked = false
                                        authenticatedPassIds = emptySet()
                                        if (state.selectedCategoryId == PROTECTED_PASSES_CATEGORY_ID) {
                                            viewModel.onAction(AppAction.SelectCategory(null))
                                        }
                                        viewModel.onAction(AppAction.SetLockAllPasses(action.value))
                                    }
                                }
                                is SettingsAction.SetShowProtectedPassLockIcon -> viewModel.onAction(
                                    AppAction.SetShowProtectedPassLockIcon(action.value),
                                )
                                is SettingsAction.SetBlurProtectedPassCards -> viewModel.onAction(
                                    AppAction.SetBlurProtectedPassCards(action.value),
                                )
                                is SettingsAction.SetSeparateProtectedPasses -> {
                                    protectedPassesUnlocked = false
                                    authenticatedPassIds = emptySet()
                                    if (state.selectedCategoryId == PROTECTED_PASSES_CATEGORY_ID) {
                                        viewModel.onAction(AppAction.SelectCategory(null))
                                    }
                                    viewModel.onAction(AppAction.SetSeparateProtectedPasses(action.value))
                                }
                                is SettingsAction.SetBlockScreenshots -> viewModel.onAction(
                                    AppAction.SetBlockScreenshots(action.value),
                                )
                                SettingsAction.OpenPrivacyPolicy -> viewModel.onAction(
                                    AppAction.OpenUrl(PRIVACY_POLICY_URL),
                                )
                                SettingsAction.OpenSourceCode -> viewModel.onAction(
                                    AppAction.OpenUrl(PROJECT_REPOSITORY_URL),
                                )
                            }
                        }
                    }
                    entry<AppDestination.CategorySettings> {
                        CategorySettingsScreen(state.categories.filter { it.role == PassCategoryRole.CUSTOM }) { action ->
                            when (action) {
                                CategorySettingsAction.Back -> popBackStack()
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
                            when (action) {
                                PassDetailLayoutSettingsAction.Back -> popBackStack()
                                is PassDetailLayoutSettingsAction.Move -> viewModel.onAction(
                                    AppAction.MovePassDetailSection(action.section, action.offset),
                                )
                                is PassDetailLayoutSettingsAction.SetVisible -> viewModel.onAction(
                                    AppAction.SetPassDetailSectionVisible(action.section, action.visible),
                                )
                            }
                        }
                    }
                    entry<AppDestination.HomeCardLayoutSettings> {
                        HomeCardLayoutSettingsScreen(
                            order = state.settings.homeCardSectionOrder,
                            hidden = state.settings.hiddenHomeCardSections,
                        ) { action ->
                            when (action) {
                                HomeCardLayoutSettingsAction.Back -> popBackStack()
                                is HomeCardLayoutSettingsAction.Move -> viewModel.onAction(
                                    AppAction.MoveHomeCardSection(action.section, action.offset),
                                )
                                is HomeCardLayoutSettingsAction.SetVisible -> viewModel.onAction(
                                    AppAction.SetHomeCardSectionVisible(action.section, action.visible),
                                )
                            }
                        }
                    }
                },
            )
            }
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

private fun imageExportFileName(description: String?): String {
    val base = description.orEmpty()
        .replace(Regex("[^A-Za-z0-9 _-]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { "pass" }
    return "$base ${org.threeten.bp.LocalDate.now()}.png"
}
