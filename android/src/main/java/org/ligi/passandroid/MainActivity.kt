package org.ligi.passandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.ligi.passandroid.ui.compose.HomeAction
import org.ligi.passandroid.ui.compose.PassDetailScreen
import org.ligi.passandroid.ui.compose.PassHomeScreen
import org.ligi.passandroid.ui.compose.SettingsScreen
import org.ligi.passandroid.ui.compose.TimelineAction
import org.ligi.passandroid.ui.compose.TimelineScreen
import org.ligi.passandroid.ui.compose.TimelineUiState
import org.ligi.passandroid.ui.compose.UndoOperation
import org.ligi.passandroid.ui.barcode.CodeScreenAction
import org.ligi.passandroid.ui.barcode.PassCodeScreen
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.CategorySettingsAction
import org.ligi.passandroid.ui.state.MainViewModel
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.SettingsAction
import org.ligi.passandroid.ui.theme.PassTheme
import org.ligi.passandroid.platform.AndroidFlashlightController
import org.ligi.passandroid.platform.FlashlightState
import org.ligi.passandroid.repository.PassCategoryRole
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
            val backStack = rememberNavBackStack(AppDestination.PassList)
            val snackbarHostState = remember { SnackbarHostState() }
            val undoCategories = remember { mutableMapOf<String, String>() }
            var pendingExportId by remember { mutableStateOf<String?>(null) }
            val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let { viewModel.onAction(AppAction.Import(it)) }
            }
            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/vnd.espass-espass+zip"),
            ) { uri ->
                val id = pendingExportId
                if (uri != null && id != null) viewModel.onAction(AppAction.Export(id, uri))
                pendingExportId = null
            }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted -> viewModel.onAction(AppAction.SetRemindersEnabled(granted)) }

            fun handleHomeAction(action: HomeAction) {
                when (action) {
                    is HomeAction.OpenPass -> backStack.add(AppDestination.PassDetail(action.id))
                    is HomeAction.SelectCategory -> viewModel.onAction(AppAction.SelectCategory(action.categoryId))
                    is HomeAction.SetSortOrder -> viewModel.onAction(AppAction.SetSortOrder(action.order))
                    is HomeAction.Archive -> {
                        state.passes.firstOrNull { it.id == action.id }?.let {
                            undoCategories["archive:${action.id}"] = it.categoryId
                        }
                        state.categories.firstOrNull { it.role == PassCategoryRole.ARCHIVE }?.let {
                            viewModel.onAction(AppAction.MovePass(action.id, it.id))
                        }
                    }
                    is HomeAction.Restore -> {
                        state.passes.firstOrNull { it.id == action.id }?.let {
                            undoCategories["restore:${action.id}"] = it.categoryId
                        }
                        state.categories.firstOrNull { it.role == PassCategoryRole.INBOX }?.let {
                            viewModel.onAction(AppAction.MovePass(action.id, it.id))
                        }
                    }
                    is HomeAction.Delete -> {
                        state.passes.firstOrNull { it.id == action.id }?.let {
                            undoCategories["delete:${action.id}"] = it.categoryId
                        }
                        state.categories.firstOrNull { it.role == PassCategoryRole.TRASH }?.let {
                            viewModel.onAction(AppAction.MovePass(action.id, it.id))
                        }
                    }
                    is HomeAction.Undo -> {
                        val key = when (action.operation) {
                            is UndoOperation.Archive -> "archive:${action.operation.passId}"
                            is UndoOperation.Restore -> "restore:${action.operation.passId}"
                            is UndoOperation.Delete -> "delete:${action.operation.passId}"
                        }
                        undoCategories.remove(key)?.let { categoryId ->
                            viewModel.onAction(AppAction.MovePass(action.operation.passId, categoryId))
                        }
                    }
                    HomeAction.CreatePass -> backStack.add(AppDestination.CreatePass)
                    HomeAction.ImportPass -> importLauncher.launch(supportedPassImportMimeTypes.toTypedArray())
                    HomeAction.OpenSettings -> backStack.add(AppDestination.Settings)
                    HomeAction.OpenTimeline -> backStack.add(AppDestination.Timeline)
                }
            }

            fun handlePassDetailAction(passId: String, passDescription: String?, action: PassDetailAction) {
                when (action) {
                    PassDetailAction.Back -> backStack.removeLastOrNull()
                    PassDetailAction.Edit -> backStack.add(AppDestination.EditPass(passId))
                    PassDetailAction.Delete -> {
                        viewModel.onAction(AppAction.DeletePass(passId))
                        backStack.removeLastOrNull()
                    }
                    PassDetailAction.Export -> {
                        pendingExportId = passId
                        exportLauncher.launch("${passDescription ?: "pass"}.espass")
                    }
                    PassDetailAction.Share -> viewModel.onAction(AppAction.SharePass(passId))
                    PassDetailAction.Print -> viewModel.onAction(AppAction.PrintPass(passId))
                    PassDetailAction.AddToCalendar -> viewModel.onAction(AppAction.AddToCalendar(passId))
                    PassDetailAction.OpenCode -> backStack.add(AppDestination.PassCode(passId))
                    PassDetailAction.UseForQuickCodeWidget -> viewModel.onAction(AppAction.SetQuickCodePass(passId))
                    is PassDetailAction.OpenLocation -> viewModel.onAction(AppAction.OpenLocation(passId, action.index))
                    is PassDetailAction.MoveToCategory -> viewModel.onAction(
                        AppAction.MovePass(passId, action.categoryId),
                    )
                }
            }

            LaunchedEffect(state.message) {
                state.message?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.onAction(AppAction.ClearMessage)
                }
            }
            LaunchedEffect(requestedPass, state.passes) {
                val request = requestedPass ?: return@LaunchedEffect
                if (state.passes.any { it.id == request.passId }) {
                    backStack.add(
                        if (request.showCode) AppDestination.PassCode(request.passId)
                        else AppDestination.PassDetail(request.passId),
                    )
                    deepLinkRequest.value = null
                }
            }
            LaunchedEffect(state.settings.remindersEnabled) {
                if (
                    state.settings.remindersEnabled &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.onAction(AppAction.SetRemindersEnabled(false))
                }
            }

            PassTheme(state.settings.themeMode) {
                Surface {
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
                                        PassDetailScreen(
                                            pass = pass,
                                            categories = state.categories,
                                            quickCodePassId = state.settings.quickCodePassId,
                                            onAction = { action ->
                                                handlePassDetailAction(selected.passId, pass?.description, action)
                                            },
                                        )
                                    },
                                )
                            }
                            entry<AppDestination.PassCode> { destination ->
                                val pass = state.passes.firstOrNull { it.id == destination.passId }
                                val context = LocalContext.current
                                var hasCameraPermission by remember {
                                    mutableStateOf(
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                            PackageManager.PERMISSION_GRANTED,
                                    )
                                }
                                val permissionLauncher = rememberLauncherForActivityResult(
                                    ActivityResultContracts.RequestPermission(),
                                ) { granted -> hasCameraPermission = granted }
                                val flashlightController = remember(hasCameraPermission) {
                                    if (hasCameraPermission) AndroidFlashlightController(context.applicationContext) else null
                                }
                                DisposableEffect(flashlightController) {
                                    onDispose { flashlightController?.close() }
                                }
                                val flashlightFlow = remember(flashlightController) {
                                    flashlightController?.state ?: MutableStateFlow(FlashlightState())
                                }
                                val flashlight by flashlightFlow.collectAsStateWithLifecycle()
                                var codeScale by rememberSaveable(destination.passId) { mutableFloatStateOf(0.82f) }
                                if (pass?.barcodeFormat != null && !pass.barcodeMessage.isNullOrBlank()) {
                                    PassCodeScreen(
                                        format = pass.barcodeFormat,
                                        message = pass.barcodeMessage,
                                        alternativeText = pass.barcodeAlternativeText,
                                        codeScale = codeScale,
                                        automaticBrightness = state.settings.automaticBrightness,
                                        flashlightAvailable = flashlight.isAvailable || !hasCameraPermission,
                                        flashlightEnabled = flashlight.isEnabled,
                                        onAction = { action ->
                                            when (action) {
                                                CodeScreenAction.Back -> backStack.removeLastOrNull()
                                                is CodeScreenAction.SetCodeScale -> codeScale = action.scale
                                                CodeScreenAction.ResetCodeScale -> codeScale = 0.82f
                                                is CodeScreenAction.SetFlashlightEnabled -> {
                                                    if (action.enabled && !hasCameraPermission) {
                                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                                    } else {
                                                        flashlightController?.setEnabled(action.enabled)
                                                    }
                                                }
                                            }
                                        },
                                    )
                                } else {
                                    LaunchedEffect(Unit) { backStack.removeLastOrNull() }
                                }
                            }
                            entry<AppDestination.Timeline> {
                                TimelineScreen(
                                    state = TimelineUiState(timeline = state.timeline),
                                    onAction = { action ->
                                    when (action) {
                                        TimelineAction.Back -> backStack.removeLastOrNull()
                                        is TimelineAction.OpenPass -> backStack.add(AppDestination.PassDetail(action.passId))
                                        is TimelineAction.AddToCalendar -> state.timeline.days
                                            .flatMap { it.events }
                                            .firstOrNull { it.id == action.eventId }
                                            ?.let { viewModel.onAction(AppAction.AddToCalendar(it.pass.passId)) }
                                        is TimelineAction.ConfigureReminder -> backStack.add(AppDestination.Settings)
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
                            entry<AppDestination.CreatePass> {
                                EditPassScreen(
                                    pass = null,
                                    isNew = true,
                                    onAction = { action ->
                                        when (action) {
                                            EditPassAction.Back -> backStack.removeLastOrNull()
                                            is EditPassAction.Save -> {
                                                viewModel.onAction(AppAction.CreatePass(action.draft))
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
                                        is SettingsAction.SetCondensedPasses -> viewModel.onAction(AppAction.SetCondensedPasses(action.value))
                                        is SettingsAction.SetAutomaticBrightness -> viewModel.onAction(AppAction.SetAutomaticBrightness(action.value))
                                        is SettingsAction.SetSortOrder -> viewModel.onAction(AppAction.SetSortOrder(action.value))
                                        SettingsAction.OpenCategories -> backStack.add(AppDestination.CategorySettings)
                                        is SettingsAction.SetHighlightTodayPasses -> viewModel.onAction(
                                            AppAction.SetHighlightTodayPasses(action.value),
                                        )
                                        is SettingsAction.SetAutomaticallyMarkPast -> viewModel.onAction(
                                            AppAction.SetAutomaticallyMarkPast(action.value),
                                        )
                                        is SettingsAction.SetOfferCalendarAfterImport -> viewModel.onAction(
                                            AppAction.SetOfferCalendarAfterImport(action.value),
                                        )
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
                                            } else {
                                                viewModel.onAction(AppAction.SetRemindersEnabled(true))
                                            }
                                        }
                                        is SettingsAction.SetDefaultReminderMinutes -> viewModel.onAction(
                                            AppAction.SetDefaultReminderMinutes(action.value),
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
                        },
                    )
                    SnackbarHost(snackbarHostState)
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
