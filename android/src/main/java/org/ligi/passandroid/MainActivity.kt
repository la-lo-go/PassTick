package org.ligi.passandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.ligi.passandroid.navigation.AppDestination
import org.ligi.passandroid.ui.compose.EditPassScreen
import org.ligi.passandroid.ui.compose.HelpScreen
import org.ligi.passandroid.ui.compose.PassDetailScreen
import org.ligi.passandroid.ui.compose.PassListScreen
import org.ligi.passandroid.ui.compose.ScannerScreen
import org.ligi.passandroid.ui.compose.SettingsScreen
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.MainViewModel
import org.ligi.passandroid.ui.state.PassDetailAction
import org.ligi.passandroid.ui.state.PassFinderAction
import org.ligi.passandroid.ui.state.PassListAction
import org.ligi.passandroid.ui.state.SettingsAction
import org.ligi.passandroid.ui.theme.PassTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) intent.importUri()?.let { viewModel.onAction(AppAction.Import(it)) }
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val backStack = rememberNavBackStack(AppDestination.PassList)
            val snackbarHostState = remember { SnackbarHostState() }
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

            LaunchedEffect(state.message) {
                state.message?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.onAction(AppAction.ClearMessage)
                }
            }

            PassTheme(state.settings.themeMode) {
                Surface {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            entry<AppDestination.PassList> {
                                PassListScreen(
                                    state = state,
                                    onAction = { action ->
                                        when (action) {
                                            is PassListAction.OpenPass -> backStack.add(AppDestination.PassDetail(action.id))
                                            PassListAction.ImportPass -> importLauncher.launch(PASS_MIME_TYPES)
                                            PassListAction.FindPassFiles -> backStack.add(AppDestination.Scanner)
                                            PassListAction.OpenSettings -> backStack.add(AppDestination.Settings)
                                            PassListAction.OpenHelp -> backStack.add(AppDestination.Help)
                                        }
                                    },
                                )
                            }
                            entry<AppDestination.PassDetail> { destination ->
                                val pass = state.passes.firstOrNull { it.id == destination.passId }
                                PassDetailScreen(
                                    pass = pass,
                                    automaticBrightness = state.settings.automaticBrightness,
                                    onAction = { action ->
                                        when (action) {
                                            PassDetailAction.Back -> backStack.removeLastOrNull()
                                            PassDetailAction.Edit -> backStack.add(AppDestination.EditPass(destination.passId))
                                            PassDetailAction.Delete -> {
                                                viewModel.onAction(AppAction.DeletePass(destination.passId))
                                                backStack.removeLastOrNull()
                                            }
                                            PassDetailAction.Export -> {
                                                pendingExportId = destination.passId
                                                exportLauncher.launch("${pass?.description ?: "pass"}.espass")
                                            }
                                            PassDetailAction.Share -> viewModel.onAction(AppAction.SharePass(destination.passId))
                                            PassDetailAction.Print -> viewModel.onAction(AppAction.PrintPass(destination.passId))
                                            PassDetailAction.AddToCalendar -> viewModel.onAction(AppAction.AddToCalendar(destination.passId))
                                            is PassDetailAction.OpenLocation -> viewModel.onAction(
                                                AppAction.OpenLocation(destination.passId, action.index),
                                            )
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
                            entry<AppDestination.Scanner> {
                                ScannerScreen(
                                    onAction = { action ->
                                        when (action) {
                                            PassFinderAction.Back -> backStack.removeLastOrNull()
                                            is PassFinderAction.Import -> viewModel.onAction(AppAction.ImportFiles(action.uris))
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
                                    }
                                }
                            }
                            entry<AppDestination.Help> { HelpScreen { backStack.removeLastOrNull() } }
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
        intent.importUri()?.let { viewModel.onAction(AppAction.Import(it)) }
    }

    private companion object {
        val PASS_MIME_TYPES = arrayOf(
            "application/vnd.apple.pkpass",
            "application/vnd.espass-espass+zip",
            "application/zip",
        )
    }
}

@Suppress("DEPRECATION")
private fun Intent.importUri(): android.net.Uri? = data ?: getParcelableExtra(Intent.EXTRA_STREAM)
