package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The app message host. A message is a compact pill on a tonal surface, centered at the bottom, so it informs without
 * covering the content like the full-width default snackbar.
 */
@Composable
internal fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data -> AppSnackbar(data) }
}

@Composable
private fun AppSnackbar(data: SnackbarData) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 2.dp,
            shadowElevation = 3.dp,
            modifier = Modifier.widthIn(max = 480.dp).heightIn(min = 44.dp),
        ) {
            Row(
                Modifier.padding(start = 20.dp, end = if (data.visuals.actionLabel != null) 6.dp else 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(vertical = 12.dp),
                )
                data.visuals.actionLabel?.let { label ->
                    TextButton(onClick = data::performAction) { Text(label) }
                }
            }
        }
    }
}
