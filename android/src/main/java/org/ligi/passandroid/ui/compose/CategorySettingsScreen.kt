package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.repository.PassCategory
import org.ligi.passandroid.repository.PassCategoryRole
import org.ligi.passandroid.ui.state.CategorySettingsAction
import java.util.UUID
import androidx.compose.ui.res.stringResource
import org.ligi.passandroid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(
    categories: List<PassCategory>,
    onAction: (CategorySettingsAction) -> Unit,
) {
    var editing by remember { mutableStateOf<PassCategory?>(null) }
    var deleting by remember { mutableStateOf<PassCategory?>(null) }
    editing?.let { category ->
        CategoryEditorDialog(
            category = category,
            onDismiss = { editing = null },
            onSave = {
                onAction(CategorySettingsAction.Save(it))
                editing = null
            },
        )
    }
    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.category_delete_tag)) },
            text = { Text("The tag will be removed from passes in ${category.name}.") },
            confirmButton = {
                TextButton(onClick = {
                    onAction(CategorySettingsAction.Delete(category.id))
                    deleting = null
                }) { Text(stringResource(R.string.category_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.pass_detail_cancel)) } },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_tags)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(CategorySettingsAction.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pass_detail_back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = PassCategory(
                        id = "custom-${UUID.randomUUID()}",
                        name = "",
                        colorArgb = 0xFF6750A4,
                    )
                },
                modifier = Modifier.semantics { contentDescription = "Add tag" },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.category_add_tag)) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
        ) {
            items(categories.filter { it.role == PassCategoryRole.CUSTOM }, key = PassCategory::id) { category ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    ListItem(
                    supportingContent = null,
                    leadingContent = {
                        Box(
                            Modifier.size(32.dp)
                                .background(Color(category.colorArgb.toInt()), RoundedCornerShape(12.dp)),
                        ) { Icon(categoryIcon(category.icon), category.icon, tint = Color.White, modifier = Modifier.padding(7.dp)) }
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { onAction(CategorySettingsAction.Move(category.id, -1)) }) {
                                Icon(Icons.Default.ArrowUpward, "Move ${category.name} up")
                            }
                            IconButton(onClick = { onAction(CategorySettingsAction.Move(category.id, 1)) }) {
                                Icon(Icons.Default.ArrowDownward, "Move ${category.name} down")
                            }
                            if (category.role == PassCategoryRole.CUSTOM) {
                                IconButton(onClick = { deleting = category }) {
                                    Icon(Icons.Default.Delete, "Delete ${category.name} tag")
                                }
                            }
                        }
                    },
                    modifier = Modifier.clickable { editing = category },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    ) { Text(category.name) }
                }
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    category: PassCategory,
    onDismiss: () -> Unit,
    onSave: (PassCategory) -> Unit,
) {
    var name by remember(category.id) { mutableStateOf(category.name) }
    var color by remember(category.id) { mutableIntStateOf(category.colorArgb.toInt()) }
    var icon by remember(category.id) { mutableStateOf(category.icon) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category.name.isBlank()) "Add tag" else "Edit tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.category_name)) }, singleLine = true)
                ColorPickerField(stringResource(R.string.category_tag_color), color, { color = it }, Modifier.fillMaxWidth())
                Text(stringResource(R.string.category_tag_icon), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("label", "star", "event", "flight").forEach { option ->
                        IconButton(onClick = { icon = option }) {
                            Icon(categoryIcon(option), option, tint = if (icon == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(category.copy(name = name.trim(), colorArgb = color.toUInt().toLong(), icon = icon))
                },
            ) { Text(stringResource(R.string.category_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pass_detail_cancel)) } },
    )
}

internal fun categoryIcon(name: String) = when (name) {
    "star" -> Icons.Default.Star
    "event" -> Icons.Default.Event
    "flight" -> Icons.Default.FlightTakeoff
    else -> Icons.AutoMirrored.Filled.Label
}
