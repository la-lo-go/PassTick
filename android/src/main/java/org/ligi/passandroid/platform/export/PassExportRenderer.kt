package org.ligi.passandroid.platform.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import org.ligi.passandroid.export.PassExportDocument
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.repository.PassExportTemplate
import org.ligi.passandroid.repository.PassImageAspectRatio
import org.ligi.passandroid.repository.PassImageExportOptions
import org.ligi.passandroid.repository.PassImageOrientation
import java.io.OutputStream
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/** One page of an export in design units. A PDF page uses the units as points. */
internal class ExportPage(val width: Float, val height: Float, val draw: Canvas.() -> Unit)

/** Lays out a document with a template. The same pages draw a bitmap and a PDF. */
internal class PassExportRenderer(
    private val document: PassExportDocument,
    private val template: PassExportTemplate,
    private val options: PassImageExportOptions,
    private val images: ExportImages,
    private val locale: Locale,
) {
    /** The pages of the export. Only a PDF of a sheet has more than one page; a PNG fits everything on one. */
    fun pages(paginate: Boolean): List<ExportPage> = when (template) {
        PassExportTemplate.TICKET -> listOf(ticketPage())
        PassExportTemplate.STORY -> listOf(storyPage())
        PassExportTemplate.SHEET -> sheetPages(paginate)
        PassExportTemplate.CODE -> listOf(codePage())
    }

    private fun ticketPage(): ExportPage {
        // An empty selection still draws a ticket, with the header that names the pass.
        val shown = if (document.hasDetails || document.codes.isNotEmpty()) document else document.copy(showHeader = true)
        val colors = ExportColors.of(document.accentColor, document.neutral, options.monochrome)
        val background = if (options.opaqueBackground) BACKDROP else Color.TRANSPARENT
        if (options.aspectRatio == PassImageAspectRatio.AUTO_HEIGHT) {
            val ticket = TicketPainter(shown, colors, images, locale).ticket(TicketPainter.WIDTH)
            return framed(ticket, TicketPainter.WIDTH, FRAME_MARGIN, background)
        }
        val fit = fittedTicket(shown, colors)
        return framed(fit.build(fit.width, fit.frameHeight), fit.width, FRAME_MARGIN, background)
    }

    /** A ticket layout for a fixed frame: its painter, layout width, and the frame height at that width. */
    private class TicketFit(
        val width: Float,
        val frameHeight: Float,
        val codeWidth: Float,
        /** Below 1 when the content is taller than the frame and the whole ticket must shrink. */
        val shrink: Float,
        val build: (Float, Float?) -> Block,
    ) {
        /**
         * Text and codes both shrink as the layout gets wider, and codes shrink as they share less room. The product of
         * the scales balances them, so neither the text nor the codes end up tiny.
         */
        val score: Float get() {
            val scale = shrink / (width + 2 * FRAME_MARGIN)
            // The text scale counts twice: a pass reads badly with tiny text. The code share keeps the codes scannable.
            return codeWidth * scale * scale * scale
        }

        /** The code width per code against the frame size, the square root of the frame area. */
        val codeShare: Float get() {
            val frameWidth = width + 2 * FRAME_MARGIN
            val frameArea = frameWidth * (frameHeight + 2 * FRAME_MARGIN)
            return codeWidth * shrink / kotlin.math.sqrt(frameArea)
        }
    }

    /**
     * The ticket fills the frame. A wide frame takes the landscape ticket. Otherwise the upright layouts (two codes per
     * row, or all codes in one row) compete. Each layout takes the narrowest width whose content fits the frame height,
     * which keeps its text largest, and then stretches to the frame height. Content that never fits takes the widest
     * layout and shrinks. The layout with the best balance of text and code size wins.
     */
    private fun fittedTicket(shown: PassExportDocument, colors: ExportColors): TicketFit {
        fun frameHeight(width: Float) = (width + 2 * FRAME_MARGIN) * heightFactor() - 2 * FRAME_MARGIN
        fun fit(widths: List<Float>, codeWidth: (Float) -> Float, build: (Float, Float?) -> Block): TicketFit {
            val width = widths.firstOrNull { build(it, null).height <= frameHeight(it) } ?: widths.last()
            val shrink = minOf(1f, frameHeight(width) / build(width, null).height)
            return TicketFit(width, frameHeight(width), codeWidth(width), shrink, build)
        }
        val rows = if (shown.codes.size <= 2) listOf(2) else listOf(2, minOf(shown.codes.size, MAX_CODE_COLUMNS))
        val upright = rows.map { count ->
            val painter = TicketPainter(shown, colors, images, locale, codeColumns = count)
            fit(UPRIGHT_TICKET_WIDTHS, { TicketPainter.uprightCodeWidth(it) / minOf(count, shown.codes.size.coerceAtLeast(1)) }) { width, height ->
                painter.ticket(width, height)
            }
        }
        val painter = TicketPainter(shown, colors, images, locale)
        // A wide frame takes the landscape ticket: an upright ticket stretched across it leaves the code lost in white.
        val landscape = if (!isWideFrame() || painter.wideTicket(TicketPainter.WIDE_WIDTH) == null) {
            null
        } else {
            val perRow = minOf(shown.codes.size, 2)
            fit(WIDE_TICKET_WIDTHS, { TicketPainter.stubWidth(it, frameHeight(it)) / perRow }) { width, height ->
                painter.wideTicket(width, height)!!
            }
        }
        return landscape ?: upright.filter { it.codeShare >= MIN_CODE_SHARE }.ifEmpty { upright }.maxBy { it.score }
    }

    private fun codePage(): ExportPage {
        // A wide frame is wider, so codes side by side keep their size.
        val width = if (isWideFrame()) TicketPainter.WIDE_WIDTH else TicketPainter.WIDTH
        val inner = width - 2 * (CODE_MARGIN + CODE_PADDING)
        val auto = options.aspectRatio == PassImageAspectRatio.AUTO_HEIGHT
        val fixedHeight = width * heightFactor()
        fun content(columns: Int) = column(
            buildList {
                if (document.codes.isNotEmpty()) add(codeGrid(document.codes, inner, maxQuadratic = TicketPainter.WIDTH, CODE_GAP, columns))
                if (document.showHeader) addAll(caption(inner))
            },
            CODE_GAP,
        )
        // A fixed frame takes the column count that leaves the largest codes after the content shrinks to fit.
        val available = fixedHeight - 2 * (CODE_MARGIN + CODE_PADDING)
        val content = if (auto) {
            content(2)
        } else {
            (1..document.codes.size.coerceIn(1, MAX_CODE_COLUMNS)).map(::content).withIndex().maxBy { (index, block) ->
                minOf(1f, available / block.height) / (index + 1)
            }.value
        }
        val height = if (auto) content.height + 2 * (CODE_MARGIN + CODE_PADDING) else fixedHeight
        return ExportPage(width, height) {
            if (options.opaqueBackground) drawColor(Color.WHITE)
            val card = RectF(CODE_MARGIN, CODE_MARGIN, width - CODE_MARGIN, height - CODE_MARGIN)
            drawRoundRect(card, CODE_CORNER, CODE_CORNER, fill(Color.WHITE))
            drawFitted(content, inner, card.left + CODE_PADDING, card.top + CODE_PADDING, inner, card.height() - 2 * CODE_PADDING)
        }
    }

    private fun caption(width: Float): List<Block> = listOfNotNull(
        document.description.ifBlank { document.issuer }.takeIf(String::isNotBlank)?.let {
            text(it, width, CAPTION_SIZE, ExportColors.INK, Weight.BOLD, Layout.Alignment.ALIGN_CENTER)
        },
        document.dateCells.takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.value }?.let {
            text(it, width, CAPTION_DETAIL_SIZE, MUTED, Weight.SEMI_BOLD, Layout.Alignment.ALIGN_CENTER)
        },
    )

    private fun storyPage(): ExportPage {
        val width = TicketPainter.WIDTH
        val height = width * heightFactor()
        val colors = ExportColors.of(document.accentColor, neutral = false, monochrome = options.monochrome)
        val inner = width - 2 * STORY_PADDING
        val top = TicketPainter(
            document.copy(banner = null, headerFields = emptyList(), seatCells = emptyList(), grid = emptyList(), backFields = emptyList(), notes = null, locations = emptyList()),
            colors,
            images,
            locale,
        ).detailsBlock(inner)
        val painter = TicketPainter(document, colors, images, locale)
        val codeWidth = inner - 2 * STORY_CARD_PADDING
        val codes = codeGrid(document.codes, codeWidth, maxQuadratic = codeWidth, CODE_GAP)
        val card = codes.takeIf { document.codes.isNotEmpty() }?.let { content ->
            Block(content.height + 2 * STORY_CARD_PADDING) { left, cardTop ->
                drawRoundRect(left, cardTop, left + inner, cardTop + content.height + 2 * STORY_CARD_PADDING, STORY_CARD_CORNER, STORY_CARD_CORNER, fill(Color.WHITE))
                content.draw(this, left + STORY_CARD_PADDING, cardTop + STORY_CARD_PADDING)
            }
        }
        val bottom = column(
            listOfNotNull(
                document.keyFacts.takeIf { it.isNotEmpty() }?.let { painter.equalCells(it, inner) },
                document.locations.firstOrNull()?.let { text(it.name, inner, STORY_LOCATION_SIZE, colors.content, Weight.SEMI_BOLD, Layout.Alignment.ALIGN_CENTER) },
                text(document.labels.footer, inner, LABEL_SIZE, colors.label, Weight.SEMI_BOLD, Layout.Alignment.ALIGN_CENTER),
            ),
            STORY_GAP,
        )
        return ExportPage(width, height) {
            drawColor(colors.container)
            val available = height - 2 * STORY_PADDING
            val natural = listOfNotNull(top, card, bottom).totalHeight(STORY_GAP)
            // Long content shrinks to fit. Short content keeps the code in the middle and the facts at the bottom.
            if (natural > available) {
                drawFitted(column(listOfNotNull(top, card, bottom), STORY_GAP), inner, STORY_PADDING, STORY_PADDING, inner, available)
                return@ExportPage
            }
            top.draw(this, STORY_PADDING, STORY_PADDING)
            bottom.draw(this, STORY_PADDING, height - STORY_PADDING - bottom.height)
            card?.let {
                val space = available - top.height - bottom.height
                it.draw(this, STORY_PADDING, STORY_PADDING + top.height + (space - it.height) / 2f)
            }
        }
    }

    private fun sheetPages(paginate: Boolean): List<ExportPage> {
        val (pageWidth, pageHeight) = sheetSize()
        val inner = pageWidth - 2 * SHEET_MARGIN
        val available = pageHeight - 2 * SHEET_MARGIN - SHEET_FOOTER
        val blocks = SheetLayout(document, options.monochrome, images, locale).blocks(inner)
        // Content that fits one page after a small shrink stays on one page. A second page with one short section is worse.
        val fitsOnePage = blocks.totalHeight(SHEET_GAP) * MIN_SHEET_SCALE <= available
        val pages = if (paginate && !fitsOnePage) paginate(blocks, available) else listOf(blocks)
        return pages.mapIndexed { index, pageBlocks ->
            ExportPage(pageWidth, pageHeight) {
                drawColor(Color.WHITE)
                drawFitted(column(pageBlocks, SHEET_GAP), inner, SHEET_MARGIN, SHEET_MARGIN, inner, available, center = false)
                val footer = if (pages.size > 1) "${document.labels.footer} · ${index + 1}/${pages.size}" else document.labels.footer
                text(footer, inner, SHEET_FOOTER_SIZE, MUTED).draw(this, SHEET_MARGIN, pageHeight - SHEET_MARGIN - SHEET_FOOTER_SIZE)
            }
        }
    }

    /** Splits blocks into pages at block boundaries. A block taller than a page gets a page of its own and shrinks. */
    private fun paginate(blocks: List<Block>, available: Float): List<List<Block>> {
        val pages = mutableListOf(mutableListOf<Block>())
        var used = 0f
        blocks.forEach { block ->
            val needed = if (pages.last().isEmpty()) block.height else used + SHEET_GAP + block.height
            if (needed > available && pages.last().isNotEmpty()) {
                pages += mutableListOf(block)
                used = block.height
            } else {
                pages.last() += block
                used = needed
            }
        }
        return pages
    }

    private fun sheetSize(): Pair<Float, Float> {
        val (short, long) = if (options.aspectRatio == PassImageAspectRatio.LETTER) LETTER_SIZE else A4_SIZE
        return if (options.orientation == PassImageOrientation.LANDSCAPE) long to short else short to long
    }

    /** A block centered in a frame, or in a frame of the selected ratio scaled to fit. */
    private fun framed(block: Block, blockWidth: Float, margin: Float, background: Int): ExportPage {
        val width = blockWidth + 2 * margin
        val auto = options.aspectRatio == PassImageAspectRatio.AUTO_HEIGHT
        val height = if (auto) block.height + 2 * margin else width * heightFactor()
        return ExportPage(width, height) {
            if (background != Color.TRANSPARENT) drawColor(background)
            drawFitted(block, blockWidth, margin, margin, blockWidth, height - 2 * margin)
        }
    }

    private fun isWideFrame(): Boolean = options.aspectRatio != PassImageAspectRatio.AUTO_HEIGHT && heightFactor() < 1f

    // Orientation selects the long dimension, so a wide ratio becomes tall in portrait and stays wide in landscape.
    private fun heightFactor(): Float {
        val ratio = options.aspectRatio
        val value = ratio.ratioWidth / ratio.ratioHeight
        val portrait = template == PassExportTemplate.STORY || options.orientation == PassImageOrientation.PORTRAIT
        return if (portrait) maxOf(value, 1f / value) else minOf(value, 1f / value)
    }

    companion object {
        private const val BACKDROP = 0xFFF2F2F5.toInt()
        private const val MUTED = 0xFF5F5E66.toInt()
        private const val FRAME_MARGIN = 12f
        // Below this share of the frame size, codes in a long row get too small to scan from a screen.
        private const val MIN_CODE_SHARE = 0.17f
        private val UPRIGHT_TICKET_WIDTHS = (280..840 step 40).map(Int::toFloat)
        // The landscape ticket starts at its usual width, so the details column never breaks words.
        private val WIDE_TICKET_WIDTHS = (640..1240 step 60).map(Int::toFloat)
        private const val CODE_MARGIN = 5.4f
        private const val CODE_PADDING = 7.2f
        private const val CODE_CORNER = 14.4f
        private const val CODE_GAP = 12f
        private const val MAX_CODE_COLUMNS = 4
        private const val CAPTION_SIZE = 16f
        private const val CAPTION_DETAIL_SIZE = 13f
        private const val STORY_PADDING = 24f
        private const val STORY_GAP = 20f
        private const val STORY_CARD_PADDING = 16f
        private const val STORY_CARD_CORNER = 24f
        private const val STORY_LOCATION_SIZE = 14f
        private const val SHEET_MARGIN = 40f
        private const val SHEET_GAP = 16f
        private const val SHEET_FOOTER = 24f
        private const val SHEET_FOOTER_SIZE = 9f
        private const val MIN_SHEET_SCALE = 0.8f
        private val A4_SIZE = 595f to 842f
        private val LETTER_SIZE = 612f to 792f
    }
}

/**
 * Draws [block] inside the frame, scaled down when it does not fit. Content never grows, so text keeps its size
 * in an Auto frame. [center] centers the block vertically.
 */
internal fun Canvas.drawFitted(block: Block, blockWidth: Float, left: Float, top: Float, width: Float, height: Float, center: Boolean = true) {
    if (block.height <= 0f) return
    val scale = minOf(width / blockWidth, height / block.height, 1f)
    val x = left + (width - blockWidth * scale) / 2f
    val y = if (center) top + (height - block.height * scale) / 2f else top
    withTranslation(x, y) { withScale(scale, scale) { block.draw(this, 0f, 0f) } }
}

/** Ink on paper: the sheet sections below the cut line. */
private class SheetLayout(
    private val document: PassExportDocument,
    private val monochrome: Boolean,
    private val images: ExportImages,
    private val locale: Locale,
) {

    fun blocks(width: Float): List<Block> = buildList {
        addAll(ticketBlocks(width))
        if (document.summary.isNotEmpty()) add(withHeading(document.typeLabel, table(document.summary, width), width))
        if (document.locations.isNotEmpty()) add(locations(width))
        document.notes?.let { addAll(sectionBlocks(document.labels.notes, notesParagraphs(it, width), width)) }
        addAll(sectionBlocks(document.labels.moreInformation, document.backFields.map { table(listOf(it), width) }, width))
    }

    /** The ticket and the cut line. Locations, notes, and back fields move below the cut line. */
    private fun ticketBlocks(width: Float): List<Block> {
        val ticketDocument = document.copy(backFields = emptyList(), notes = null, locations = emptyList())
        if (!ticketDocument.hasDetails && document.codes.isEmpty()) return emptyList()
        val colors = ExportColors.of(document.accentColor, document.neutral, monochrome)
        val painter = TicketPainter(ticketDocument, colors, images, locale, SHEET_CODE_MAX_HEIGHT, SHEET_CODE_COLUMNS)
        // A landscape page has room for the landscape ticket.
        val wide = if (width >= TicketPainter.WIDE_WIDTH) painter.wideTicket(TicketPainter.WIDE_WIDTH) else null
        val ticketWidth = if (wide != null) TicketPainter.WIDE_WIDTH else minOf(TicketPainter.WIDTH, width)
        val ticket = wide ?: painter.ticket(ticketWidth)
        return listOf(Block(ticket.height) { left, top -> ticket.draw(this, left + (width - ticketWidth) / 2f, top) }, cutLine(width))
    }

    private fun locations(width: Float): Block {
        val lines = document.locations.map { location ->
            if (location.name == location.coordinates) location.name else "${location.name}\n${location.coordinates}"
        }
        return withHeading(document.labels.location, column(lines.map { text(it, width, BODY_SIZE, ExportColors.INK) }, 6f), width)
    }

    // Each paragraph is its own block, so long notes continue on the next page.
    private fun notesParagraphs(notes: String, width: Float): List<Block> =
        notes.split(Regex("\\n\\s*\\n")).map { text(it.trim(), width, BODY_SIZE, ExportColors.INK) }

    /** A section of separate blocks, so it can continue on the next page. The heading joins the first block. */
    private fun sectionBlocks(title: String, blocks: List<Block>, width: Float): List<Block> =
        if (blocks.isEmpty()) emptyList() else listOf(withHeading(title, blocks.first(), width)) + blocks.drop(1)

    /** Label and value rows: the label column on the left, the value wraps in the rest of the width. */
    private fun table(fields: List<PassFieldUiModel>, width: Float) = column(
        fields.map { field ->
            val label = text(field.label, TABLE_LABEL_WIDTH, BODY_SIZE, MUTED_INK, Weight.SEMI_BOLD)
            val value = text(field.value, width - TABLE_LABEL_WIDTH - TABLE_GAP, BODY_SIZE, ExportColors.INK)
            Block(maxOf(label.height, value.height)) { left, top ->
                label.draw(this, left, top)
                value.draw(this, left + TABLE_LABEL_WIDTH + TABLE_GAP, top)
            }
        },
        TABLE_ROW_GAP,
    )

    // The heading stays with the first block of its section, so a page never ends with a lone heading.
    private fun withHeading(title: String, first: Block, width: Float) = column(
        listOf(text(title, width, HEADING_SIZE, ExportColors.INK, Weight.BOLD), first),
        HEADING_GAP,
    )

    private fun cutLine(width: Float) = Block(CUT_HEIGHT) { left, top ->
        val y = top + CUT_HEIGHT / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ExportColors.OUTLINE
            strokeWidth = 1f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
        }
        drawLine(left + SCISSORS_WIDTH, y, left + width, y, paint)
        val scissors = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8A8990.toInt()
            textSize = SCISSORS_SIZE
        }
        drawText("✂", left, y - (scissors.descent() + scissors.ascent()) / 2f, scissors)
    }

    companion object {
        // About six centimeters: large enough for any scanner, small enough to leave room for the details.
        private const val SHEET_CODE_MAX_HEIGHT = 170f
        private const val SHEET_CODE_COLUMNS = 4
        private const val TABLE_LABEL_WIDTH = 120f
        private const val TABLE_GAP = 12f
        private const val TABLE_ROW_GAP = 5f
        private const val MUTED_INK = 0xFF5F5E66.toInt()
        private const val BODY_SIZE = 11f
        private const val HEADING_SIZE = 13f
        private const val HEADING_GAP = 8f
        private const val CUT_HEIGHT = 20f
        private const val SCISSORS_WIDTH = 18f
        private const val SCISSORS_SIZE = 12f
    }
}

/** Draws a page into a bitmap [widthPx] wide. */
internal fun ExportPage.toBitmap(widthPx: Int): Bitmap {
    require(widthPx > 0) { "Width must be positive" }
    val scale = widthPx / width
    val bitmap = createBitmap(widthPx, ceil(height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.TRANSPARENT)
    Canvas(bitmap).apply { scale(scale, scale) }.draw()
    return bitmap
}

/** Writes the pages as a vector PDF. One unit is one point. */
internal fun List<ExportPage>.writePdf(output: OutputStream) {
    val pdf = PdfDocument()
    try {
        forEachIndexed { index, exportPage ->
            val info = PdfDocument.PageInfo.Builder(exportPage.width.roundToInt(), exportPage.height.roundToInt(), index + 1).create()
            val page = pdf.startPage(info)
            exportPage.draw(page.canvas)
            pdf.finishPage(page)
        }
        pdf.writeTo(output)
    } finally {
        pdf.close()
    }
}
