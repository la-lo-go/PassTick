package org.ligi.passandroid.export

import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassExportContent
import org.ligi.passandroid.repository.PassExportTemplate
import org.ligi.passandroid.repository.PassImageContent
import org.ligi.passandroid.repository.PassImageExportOptions
import org.ligi.passandroid.ui.state.DisplayableCode
import org.ligi.passandroid.ui.state.PassArtworkUiModel
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassTicketLayout
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.ui.state.backFields
import org.ligi.passandroid.ui.state.displayArtwork
import org.ligi.passandroid.ui.state.displayableCodes
import org.ligi.passandroid.ui.state.issuerName
import org.ligi.passandroid.ui.state.ticketDateLabel
import org.ligi.passandroid.ui.state.ticketFields
import org.ligi.passandroid.ui.state.ticketLayout
import org.ligi.passandroid.ui.state.ticketTimeLabel
import org.ligi.passandroid.ui.state.unlessIssuer
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

/** Resolved user interface text for an export. The document model has no access to Android resources. */
data class PassExportLabels(
    val typeLabel: String,
    val date: String,
    val time: String,
    val starts: String,
    val ends: String,
    val expires: String,
    val location: String,
    val notes: String,
    val moreInformation: String,
    val pass: String,
    val createdBy: String,
    val footer: String,
)

/** The main block of the ticket, below the header. */
sealed interface ExportMain {
    data class Route(val origin: PassFieldUiModel, val destination: PassFieldUiModel) : ExportMain

    /** A main value. A short value uses the display size, like the pass view. */
    data class Value(val label: String, val value: String, val large: Boolean) : ExportMain
}

data class ExportLocation(val name: String, val coordinates: String)

/**
 * Everything an export template can draw, already filtered by the content options. A null or empty part is not drawn.
 * The model is pure, so its rules are testable on the JVM.
 */
data class PassExportDocument(
    val issuer: String,
    val description: String,
    val typeLabel: String,
    val accentColor: Int,
    /** A pass without a reliable type layout uses neutral colors, like the pass view. */
    val neutral: Boolean,
    val showHeader: Boolean,
    val icon: PassArtworkUiModel?,
    val banner: PassArtworkUiModel?,
    val headerFields: List<PassFieldUiModel>,
    val main: ExportMain?,
    val progress: Float?,
    val dateCells: List<PassFieldUiModel>,
    val seatCells: List<PassFieldUiModel>,
    val grid: List<PassFieldUiModel>,
    /** Column weights of the field grid. One column means a plain list. */
    val gridColumns: List<Float>,
    val backFields: List<PassFieldUiModel>,
    val notes: String?,
    val locations: List<ExportLocation>,
    /** Pass name, issuer, and full dates for the sheet. */
    val summary: List<PassFieldUiModel>,
    val codes: List<DisplayableCode>,
    val codeOnTop: Boolean,
    val labels: PassExportLabels,
) {
    /** True when the colored part of the ticket has content. */
    val hasDetails: Boolean
        get() = showHeader || banner != null || main != null || dateCells.isNotEmpty() || seatCells.isNotEmpty() ||
            grid.isNotEmpty() || backFields.isNotEmpty() || notes != null || locations.isNotEmpty()

    /** The short facts that the story template shows below the code. */
    val keyFacts: List<PassFieldUiModel> get() = (seatCells + headerFields + grid).take(KEY_FACT_COUNT)
}

/** A field identity that stays the same while the user picks fields for one export. */
val PassFieldUiModel.exportId: String get() = key ?: "$label\u0000$value"

private const val KEY_FACT_COUNT = 3
private const val LARGE_VALUE_MAX_LENGTH = 10
private val ICON_KINDS = listOf(PassArtworkKind.ICON, PassArtworkKind.THUMBNAIL, PassArtworkKind.LOGO)
private val BANNER_KINDS = listOf(PassArtworkKind.STRIP, PassArtworkKind.LOGO, PassArtworkKind.THUMBNAIL)
private val EVENT_COLUMNS = listOf(1.4f, 1f)
private val GRID_COLUMNS = listOf(1f, 1f, 1f)
private val LIST_COLUMNS = listOf(1f)

fun buildPassExportDocument(
    pass: PassUiModel,
    content: PassImageContent,
    labels: PassExportLabels,
    locale: Locale,
    codeOnTop: Boolean = false,
    excludedFieldIds: Set<String> = emptySet(),
    today: LocalDate? = null,
): PassExportDocument {
    val visible = pass.copy(fields = pass.fields.filterNot { it.exportId in excludedFieldIds })
    val layout = visible.ticketLayout()
    val front = if (content.details) visible.frontParts(layout, labels.typeLabel) else FrontParts()
    val icon = visible.displayArtwork(ICON_KINDS).takeIf { content.artwork }
    return PassExportDocument(
        issuer = visible.issuerName(),
        description = visible.description,
        typeLabel = labels.typeLabel,
        accentColor = visible.accentColor,
        neutral = layout is PassTicketLayout.Simple,
        showHeader = content.details,
        icon = icon,
        banner = visible.bannerArtwork(icon).takeIf { content.artwork },
        headerFields = front.header,
        main = front.main,
        progress = front.progress,
        dateCells = visible.dateCells(layout, labels, locale, today).takeIf { content.dateTime }.orEmpty(),
        seatCells = front.seat,
        grid = front.grid,
        gridColumns = when (layout) {
            PassTicketLayout.Simple -> LIST_COLUMNS
            is PassTicketLayout.Event -> EVENT_COLUMNS
            else -> GRID_COLUMNS
        },
        backFields = visible.backFields.takeIf { content.hiddenFields }.orEmpty(),
        notes = visible.notes.trim().takeIf { content.notes && it.isNotEmpty() },
        locations = visible.exportLocations().takeIf { content.location }.orEmpty(),
        summary = visible.summary(content.details, content.dateTime, labels, locale),
        codes = visible.displayableCodes().takeIf { content.barcode }.orEmpty(),
        codeOnTop = codeOnTop,
        labels = labels,
    )
}

/** The parts of the colored ticket that the details toggle controls. */
private data class FrontParts(
    val header: List<PassFieldUiModel> = emptyList(),
    val main: ExportMain? = null,
    val progress: Float? = null,
    val seat: List<PassFieldUiModel> = emptyList(),
    val grid: List<PassFieldUiModel> = emptyList(),
)

private fun PassUiModel.frontParts(layout: PassTicketLayout, typeLabel: String): FrontParts {
    val fields = ticketFields(layout, typeLabel)
    return FrontParts(
        header = fields.header,
        main = layout.main(this, issuerName()),
        progress = (layout as? PassTicketLayout.StoreCard)?.progress?.fraction,
        seat = (layout as? PassTicketLayout.Event)?.seat?.fields.orEmpty(),
        grid = fields.grid,
    )
}

private fun PassUiModel.bannerArtwork(icon: PassArtworkUiModel?): PassArtworkUiModel? {
    val banner = displayArtwork(BANNER_KINDS) ?: artwork.firstOrNull()
    // The header already shows a square icon. The same image again as a banner adds nothing.
    return banner?.takeUnless { icon != null && it.bytes.contentEquals(icon.bytes) && it.kind != PassArtworkKind.STRIP }
}

private fun PassTicketLayout.main(pass: PassUiModel, issuer: String): ExportMain? = when (this) {
    is PassTicketLayout.Boarding -> ExportMain.Route(origin, destination)
    is PassTicketLayout.Event -> ExportMain.Value(title.label.unlessIssuer(issuer), title.value, large = false)
    is PassTicketLayout.Coupon -> offer.mainValue(issuer)
    is PassTicketLayout.StoreCard -> primary.mainValue(issuer)
    is PassTicketLayout.Generic -> primary.mainValue(issuer)
    PassTicketLayout.Simple -> pass.description.ifBlank { pass.creator.orEmpty() }
        .takeIf(String::isNotBlank)
        ?.let { ExportMain.Value("", it, large = false) }
}

private fun PassFieldUiModel.mainValue(issuer: String) =
    ExportMain.Value(label.unlessIssuer(issuer), value, large = value.length <= LARGE_VALUE_MAX_LENGTH)

private fun cell(label: String, value: String) = PassFieldUiModel(null, label, value, false, null)

private fun PassUiModel.dateCells(
    layout: PassTicketLayout,
    labels: PassExportLabels,
    locale: Locale,
    today: LocalDate?,
): List<PassFieldUiModel> {
    val from = calendarTimeSpan?.from ?: return emptyList()
    val day = today ?: LocalDate.now(from.zone)
    // The boarding route already shows the times, so its date cell has no end and no time.
    if (layout is PassTicketLayout.Boarding) return listOf(cell(labels.date, ticketDateLabel(from, null, locale, day)))
    return listOfNotNull(
        cell(labels.date, ticketDateLabel(from, calendarTimeSpan.to, locale, day)),
        ticketTimeLabel(from, locale)?.let { cell(labels.time, it) },
    )
}

private fun PassUiModel.exportLocations() = locations.map { location ->
    val coordinates = String.format(Locale.ROOT, "%.5f, %.5f", location.latitude, location.longitude)
    ExportLocation(location.name?.takeIf(String::isNotBlank) ?: coordinates, coordinates)
}

private fun PassUiModel.summary(details: Boolean, dateTime: Boolean, labels: PassExportLabels, locale: Locale) = buildList {
    if (details) {
        if (description.isNotBlank()) add(cell(labels.pass, description))
        creator?.takeIf(String::isNotBlank)?.let { add(cell(labels.createdBy, it)) }
    }
    if (dateTime) {
        val formatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm z", locale)
        fun ZonedDateTime.full() = format(formatter)
        calendarTimeSpan?.from?.let { add(cell(labels.starts, it.full())) }
        calendarTimeSpan?.to?.let { add(cell(labels.ends, it.full())) }
        expiresAt?.let { add(cell(labels.expires, it.full())) }
    }
}

/** One export: the template, its options, and the choices that only last for this export. */
data class PassExportRequest(
    val template: PassExportTemplate,
    val options: PassImageExportOptions,
    /** True when the pass view shows the code before the details, so the ticket is upside down. */
    val codeOnTop: Boolean = false,
    val excludedFieldIds: Set<String> = emptySet(),
) {
    /**
     * The content the template can show. The code template always shows the code; its details toggle is the caption,
     * which names the pass and its date.
     */
    val content: PassImageContent
        get() = PassExportContent.entries.fold(options.content) { content, item ->
            when {
                template != PassExportTemplate.CODE -> content.with(item, content[item] && item in template.content)
                item == PassExportContent.BARCODE -> content.with(item, true)
                item == PassExportContent.DATE_TIME -> content.with(item, options.content.details)
                else -> content.with(item, content[item] && item in template.content)
            }
        }
}
