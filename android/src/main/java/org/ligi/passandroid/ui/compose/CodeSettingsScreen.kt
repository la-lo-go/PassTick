package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.R
import org.ligi.passandroid.repository.AppSettings
import org.ligi.passandroid.ui.state.SettingsAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeSettingsScreen(settings: AppSettings, onAction: (SettingsAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_code)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pass_detail_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
        ) {
            item { CodeSettingsGroup(settings, onAction) }
        }
    }
}
