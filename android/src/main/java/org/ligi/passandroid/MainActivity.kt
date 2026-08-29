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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import org.ligi.passandroid.navigation.AppDestination
import org.ligi.passandroid.platform.PlatformActions
import org.ligi.passandroid.repository.PassRepository
import org.ligi.passandroid.ui.compose.EditPassScreen
import org.ligi.passandroid.ui.compose.HelpScreen
import org.ligi.passandroid.ui.compose.PassDetailScreen
import org.ligi.passandroid.ui.compose.PassListScreen
import org.ligi.passandroid.ui.compose.ScannerScreen
import org.ligi.passandroid.ui.compose.SettingsScreen
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.MainViewModel
import org.ligi.passandroid.ui.theme.PassTheme
import kotlinx.coroutines.launch

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
            val repository = koinInject<PassRepository>()
            val platformActions = koinInject<PlatformActions>()
            val scope = rememberCoroutineScope()
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
                                    onOpen = { backStack.add(AppDestination.PassDetail(it)) },
                                    onImport = { importLauncher.launch(PASS_MIME_TYPES) },
                                    onScan = { backStack.add(AppDestination.Scanner) },
                                    onSettings = { backStack.add(AppDestination.Settings) },
                                    onHelp = { backStack.add(AppDestination.Help) },
                                )
                            }
                            entry<AppDestination.PassDetail> { destination ->
                                val pass = state.passes.firstOrNull { it.id == destination.passId }
                                PassDetailScreen(
                                    pass = pass,
                                    source = repository.find(destination.passId),
                                    onBack = { backStack.removeLastOrNull() },
                                    onEdit = { backStack.add(AppDestination.EditPass(destination.passId)) },
                                    onDelete = {
                                        viewModel.onAction(AppAction.DeletePass(destination.passId))
                                        backStack.removeLastOrNull()
                                    },
                                    onExport = {
                                        pendingExportId = destination.passId
                                        exportLauncher.launch("${pass?.description ?: "pass"}.espass")
                                    },
                                    onShare = {
                                        scope.launch {
                                            repository.prepareShare(destination.passId).onSuccess {
                                                platformActions.share(it, "application/vnd.espass-espass+zip")
                                            }
                                        }
                                    },
                                    automaticBrightness = state.settings.automaticBrightness,
                                    platformActions = platformActions,
                                )
                            }
                            entry<AppDestination.EditPass> { destination ->
                                EditPassScreen(
                                    pass = state.passes.firstOrNull { it.id == destination.passId },
                                    onBack = { backStack.removeLastOrNull() },
                                    onSave = {
                                        viewModel.onAction(AppAction.SavePass(destination.passId, it))
                                        backStack.removeLastOrNull()
                                    },
                                )
                            }
                            entry<AppDestination.Scanner> {
                                ScannerScreen(
                                    onBack = { backStack.removeLastOrNull() },
                                    onFileSelected = { viewModel.onAction(AppAction.Import(it)) },
                                )
                            }
                            entry<AppDestination.Settings> {
                                SettingsScreen(state.settings, viewModel::onAction) { backStack.removeLastOrNull() }
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
