package org.ligi.passandroid.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.navigation3.runtime.rememberNavBackStack
import org.ligi.passandroid.imports.ImportSource
import org.ligi.passandroid.navigation.AppDestination
import org.ligi.passandroid.navigation.passCustomizationDestination
import org.ligi.passandroid.navigation.PassDeepLinkRequest
import org.ligi.passandroid.repository.passFileImportMimeTypes
import org.ligi.passandroid.ui.compose.HomeAction
import org.ligi.passandroid.ui.compose.ImportReviewScreen
import org.ligi.passandroid.ui.compose.ImportSourceSheet
import org.ligi.passandroid.ui.compose.toAppAction
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.ImportReviewAction
import org.ligi.passandroid.ui.state.MainViewModel
import org.ligi.passandroid.ui.state.PROTECTED_PASSES_CATEGORY_ID
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.theme.PassTheme
import org.ligi.passandroid.repository.StartupAppearanceStore
import org.ligi.passandroid.platform.AndroidFlashlightController
import org.ligi.passandroid.platform.PassAuthenticator
import org.ligi.passandroid.platform.PassImageExporter
import org.ligi.passandroid.repository.PassImageContent
import org.ligi.passandroid.reminder.reminderNotificationsAvailable
import androidx.compose.runtime.Composable
import org.ligi.passandroid.MainActivity
import org.ligi.passandroid.R
import org.ligi.passandroid.repository.StartupAppearance

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PassTickApp(
    activity: MainActivity,
    viewModel: MainViewModel,
    deepLinkRequest: MutableStateFlow<PassDeepLinkRequest?>,
    documentImportRequest: MutableStateFlow<Uri?>,
    startupAppearance: StartupAppearance,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val displayedThemeMode = if (state.isContentLoading) startupAppearance.themeMode else state.settings.themeMode
    val displayedAmoled = if (state.isContentLoading) {
        startupAppearance.amoledBlackBackground
    } else {
        state.settings.amoledBlackBackground
    }
    val displayedAccentColor = if (state.isContentLoading) {
        startupAppearance.accentColor
    } else {
        state.settings.accentColor
    }
    val displayedDynamicColors = if (state.isContentLoading) {
        startupAppearance.dynamicColors
    } else {
        state.settings.dynamicColors
    }
    val displayedColorStyle = if (state.isContentLoading) {
        startupAppearance.colorStyle
    } else {
        state.settings.colorStyle
    }
    val darkSystemBars = displayedThemeMode == org.ligi.passandroid.repository.ThemeMode.DARK ||
        displayedThemeMode == org.ligi.passandroid.repository.ThemeMode.SYSTEM && isSystemInDarkTheme()
    SideEffect {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars = !darkSystemBars
    }
    LaunchedEffect(
        state.isContentLoading,
        state.settings.themeMode,
        state.settings.amoledBlackBackground,
        state.settings.dynamicColors,
        state.settings.accentColor,
        state.settings.colorStyle,
    ) {
        if (!state.isContentLoading) {
            StartupAppearanceStore.write(
                activity,
                state.settings.themeMode,
                state.settings.amoledBlackBackground,
                state.settings.dynamicColors,
                state.settings.accentColor,
                state.settings.colorStyle,
            )
        }
    }
    val requestedPass by deepLinkRequest.collectAsStateWithLifecycle()
    val requestedDocumentImport by documentImportRequest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backStack = rememberNavBackStack(AppDestination.PassList)
    fun popBackStack() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }
    var appUnlocked by remember { mutableStateOf(false) }
    var protectedSectionUnlocked by remember { mutableStateOf(false) }
    val inProtectedSection = state.selectedCategoryId == PROTECTED_PASSES_CATEGORY_ID
    val protectedPassesRevealed = (state.settings.lockAllPasses && appUnlocked) ||
        (inProtectedSection && protectedSectionUnlocked)
    LaunchedEffect(state.selectedCategoryId) {
        if (state.selectedCategoryId != PROTECTED_PASSES_CATEGORY_ID) protectedSectionUnlocked = false
    }
    fun requiresProtection(pass: PassUiModel): Boolean = state.settings.lockAllPasses || pass.isProtected
    val protectionSourcePasses = state.passes + state.trashedPasses.takeIf {
        state.selectedCategoryId == org.ligi.passandroid.ui.state.TRASHED_PASSES_CATEGORY_ID
    }.orEmpty()
    val protectedContentVisible = when (val destination = backStack.lastOrNull()) {
        is AppDestination.PassDetail -> state.passes.any { it.id == destination.passId && requiresProtection(it) }
        is AppDestination.EditPass -> state.passes.any { it.id == destination.passId && requiresProtection(it) }
        is AppDestination.PassCustomization -> state.passes.any { it.id == destination.passId && requiresProtection(it) }
        AppDestination.PassList, AppDestination.Timeline -> state.passes.any(::requiresProtection) &&
            (!state.settings.separateProtectedPasses || protectedPassesRevealed)
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
    val appLocked = state.settings.lockAllPasses && !appUnlocked
    var startupUnlockRequested by remember { mutableStateOf(false) }
    fun requestAppUnlock() {
        if (!passAuthenticator.canAuthenticate()) {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(activity.getString(R.string.app_set_screen_lock_before_protecting_every_pass))
            }
        } else {
            passAuthenticator.authenticate { authenticated ->
                if (authenticated) {
                    appUnlocked = true
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
    var showImportSourceSheet by remember { mutableStateOf(false) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCaptureFile by remember { mutableStateOf<java.io.File?>(null) }
    fun prepareDocumentImport(uri: Uri, source: ImportSource) {
        coroutineScope.launch {
            if (viewModel.prepareDocumentImport(uri, source).isSuccess) {
                backStack.removeAll { it == AppDestination.ImportReview }
                backStack.add(AppDestination.ImportReview)
            }
        }
    }
    LaunchedEffect(requestedDocumentImport) {
        val uri = requestedDocumentImport ?: return@LaunchedEffect
        documentImportRequest.value = null
        val mimeType = context.contentResolver.getType(uri)
        val source = if (mimeType == "application/pdf") ImportSource.PDF else ImportSource.IMAGE
        prepareDocumentImport(uri, source)
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { prepareDocumentImport(it, ImportSource.IMAGE) }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { prepareDocumentImport(it, ImportSource.PDF) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCaptureUri
        val file = pendingCaptureFile
        pendingCaptureUri = null
        pendingCaptureFile = null
        when {
            success && uri != null -> prepareDocumentImport(uri, ImportSource.CAMERA)
            else -> file?.delete()
        }
    }
    fun launchCamera() {
        val file = java.io.File(
            java.io.File(activity.cacheDir, "capture").apply { mkdirs() },
            "capture-${System.currentTimeMillis()}.jpg",
        )
        val uri = runCatching {
            FileProvider.getUriForFile(activity, activity.getString(R.string.authority_fileprovider), file)
        }.getOrNull()
        if (uri == null) {
            file.delete()
        } else {
            pendingCaptureUri = uri
            pendingCaptureFile = file
            cameraLauncher.launch(uri)
        }
    }
    var exportPassId by remember { mutableStateOf<String?>(null) }
    val exportPassLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.espass-espass+zip"),
    ) { uri ->
        val passId = exportPassId
        exportPassId = null
        if (uri != null && passId != null) viewModel.onAction(AppAction.Export(passId, uri))
    }
    var imageExporting by remember { mutableStateOf(false) }
    fun exportImageToGallery(pass: PassUiModel, barcodeOnly: Boolean, returnToPass: Boolean) {
        val options = if (barcodeOnly) {
            state.settings.imageExportOptions.copy(
                content = PassImageContent(
                    artwork = false,
                    details = false,
                    notes = false,
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
                                            snackbarHostState.showSnackbar(activity.getString(R.string.app_image_saved_to_gallery))
                }.onFailure { error ->
                    snackbarHostState.showSnackbar(activity.getString(R.string.app_image_export_failed, error.message.orEmpty()))
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
            snackbarHostState.showSnackbar(activity.getString(R.string.app_calendar_permission_required))
        }
    }

    fun requestDelete(passId: String) {
        val trashEnabled = state.settings.trashEnabled
        viewModel.onAction(AppAction.SetPassPendingDeletion(passId, true))
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val timeout = launch {
                delay(5_000)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
                            message = activity.getString(
                                if (trashEnabled) R.string.app_pass_moved_to_trash else R.string.app_pass_deleted,
                            ),
                            actionLabel = activity.getString(R.string.home_undo),
                withDismissAction = false,
                duration = SnackbarDuration.Indefinite,
            )
            timeout.cancel()
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onAction(AppAction.SetPassPendingDeletion(passId, false))
            } else {
                viewModel.onAction(if (trashEnabled) AppAction.TrashPass(passId) else AppAction.DeletePass(passId))
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
        if (!requiresProtection(pass) || protectedPassesRevealed) {
            navigate()
        } else if (!passAuthenticator.canAuthenticate()) {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(activity.getString(R.string.app_set_screen_lock_before_opening_protected_passes))
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
                snackbarHostState.showSnackbar(activity.getString(R.string.app_set_screen_lock_before_protecting_passes))
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
                        snackbarHostState.showSnackbar(activity.getString(R.string.app_set_screen_lock_before_opening_protected_passes))
                    }
                } else {
                    passAuthenticator.authenticate { authenticated ->
                        if (authenticated) {
                            protectedSectionUnlocked = true
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
            is HomeAction.RestoreFromTrash -> viewModel.onAction(AppAction.RestoreFromTrash(action.id))
            is HomeAction.DeleteForever -> viewModel.onAction(AppAction.DeleteForever(action.id))
            is HomeAction.SetTrashEnabled -> viewModel.onAction(AppAction.SetTrashEnabled(action.value))
            HomeAction.EmptyTrash -> viewModel.onAction(AppAction.EmptyTrash)
            is HomeAction.Undo -> viewModel.onAction(action.operation.toAppAction().copy(announce = false))
            HomeAction.ImportPass -> showImportSourceSheet = true
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
            PassDetailAction.Export -> {
                val description = state.passes.firstOrNull { it.id == passId }?.description.orEmpty()
                exportPassId = passId
                exportPassLauncher.launch(exportFileName(description))
            }
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
                val pass = state.passes.firstOrNull { it.id == passId }
                val artworkKinds = pass?.artwork.orEmpty().map { it.kind }
                // Imported documents only carry one image at two sizes; nothing to choose.
                backStack.add(
                    if (pass?.isDocumentPass == true) {
                        AppDestination.PassDetailLayoutSettings
                    } else {
                        passCustomizationDestination(passId, artworkKinds)
                    },
                )
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
                            "package:${activity.packageName}".toUri(),
                        ),
                    )
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(activity.getString(R.string.app_allow_exact_alarms_then_select_again))
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
            is PassDetailAction.SetNotes -> viewModel.onAction(AppAction.SetPassNotes(passId, action.text))
            PassDetailAction.Duplicate -> viewModel.onAction(AppAction.DuplicatePass(passId))
            is PassDetailAction.SetProtected -> setProtectedWithAuthentication(passId, action.isProtected)
            is PassDetailAction.SetArchived -> viewModel.onAction(
                AppAction.SetPassArchived(passId, action.isArchived, announce = false),
            )
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

    PassTheme(
        displayedThemeMode,
        displayedAmoled,
        displayedDynamicColors,
        displayedAccentColor,
        displayedColorStyle,
    ) {
        if (showCalendarPermissionWarning) {
            AlertDialog(
                onDismissRequest = { showCalendarPermissionWarning = false },
                title = { Text(stringResource(R.string.app_add_events_automatically)) },
                text = {
                            Text(
                                stringResource(R.string.app_calendar_auto_add_message),
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
                    ) { Text(stringResource(R.string.app_allow)) }
                },
                dismissButton = {
                    TextButton(onClick = { showCalendarPermissionWarning = false }) { Text(stringResource(R.string.pass_detail_cancel)) }
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
                    Button(onClick = ::requestAppUnlock) { Text(stringResource(R.string.app_unlock)) }
                }
            } else {
            AppNavDisplay(
                AppNavigationDependencies(
                    state = state,
                    viewModel = viewModel,
                    activity = activity,
                    backStack = backStack,
                    onBack = {
                        if (backStack.lastOrNull() == AppDestination.ImportReview) {
                            viewModel.onImportReviewAction(ImportReviewAction.Discard)
                        }
                        popBackStack()
                    },
                    onOpenPass = { passId, replaceCurrent -> openPass(passId, replaceCurrent = replaceCurrent) },
                    onHomeAction = ::handleHomeAction,
                    onPassDetailAction = ::handlePassDetailAction,
                    protectedPassIds = protectionSourcePasses.filter(::requiresProtection).mapTo(mutableSetOf(), PassUiModel::id),
                    protectedPassesUnlocked = protectedPassesRevealed,
                    authenticatedPassIds = authenticatedPassIds,
                    passAuthenticator = passAuthenticator,
                    onPassAuthenticated = { passId -> authenticatedPassIds = authenticatedPassIds + passId },
                    onResetProtectedState = {
                        appUnlocked = false
                        protectedSectionUnlocked = false
                        authenticatedPassIds = emptySet()
                    },
                    onExportImage = ::exportImageToGallery,
                    imageExporting = imageExporting,
                    expandedCodePassId = expandedCodePassId,
                    onExpandedCodeShown = { expandedCodePassId = null },
                    onShowCalendarPermissionWarning = { showCalendarPermissionWarning = true },
                    onRequestNotificationPermission = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onOpenNotificationSettings = {
                        scrollSettingsToNotifications = true
                        backStack.add(AppDestination.Settings)
                    },
                    flashlightAvailable = flashlight.isAvailable,
                    flashlightEnabled = flashlight.isEnabled,
                    scrollSettingsToNotifications = scrollSettingsToNotifications,
                    onSettingsScrollConsumed = { scrollSettingsToNotifications = false },
                    coroutineScope = coroutineScope,
                    snackbarHostState = snackbarHostState,
                ),
            )
            if (showImportSourceSheet) {
                ImportSourceSheet(
                    onDismiss = { showImportSourceSheet = false },
                    onPickPassFile = { importLauncher.launch(passFileImportMimeTypes.toTypedArray()) },
                    onPickPhoto = {
                        photoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onPickPdf = { pdfLauncher.launch(arrayOf("application/pdf")) },
                    onTakePhoto = ::launchCamera,
                    onCreateManually = { backStack.add(AppDestination.CreatePass) },
                )
            }
            }
            if (state.isPreparingImport) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
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

private fun exportFileName(description: String?): String {
    val base = description.orEmpty()
        .replace(Regex("[^A-Za-z0-9 _-]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { "pass" }
    return "$base.espass"
}
