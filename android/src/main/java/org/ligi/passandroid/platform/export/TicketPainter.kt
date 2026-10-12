package org.ligi.passandroid.platform.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import org.ligi.passandroid.export.ExportMain
import org.ligi.passandroid.export.PassExportDocument
import org.ligi.passandroid.ui.state.DisplayableCode
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.gridRows
import java.util.Locale

/** The pass view ticket drawn on a Canvas. Sizes match the Compose ticket in dp, so one unit is one dp. */
internal class TicketPainter(
    private val document: PassExportDocument,
    private val colors: ExportColors,
    private val images: ExportImages,
    private val locale: Locale,
    /** The largest square code. The sheet uses a smaller code, so the ticket and its details fit one page. */
    private val codeMaxHeight: Float = CODE_MAX_HEIGHT,
) {
    fun ticket(width: Float): Block {
        val inner = width - 2 * EDGE
        val parts = listOfNotNull(detailsPart(inner), codePart(inner)).let { if (document.codeOnTop) it.reversed() else it }
        val perforation = if (parts.size == 2) PERFORATION_HEIGHT else 0f
        val height = 2 * EDGE + parts.sumOf { it.height.toDouble() }.toFloat() + perforation
        val tearLine = if (parts.size == 2) EDGE + parts[0].height + perforation / 2f else null
        return Block(height) { left, top ->
            withTranslation(left, top) { drawTicket(width, height, parts, tearLine, perforation) }
        }
    }

    /**
     * A landscape ticket: the details on one side and the code stub on the other, with an upright perforation.
     * Null when the ticket has only one part, because a single part has no stub to separate.
     */
    fun wideTicket(width: Float): Block? {
        if (!document.hasDetails || document.codes.isEmpty()) return null
        val detailsWidth = width - 2 * EDGE - PERFORATION_HEIGHT - STUB_WIDTH
        val details = detailsBlock(detailsWidth - 2 * PADDING)
        val codes = codeArea(document.codes, STUB_WIDTH)
        val innerHeight = maxOf(details.height + PADDING_TOP + PADDING, codes.height)
        val height = innerHeight + 2 * EDGE
        // The code stub follows the pass view order: first means on the left.
        val detailsLeft = if (document.codeOnTop) EDGE + STUB_WIDTH + PERFORATION_HEIGHT else EDGE
        val codesLeft = if (document.codeOnTop) EDGE else EDGE + detailsWidth + PERFORATION_HEIGHT
        val tear = EDGE + (if (document.codeOnTop) STUB_WIDTH else detailsWidth) + PERFORATION_HEIGHT / 2f
        return Block(height) { left, top ->
            withTranslation(left, top) {
                // The shape is built for an upright ticket and mirrored across the diagonal, so the tear line stands up.
                val holes = perforationHoles(height, tear)
                drawPath(shape(RectF(0f, 0f, height, width), CORNER, tear, NOTCH, holes).transposed(), fill(colors.edge))
                withClip(shape(RectF(EDGE, EDGE, height - EDGE, width - EDGE), CORNER - EDGE, tear, NOTCH + EDGE, holes).transposed()) {
                    // Each color reaches the tear line, like a paper ticket torn at its perforation.
                    val (leftColor, rightColor) = if (document.codeOnTop) Color.WHITE to colors.container else colors.container to Color.WHITE
                    drawRect(EDGE, EDGE, tear, height - EDGE, fill(leftColor))
                    drawRect(tear, EDGE, width - EDGE, height - EDGE, fill(rightColor))
                    details.draw(this, detailsLeft + PADDING, EDGE + PADDING_TOP)
                    codes.draw(this, codesLeft, EDGE + (innerHeight - codes.height) / 2f)
                }
            }
        }
    }

    /** One side of the perforation: its background color and its content. */
    private class TicketPart(val height: Float, val color: Int, val draw: Canvas.(top: Float) -> Unit)

    private fun detailsPart(width: Float): TicketPart? {
        if (!document.hasDetails) return null
        val details = detailsBlock(width - 2 * PADDING)
        return TicketPart(details.height + PADDING_TOP + PADDING, colors.container) { top -> details.draw(this, EDGE + PADDING, top + PADDING_TOP) }
    }

    private fun codePart(width: Float): TicketPart? {
        val codes = document.codes.takeIf { it.isNotEmpty() }?.let { codeArea(it, width) } ?: return null
        return TicketPart(codes.height, Color.WHITE) { top -> codes.draw(this, EDGE, top) }
    }

    private fun Canvas.drawTicket(width: Float, height: Float, parts: List<TicketPart>, tearLine: Float?, perforation: Float) {
        val holes = tearLine?.let { perforationHoles(width, it) } ?: Path()
        drawPath(shape(RectF(0f, 0f, width, height), CORNER, tearLine, NOTCH, holes), fill(colors.edge))
        withClip(shape(RectF(EDGE, EDGE, width - EDGE, height - EDGE), CORNER - EDGE, tearLine, NOTCH + EDGE, holes)) {
            var y = EDGE
            parts.forEachIndexed { index, part ->
                // Each color reaches the tear line, like a paper ticket torn at its perforation.
                val colorTop = if (index > 0) y - perforation / 2f else y
                val colorBottom = if (index < parts.lastIndex) y + part.height + perforation / 2f else y + part.height
                drawRect(EDGE, colorTop, width - EDGE, colorBottom, fill(part.color))
                part.draw(this, y)
                y += part.height + perforation
            }
        }
    }

    /** The colored part: everything except the codes. */
    fun detailsBlock(width: Float): Block = column(
        buildList {
            images[document.banner]?.let { add(imageBlock(it, width, BANNER_MAX_HEIGHT, BANNER_CORNER)) }
            if (document.showHeader) add(header(width))
            document.main?.let { add(main(it, width)) }
            document.progress?.let { add(progress(it, width)) }
            if (document.dateCells.isNotEmpty()) add(tonalCells(document.dateCells.first(), document.dateCells.drop(1), width))
            seatRows(width)?.let(::add)
            if (document.grid.isNotEmpty()) add(grid(document.grid, document.gridColumns, width))
            addAll(extraSections(width))
        },
        SECTION_GAP,
    )

    /** Locations, notes, and back fields. The sheet draws them below the cut line instead. */
    fun extraSections(width: Float): List<Block> = buildList {
        if (document.locations.isNotEmpty()) {
            add(section(document.labels.location, document.locations.map { it.name }, width))
        }
        document.notes?.let { add(section(document.labels.notes, listOf(it), width)) }
        if (document.backFields.isNotEmpty()) {
            add(column(listOfNotNull(label(document.labels.moreInformation, width, colors.label, locale), fieldList(document.backFields, width)), 6f))
        }
    }

    private fun header(width: Float): Block {
        val fieldBlocks = document.headerFields.map { field ->
            // Rounding up keeps a measured line from wrapping when the layout width is truncated to whole units.
            val fieldWidth = kotlin.math.ceil(
                maxOf(
                    textWidth(field.value, HEADER_VALUE_SIZE, Weight.BOLD),
                    textWidth(field.label.uppercase(locale), LABEL_SIZE, Weight.SEMI_BOLD, LABEL_LETTER_SPACING),
                ),
            ) + 2f
            fieldWidth to column(
                listOfNotNull(
                    label(field.label, fieldWidth, colors.label, locale, Layout.Alignment.ALIGN_OPPOSITE),
                    text(field.value, fieldWidth, HEADER_VALUE_SIZE, colors.content, Weight.BOLD, Layout.Alignment.ALIGN_OPPOSITE),
                ),
                0f,
            )
        }
        val iconSize = if (document.neutral) DOT_SIZE else ICON_SIZE
        val titleWidth = (width - iconSize - HEADER_GAP - fieldBlocks.sumOf { (it.first + HEADER_GAP).toDouble() }.toFloat()).coerceAtLeast(1f)
        val title = column(
            listOf(
                text(document.issuer, titleWidth, ISSUER_SIZE, colors.content, Weight.BOLD, maxLines = 1),
                text(document.typeLabel, titleWidth, LABEL_SIZE, colors.label, Weight.SEMI_BOLD, maxLines = 1),
            ),
            0f,
        )
        val height = maxOf(iconSize, title.height, fieldBlocks.maxOfOrNull { it.second.height } ?: 0f)
        return Block(height) { left, top ->
            drawIcon(left, top + (height - iconSize) / 2f, iconSize)
            title.draw(this, left + iconSize + HEADER_GAP, top + (height - title.height) / 2f)
            var x = left + width
            fieldBlocks.reversed().forEach { (fieldWidth, block) ->
                x -= fieldWidth
                block.draw(this, x, top + (height - block.height) / 2f)
                x -= HEADER_GAP
            }
        }
    }

    private fun Canvas.drawIcon(left: Float, top: Float, size: Float) {
        val bounds = RectF(left, top, left + size, top + size)
        if (document.neutral) {
            drawOval(bounds, fill(document.accentColor or 0xFF000000.toInt()))
            return
        }
        drawRoundRect(bounds, ICON_CORNER, ICON_CORNER, fill(colors.tonal))
        val icon = images[document.icon]
        if (icon != null) {
            drawCropped(icon, bounds, ICON_CORNER)
        } else {
            val initial = document.issuer.take(1).uppercase(locale)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
                color = colors.content
                textSize = INITIAL_SIZE
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, Weight.EXTRA_BOLD.value, false)
                textAlign = Paint.Align.CENTER
            }
            drawText(initial, bounds.centerX(), bounds.centerY() - (paint.descent() + paint.ascent()) / 2f, paint)
        }
    }

    private fun main(main: ExportMain, width: Float): Block = when (main) {
        is ExportMain.Route -> route(main.origin, main.destination, width)
        is ExportMain.Value -> column(
            listOfNotNull(
                label(main.label, width, colors.label, locale),
                text(main.value, width, if (main.large) LARGE_VALUE_SIZE else VALUE_SIZE, colors.content, Weight.EXTRA_BOLD),
            ),
            2f,
        )
    }

    private fun route(origin: PassFieldUiModel, destination: PassFieldUiModel, width: Float): Block {
        // Airport codes and departure times read best large. Station names need the smaller size.
        fun PassFieldUiModel.isShort() = value.length <= ROUTE_CODE_MAX_LENGTH || ROUTE_TIME.matches(value.trim())
        val size = if (origin.isShort() && destination.isShort()) ROUTE_LARGE_SIZE else ROUTE_SMALL_SIZE
        val columnWidth = (width - ROUTE_GAP) / 2f
        val ends = listOf(origin to Layout.Alignment.ALIGN_NORMAL, destination to Layout.Alignment.ALIGN_OPPOSITE).map { (field, align) ->
            column(
                listOfNotNull(
                    text(field.value, columnWidth, size, colors.content, Weight.EXTRA_BOLD, align),
                    field.label.takeIf(String::isNotBlank)?.let { text(it, columnWidth, ROUTE_LABEL_SIZE, colors.label, Weight.SEMI_BOLD, align) },
                ),
                0f,
            )
        }
        return Block(ends.maxOf { it.height }) { left, top ->
            ends[0].draw(this, left, top)
            ends[1].draw(this, left + columnWidth + ROUTE_GAP, top)
        }
    }

    private fun progress(fraction: Float, width: Float) = Block(PROGRESS_HEIGHT) { left, top ->
        val radius = PROGRESS_HEIGHT / 2f
        drawRoundRect(left, top, left + width, top + PROGRESS_HEIGHT, radius, radius, fill(colors.tonal))
        drawRoundRect(left, top, left + width * fraction.coerceIn(0f, 1f), top + PROGRESS_HEIGHT, radius, radius, fill(colors.content))
    }

    private fun seatRows(width: Float): Block? {
        val cells = document.seatCells.takeIf { it.isNotEmpty() } ?: return null
        // Three cells do not fit in one row. The section takes its own row; row and seat keep the columns.
        val rows = if (cells.size == 3) listOf(cells.take(1), cells.drop(1)) else listOf(cells)
        return column(rows.map { tonalCells(it.first(), it.drop(1), width) }, CELL_GAP)
    }

    /** One row of tonal cells that share the width equally. */
    fun equalCells(fields: List<PassFieldUiModel>, width: Float): Block {
        val cellWidth = (width - CELL_GAP * (fields.size - 1)) / fields.size
        return cellRow(fields.map { cellWidth to tonalCell(it, cellWidth) }, width)
    }

    /** Every row splits at the same point, so cells and fields line up vertically. */
    private fun tonalCells(start: PassFieldUiModel, end: List<PassFieldUiModel>, width: Float): Block {
        val startWidth = if (end.isEmpty()) width else (width - CELL_GAP) * CELL_SPLIT
        val endWidth = if (end.isEmpty()) 0f else (width - CELL_GAP - startWidth - CELL_GAP * (end.size - 1)) / end.size
        return cellRow(listOf(startWidth to tonalCell(start, startWidth)) + end.map { endWidth to tonalCell(it, endWidth) }, width)
    }

    private fun cellRow(cells: List<Pair<Float, Block>>, width: Float): Block {
        val height = cells.maxOf { it.second.height } + 2 * CELL_PADDING_VERTICAL
        return Block(height) { left, top ->
            val group = Path().apply { addRoundRect(RectF(left, top, left + width, top + height), CELL_CORNER, CELL_CORNER, Path.Direction.CW) }
            withClip(group) {
                var x = left
                cells.forEach { (cellWidth, content) ->
                    drawRect(x, top, x + cellWidth, top + height, fill(colors.tonal))
                    content.draw(this, x + CELL_PADDING_HORIZONTAL, top + CELL_PADDING_VERTICAL)
                    x += cellWidth + CELL_GAP
                }
            }
        }
    }

    private fun tonalCell(field: PassFieldUiModel, width: Float): Block {
        val contentWidth = width - 2 * CELL_PADDING_HORIZONTAL
        return column(
            listOfNotNull(
                label(field.label, contentWidth, colors.label, locale),
                text(field.value, contentWidth, CELL_VALUE_SIZE, colors.content, Weight.EXTRA_BOLD),
            ),
            0f,
        )
    }

    fun grid(fields: List<PassFieldUiModel>, columns: List<Float>, width: Float): Block {
        if (columns.size == 1) return fieldList(fields, width)
        val rows = fields.gridRows(columns.size).map { row ->
            val available = width - GRID_GAP * (columns.size - 1)
            var x = 0f
            val placed = row.mapIndexed { index, field ->
                val cellWidth = if (row.size == 1) width else available * columns[index] / columns.sum()
                val cell = gridCell(field, cellWidth) to x
                x += cellWidth + GRID_GAP
                cell
            }
            Block(placed.maxOf { it.first.height }) { left, top -> placed.forEach { (cell, offset) -> cell.draw(this, left + offset, top) } }
        }
        return column(rows, GRID_ROW_GAP)
    }

    private fun gridCell(field: PassFieldUiModel, width: Float) = column(
        listOfNotNull(
            label(field.label, width, colors.label, locale),
            text(field.value, width, FIELD_VALUE_SIZE, colors.content, Weight.SEMI_BOLD),
        ),
        0f,
    )

    fun fieldList(fields: List<PassFieldUiModel>, width: Float) = column(
        fields.map { field ->
            column(
                listOfNotNull(
                    field.label.takeIf(String::isNotBlank)?.let { text(it, width, LIST_LABEL_SIZE, colors.label, Weight.SEMI_BOLD) },
                    text(field.value, width, FIELD_VALUE_SIZE, colors.content, Weight.SEMI_BOLD),
                ),
                0f,
            )
        },
        LIST_GAP,
    )

    private fun section(title: String, lines: List<String>, width: Float) = column(
        listOfNotNull(label(title, width, colors.label, locale)) +
            lines.map { text(it, width, BODY_SIZE, colors.content) },
        4f,
    )

    private fun codeArea(codes: List<DisplayableCode>, width: Float): Block {
        val codeWidth = width - 2 * CODE_PADDING
        val content = codeGrid(codes, codeWidth, codeMaxHeight, CODE_GAP)
        return Block(content.height + 2 * CODE_PADDING_VERTICAL) { left, top ->
            content.draw(this, left + CODE_PADDING, top + CODE_PADDING_VERTICAL)
        }
    }

    companion object {
        const val WIDTH = 360f
        private const val EDGE = 2f
        private const val CORNER = 28f
        private const val PADDING = 16f
        private const val PADDING_TOP = 14f
        private const val SECTION_GAP = 16f
        private const val BANNER_MAX_HEIGHT = 120f
        private const val BANNER_CORNER = 12f
        private const val ICON_SIZE = 36f
        private const val DOT_SIZE = 12f
        private const val ICON_CORNER = 11f
        private const val INITIAL_SIZE = 16f
        private const val HEADER_GAP = 10f
        private const val ISSUER_SIZE = 14f
        private const val HEADER_VALUE_SIZE = 16f
        private const val VALUE_SIZE = 24f
        private const val LARGE_VALUE_SIZE = 45f
        private const val ROUTE_LARGE_SIZE = 40f
        private const val ROUTE_SMALL_SIZE = 22f
        private const val ROUTE_LABEL_SIZE = 12f
        private const val ROUTE_GAP = 16f
        private const val ROUTE_CODE_MAX_LENGTH = 4
        private val ROUTE_TIME = Regex("\\d{1,2}:\\d{2}")
        private const val PROGRESS_HEIGHT = 4f
        private const val CELL_GAP = 2f
        private const val CELL_SPLIT = 1.4f / 2.4f
        private const val CELL_CORNER = 18f
        private const val CELL_PADDING_HORIZONTAL = 12f
        private const val CELL_PADDING_VERTICAL = 9f
        private const val CELL_VALUE_SIZE = 22f
        private const val GRID_GAP = 12f
        private const val GRID_ROW_GAP = 12f
        private const val FIELD_VALUE_SIZE = 16f
        private const val LIST_LABEL_SIZE = 12f
        private const val LIST_GAP = 10f
        private const val BODY_SIZE = 14f
        private const val PERFORATION_HEIGHT = 28f
        private const val NOTCH = 14f
        private const val HOLE_WIDTH = 16f
        private const val HOLE_HEIGHT = 8f
        private const val HOLE_GAP = 7f
        private const val CODE_PADDING = 16f
        private const val CODE_PADDING_VERTICAL = 14f
        private const val CODE_GAP = 16f
        private const val CODE_MAX_HEIGHT = 264f
        const val WIDE_WIDTH = 640f
        private const val STUB_WIDTH = 250f
        private val TRANSPOSE = android.graphics.Matrix().apply { setValues(floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)) }

        private fun Path.transposed(): Path = apply { transform(TRANSPOSE) }

        /** A rounded rectangle with notches at the tear line and perforation holes cut out. */
        private fun shape(bounds: RectF, radius: Float, tearLine: Float?, notch: Float, holes: Path): Path {
            val path = Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) }
            if (tearLine == null) return path
            val cuts = Path(holes).apply {
                addCircle(0f, tearLine, notch, Path.Direction.CW)
                addCircle(bounds.right + bounds.left, tearLine, notch, Path.Direction.CW)
            }
            path.op(cuts, Path.Op.DIFFERENCE)
            return path
        }

        // Holes spread evenly between the notches, with one gap of margin on each side.
        private fun perforationHoles(width: Float, center: Float): Path {
            val span = width - 2 * (NOTCH + HOLE_GAP)
            val count = ((span + HOLE_GAP) / (HOLE_WIDTH + HOLE_GAP)).toInt().coerceAtLeast(1)
            val step = if (count > 1) (span - HOLE_WIDTH) / (count - 1) else 0f
            val first = if (count > 1) NOTCH + HOLE_GAP else (width - HOLE_WIDTH) / 2f
            return Path().apply {
                repeat(count) { index ->
                    val x = first + index * step
                    addRoundRect(RectF(x, center - HOLE_HEIGHT / 2f, x + HOLE_WIDTH, center + HOLE_HEIGHT / 2f), HOLE_HEIGHT / 2f, HOLE_HEIGHT / 2f, Path.Direction.CW)
                }
            }
        }
    }
}

private const val ALT_TEXT_SIZE = 13f
private const val ALT_TEXT_GAP = 6f

/**
 * All codes of a pass. Several square codes share a grid of up to [maxColumns] columns, so a pass with many codes stays compact.
 * Linear and wide codes need the full width to stay readable.
 */
internal fun codeGrid(codes: List<DisplayableCode>, width: Float, maxQuadratic: Float, gap: Float, maxColumns: Int = 2): Block {
    val grid = codes.size > 1 && codes.all { it.format.isQuadratic() }
    if (!grid) return column(codes.mapNotNull { codeWithText(it, width, maxQuadratic) }, gap)
    val columns = minOf(maxColumns, codes.size)
    val cellWidth = (width - gap * (columns - 1)) / columns
    val rows = codes.mapNotNull { codeWithText(it, cellWidth, maxQuadratic) }.chunked(columns).map { row ->
        Block(row.maxOf { it.height }) { left, top ->
            // A short last row stays centered.
            val start = left + (width - row.size * cellWidth - (row.size - 1) * gap) / 2f
            row.forEachIndexed { index, cell -> cell.draw(this, start + index * (cellWidth + gap), top) }
        }
    }
    return column(rows, gap)
}

/** A code with its alternative text below, centered in [width]. */
internal fun codeWithText(code: DisplayableCode, width: Float, maxQuadratic: Float, textColor: Int = 0xFF46464F.toInt()): Block? {
    val image = codeBlock(code.format, code.message, width, codeHeight(code.format, width, maxQuadratic)) ?: return null
    val caption = code.alternativeText?.let { text(it, width, ALT_TEXT_SIZE, textColor, Weight.SEMI_BOLD, Layout.Alignment.ALIGN_CENTER) }
    return column(listOfNotNull(image, caption), ALT_TEXT_GAP)
}
