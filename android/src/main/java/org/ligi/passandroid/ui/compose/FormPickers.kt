package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val PickerColors = listOf(
    0xFF2859C5.toInt(),
    0xFF6750A4.toInt(),
    0xFF006C4C.toInt(),
    0xFF8C4A60.toInt(),
    0xFF9A4522.toInt(),
    0xFF745B00.toInt(),
    0xFF455D92.toInt(),
    0xFF5F5E62.toInt(),
)

@Composable
fun ColorPickerField(
    label: String,
    color: Int,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(Color(color)))
        Text("$label  ${formatPickerColor(color)}", Modifier.padding(start = 12.dp))
    }
    if (open) {
        ColorPickerDialog(
            title = label,
            initialColor = color,
            onDismiss = { open = false },
            onColorSelected = {
                onColorChange(it)
                open = false
            },
        )
    }
}

@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
) {
    var customColor by remember(initialColor) { mutableStateOf(formatPickerColor(initialColor)) }
    val parsed = remember(customColor) { parsePickerColor(customColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PickerColors.forEach { candidate ->
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(Color(candidate))
                                .clickable(role = Role.RadioButton) { onColorSelected(candidate) }
                                .semantics {
                                    role = Role.RadioButton
                                    contentDescription = "Select ${formatPickerColor(candidate)}"
                                },
                        )
                    }
                }
                OutlinedTextField(
                    value = customColor,
                    onValueChange = { customColor = it },
                    label = { Text("Custom color (#AARRGGBB)") },
                    supportingText = if (parsed == null) ({ Text("Enter a valid hex color") }) else null,
                    isError = parsed == null,
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { parsed?.let(onColorSelected) }) { Text("Use color") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: ZonedDateTime?,
    onValueChange: (ZonedDateTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val selectedMillis = value?.toInstant()?.toEpochMilli()
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.CalendarMonth, null)
            Text(
                value?.let { DateTimeFormatter.ofPattern("EEE, d MMM yyyy").format(java.time.Instant.ofEpochMilli(it.toInstant().toEpochMilli()).atZone(java.time.ZoneId.systemDefault())) }
                    ?: label,
                Modifier.padding(start = 10.dp),
            )
        }
        if (value != null) {
            IconButton(onClick = { onValueChange(null) }) { Icon(Icons.Default.Close, "Clear $label") }
        }
    }
    if (open) {
        val state = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        val zone = value?.zone ?: ZoneId.systemDefault()
                        val time = value?.toLocalTime() ?: LocalTime.NOON
                        onValueChange(ZonedDateTime.of(LocalDate.of(date.year, date.monthValue, date.dayOfMonth), time, zone))
                    }
                    open = false
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

fun formatPickerColor(color: Int): String = "#%08X".format(java.util.Locale.ROOT, color)

fun parsePickerColor(value: String): Int? = runCatching { android.graphics.Color.parseColor(value) }.getOrNull()
