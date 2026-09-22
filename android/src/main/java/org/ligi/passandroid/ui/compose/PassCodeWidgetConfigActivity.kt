package org.ligi.passandroid.ui.compose

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.ligi.passandroid.repository.StartupAppearanceStore
import org.ligi.passandroid.ui.state.AppAction
import org.ligi.passandroid.ui.state.MainViewModel
import org.ligi.passandroid.ui.theme.PassTheme

class PassCodeWidgetConfigActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startupAppearance = StartupAppearanceStore.read(this)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            PassTheme(
                themeMode = startupAppearance.themeMode,
                dynamicColors = startupAppearance.dynamicColors,
                accentColor = startupAppearance.accentColor,
                colorStyle = startupAppearance.colorStyle,
            ) {
                PassCodePickerScreen(
                    rows = state.codePassPickerRows,
                    onPick = { passId ->
                        viewModel.onAction(AppAction.SetCodePassId(passId))
                        finishConfigured(widgetId)
                    },
                    onBack = { finishConfigured(widgetId) },
                )
            }
        }
    }

    private fun finishConfigured(widgetId: Int) {
        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
        )
        finish()
    }
}
