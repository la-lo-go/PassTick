package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.ui.state.PassCustomizationAction
import org.ligi.passandroid.ui.state.PassUiModel
import androidx.compose.ui.res.stringResource
import org.ligi.passandroid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassCustomizationScreen(
    pass: PassUiModel?,
    onAction: (PassCustomizationAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pass_detail_customize_pass)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { onAction(PassCustomizationAction.Back) }, shape = CircleShape) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pass_detail_back))
                    }
                },
            )
        },
    ) { padding ->
        if (pass == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { Text(stringResource(R.string.pass_detail_pass_not_found)) }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.layout_pass_image), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.customize_choose_an_image_stored_in_this_pass_automatic_us),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                ArtworkChoice(
                    label = stringResource(R.string.customize_automatic),
                    selected = pass.preferredArtworkKind == null,
                    onClick = { onAction(PassCustomizationAction.SelectArtwork(null)) },
                )
            }
            items(pass.artwork, key = { it.kind }) { artwork ->
                ArtworkChoice(
                    label = artwork.kind.displayName(),
                    selected = pass.preferredArtworkKind == artwork.kind,
                    preview = {
                        AdaptivePassArtwork(
                            bytes = artwork.bytes,
                            kind = artwork.kind,
                            accentColor = pass.accentColor,
                            contentDescription = "${artwork.kind.displayName()} preview",
                            modifier = Modifier.width(112.dp).height(72.dp),
                        )
                    },
                    onClick = { onAction(PassCustomizationAction.SelectArtwork(artwork.kind)) },
                )
            }
            item {
                ListItem(
                    supportingContent = { Text(stringResource(R.string.customize_choose_the_sections_shown_in_the_pass)) },
                    leadingContent = { Icon(Icons.Default.ViewAgenda, null) },
                    modifier = Modifier.fillMaxWidth().clickable { onAction(PassCustomizationAction.OpenLayout) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                ) { Text(stringResource(R.string.customize_pass_layout)) }
            }
        }
    }
}

@Composable
private fun ArtworkChoice(
    label: String,
    selected: Boolean,
    preview: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            preview?.invoke()
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

private fun PassArtworkKind.displayName() = name.lowercase().replaceFirstChar(Char::uppercase)
