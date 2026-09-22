package org.ligi.passandroid.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ligi.passandroid.R
import org.ligi.passandroid.MainActivity
import org.ligi.passandroid.navigation.AppDestination
import org.ligi.passandroid.platform.PassAuthenticator
import org.ligi.passandroid.reminder.reminderNotificationsAvailable
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.ui.adaptive.AdaptivePassListDetailShell
import org.ligi.passandroid.ui.barcode.CodeViewOptions
import org.ligi.passandroid.ui.compose.CategorySettingsScreen
import org.ligi.passandroid.ui.compose.CodeSettingsScreen
import org.ligi.passandroid.ui.compose.EditPassScreen
import org.ligi.passandroid.ui.compose.ExportImageScreen
import org.ligi.passandroid.ui.compose.HomeAction
import org.ligi.passandroid.ui.compose.HomeCardLayoutSettingsScreen
import org.ligi.passandroid.ui.compose.ImportReviewScreen
import org.ligi.passandroid.ui.compose.LicensesScreen
import org.ligi.passandroid.ui.compose.PassCodePickerScreen
import org.ligi.passandroid.ui.compose.PassCustomizationScreen
import org.ligi.passandroid.ui.compose.PassDetailLayoutSettingsScreen
import org.ligi.passandroid.ui.compose.PassDetailScreen
import org.ligi.passandroid.ui.compose.PassHomeScreen
import org.ligi.passandroid.ui.compose.BUG_REPORT_MAIL_URL
import org.ligi.passandroid.ui.compose.BUG_REPORT_URL
import org.ligi.passandroid.ui.compose.PRIVACY_POLICY_URL
import org.ligi.passandroid.ui.compose.PROJECT_REPOSITORY_URL
import org.ligi.passandroid.ui.compose.SettingsScreen
import org.ligi.passandroid.ui.compose.TimelineAction
import org.ligi.passandroid.ui.compose.TimelineScreen
import org.ligi.passandroid.ui.compose.TimelineUiState
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.CategorySettingsAction
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction
import org.ligi.passandroid.ui.state.MainUiState
import org.ligi.passandroid.ui.state.MainViewModel
import org.ligi.passandroid.ui.state.resolvePassCardTitle
import org.ligi.passandroid.ui.state.PROTECTED_PASSES_CATEGORY_ID
import org.ligi.passandroid.ui.state.PassCustomizationAction
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction
import org.ligi.passandroid.ui.state.PassImageExportAction
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.resolvePassCardTitle
import org.ligi.passandroid.ui.state.SettingsAction

internal class AppNavigationDependencies(
    val state: MainUiState,
    val viewModel: MainViewModel,
    val activity: MainActivity,
    val backStack: MutableList<NavKey>,
    val onBack: () -> Unit,
    val onOpenPass: (passId: String, replaceCurrent: Boolean) -> Unit,
    val onHomeAction: (HomeAction) -> Unit,
    val onPassDetailAction: (passId: String, action: PassDetailAction) -> Unit,
    val protectedPassIds: Set<String>,
    val protectedPassesUnlocked: Boolean,
    val authenticatedPassIds: Set<String>,
    val passAuthenticator: PassAuthenticator,
    val onPassAuthenticated: (passId: String) -> Unit,
    val onResetProtectedState: () -> Unit,
    val onExportImage: (PassUiModel, Boolean, Boolean) -> Unit,
    val imageExporting: Boolean,
    val onExportArchive: () -> Unit,
    val onImportArchive: () -> Unit,
    val expandedCodePassId: String?,
    val onExpandedCodeShown: () -> Unit,
    val onShowCalendarPermissionWarning: () -> Unit,
    val onRequestNotificationPermission: () -> Unit,
    val onOpenNotificationSettings: () -> Unit,
    val flashlightAvailable: Boolean,
    val flashlightEnabled: Boolean,
    val scrollSettingsToNotifications: Boolean,
    val onSettingsScrollConsumed: () -> Unit,
    val coroutineScope: CoroutineScope,
    val snackbarHostState: SnackbarHostState,
    val homeListState: LazyListState,
)

@Composable
internal fun AppNavDisplay(dependencies: AppNavigationDependencies) {
    val state = dependencies.state
    val backStack = dependencies.backStack
    NavDisplay(
        backStack = backStack,
        onBack = { dependencies.onBack() },
        entryProvider = entryProvider {
            entry<AppDestination.PassList> {
                PassHomeScreen(
                    state = state,
                    showTodayHero = state.settings.highlightTodayPasses,
                    protectedPassesUnlocked = dependencies.protectedPassesUnlocked,
                    recentlyImportedIds = state.recentlyImportedIds,
                    listState = dependencies.homeListState,
                    onAction = dependencies.onHomeAction,
                )
            }
            entry<AppDestination.PassDetail> { destination ->
                AdaptivePassListDetailShell(
                    selectedDestination = destination,
                    listPane = {
                        PassHomeScreen(
                            state = state,
                            showTodayHero = state.settings.highlightTodayPasses,
                            protectedPassesUnlocked = dependencies.protectedPassesUnlocked,
                            recentlyImportedIds = state.recentlyImportedIds,
                            listState = dependencies.homeListState,
                            onAction = { action ->
                                if (action is HomeAction.OpenPass) {
                                    dependencies.onOpenPass(action.id, true)
                                } else {
                                    dependencies.onHomeAction(action)
                                }
                            },
                        )
                    },
                    detailPane = { selected ->
                        val pass = state.passes.firstOrNull { it.id == selected.passId }
                        val requiresUnlock = selected.passId in dependencies.protectedPassIds &&
                            selected.passId !in dependencies.authenticatedPassIds &&
                            !dependencies.protectedPassesUnlocked
                        if (requiresUnlock) {
                            LaunchedEffect(selected.passId, requiresUnlock) {
                                if (!dependencies.passAuthenticator.canAuthenticate()) {
                                    dependencies.onBack()
                                    dependencies.snackbarHostState.showSnackbar(
                                        dependencies.activity.getString(
                                            R.string.app_set_screen_lock_before_opening_protected_passes,
                                        ),
                                    )
                                } else {
                                    dependencies.passAuthenticator.authenticate { authenticated ->
                                        if (authenticated) {
                                            dependencies.onPassAuthenticated(selected.passId)
                                        } else if (backStack.lastOrNull() == selected) {
                                            dependencies.onBack()
                                        }
                                    }
                                }
                            }
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.app_unlocking_protected_pass))
                            }
                        } else {
                            var calendarEventPresent by remember(selected.passId) { mutableStateOf(false) }
                            LaunchedEffect(pass?.calendarEvent) {
                                calendarEventPresent = pass?.let { dependencies.viewModel.isCalendarEventPresent(it) } == true
                            }
                            val lifecycleOwner = LocalLifecycleOwner.current
                            DisposableEffect(lifecycleOwner, pass?.id, pass?.calendarEvent) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME && pass != null) {
                                        dependencies.coroutineScope.launch {
                                            calendarEventPresent = dependencies.viewModel.isCalendarEventPresent(pass)
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
                                initialCodeExpanded = dependencies.expandedCodePassId == selected.passId,
                                onInitialCodeShown = { dependencies.onExpandedCodeShown() },
                                flashlightAvailable = dependencies.flashlightAvailable,
                                flashlightEnabled = dependencies.flashlightEnabled,
                                enhanceCodeBrightness = state.settings.automaticBrightness,
                                codeViewOptions = CodeViewOptions(
                                    sizeStep = state.settings.codeSizeStep,
                                    whiteSurround = state.settings.codeWhiteSurround,
                                    extraQuietZone = state.settings.codeExtraQuietZone,
                                    rotateQuarterTurn = state.settings.codeRotateQuarterTurn,
                                    keepScreenOn = state.settings.codeKeepScreenOn,
                                ),
                                calendarEventPresent = calendarEventPresent,
                                passDetailSectionOrder = state.settings.passDetailSectionOrder,
                                hiddenPassDetailSections = state.settings.hiddenPassDetailSections,
                                documentPages = dependencies.viewModel.documentPages,
                                onAction = { action ->
                                    dependencies.onPassDetailAction(selected.passId, action)
                                },
                            )
                        }
                    },
                )
            }
            entry<AppDestination.ImportReview> {
                val review = state.importReview
                if (review == null) {
                    LaunchedEffect(Unit) { dependencies.onBack() }
                } else {
                    ImportReviewScreen(
                        state = review,
                        onAction = dependencies.viewModel::onImportReviewAction,
                    )
                }
            }
            entry<AppDestination.Timeline> {
                val passById = state.passes.associateBy(PassUiModel::id)
                TimelineScreen(
                    state = TimelineUiState(
                        timeline = if (state.settings.separateProtectedPasses && !dependencies.protectedPassesUnlocked) {
                            state.timeline.copy(
                                days = state.timeline.days.mapNotNull { day ->
                                    day.copy(events = day.events.filterNot { it.pass.passId in dependencies.protectedPassIds })
                                        .takeIf { it.events.isNotEmpty() }
                                },
                                nearestEventId = state.timeline.nearestEventId?.takeUnless { eventId ->
                                    state.timeline.days.flatMap { it.events }.any {
                                        it.id == eventId && it.pass.passId in dependencies.protectedPassIds
                                    }
                                },
                            )
                        } else {
                            state.timeline
                        },
                        reminderEventIds = if (state.settings.remindersEnabled) {
                            state.timeline.days.flatMap { it.events }
                                .filterNot { it.pass.passId in state.settings.reminderExcludedPassIds }
                                .mapTo(mutableSetOf()) { it.id }
                        } else {
                            emptySet()
                        },
                        passCardTitles = state.timeline.days.flatMap { it.events }.associate { event ->
                            event.pass.passId to (passById[event.pass.passId]?.let { pass ->
                                resolvePassCardTitle(
                                    pass,
                                    state.settings.homeCardSectionOrder,
                                    state.settings.hiddenHomeCardSections,
                                    state.categories,
                                )
                            } ?: event.title)
                        },
                    ),
                    onAction = { action ->
                        when (action) {
                            TimelineAction.Back -> dependencies.onBack()
                            is TimelineAction.OpenPass -> dependencies.onOpenPass(action.passId, false)
                            is TimelineAction.AddToCalendar -> state.timeline.days
                                .flatMap { it.events }
                                .firstOrNull { it.id == action.eventId }
                                ?.let { dependencies.viewModel.onAction(AppAction.AddToCalendar(it.pass.passId)) }
                            is TimelineAction.ConfigureReminder -> {
                                val event = state.timeline.days.flatMap { it.events }
                                    .firstOrNull { it.id == action.eventId }
                                if (!state.settings.remindersEnabled) {
                                    dependencies.onOpenNotificationSettings()
                                } else if (event != null) {
                                    dependencies.viewModel.onAction(AppAction.TogglePassReminder(event.pass.passId))
                                }
                            }
                        }
                    },
                )
            }
            entry<AppDestination.EditPass> { destination ->
                val pass = state.passes.firstOrNull { it.id == destination.passId }
                if (destination.passId in dependencies.protectedPassIds &&
                    destination.passId !in dependencies.authenticatedPassIds &&
                    !dependencies.protectedPassesUnlocked
                ) {
                    LaunchedEffect(destination.passId) {
                        dependencies.onBack()
                        backStack.add(AppDestination.PassDetail(destination.passId))
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.app_unlocking_protected_pass))
                    }
                } else {
                    EditPassScreen(
                        pass = pass,
                        initialDateField = destination.dateField,
                        onAction = { action ->
                            when (action) {
                                EditPassAction.Back -> dependencies.onBack()
                                is EditPassAction.Save -> {
                                    dependencies.viewModel.onAction(AppAction.SavePass(destination.passId, action.draft))
                                    dependencies.onBack()
                                }
                            }
                        },
                    )
                }
            }
            entry<AppDestination.CreatePass> {
                EditPassScreen(
                    pass = null,
                    onAction = { action ->
                        when (action) {
                            EditPassAction.Back -> dependencies.onBack()
                            is EditPassAction.Save -> dependencies.coroutineScope.launch {
                                dependencies.viewModel.createPass(action.draft).onSuccess { created ->
                                    backStack.removeAll { it == AppDestination.CreatePass }
                                    backStack.add(AppDestination.PassDetail(created.id))
                                }
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
                            PassCustomizationAction.Back -> dependencies.onBack()
                            PassCustomizationAction.OpenLayout -> backStack.add(AppDestination.PassDetailLayoutSettings)
                            is PassCustomizationAction.SelectArtwork -> dependencies.viewModel.onAction(
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
                    isBusy = state.isBusy || dependencies.imageExporting,
                    onAction = { action ->
                        when (action) {
                            PassImageExportAction.Back -> dependencies.onBack()
                            is PassImageExportAction.SetOptions -> dependencies.viewModel.onAction(
                                AppAction.SetImageExportOptions(action.value),
                            )
                            PassImageExportAction.Save -> {
                                state.passes.firstOrNull { it.id == destination.passId }?.let { pass ->
                                    dependencies.onExportImage(pass, false, true)
                                }
                            }
                            PassImageExportAction.Share -> {
                                dependencies.viewModel.onAction(
                                    AppAction.ShareImage(destination.passId, state.settings.imageExportOptions),
                                )
                                dependencies.onBack()
                            }
                            PassImageExportAction.Print -> dependencies.viewModel.onAction(
                                AppAction.PrintImage(destination.passId, state.settings.imageExportOptions),
                            )
                        }
                    },
                )
            }
            entry<AppDestination.Settings> {
                SettingsScreen(
                    settings = state.settings,
                    codePassLabel = state.settings.codePassId?.let { id ->
                        state.passes.firstOrNull { it.id == id }?.let { pass ->
                            resolvePassCardTitle(
                                pass,
                                state.settings.homeCardSectionOrder,
                                state.settings.hiddenHomeCardSections,
                                state.settings.categories,
                            )
                        }
                    },
                    systemAccentColor = state.systemAccentColor,
                    scrollToNotifications = dependencies.scrollSettingsToNotifications,
                    onNotificationScrollConsumed = { dependencies.onSettingsScrollConsumed() },
                ) { action ->
                    handleSettingsAction(dependencies, action)
                }
            }
            entry<AppDestination.CodeSettings> {
                CodeSettingsScreen(
                    settings = state.settings,
                    codePassLabel = state.settings.codePassId?.let { id ->
                        state.passes.firstOrNull { it.id == id }?.let { pass ->
                            resolvePassCardTitle(
                                pass,
                                state.settings.homeCardSectionOrder,
                                state.settings.hiddenHomeCardSections,
                                state.settings.categories,
                            )
                        }
                    },
                ) { action ->
                    handleSettingsAction(dependencies, action)
                }
            }
            entry<AppDestination.CategorySettings> {
                CategorySettingsScreen(state.categories.filter { it.role == PassCategoryRole.CUSTOM }) { action ->
                    when (action) {
                        CategorySettingsAction.Back -> dependencies.onBack()
                        is CategorySettingsAction.Save -> dependencies.viewModel.onAction(
                            AppAction.SaveCategory(action.category),
                        )
                        is CategorySettingsAction.Delete -> dependencies.viewModel.onAction(
                            AppAction.DeleteCategory(action.categoryId),
                        )
                        is CategorySettingsAction.Move -> dependencies.viewModel.onAction(
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
                        PassDetailLayoutSettingsAction.Back -> dependencies.onBack()
                        PassDetailLayoutSettingsAction.OpenCodeSettings -> backStack.add(AppDestination.CodeSettings)
                        is PassDetailLayoutSettingsAction.Move -> dependencies.viewModel.onAction(
                            AppAction.MovePassDetailSection(action.section, action.offset),
                        )
                        is PassDetailLayoutSettingsAction.SetVisible -> dependencies.viewModel.onAction(
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
                        HomeCardLayoutSettingsAction.Back -> dependencies.onBack()
                        is HomeCardLayoutSettingsAction.Move -> dependencies.viewModel.onAction(
                            AppAction.MoveHomeCardSection(action.section, action.offset),
                        )
                        is HomeCardLayoutSettingsAction.SetVisible -> dependencies.viewModel.onAction(
                            AppAction.SetHomeCardSectionVisible(action.section, action.visible),
                        )
                    }
                }
            }
            entry<AppDestination.PassCodeSettings> {
                PassCodePickerScreen(
                    rows = state.codePassPickerRows,
                    onPick = { dependencies.viewModel.onAction(AppAction.SetCodePassId(it)) },
                    onBack = { dependencies.onBack() },
                )
            }
            entry<AppDestination.Licenses> {
                LicensesScreen(onBack = { dependencies.onBack() })
            }
        },
    )
}

private fun handleSettingsAction(dependencies: AppNavigationDependencies, action: SettingsAction) {
    if (handleNotificationSettingsAction(dependencies, action)) return
    if (handleProtectedSettingsAction(dependencies, action)) return
    if (handleAppearanceSettingsAction(dependencies, action)) return
    if (handleDetailSettingsAction(dependencies, action)) return
    if (handleDataSettingsAction(dependencies, action)) return
    handleNavigationSettingsAction(dependencies, action)
}

private fun handleDataSettingsAction(
    dependencies: AppNavigationDependencies,
    action: SettingsAction,
): Boolean = when (action) {
    SettingsAction.ExportArchive -> {
        dependencies.onExportArchive()
        true
    }
    SettingsAction.ImportArchive -> {
        dependencies.onImportArchive()
        true
    }
    else -> false
}

private fun handleNavigationSettingsAction(
    dependencies: AppNavigationDependencies,
    action: SettingsAction,
): Boolean = when (action) {
    SettingsAction.Back -> {
        dependencies.onBack()
        true
    }
    SettingsAction.OpenCategories -> {
        dependencies.backStack.add(AppDestination.CategorySettings)
        true
    }
    SettingsAction.OpenCodeSettings -> {
        dependencies.backStack.add(AppDestination.CodeSettings)
        true
    }
    SettingsAction.OpenPassViewSettings -> {
        dependencies.backStack.add(AppDestination.PassDetailLayoutSettings)
        true
    }
    SettingsAction.OpenHomeCardSettings -> {
        dependencies.backStack.add(AppDestination.HomeCardLayoutSettings)
        true
    }
    SettingsAction.OpenPassCodeSettings -> {
        dependencies.backStack.add(AppDestination.PassCodeSettings)
        true
    }
    SettingsAction.OpenPrivacyPolicy -> {
        dependencies.viewModel.onAction(AppAction.OpenUrl(PRIVACY_POLICY_URL))
        true
    }
    SettingsAction.OpenThirdPartyLicenses -> {
        dependencies.backStack.add(AppDestination.Licenses)
        true
    }
    SettingsAction.OpenSourceCode -> {
        dependencies.viewModel.onAction(AppAction.OpenUrl(PROJECT_REPOSITORY_URL))
        true
    }
    SettingsAction.OpenBugReportGitHub -> {
        dependencies.viewModel.onAction(AppAction.OpenUrl(BUG_REPORT_URL))
        true
    }
    SettingsAction.OpenBugReportEmail -> {
        dependencies.viewModel.onAction(AppAction.OpenUrl(BUG_REPORT_MAIL_URL))
        true
    }
    else -> false
}

private fun handleAppearanceSettingsAction(
    dependencies: AppNavigationDependencies,
    action: SettingsAction,
): Boolean = when (action) {
    is SettingsAction.SetTheme -> {
        dependencies.viewModel.onAction(AppAction.SetTheme(action.value))
        true
    }
    is SettingsAction.SetAmoledBlackBackground -> {
        dependencies.viewModel.onAction(AppAction.SetAmoledBlackBackground(action.value))
        true
    }
    is SettingsAction.SetDynamicColors -> {
        dependencies.viewModel.onAction(AppAction.SetDynamicColors(action.value))
        true
    }
    is SettingsAction.SetAccentColor -> {
        dependencies.viewModel.onAction(AppAction.SetAccentColor(action.value))
        true
    }
    is SettingsAction.SetColorStyle -> {
        dependencies.viewModel.onAction(AppAction.SetColorStyle(action.value))
        true
    }
    is SettingsAction.SetAutomaticBrightness -> {
        dependencies.viewModel.onAction(AppAction.SetAutomaticBrightness(action.value))
        true
    }
    is SettingsAction.SetSortOrder -> {
        dependencies.viewModel.onAction(AppAction.SetSortOrder(action.value))
        true
    }
    is SettingsAction.SetHighlightTodayPasses -> {
        dependencies.viewModel.onAction(AppAction.SetHighlightTodayPasses(action.value))
        true
    }
    is SettingsAction.SetAutomaticallyMarkPast -> {
        dependencies.viewModel.onAction(AppAction.SetAutomaticallyMarkPast(action.value))
        true
    }
    is SettingsAction.SetTrashEnabled -> {
        dependencies.viewModel.onAction(AppAction.SetTrashEnabled(action.value))
        true
    }
    else -> handleCodeViewSettingsAction(dependencies, action)
}

private fun handleCodeViewSettingsAction(
    dependencies: AppNavigationDependencies,
    action: SettingsAction,
): Boolean = when (action) {
    is SettingsAction.SetCodeSizeStep -> {
        dependencies.viewModel.onAction(AppAction.SetCodeSizeStep(action.value))
        true
    }
    is SettingsAction.SetCodeWhiteSurround -> {
        dependencies.viewModel.onAction(AppAction.SetCodeWhiteSurround(action.value))
        true
    }
    is SettingsAction.SetCodeExtraQuietZone -> {
        dependencies.viewModel.onAction(AppAction.SetCodeExtraQuietZone(action.value))
        true
    }
    is SettingsAction.SetCodeRotateQuarterTurn -> {
        dependencies.viewModel.onAction(AppAction.SetCodeRotateQuarterTurn(action.value))
        true
    }
    is SettingsAction.SetCodeKeepScreenOn -> {
        dependencies.viewModel.onAction(AppAction.SetCodeKeepScreenOn(action.value))
        true
    }
    else -> false
}

private fun handleDetailSettingsAction(
    dependencies: AppNavigationDependencies,
    action: SettingsAction,
): Boolean = when (action) {
    is SettingsAction.SetReminderMinutes -> {
        dependencies.viewModel.onAction(AppAction.SetReminderMinutes(action.value))
        true
    }
    is SettingsAction.SetNotificationAccessWindow -> {
        dependencies.viewModel.onAction(AppAction.SetNotificationAccessWindow(action.minutes))
        true
    }
    is SettingsAction.SetNotificationActionsEnabled -> {
        dependencies.viewModel.onAction(AppAction.SetNotificationActionsEnabled(action.value))
        true
    }
    is SettingsAction.SetShowProtectedPassLockIcon -> {
        dependencies.viewModel.onAction(AppAction.SetShowProtectedPassLockIcon(action.value))
        true
    }
    is SettingsAction.SetBlurProtectedPassCards -> {
        dependencies.viewModel.onAction(AppAction.SetBlurProtectedPassCards(action.value))
        true
    }
    is SettingsAction.SetBlockScreenshots -> {
        dependencies.viewModel.onAction(AppAction.SetBlockScreenshots(action.value))
        true
    }
    else -> false
}

private fun handleNotificationSettingsAction(
    dependencies: AppNavigationDependencies,
    action: SettingsAction,
): Boolean = when (action) {
    is SettingsAction.SetRemindersEnabled -> {
        updateReminders(dependencies, action.value)
        true
    }
    is SettingsAction.SetNotificationExactTiming -> {
        updateExactTiming(dependencies, action.value)
        true
    }
    else -> false
}

private fun handleProtectedSettingsAction(
    dependencies: AppNavigationDependencies,
    action: SettingsAction,
): Boolean = when (action) {
    is SettingsAction.SetLockAllPasses -> {
        if (action.value && !dependencies.passAuthenticator.canAuthenticate()) {
            showSnackbar(dependencies, R.string.app_set_screen_lock_before_protecting_every_pass)
        } else {
            resetProtectedState(dependencies)
            dependencies.viewModel.onAction(AppAction.SetLockAllPasses(action.value))
        }
        true
    }
    is SettingsAction.SetSeparateProtectedPasses -> {
        resetProtectedState(dependencies)
        dependencies.viewModel.onAction(AppAction.SetSeparateProtectedPasses(action.value))
        true
    }
    is SettingsAction.SetOfferCalendarAfterImport -> {
        if (action.value) {
            dependencies.onShowCalendarPermissionWarning()
        } else {
            dependencies.viewModel.onAction(AppAction.SetOfferCalendarAfterImport(false))
        }
        true
    }
    else -> false
}

private fun updateReminders(dependencies: AppNavigationDependencies, enabled: Boolean) {
    if (!enabled) {
        dependencies.viewModel.onAction(AppAction.SetRemindersEnabled(false))
        return
    }
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            dependencies.activity,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
    if (needsPermission) {
        dependencies.onRequestNotificationPermission()
        return
    }
    if (reminderNotificationsAvailable(dependencies.activity)) {
        dependencies.viewModel.onAction(AppAction.SetRemindersEnabled(true))
        return
    }
    dependencies.viewModel.onAction(AppAction.SetRemindersEnabled(false))
    dependencies.activity.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, dependencies.activity.packageName)
        },
    )
    showSnackbar(dependencies, R.string.app_allow_pass_reminders_in_settings)
}

private fun updateExactTiming(dependencies: AppNavigationDependencies, enabled: Boolean) {
    val alarmManager = dependencies.activity.getSystemService(AlarmManager::class.java)
    val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    if (!enabled || exactAllowed) {
        dependencies.viewModel.onAction(AppAction.SetNotificationExactTiming(enabled))
        return
    }
    dependencies.viewModel.onAction(AppAction.SetNotificationExactTiming(false))
    dependencies.activity.startActivity(
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            "package:${dependencies.activity.packageName}".toUri(),
        ),
    )
    showSnackbar(dependencies, R.string.app_allow_exact_alarms_then_enable_again)
}

private fun resetProtectedState(dependencies: AppNavigationDependencies) {
    dependencies.onResetProtectedState()
    if (dependencies.state.selectedCategoryId == PROTECTED_PASSES_CATEGORY_ID) {
        dependencies.viewModel.onAction(AppAction.SelectCategory(null))
    }
}

private fun showSnackbar(dependencies: AppNavigationDependencies, messageRes: Int) {
    dependencies.coroutineScope.launch {
        dependencies.snackbarHostState.currentSnackbarData?.dismiss()
        dependencies.snackbarHostState.showSnackbar(dependencies.activity.getString(messageRes))
    }
}
