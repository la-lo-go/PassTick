package org.ligi.passandroid.ui.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.ligi.passandroid.R
import org.ligi.passandroid.functions.getHumanCategoryString
import org.ligi.passandroid.navigation.PassDateField
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.ui.barcode.PassCodeImage
import org.ligi.passandroid.ui.state.PassArtworkDraft
import org.ligi.passandroid.ui.state.PassCodeDraft
import org.ligi.passandroid.ui.state.PassDraft
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationDraft
import org.ligi.passandroid.ui.state.PassUiModel

/** A field in the editor. The id stays with the field while it moves. */
internal data class FieldRow(val id: Int, val field: PassFieldUiModel)

private val RowGap = 8.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BasicsEditor(draft: PassDraft, onChange: (PassDraft) -> Unit) {
    OutlinedTextField(
        draft.description,
        { onChange(draft.copy(description = it)) },
        label = { Text(stringResource(R.string.edit_pass_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        draft.creator,
        { onChange(draft.copy(creator = it)) },
        label = { Text(stringResource(R.string.edit_pass_issuer)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.edit_pass_type), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EditorPassTypes.forEach { type ->
            FilterChip(
                selected = draft.type.editorChoice() == type,
                // A tap on the selected boarding chip keeps a legacy boarding type unchanged.
                onClick = { if (draft.type.editorChoice() != type) onChange(draft.copy(type = type)) },
                label = { Text(stringResource(getHumanCategoryString(type))) },
            )
        }
    }
    ColorPickerField(stringResource(R.string.edit_pass_accent_color), draft.accentColor, { onChange(draft.copy(accentColor = it)) }, Modifier.fillMaxWidth())
}

@Composable
internal fun DateEditor(draft: PassDraft, initialDateField: PassDateField?, onChange: (PassDraft) -> Unit) {
    val start = parseDraftDate(draft.calendarStart)
    DatePickerField(
        stringResource(R.string.edit_pass_start_date),
        start,
        { value ->
            // A new start gets a two hour event, the usual length the calendar export uses.
            val end = if (value != null && draft.calendarEnd.isBlank()) value.plusHours(2).toString() else draft.calendarEnd
            onChange(draft.copy(calendarStart = value?.toString().orEmpty(), calendarEnd = end))
        },
        Modifier.fillMaxWidth(),
        initiallyOpen = initialDateField == PassDateField.START,
    )
    DatePickerField(
        stringResource(R.string.edit_pass_end_date),
        parseDraftDate(draft.calendarEnd),
        { onChange(draft.copy(calendarEnd = it?.toString().orEmpty())) },
        Modifier.fillMaxWidth(),
        initiallyOpen = initialDateField == PassDateField.END,
    )
}

/**
 * One row per field. A long press on the handle drags the row; it swaps with a neighbor once it passes the
 * neighbor's middle. A tap opens the row to edit it.
 */
@Composable
internal fun FieldsEditor(rows: List<FieldRow>, onRowsChange: (List<FieldRow>) -> Unit) {
    var openId by rememberSaveable { mutableStateOf<Int?>(null) }
    var draggedId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<Int, Int>() }
    val gapPx = with(LocalDensity.current) { RowGap.toPx() }
    val haptics = LocalHapticFeedback.current
    var nextId by remember { mutableIntStateOf((rows.maxOfOrNull(FieldRow::id) ?: -1) + 1) }
    // The latest rows, for the drag callbacks that outlive one composition.
    var current by remember { mutableStateOf(rows) }
    current = rows

    fun drag(delta: Float) {
        val id = draggedId ?: return
        dragOffset += delta
        val index = current.indexOfFirst { it.id == id }
        val next = current.getOrNull(index + 1)
        val previous = current.getOrNull(index - 1)
        val nextHeight = next?.let { heights[it.id] }?.toFloat()
        val previousHeight = previous?.let { heights[it.id] }?.toFloat()
        if (nextHeight != null && dragOffset > nextHeight / 2f) {
            current = current.toMutableList().apply { add(index + 1, removeAt(index)) }
            dragOffset -= nextHeight + gapPx
            onRowsChange(current)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else if (previousHeight != null && dragOffset < -previousHeight / 2f) {
            current = current.toMutableList().apply { add(index - 1, removeAt(index)) }
            dragOffset += previousHeight + gapPx
            onRowsChange(current)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
        rows.forEach { row ->
            val dragged = row.id == draggedId
            FieldEditorRow(
                row = row,
                open = openId == row.id,
                modifier = Modifier
                    .onSizeChanged { heights[row.id] = it.height }
                    .zIndex(if (dragged) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (dragged) dragOffset else 0f
                        shadowElevation = if (dragged) 8.dp.toPx() else 0f
                        shape = RoundedCornerShape(16.dp)
                    },
                dragHandle = Modifier.pointerInput(row.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggedId = row.id
                            dragOffset = 0f
                            openId = null
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = { draggedId = null; dragOffset = 0f },
                        onDragCancel = { draggedId = null; dragOffset = 0f },
                        onDrag = { change, amount ->
                            change.consume()
                            drag(amount.y)
                        },
                    )
                },
                onToggle = { openId = if (openId == row.id) null else row.id },
                onChange = { field -> onRowsChange(rows.map { if (it.id == row.id) it.copy(field = field) else it }) },
                onDelete = { onRowsChange(rows.filterNot { it.id == row.id }) },
            )
        }
        OutlinedButton(
            onClick = {
                val row = FieldRow(nextId++, PassFieldUiModel("local-${System.nanoTime()}", "", "", false, null).withSlot(FieldSlot.DETAILS))
                onRowsChange(rows + row)
                openId = row.id
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Add, null)
            Text(stringResource(R.string.edit_pass_add_field))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldEditorRow(
    row: FieldRow,
    open: Boolean,
    modifier: Modifier,
    dragHandle: Modifier,
    onToggle: () -> Unit,
    onChange: (PassFieldUiModel) -> Unit,
    onDelete: () -> Unit,
) {
    val field = row.field
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DragHandle,
                    stringResource(R.string.edit_pass_reorder_field),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragHandle.size(48.dp).padding(12.dp),
                )
                Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                    Text(
                        field.label.ifBlank { stringResource(R.string.edit_pass_no_label) },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(field.value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        stringResource(field.slot.label),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.padding(start = 4.dp))
            }
            AnimatedVisibility(open) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(field.label, { onChange(field.copy(label = it)) }, label = { Text(stringResource(R.string.edit_pass_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(field.value, { onChange(field.copy(value = it)) }, label = { Text(stringResource(R.string.edit_pass_value)) }, modifier = Modifier.fillMaxWidth())
                    // The slot choice and the delete action share one row, so the open field has no line for one control.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                            FieldSlot.entries.forEachIndexed { index, slot ->
                                SegmentedButton(
                                    selected = field.slot == slot,
                                    onClick = { if (field.slot != slot) onChange(field.withSlot(slot)) },
                                    shape = SegmentedButtonDefaults.itemShape(index, FieldSlot.entries.size),
                                    // The default padding cuts the labels when four segments share a phone width.
                                    contentPadding = PaddingValues(horizontal = 2.dp),
                                    icon = {},
                                    label = { Text(stringResource(slot.label), maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                                )
                            }
                        }
                        DeleteIconButton(R.string.edit_pass_delete_field, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteIconButton(label: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Default.Delete, stringResource(label), tint = MaterialTheme.colorScheme.error) }
}

@Composable
internal fun CodesEditor(codes: List<PassCodeDraft>, onCodesChange: (List<PassCodeDraft>) -> Unit, onReadFromPhoto: () -> Unit) {
    codes.forEachIndexed { index, code ->
        CodeEditorCard(
            code = code,
            onChange = { changed -> onCodesChange(codes.toMutableList().also { it[index] = changed }) },
            onRemove = { onCodesChange(codes.toMutableList().also { it.removeAt(index) }) },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onCodesChange(codes + PassCodeDraft(EditorCodeFormats.first(), "")) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Add, null)
            Text(stringResource(R.string.edit_pass_add_code), maxLines = 1)
        }
        OutlinedButton(onClick = onReadFromPhoto, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.CameraAlt, null)
            Text(stringResource(R.string.edit_pass_read_code_from_photo), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CodeEditorCard(code: PassCodeDraft, onChange: (PassCodeDraft) -> Unit, onRemove: () -> Unit) {
    var formatMenuOpen by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // The code renders on white, like on the ticket, so the user sees whether the data encodes.
                Surface(shape = RoundedCornerShape(8.dp), color = androidx.compose.ui.graphics.Color.White) {
                    Box(Modifier.size(width = if (code.format.isQuadratic()) 72.dp else 120.dp, height = 72.dp).padding(6.dp), contentAlignment = Alignment.Center) {
                        if (code.message.isNotBlank()) PassCodeImage(code.format, code.message, modifier = Modifier.fillMaxSize())
                    }
                }
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { formatMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(code.format.displayName())
                        Icon(Icons.Default.ExpandMore, null)
                    }
                    DropdownMenu(formatMenuOpen, { formatMenuOpen = false }) {
                        EditorCodeFormats.forEach { format ->
                            DropdownMenuItem({ Text(format.displayName()) }, onClick = {
                                formatMenuOpen = false
                                onChange(code.copy(format = format))
                            })
                        }
                    }
                }
                DeleteIconButton(R.string.edit_pass_remove_code, onRemove)
            }
            OutlinedTextField(code.message, { onChange(code.copy(message = it)) }, label = { Text(stringResource(R.string.edit_pass_code_data)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                code.alternativeText,
                { onChange(code.copy(alternativeText = it)) },
                label = { Text(stringResource(R.string.edit_pass_code_text)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun PlacesEditor(locations: List<PassLocationDraft>, onLocationsChange: (List<PassLocationDraft>) -> Unit) {
    fun update(index: Int, location: PassLocationDraft) = onLocationsChange(locations.toMutableList().also { it[index] = location })
    locations.forEachIndexed { index, location ->
        // The exact position starts open only when the place already has coordinates.
        var exact by rememberSaveable(index) { mutableStateOf(location.latitude.isNotBlank() || location.longitude.isNotBlank()) }
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        location.name,
                        { update(index, location.copy(name = it)) },
                        label = { Text(stringResource(R.string.edit_pass_address_or_place)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onLocationsChange(locations.toMutableList().also { it.removeAt(index) }) }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.edit_pass_delete_location))
                    }
                }
                TextButton(onClick = { exact = !exact }) {
                    Text(stringResource(R.string.edit_pass_exact_position))
                    Icon(if (exact) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
                AnimatedVisibility(exact) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(location.latitude, { update(index, location.copy(latitude = it)) }, label = { Text(stringResource(R.string.edit_pass_latitude)) }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(location.longitude, { update(index, location.copy(longitude = it)) }, label = { Text(stringResource(R.string.edit_pass_longitude)) }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
    OutlinedButton(onClick = { onLocationsChange(locations + PassLocationDraft("", "", "")) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, null)
        Text(stringResource(R.string.edit_pass_add_location))
    }
}

private val PassArtworkKind.label: Int
    get() = when (this) {
        PassArtworkKind.ICON -> R.string.edit_pass_image_icon
        PassArtworkKind.LOGO -> R.string.edit_pass_image_logo
        PassArtworkKind.STRIP -> R.string.edit_pass_image_banner
        PassArtworkKind.THUMBNAIL -> R.string.edit_pass_image_thumbnail
        PassArtworkKind.FOOTER -> R.string.edit_pass_image_footer
    }

@Composable
internal fun ImagesEditor(pass: PassUiModel?, draft: PassDraft, onChange: (PassDraft) -> Unit) {
    var pendingKind by remember { mutableStateOf<PassArtworkKind?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val kind = pendingKind
        pendingKind = null
        if (uri != null && kind != null) {
            onChange(
                draft.copy(
                    artworkUpdates = draft.artworkUpdates.filterNot { it.kind == kind } + PassArtworkDraft(kind, uri),
                    artworkRemovals = draft.artworkRemovals - kind,
                ),
            )
        }
    }
    PassArtworkKind.entries.forEach { kind ->
        val existing = pass?.artwork?.firstOrNull { it.kind == kind }?.takeIf { kind !in draft.artworkRemovals }
        val replaced = draft.artworkUpdates.any { it.kind == kind }
        val name = stringResource(kind.label)
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    if (existing != null && !replaced) {
                        AdaptivePassArtwork(existing.bytes, kind, draft.accentColor, name, Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    val status = when {
                        replaced -> R.string.edit_pass_image_new
                        existing == null -> R.string.edit_pass_image_none
                        else -> null
                    }
                    status?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                TextButton(onClick = {
                    pendingKind = kind
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(if (existing != null || replaced) R.string.edit_pass_replace_image else R.string.edit_pass_add_image)) }
                if (existing != null || replaced) {
                    IconButton(onClick = {
                        onChange(
                            draft.copy(
                                artworkUpdates = draft.artworkUpdates.filterNot { it.kind == kind },
                                artworkRemovals = if (pass?.artwork?.any { it.kind == kind } == true) draft.artworkRemovals + kind else draft.artworkRemovals,
                            ),
                        )
                    }) { Icon(Icons.Default.Delete, stringResource(R.string.edit_pass_remove_image, name)) }
                }
            }
        }
    }
}

@Composable
internal fun NoteEditor(notes: String, onNotesChange: (String) -> Unit) {
    OutlinedTextField(notes, onNotesChange, label = { Text(stringResource(R.string.pass_detail_note)) }, minLines = 4, modifier = Modifier.fillMaxWidth())
}
