package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.repository.PassDetailSection
import org.ligi.passandroid.repository.HomeCardSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassDetailLayoutSettingsScreen(
    order: List<PassDetailSection>,
    hidden: Set<PassDetailSection>,
    onAction: (org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pass view") },
                navigationIcon = {
                    IconButton(onClick = { onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Show or hide sections, then arrange their order.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            itemsIndexed(order, key = { _, section -> section.name }) { index, section ->
                Surface(
                    modifier = Modifier.animateItem(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    ListItem(
                        trailingContent = {
                            Row {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.Move(section, -1)) },
                                ) { Icon(Icons.Default.ArrowUpward, "Move ${section.displayName()} up") }
                                IconButton(
                                    enabled = index < order.lastIndex,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.Move(section, 1)) },
                                ) { Icon(Icons.Default.ArrowDownward, "Move ${section.displayName()} down") }
                                Switch(
                                    checked = section !in hidden,
                                    onCheckedChange = { visible ->
                                        onAction(org.ligi.passandroid.ui.state.PassDetailLayoutSettingsAction.SetVisible(section, visible))
                                    },
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { Text(section.displayName()) }
                }
            }
        }
    }
}

private fun PassDetailSection.displayName() = when (this) {
    PassDetailSection.ARTWORK -> "Pass image"
    PassDetailSection.BARCODE -> "Barcode"
    PassDetailSection.FIELDS -> "Pass details"
    PassDetailSection.LOCATIONS -> "Locations"
    PassDetailSection.CALENDAR -> "Date and time"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCardLayoutSettingsScreen(
    order: List<HomeCardSection>,
    hidden: Set<HomeCardSection>,
    onAction: (org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home cards") },
                navigationIcon = {
                    IconButton(onClick = { onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Choose the card image and arrange the text lines.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            item(key = "artwork") {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    ListItem(
                        supportingContent = { Text("The image stays beside the text") },
                        trailingContent = {
                            Switch(
                                checked = HomeCardSection.ARTWORK !in hidden,
                                onCheckedChange = { visible ->
                                    onAction(
                                        org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.SetVisible(
                                            HomeCardSection.ARTWORK,
                                            visible,
                                        ),
                                    )
                                },
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { Text("Pass image") }
                }
            }
            val textSections = order.filterNot { it == HomeCardSection.ARTWORK }
            itemsIndexed(textSections, key = { _, section -> section.name }) { index, section ->
                Surface(
                    modifier = Modifier.animateItem(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    ListItem(
                        trailingContent = {
                            Row {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.Move(section, -1)) },
                                ) { Icon(Icons.Default.ArrowUpward, "Move ${section.displayName()} up") }
                                IconButton(
                                    enabled = index < textSections.lastIndex,
                                    onClick = { onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.Move(section, 1)) },
                                ) { Icon(Icons.Default.ArrowDownward, "Move ${section.displayName()} down") }
                                Switch(
                                    checked = section !in hidden,
                                    onCheckedChange = { visible ->
                                        onAction(org.ligi.passandroid.ui.state.HomeCardLayoutSettingsAction.SetVisible(section, visible))
                                    },
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { Text(section.displayName()) }
                }
            }
        }
    }
}

private fun HomeCardSection.displayName() = when (this) {
    HomeCardSection.ARTWORK -> "Pass image"
    HomeCardSection.TITLE -> "Title"
    HomeCardSection.PRIMARY_FIELD -> "Primary field"
    HomeCardSection.DATE -> "Date and time"
    HomeCardSection.CREATOR -> "Creator"
    HomeCardSection.CATEGORY -> "Tag"
    HomeCardSection.PASS_TYPE -> "Pass type"
}
