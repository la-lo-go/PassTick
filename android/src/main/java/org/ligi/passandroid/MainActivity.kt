package org.ligi.passandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
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
import kotlinx.coroutines.delay
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
import org.ligi.passandroid.ui.compose.toAppAction
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.CategorySettingsAction
import org.ligi.passandroid.ui.state.MainViewModel
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.SettingsAction
import org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction
import org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction
import org.ligi.passandroid.ui.theme.PassTheme
import org.ligi.passandroid.platform.AndroidFlashlightController
import org.ligi.passandroid.platform.FlashlightState
import org.ligi.passandroid.platform.PassAuthenticator
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
            val protectedContentVisible = when (val destination = backStack.lastOrNull()) {
                is AppDestination.PassDetail -> state.passes.any { it.id == destination.passId && it.isProtected }
                is AppDestination.EditPass -> state.passes.any { it.id == destination.passId && it.isProtected }
                AppDestination.PassList, AppDestination.Timeline -> state.passes.any(PassUiModel::isProtected)
                else -> false
            }
            DisposableEffect(protectedContentVisible) {
                if (protectedContentVisible) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                onDispose {
                    if (protectedContentVisible) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            val passAuthenticator = remember { PassAuthenticator(this@MainActivity) }
            var showCalendarPermissionWarning by remember { mutableStateOf(false) }
            var expandedCodePassId by remember { mutableStateOf<String?>(null) }
            var authenticatedPassIds by remember { mutableStateOf(emptySet<String>()) }
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
                    if (replaceCurrent) backStack.removeLastOrNull()
                    backStack.add(AppDestination.PassDetail(passId))
                }
                if (!pass.isProtected) {
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

            fun handleHomeAction(action: HomeAction) {
                when (action) {
                    is HomeAction.OpenPass -> openPass(action.id)
                    is HomeAction.SelectCategory -> viewModel.onAction(AppAction.SelectCategory(action.categoryId))
                    is HomeAction.SetSortOrder -> viewModel.onAction(AppAction.SetSortOrder(action.order))
                    is HomeAction.ReorderPass -> viewModel.onAction(AppAction.ReorderPass(action.orderedVisibleIds))
                    is HomeAction.Archive -> {
                        state.categories.firstOrNull { it.role == PassCategoryRole.ARCHIVE }?.let {
                            viewModel.onAction(AppAction.MovePass(action.id, it.id, announce = false))
                        }
                    }
                    is HomeAction.Restore -> {
                        state.categories.firstOrNull { it.role == PassCategoryRole.INBOX }?.let {
                            viewModel.onAction(AppAction.MovePass(action.id, it.id, announce = false))
                        }
                    }
                    is HomeAction.Delete -> {
                        requestDelete(action.id)
                    }
                    is HomeAction.Undo -> viewModel.onAction(action.operation.toAppAction())
                    HomeAction.ImportPass -> importLauncher.launch(supportedPassImportMimeTypes.toTypedArray())
                    HomeAction.OpenSettings -> backStack.add(AppDestination.Settings)
                    HomeAction.OpenPassViewSettings -> backStack.add(AppDestination.PassDetailLayoutSettings)
                    HomeAction.OpenHomeCardSettings -> backStack.add(AppDestination.HomeCardLayoutSettings)
                    HomeAction.OpenTimeline -> backStack.add(AppDestination.Timeline)
                }
            }

            fun handlePassDetailAction(passId: String, action: PassDetailAction) {
                when (action) {
                    PassDetailAction.Back -> backStack.removeLastOrNull()
                    PassDetailAction.Edit -> backStack.add(AppDestination.EditPass(passId))
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
                    is PassDetailAction.SetProtected -> {
                        if (action.isProtected && !passAuthenticator.canAuthenticate()) {
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar("Set a screen lock before protecting passes")
                            }
                        } else {
                            if (action.isProtected) authenticatedPassIds = authenticatedPassIds + passId
                            else authenticatedPassIds = authenticatedPassIds - passId
                            viewModel.onAction(AppAction.SetPassProtected(passId, action.isProtected))
                        }
                    }
                    PassDetailAction.Delete -> {
                        requestDelete(passId)
                        backStack.removeLastOrNull()
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
                                                    openPass(action.id, replaceCurrent = true)
                                                } else {
                                                    handleHomeAction(action)
                                                }
                                            },
                                        )
                                    },
                                    detailPane = { selected ->
                                        val pass = state.passes.firstOrNull { it.id == selected.passId }
                                        val requiresUnlock = pass?.isProtected == true &&
                                            selected.passId !in authenticatedPassIds
                                        if (requiresUnlock) {
                                            LaunchedEffect(selected.passId) {
                                                if (!passAuthenticator.canAuthenticate()) {
                                                    backStack.removeLastOrNull()
                                                    snackbarHostState.showSnackbar(
                                                        "Set a screen lock before opening protected passes",
                                                    )
                                                } else {
                                                    passAuthenticator.authenticate { authenticated ->
                                                        if (authenticated) {
                                                            authenticatedPassIds = authenticatedPassIds + selected.passId
                                                        } else if (backStack.lastOrNull() == selected) {
                                                            backStack.removeLastOrNull()
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
                                        }
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
                                        is TimelineAction.OpenPass -> openPass(action.passId)
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
                                val pass = state.passes.firstOrNull { it.id == destination.passId }
                                if (pass?.isProtected == true && destination.passId !in authenticatedPassIds) {
                                    LaunchedEffect(destination.passId) {
                                        backStack.removeLastOrNull()
                                        backStack.add(AppDestination.PassDetail(destination.passId))
                                    }
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Unlocking protected pass...")
                                    }
                                } else EditPassScreen(
                                    pass = pass,
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
                                        is SettingsAction.SetLockAllPasses -> viewModel.onAction(
                                            AppAction.SetLockAllPasses(action.value),
                                        )
                                        is SettingsAction.SetShowProtectedPassLockIcon -> viewModel.onAction(
                                            AppAction.SetShowProtectedPassLockIcon(action.value),
                                        )
                                        is SettingsAction.SetBlurProtectedPassCards -> viewModel.onAction(
                                            AppAction.SetBlurProtectedPassCards(action.value),
                                        )
                                        is SettingsAction.SetSeparateProtectedPasses -> viewModel.onAction(
                                            AppAction.SetSeparateProtectedPasses(action.value),
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
                                    when (action) {
                                        PassDetailLayoutSettingsAction.Back -> backStack.removeLastOrNull()
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
                                        HomeCardLayoutSettingsAction.Back -> backStack.removeLastOrNull()
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
