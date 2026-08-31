package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.ligi.passandroid.repository.PassSnapshot
import org.ligi.passandroid.ui.state.ImportEntryStatus
import org.ligi.passandroid.ui.state.ImportInspectionEntry
import org.ligi.passandroid.ui.state.ImportInspectionState
import java.security.MessageDigest

@Composable
fun ImportInspectionDialog(state: ImportInspectionState, onDismiss: () -> Unit) {
    if (!state.isVisible) return
    Dialog(
        onDismissRequest = { if (!state.isImporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Import inspection", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    if (state.isImporting) "Reading ${state.entries.size} selected files" else "${state.importedCount} passes imported",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.isImporting) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.entries, key = ImportInspectionEntry::uri) { entry -> ImportInspectionRow(entry) }
                }
                if (!state.isImporting) {
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun ImportInspectionRow(entry: ImportInspectionEntry) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.displayName, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            when (entry.status) {
                ImportEntryStatus.WAITING -> Text("Waiting", color = MaterialTheme.colorScheme.onSurfaceVariant)
                ImportEntryStatus.READING -> Text("Reading pass data…", color = MaterialTheme.colorScheme.primary)
                ImportEntryStatus.FAILED -> Text(entry.error ?: "Unreadable pass", color = MaterialTheme.colorScheme.error)
                ImportEntryStatus.IMPORTED -> entry.pass?.let { pass -> ImportedPassSummary(pass) }
            }
        }
    }
}

@Composable
private fun ImportedPassSummary(pass: PassSnapshot) {
    Text(pass.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    pass.creator?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    val details = buildList {
        add(pass.type.name.lowercase().replaceFirstChar(Char::uppercase))
        pass.barcodeFormat?.let { format ->
            add("$format · ${pass.barcodeMessage.orEmpty().fingerprint()}")
        } ?: add("No barcode")
        add("${pass.fields.size} fields")
        pass.calendarTimeSpan?.from?.let { add(it.toLocalDateTime().toString()) }
        if (pass.artwork.isNotEmpty()) add(pass.artwork.joinToString { it.kind.name.lowercase() })
    }
    Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    pass.fields.take(3).takeIf { it.isNotEmpty() }?.let { fields ->
        Text(
            fields.joinToString(" · ") { field -> "${field.label}: ${field.value.take(48)}" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

private fun String.fingerprint(): String {
    if (isEmpty()) return "empty code"
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return "code ${digest.take(4).joinToString("") { byte -> "%02x".format(byte) }}"
}
