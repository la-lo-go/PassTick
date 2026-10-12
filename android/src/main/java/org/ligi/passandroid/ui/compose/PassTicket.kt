package org.ligi.passandroid.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import org.ligi.passandroid.R
import org.ligi.passandroid.functions.getHumanCategoryString
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.displayArtwork
import org.ligi.passandroid.ui.state.PassTicketLayout
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.gridRows
import org.ligi.passandroid.ui.state.issuerName
import org.ligi.passandroid.ui.state.ticketDateLabel
import org.ligi.passandroid.ui.state.ticketFields
import org.ligi.passandroid.ui.state.ticketTimeLabel
import org.ligi.passandroid.ui.state.unlessIssuer

private val TicketCornerRadius = 28.dp
private val TicketEdgeWidth = 2.dp
private val InnerCornerRadius = TicketCornerRadius - TicketEdgeWidth
private val InnerShape = RoundedCornerShape(InnerCornerRadius)
private val InnerTopShape = RoundedCornerShape(topStart = InnerCornerRadius, topEnd = InnerCornerRadius)
private val InnerBottomShape = RoundedCornerShape(bottomStart = InnerCornerRadius, bottomEnd = InnerCornerRadius)
private val TicketShape = RoundedCornerShape(TicketCornerRadius)
private const val GRID_COLUMNS = 3
private const val LARGE_VALUE_MAX_LENGTH = 10
private const val ROUTE_CODE_MAX_LENGTH = 4
private val ROUTE_TIME = Regex("\\d{1,2}:\\d{2}")
private val HEADER_ICON_KINDS = listOf(PassArtworkKind.ICON, PassArtworkKind.THUMBNAIL, PassArtworkKind.LOGO)

private class TicketColors(val container: Color, val content: Color, val label: Color, val tonal: Color, val outlined: Boolean)

@Composable
private fun ticketColors(pass: PassUiModel, layout: PassTicketLayout): TicketColors {
    if (layout is PassTicketLayout.Simple) {
        val scheme = MaterialTheme.colorScheme
        return TicketColors(scheme.surfaceContainerHigh, scheme.onSurface, scheme.onSurfaceVariant, scheme.surfaceContainerHighest, false)
    }
    val container = Color(pass.accentColor).copy(alpha = 1f)
    val luminance = container.luminance()
    val content = if (luminance > 0.45f) Color(0xFF1B1B1F) else Color.White
    // Issuers often pick white backgrounds. An outline keeps the ticket edge visible on a light screen.
    return TicketColors(container, content, content.copy(alpha = 0.74f), content.copy(alpha = 0.12f), luminance > 0.9f)
}

/** The pass shown as one ticket: issuer-colored details, then the code on white below a perforation. */
@Composable
internal fun PassTicket(
    pass: PassUiModel,
    layout: PassTicketLayout,
    modifier: Modifier = Modifier,
    /** True when the code comes before the details in the pass view order, so the ticket is upside down. */
    codeOnTop: Boolean = false,
    code: (@Composable () -> Unit)? = null,
) {
    val colors = ticketColors(pass, layout)
    val issuer = pass.issuerName()
    val typeLabel = stringResource(getHumanCategoryString(pass.type))
    val (headerFields, gridFields) = pass.ticketFields(layout, typeLabel)
    // A thin edge in the pass color outlines the white code area. A white pass uses the outline color instead.
    val edgeColor = if (colors.outlined) MaterialTheme.colorScheme.outlineVariant else colors.container
    var ticketTop by remember { mutableFloatStateOf(0f) }
    var tearLine by remember { mutableFloatStateOf(Float.NaN) }
    // The edge color fills the whole ticket and the content sits inset by the edge width. Only the edge color reaches
    // the rounded clip, so its anti-aliased pixels never mix with the white code area.
    Column(
        modifier.fillMaxWidth()
            .onGloballyPositioned { ticketTop = it.positionInRoot().y }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .clip(TicketShape)
            .drawWithContent {
                drawContent()
                drawTicketNotches(edgeColor, tearLine)
            }
            .background(edgeColor)
            .padding(TicketEdgeWidth),
    ) {
        val detailsShape = when {
            code == null -> InnerShape
            codeOnTop -> InnerBottomShape
            else -> InnerTopShape
        }
        val details: @Composable () -> Unit = {
            Column(
                Modifier.fillMaxWidth()
                    .clip(detailsShape)
                    .background(colors.container)
                    .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TicketHeader(pass, layout, issuer, typeLabel, headerFields, colors)
                when (layout) {
                    is PassTicketLayout.Boarding -> {
                        Route(layout.origin, layout.destination, colors)
                        pass.calendarTimeSpan?.from?.let { from ->
                            TonalCells(
                                stringResource(R.string.pass_detail_ticket_date) to ticketDateLabel(from, null, LocalConfiguration.current.locales[0]),
                                emptyList(),
                                colors,
                            )
                        }
                    }
                    is PassTicketLayout.Event -> EventMain(pass, layout, issuer, colors)
                    is PassTicketLayout.Coupon -> PrimaryValue(layout.offer, issuer, colors)
                    is PassTicketLayout.StoreCard -> {
                        PrimaryValue(layout.primary, issuer, colors)
                        layout.progress?.let { progress ->
                            LinearProgressIndicator(
                                progress = { progress.fraction },
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.content,
                                trackColor = colors.tonal,
                            )
                        }
                    }
                    is PassTicketLayout.Generic -> PrimaryValue(layout.primary, issuer, colors)
                    PassTicketLayout.Simple -> SimpleTitle(pass, colors)
                }
                // Event and boarding layouts place the date in their own block.
            if (layout !is PassTicketLayout.Event && layout !is PassTicketLayout.Boarding) DateCells(pass, colors)
            if (gridFields.isNotEmpty()) {
                    when (layout) {
                        PassTicketLayout.Simple -> FieldList(gridFields, colors)
                        is PassTicketLayout.Event -> FieldGrid(gridFields, colors, EventColumns, columnGap = 2.dp, cellPadding = 12.dp)
                        else -> FieldGrid(gridFields, colors, List(GRID_COLUMNS) { 1f }, columnGap = 12.dp, cellPadding = 0.dp)
                    }
                }
            }
        }
        val codeArea: @Composable () -> Unit = {
            Box(
                Modifier.fillMaxWidth()
                    .clip(if (codeOnTop) InnerTopShape else InnerBottomShape)
                    .background(Color.White)
                    .padding(start = 6.dp, end = 6.dp, top = if (codeOnTop) 10.dp else 0.dp, bottom = if (codeOnTop) 0.dp else 10.dp),
            ) { code?.invoke() }
        }
        val perforation: @Composable () -> Unit = {
            Perforation(
                colors.container,
                colorOnTop = !codeOnTop,
                modifier = Modifier.onGloballyPositioned { tearLine = it.positionInRoot().y - ticketTop + it.size.height / 2f },
            )
        }
        when {
            code == null -> details()
            codeOnTop -> {
                codeArea()
                perforation()
                details()
            }
            else -> {
                details()
                perforation()
                codeArea()
            }
        }
    }
}

@Composable
private fun TicketHeader(
    pass: PassUiModel,
    layout: PassTicketLayout,
    issuer: String,
    typeLabel: String,
    headerFields: List<PassFieldUiModel>,
    colors: TicketColors,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (layout is PassTicketLayout.Simple) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(pass.accentColor).copy(alpha = 1f)))
        } else {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(colors.tonal), contentAlignment = Alignment.Center) {
                val icon = pass.displayArtwork(HEADER_ICON_KINDS)
                if (icon != null) {
                    AdaptivePassArtwork(
                        bytes = icon.bytes,
                        kind = icon.kind,
                        accentColor = pass.accentColor,
                        contentDescription = stringResource(R.string.pass_detail_pass_artwork),
                        modifier = Modifier.fillMaxSize(),
                        context = PassArtworkContext.HOME_THUMBNAIL,
                    )
                } else {
                    Text(issuer.take(1).uppercase(), color = colors.content, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                issuer,
                color = colors.content,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(typeLabel, color = colors.label, style = MaterialTheme.typography.labelSmall)
        }
        if (layout !is PassTicketLayout.Simple) {
            headerFields.forEach { field ->
                Column(horizontalAlignment = Alignment.End) {
                    FieldLabel(field.label, colors, TextAlign.End)
                    Text(field.value, color = colors.content, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Route(origin: PassFieldUiModel, destination: PassFieldUiModel, colors: TicketColors) {
    // Airport codes and departure times read best large. Station names need the smaller size.
    fun PassFieldUiModel.isShort() = value.length <= ROUTE_CODE_MAX_LENGTH || ROUTE_TIME.matches(value.trim())
    val short = origin.isShort() && destination.isShort()
    val size = if (short) 40.sp else 22.sp
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(origin to TextAlign.Start, destination to TextAlign.End).forEach { (field, align) ->
            Column(Modifier.weight(1f), horizontalAlignment = if (align == TextAlign.End) Alignment.End else Alignment.Start) {
                Text(
                    field.value,
                    color = colors.content,
                    fontSize = size,
                    lineHeight = size * 1.1f,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = align,
                )
                if (field.label.isNotBlank()) {
                    Text(field.label, color = colors.label, style = MaterialTheme.typography.labelMedium, textAlign = align)
                }
            }
        }
    }
}

@Composable
private fun EventMain(pass: PassUiModel, layout: PassTicketLayout.Event, issuer: String, colors: TicketColors) {
    Column {
        FieldLabel(layout.title.label.unlessIssuer(issuer), colors)
        Text(
            layout.title.value,
            color = colors.content,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
    }
    DateCells(pass, colors)
    layout.seat?.fields?.map { it.label to it.value }?.let { cells ->
        // Three cells do not fit in one row on a phone. The section takes its own row; row and seat keep the columns.
        if (cells.size == 3) {
            TonalCells(cells[0], emptyList(), colors)
            TonalCells(cells[1], cells.drop(2), colors)
        } else {
            TonalCells(cells.first(), cells.drop(1), colors)
        }
    }
}

@Composable
private fun DateCells(pass: PassUiModel, colors: TicketColors) {
    val from = pass.calendarTimeSpan?.from ?: return
    val locale = LocalConfiguration.current.locales[0]
    val time = ticketTimeLabel(from, locale)
    TonalCells(
        stringResource(R.string.pass_detail_ticket_date) to ticketDateLabel(from, pass.calendarTimeSpan?.to, locale),
        listOfNotNull(time?.let { stringResource(R.string.pass_detail_ticket_time) to it }),
        colors,
    )
}

/** Every row of the event ticket splits at the same point, so cells and fields line up vertically. */
private val EventColumns = listOf(1.4f, 1f)

@Composable
private fun TonalCells(start: Pair<String, String>, end: List<Pair<String, String>>, colors: TicketColors) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(18.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TonalCell(start, colors, Modifier.weight(EventColumns[0]))
        if (end.isNotEmpty()) {
            Row(Modifier.weight(EventColumns[1]).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                end.forEach { TonalCell(it, colors, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TonalCell(cell: Pair<String, String>, colors: TicketColors, modifier: Modifier) {
    Column(modifier.fillMaxHeight().background(colors.tonal).padding(horizontal = 12.dp, vertical = 9.dp)) {
        FieldLabel(cell.first, colors)
        Text(cell.second, color = colors.content, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun PrimaryValue(field: PassFieldUiModel, issuer: String, colors: TicketColors) {
    val large = field.value.length <= LARGE_VALUE_MAX_LENGTH
    Column {
        FieldLabel(field.label.unlessIssuer(issuer), colors)
        Text(
            field.value,
            color = colors.content,
            style = if (large) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun SimpleTitle(pass: PassUiModel, colors: TicketColors) {
    Text(
        pass.description.ifBlank { pass.creator.orEmpty() },
        color = colors.content,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
    )
}

@Composable
private fun FieldGrid(fields: List<PassFieldUiModel>, colors: TicketColors, columns: List<Float>, columnGap: Dp, cellPadding: Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        fields.gridRows(columns.size).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(columnGap)) {
                row.forEachIndexed { index, field ->
                    val weight = if (row.size == 1) columns.sum() else columns[index]
                    Column(Modifier.weight(weight).padding(horizontal = cellPadding)) {
                        FieldLabel(field.label, colors)
                        Text(field.value, color = colors.content, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (row.size > 1) columns.drop(row.size).forEach { Spacer(Modifier.weight(it)) }
            }
        }
    }
}

@Composable
private fun FieldList(fields: List<PassFieldUiModel>, colors: TicketColors) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        fields.forEach { field ->
            Column {
                if (field.label.isNotBlank()) {
                    Text(field.label, color = colors.label, style = MaterialTheme.typography.labelMedium)
                }
                Text(field.value, color = colors.content, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FieldLabel(label: String, colors: TicketColors, align: TextAlign = TextAlign.Start) {
    if (label.isBlank()) return
    Text(
        label.uppercase(LocalConfiguration.current.locales[0]),
        color = colors.label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        textAlign = align,
    )
}

/**
 * Draws the outline, then cuts the edge notches at [tearLine]. The cut comes after the outline, so the outline
 * follows the notches instead of crossing them.
 */
private fun DrawScope.drawTicketNotches(edgeColor: Color, tearLine: Float) {
    if (tearLine.isNaN()) return
    // A disc one edge width larger than the notch is painted first and the notch is cut out of it. The ring left
    // over has the same width as the outer edge, and the cut never leaves white pixels at its rim.
    val edge = TicketEdgeWidth.toPx()
    val notch = PerforationNotchRadius.toPx()
    listOf(0f, size.width).forEach { x ->
        drawCircle(edgeColor, notch + edge, Offset(x, tearLine))
        drawCircle(Color.Black, notch, Offset(x, tearLine), blendMode = BlendMode.Clear)
    }
}

// The pass color ends at the tear line, like a paper ticket torn at its perforation. A row of holes runs between
// the edge notches. The holes are cut out of the ticket, so the screen shows through.
@Composable
private fun Perforation(passColor: Color, colorOnTop: Boolean, modifier: Modifier) {
    Canvas(modifier.fillMaxWidth().height(PerforationHeight)) {
        val center = size.height / 2f
        val colorTop = if (colorOnTop) 0f else center
        drawRect(passColor, topLeft = Offset(0f, colorTop), size = Size(size.width, center))
        drawRect(Color.White, topLeft = Offset(0f, center - colorTop), size = Size(size.width, center))
        val notch = PerforationNotchRadius.toPx()
        val holeWidth = PerforationHoleWidth.toPx()
        val holeHeight = PerforationHoleHeight.toPx()
        val gap = PerforationHoleGap.toPx()
        // Holes spread evenly between the notches, with one gap of margin on each side.
        val span = size.width - 2 * (notch + gap)
        val holes = ((span + gap) / (holeWidth + gap)).toInt().coerceAtLeast(1)
        val step = if (holes > 1) (span - holeWidth) / (holes - 1) else 0f
        val first = if (holes > 1) notch + gap else (size.width - holeWidth) / 2f
        repeat(holes) { index ->
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(first + index * step, center - holeHeight / 2f),
                size = Size(holeWidth, holeHeight),
                cornerRadius = CornerRadius(holeHeight / 2f),
                blendMode = BlendMode.Clear,
            )
        }
    }
}

private val PerforationHeight = 28.dp
private val PerforationNotchRadius = 14.dp
private val PerforationHoleWidth = 16.dp
private val PerforationHoleHeight = 8.dp
private val PerforationHoleGap = 7.dp
