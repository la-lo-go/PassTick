package org.ligi.passandroid.ui.compose

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ligi.passandroid.R
import org.ligi.passandroid.functions.getHumanCategoryString
import org.ligi.passandroid.imports.DocumentImportProcessor
import org.ligi.passandroid.navigation.PassDateField
import org.ligi.passandroid.ui.state.EditPassAction
import org.ligi.passandroid.ui.state.PassCodeDraft
import org.ligi.passandroid.ui.state.PassDraft
import org.ligi.passandroid.ui.state.PassUiModel
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPassScreen(pass: PassUiModel?, initialDateField: PassDateField? = null, onAction: (EditPassAction) -> Unit) {
    var draft by remember(pass?.id) { mutableStateOf(pass?.toEditableDraft() ?: emptyPassDraft()) }
    // Rows carry an id, so a dragged or opened field keeps its state while the list changes.
    var fieldRows by remember(pass?.id) { mutableStateOf(draft.fields.mapIndexed(::FieldRow)) }
    var openPart by rememberSaveable(pass?.id) {
        mutableStateOf(
            when {
                initialDateField != null -> EditorPart.DATE
                pass == null -> EditorPart.BASICS
                else -> null
            },
        )
    }
    var issue by remember { mutableStateOf<PassDraftIssue?>(null) }
    var editorMenuOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val context = LocalContext.current
    fun currentDraft() = draft.copy(fields = fieldRows.map(FieldRow::field))
    fun open(part: EditorPart) {
        openPart = part
        scope.launch { listState.animateScrollToItem(part.ordinal) }
    }
    // Any edit clears the shown error, so a stale message never points at fixed content.
    LaunchedEffect(draft, fieldRows) { issue = null }

    val codePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val detected = withContext(Dispatchers.Default) {
                DocumentImportProcessor.decodeNormalizedBitmap(context, uri)
                    ?.let { bitmap -> DocumentImportProcessor.detectCodes(bitmap) }
                    ?.firstOrNull()
            }
            if (detected == null) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(resources.getString(R.string.edit_pass_code_not_found))
            } else {
                draft = draft.copy(codes = draft.codes + PassCodeDraft(detected.format, detected.message))
            }
        }
    }

    fun saveAndClose() {
        val current = currentDraft()
        val problem = validatePassDraft(current)
        when {
            problem != null -> {
                issue = problem
                open(problem.part)
            }
            pass != null && current == pass.toEditableDraft() -> onAction(EditPassAction.Back)
            else -> onAction(EditPassAction.Save(current))
        }
    }

    fun closeEditor() = onAction(EditPassAction.Back)

    BackHandler(onBack = if (pass == null) ::closeEditor else ::saveAndClose)
    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (pass == null) R.string.edit_pass_new_pass else R.string.pass_detail_edit_pass)) },
                navigationIcon = {
                    IconButton(onClick = if (pass == null) ::closeEditor else ::saveAndClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(if (pass == null) R.string.edit_pass_discard_changes else R.string.edit_pass_save_and_go_back),
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { editorMenuOpen = true }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.edit_pass_editor_actions))
                        }
                        DropdownMenu(editorMenuOpen, { editorMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_pass_discard_changes)) },
                                onClick = {
                                    editorMenuOpen = false
                                    closeEditor()
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding).testTag("edit_pass_list"),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EditorPart.entries.forEach { part ->
                        item(key = part.name) {
                            EditorSection(
                                title = stringResource(part.title),
                                summary = sectionSummary(part, draft, fieldRows, keptImageCount(pass, draft)),
                                open = openPart == part,
                                error = issue?.takeIf { it.part == part }?.let { stringResource(it.messageRes) },
                                onToggle = { openPart = if (openPart == part) null else part },
                            ) {
                                when (part) {
                                    EditorPart.BASICS -> BasicsEditor(draft) { draft = it }
                                    EditorPart.DATE -> DateEditor(draft, initialDateField) { draft = it }
                                    EditorPart.FIELDS -> FieldsEditor(fieldRows) { fieldRows = it }
                                    EditorPart.CODES -> CodesEditor(
                                        codes = draft.codes,
                                        onCodesChange = { draft = draft.copy(codes = it) },
                                        onReadFromPhoto = {
                                            codePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                    )
                                    EditorPart.PLACES -> PlacesEditor(draft.locations) { draft = draft.copy(locations = it) }
                                    EditorPart.IMAGES -> ImagesEditor(pass, draft) { draft = it }
                                    EditorPart.NOTE -> NoteEditor(draft.notes) { draft = draft.copy(notes = it) }
                                }
                            }
                        }
                    }
                    item(key = "save") {
                        Button(onClick = ::saveAndClose, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text(stringResource(R.string.edit_pass_save_and_close))
                        }
                    }
                }
    }
}

private val EditorPart.title: Int
    get() = when (this) {
        EditorPart.BASICS -> R.string.edit_pass_basics
        EditorPart.DATE -> R.string.edit_pass_date_time
        EditorPart.FIELDS -> R.string.edit_pass_fields
        EditorPart.CODES -> R.string.edit_pass_codes
        EditorPart.PLACES -> R.string.edit_pass_places
        EditorPart.IMAGES -> R.string.edit_pass_images
        EditorPart.NOTE -> R.string.pass_detail_note
    }

/** The images that the pass keeps after the pending replacements and removals. */
private fun keptImageCount(pass: PassUiModel?, draft: PassDraft): Int =
    (pass?.artwork.orEmpty().map { it.kind } + draft.artworkUpdates.map { it.kind }).toSet()
        .count { kind -> kind !in draft.artworkRemovals || draft.artworkUpdates.any { it.kind == kind } }

@Composable
private fun sectionSummary(part: EditorPart, draft: PassDraft, fieldRows: List<FieldRow>, imageCount: Int): String = when (part) {
    EditorPart.BASICS -> listOf(stringResource(getHumanCategoryString(draft.type)), draft.description)
        .filter(String::isNotBlank).joinToString(" · ")
    EditorPart.DATE -> parseDraftDate(draft.calendarStart)
        ?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
        ?: stringResource(R.string.edit_pass_summary_no_date)
    EditorPart.FIELDS -> {
        val back = fieldRows.count { it.field.hidden }
        stringResource(R.string.edit_pass_summary_fields, fieldRows.size - back, back)
    }
    EditorPart.CODES -> when (draft.codes.size) {
        0 -> stringResource(R.string.edit_pass_summary_no_code)
        1 -> draft.codes.single().format.displayName()
        else -> pluralStringResource(R.plurals.edit_pass_summary_codes, draft.codes.size, draft.codes.size)
    }
    EditorPart.PLACES -> when {
        draft.locations.isEmpty() -> stringResource(R.string.edit_pass_summary_no_places)
        else -> draft.locations.singleOrNull()?.name?.takeIf(String::isNotBlank)
            ?: pluralStringResource(R.plurals.edit_pass_summary_places, draft.locations.size, draft.locations.size)
    }
    EditorPart.IMAGES -> if (imageCount == 0) {
        stringResource(R.string.edit_pass_summary_no_images)
    } else {
        pluralStringResource(R.plurals.edit_pass_summary_images, imageCount, imageCount)
    }
    EditorPart.NOTE -> draft.notes.lineSequence().firstOrNull()?.takeIf(String::isNotBlank)
        ?: stringResource(R.string.edit_pass_summary_empty)
}

/** A folded section shows its title and a summary. The open section shows its editor and its error. */
@Composable
private fun EditorSection(
    title: String,
    summary: String,
    open: Boolean,
    error: String?,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (open) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (!open) {
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    stringResource(if (open) R.string.edit_pass_collapse_section else R.string.edit_pass_expand_section, title),
                )
            }
            AnimatedVisibility(visible = open, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    content()
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}
