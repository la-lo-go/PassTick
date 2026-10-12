package org.ligi.passandroid.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ligi.passandroid.R
import org.ligi.passandroid.export.PassExportRequest
import org.ligi.passandroid.export.exportId
import org.ligi.passandroid.platform.PassImageExporter
import org.ligi.passandroid.platform.exportLabels
import org.ligi.passandroid.repository.PassExportContent
import org.ligi.passandroid.repository.PassExportFormat
import org.ligi.passandroid.repository.PassExportSettings
import org.ligi.passandroid.repository.PassExportStyle
import org.ligi.passandroid.repository.PassExportTemplate
import org.ligi.passandroid.repository.PassImageAspectRatio
import org.ligi.passandroid.repository.PassImageExportOptions
import org.ligi.passandroid.repository.PassImageOrientation
import org.ligi.passandroid.ui.state.PassImageExportAction
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.hasDisplayableCode

private const val OPTIONS_HEIGHT_FRACTION = 0.5f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExportImageScreen(
    pass: PassUiModel?,
    settings: PassExportSettings,
    codeOnTop: Boolean,
    isBusy: Boolean,
    onAction: (PassImageExportAction) -> Unit,
) {
    // Field picks last only for this export, so another pass never inherits them.
    var excludedFieldIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val template = settings.template
    val request = PassExportRequest(template, settings.selectedOptions, codeOnTop, excludedFieldIds.toSet())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_export_image)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { onAction(PassImageExportAction.Back) }, shape = CircleShape) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pass_detail_back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (pass != null) ExportToolbar(request, isBusy, onAction)
        },
    ) { padding ->
        if (pass == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.pass_detail_pass_not_found))
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                // The options scroll inside at most half the screen, so the preview always keeps its space.
                val optionsMaxHeight = maxHeight * OPTIONS_HEIGHT_FRACTION
                Column(Modifier.fillMaxSize()) {
                    ExportPreview(pass, request, Modifier.fillMaxWidth().weight(1f).padding(12.dp))
                    ExportOptions(
                        modifier = Modifier.heightIn(max = optionsMaxHeight),
                        pass = pass,
                    template = template,
                    options = settings.selectedOptions,
                    excludedFieldIds = excludedFieldIds.toSet(),
                        onExcludedFieldIds = { excludedFieldIds = it.toList() },
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExportToolbar(request: PassExportRequest, isBusy: Boolean, onAction: (PassImageExportAction) -> Unit) {
    val saveLabel = stringResource(if (request.options.format == PassExportFormat.PDF) R.string.export_save_pdf else R.string.export_save_png)
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        floatingActionButton = {
            FloatingToolbarDefaults.StandardFloatingActionButton(
                onClick = { if (!isBusy) onAction(PassImageExportAction.Save(request)) },
            ) {
                if (isBusy) {
                    LoadingIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                } else {
                    Icon(Icons.Default.Save, saveLabel)
                }
            }
        },
    ) {
        IconButton(onClick = { if (!isBusy) onAction(PassImageExportAction.Share(request)) }) {
            Icon(Icons.Default.Share, stringResource(R.string.export_share))
        }
        IconButton(onClick = { if (!isBusy) onAction(PassImageExportAction.Print(request)) }) {
            Icon(Icons.Default.Print, stringResource(R.string.export_print_pass))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExportOptions(
    modifier: Modifier,
    pass: PassUiModel,
    template: PassExportTemplate,
    options: PassImageExportOptions,
    excludedFieldIds: Set<String>,
    onExcludedFieldIds: (Set<String>) -> Unit,
    onAction: (PassImageExportAction) -> Unit,
) {
    fun setOptions(updated: PassImageExportOptions) = onAction(PassImageExportAction.SetOptions(template, updated))
    val motion = MaterialTheme.motionScheme
    val spatial = remember { spring<IntSize>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f) }
    val effects = remember(motion) { motion.fastEffectsSpec<Float>() }
    val fields = exportableFields(pass, template, options)
    var fieldsExpanded by rememberSaveable { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConnectedChoice(PassExportTemplate.entries, template, Modifier.height(56.dp), { stringResource(templateLabel(it)) }, ::templateIcon) {
                onAction(PassImageExportAction.SelectTemplate(it))
            }
            SectionTitle(R.string.export_section_format)
            // The format is one choice, so it uses a segmented control. Styles below are independent switches.
            ConnectedChoice(PassExportFormat.entries, options.format, Modifier.height(40.dp), { stringResource(formatLabel(it)) }) {
                setOptions(options.copy(format = it))
            }
            ChipRow {
                template.styles.forEach { style ->
                    ToggleChip(stringResource(styleLabel(style)), options[style]) { setOptions(options.with(style, !options[style])) }
                }
            }
            if (template.aspectRatios.size > 1) {
                SectionTitle(R.string.export_section_size)
                ChipRow {
                    template.aspectRatios.forEach { ratio ->
                        FilterChip(
                            selected = options.aspectRatio == ratio,
                            onClick = { setOptions(options.copy(aspectRatio = ratio)) },
                            label = { Text(aspectRatioLabel(ratio, if (template.hasOrientation(ratio)) options.orientation else PassImageOrientation.PORTRAIT)) },
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = template.hasOrientation(options.aspectRatio),
                enter = fadeIn(effects) + expandVertically(spatial, expandFrom = Alignment.Top),
                exit = fadeOut(effects) + shrinkVertically(spatial, shrinkTowards = Alignment.Top),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PassImageOrientation.entries.forEach { orientation ->
                        val portrait = orientation == PassImageOrientation.PORTRAIT
                        FilterChip(
                            selected = options.orientation == orientation,
                            onClick = { setOptions(options.copy(orientation = orientation)) },
                            label = { Text(stringResource(if (portrait) R.string.export_vertical else R.string.export_horizontal)) },
                            leadingIcon = { Icon(if (portrait) Icons.Default.CropPortrait else Icons.Default.CropLandscape, null) },
                        )
                    }
                }
            }
            SectionTitle(R.string.export_section_content)
            ChipRow {
                availableContent(pass, template).forEach { item ->
                    ToggleChip(stringResource(contentLabel(item, template)), options.content[item]) {
                        setOptions(options.copy(content = options.content.with(item, !options.content[item])))
                    }
                }
            }
            if (fields.isNotEmpty()) {
                // The count stays in sight while the list is folded, so the export content is never hidden.
                val included = fields.count { it.exportId !in excludedFieldIds }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { fieldsExpanded = !fieldsExpanded }
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.export_fields),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.export_fields_included, included, fields.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        if (fieldsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AnimatedVisibility(
                visible = fieldsExpanded && fields.isNotEmpty(),
                enter = fadeIn(effects) + expandVertically(spatial, expandFrom = Alignment.Top),
                exit = fadeOut(effects) + shrinkVertically(spatial, shrinkTowards = Alignment.Top),
            ) {
                ChipRow {
                    fields.forEach { field ->
                        val id = field.exportId
                        val included = id !in excludedFieldIds
                        ToggleChip(field.chipLabel(), included, Modifier.widthIn(max = 280.dp)) {
                            onExcludedFieldIds(if (included) excludedFieldIds + id else excludedFieldIds - id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** The fields the template can show with the current content: front fields with details, back fields when selected. */
private fun exportableFields(pass: PassUiModel, template: PassExportTemplate, options: PassImageExportOptions): List<PassFieldUiModel> {
    if (template == PassExportTemplate.CODE) return emptyList()
    val content = PassExportRequest(template, options).content
    return pass.fields.filter { field -> field.value.isNotBlank() && if (field.hidden) content.hiddenFields else content.details }
}

private fun PassFieldUiModel.chipLabel(): String = if (label.isBlank()) value else "$label · $value"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

/** An on and off switch. The check mark tells it apart from the single-choice chips. */
@Composable
private fun ToggleChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        modifier = modifier,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
        } else {
            null
        },
    )
}

/** A single choice as connected buttons: exactly one item is selected. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> ConnectedChoice(
    items: List<T>,
    selected: T,
    modifier: Modifier,
    label: @Composable (T) -> String,
    icon: ((T) -> ImageVector)? = null,
    onSelect: (T) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        items.forEachIndexed { index, item ->
            ToggleButton(
                checked = item == selected,
                onCheckedChange = { onSelect(item) },
                modifier = modifier.weight(1f).semantics { role = Role.RadioButton },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    items.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    icon?.let { Icon(it(item), null, Modifier.size(18.dp)) }
                    Text(label(item), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** The content toggles of the template, limited to what the pass has. */
private fun availableContent(pass: PassUiModel, template: PassExportTemplate): List<PassExportContent> =
    PassExportContent.entries.filter { item ->
        item in template.content && when (item) {
            PassExportContent.ARTWORK -> pass.artwork.isNotEmpty()
            PassExportContent.DETAILS -> true
            PassExportContent.NOTES -> pass.notes.isNotBlank()
            PassExportContent.BARCODE -> pass.hasDisplayableCode()
            PassExportContent.DATE_TIME -> pass.calendarTimeSpan?.from != null || pass.calendarTimeSpan?.to != null || pass.expiresAt != null
            PassExportContent.LOCATION -> pass.locations.isNotEmpty()
            PassExportContent.HIDDEN_FIELDS -> pass.fields.any { it.hidden }
        }
    }

private fun contentLabel(item: PassExportContent, template: PassExportTemplate): Int = when (item) {
    PassExportContent.ARTWORK -> R.string.export_artwork
    PassExportContent.DETAILS -> if (template == PassExportTemplate.CODE) R.string.export_caption else R.string.export_details
    PassExportContent.NOTES -> R.string.export_notes
    PassExportContent.BARCODE -> R.string.edit_pass_code
    PassExportContent.DATE_TIME -> R.string.export_date
    PassExportContent.LOCATION -> R.string.export_location
    PassExportContent.HIDDEN_FIELDS -> R.string.export_hidden
}

private fun styleLabel(style: PassExportStyle): Int = when (style) {
    PassExportStyle.MONOCHROME -> R.string.export_monochrome
    PassExportStyle.OPAQUE_BACKGROUND -> R.string.export_opaque_background
}

private fun templateLabel(template: PassExportTemplate): Int = when (template) {
    PassExportTemplate.TICKET -> R.string.export_template_ticket
    PassExportTemplate.STORY -> R.string.export_template_story
    PassExportTemplate.SHEET -> R.string.export_template_sheet
    PassExportTemplate.CODE -> R.string.export_template_code
}

private fun formatLabel(format: PassExportFormat): Int =
    if (format == PassExportFormat.PDF) R.string.export_format_pdf else R.string.export_format_png

private fun templateIcon(template: PassExportTemplate): ImageVector = when (template) {
    PassExportTemplate.TICKET -> Icons.Default.ConfirmationNumber
    PassExportTemplate.STORY -> Icons.Default.Smartphone
    PassExportTemplate.SHEET -> Icons.Default.Description
    PassExportTemplate.CODE -> Icons.Default.QrCode2
}

@Composable
private fun aspectRatioLabel(ratio: PassImageAspectRatio, orientation: PassImageOrientation): String {
    if (ratio == PassImageAspectRatio.A4) return "A4"
    if (ratio == PassImageAspectRatio.LETTER) return stringResource(R.string.export_letter)
    if (ratio.ratioWidth == 0f) return stringResource(R.string.export_auto)
    val (width, height) = if (orientation == PassImageOrientation.LANDSCAPE) {
        ratio.ratioHeight to ratio.ratioWidth
    } else {
        ratio.ratioWidth to ratio.ratioHeight
    }
    return "${width.toInt()}:${height.toInt()}"
}

private val CanvasShape = RoundedCornerShape(6.dp)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExportPreview(pass: PassUiModel, request: PassExportRequest, modifier: Modifier) {
    BoxWithConstraints(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val context = LocalContext.current
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth
        val targetWidth = remember(maxWidthPx) { maxWidthPx.coerceIn(1, with(density) { 1000.dp.toPx() }.toInt()) }
        val boxAspect = maxWidth / maxHeight
        val preview by produceState<ImageBitmap?>(null, pass, request, targetWidth) {
            value = withContext(Dispatchers.Default) {
                runCatching {
                    PassImageExporter.renderBitmap(pass, request, exportLabels(context, pass), targetWidth).asImageBitmap()
                }.getOrNull()
            }
        }
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 6f)
            offset = if (scale > 1f) offset + panChange else Offset.Zero
        }
        val motion = MaterialTheme.motionScheme
        val effects = remember(motion) { motion.fastEffectsSpec<Float>() }
        Crossfade(targetState = preview, animationSpec = effects, label = "preview") { image ->
            if (image == null) {
                LoadingIndicator()
            } else {
                // The canvas shows its real shape on a tonal surface, so a change of ratio or orientation is visible
                // even when the export background is transparent.
                val canvasColor = MaterialTheme.colorScheme.surfaceContainerHighest
                val canvasEdge = MaterialTheme.colorScheme.outlineVariant
                val aspect = image.width.toFloat() / image.height
                Box(Modifier.fillMaxSize().padding(12.dp).transformable(transformState), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = image,
                        contentDescription = stringResource(R.string.export_preview),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .aspectRatio(aspect, matchHeightConstraintsFirst = aspect < boxAspect)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                            .clip(CanvasShape)
                            .background(canvasColor)
                            .border(1.dp, canvasEdge, CanvasShape),
                    )
                }
            }
        }
    }
}
