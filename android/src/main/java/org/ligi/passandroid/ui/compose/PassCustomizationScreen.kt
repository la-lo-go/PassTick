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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassCustomizationScreen(
    pass: PassUiModel?,
    onAction: (PassCustomizationAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize pass") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { onAction(PassCustomizationAction.Back) }, shape = CircleShape) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (pass == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { Text("Pass not found") }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Pass image", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Choose an image stored in this pass. Automatic uses the best image for each view.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                ArtworkChoice(
                    label = "Automatic",
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
                    supportingContent = { Text("Choose the sections shown in the pass") },
                    leadingContent = { Icon(Icons.Default.ViewAgenda, null) },
                    modifier = Modifier.fillMaxWidth().clickable { onAction(PassCustomizationAction.OpenLayout) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                ) { Text("Pass layout") }
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
