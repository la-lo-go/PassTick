package org.ligi.passandroid.ui.state

import org.ligi.passandroid.model.pass.PassType

/**
 * The detail layout for a pass. A type layout applies only when the declared type and the field groups agree.
 * Every other pass uses [Simple], so a badly encoded pass never gets a guessed layout.
 */
sealed interface PassTicketLayout {
    data class Boarding(val origin: PassFieldUiModel, val destination: PassFieldUiModel) : PassTicketLayout
    data class Event(val title: PassFieldUiModel, val seat: PassSeat?) : PassTicketLayout
    data class Coupon(val offer: PassFieldUiModel) : PassTicketLayout
    data class StoreCard(val primary: PassFieldUiModel, val progress: PassProgress?) : PassTicketLayout
    data class Generic(val primary: PassFieldUiModel) : PassTicketLayout
    data object Simple : PassTicketLayout
}

data class PassSeat(val section: PassFieldUiModel?, val row: PassFieldUiModel?, val seat: PassFieldUiModel?) {
    val fields: List<PassFieldUiModel> get() = listOfNotNull(section, row, seat)
}

data class PassProgress(val current: Int, val total: Int) {
    val fraction: Float get() = current.coerceIn(0, total) / total.toFloat()
}

private const val PRIMARY = "primaryFields"
private const val HEADER = "headerFields"
private const val MAX_PROGRESS_TOTAL = 100
private const val WIDE_VALUE_LENGTH = 12
private const val HEADER_VALUE_LENGTH = 8
private const val HEADER_LABEL_LENGTH = 12
private const val MAX_HEADER_FIELDS = 2

private val SECTION_LABEL = Regex("\\b(section|sector|zone|zona|area|block|bloque)\\b", RegexOption.IGNORE_CASE)
private val ROW_LABEL = Regex("\\b(row|fila|reihe)\\b", RegexOption.IGNORE_CASE)
private val SEAT_LABEL = Regex("\\b(seat|butaca|asiento|platz|sitz)\\b", RegexOption.IGNORE_CASE)
private val PROGRESS_VALUE = Regex("^\\s*(\\d+)\\s*(?:/|of|de|von)\\s*(\\d+)\\s*$", RegexOption.IGNORE_CASE)

private val PassUiModel.frontFields get() = fields.filter { !it.hidden && it.value.isNotBlank() }

val PassUiModel.primaryFields: List<PassFieldUiModel> get() = frontFields.filter { it.hint == PRIMARY }

val PassUiModel.headerFields: List<PassFieldUiModel> get() = frontFields.filter { it.hint == HEADER }

val PassUiModel.backFields: List<PassFieldUiModel> get() = fields.filter { it.hidden && it.value.isNotBlank() }

fun PassUiModel.ticketLayout(): PassTicketLayout {
    val primaries = primaryFields
    val first = primaries.firstOrNull() ?: return PassTicketLayout.Simple
    return when (type) {
        PassType.BOARDING, PassType.PKBOARDING ->
            if (primaries.size == 2) PassTicketLayout.Boarding(primaries[0], primaries[1]) else PassTicketLayout.Simple
        PassType.EVENT ->
            if (calendarTimeSpan?.from != null) PassTicketLayout.Event(first, seatOf(frontFields)) else PassTicketLayout.Simple
        PassType.COUPON, PassType.VOUCHER -> PassTicketLayout.Coupon(first)
        PassType.LOYALTY -> PassTicketLayout.StoreCard(first, progressOf(first.value))
        PassType.GENERIC -> PassTicketLayout.Generic(first)
    }
}

/** Front fields for the grid: everything that the header, the main slot, or the seat block does not show. */
fun PassUiModel.ticketGridFields(layout: PassTicketLayout): List<PassFieldUiModel> {
    val shown = buildSet {
        addAll(headerFields)
        if (layout !is PassTicketLayout.Simple) addAll(primaryFields)
        if (layout is PassTicketLayout.Event) layout.seat?.fields?.let(::addAll)
    }
    return frontFields.filter { it !in shown }
}

/**
 * Packs fields into grid rows. A long value gets a full row, because a narrow column would break it letter by letter.
 */
fun List<PassFieldUiModel>.gridRows(columns: Int): List<List<PassFieldUiModel>> {
    fun PassFieldUiModel.isWide() = value.length > WIDE_VALUE_LENGTH
    val rows = mutableListOf<MutableList<PassFieldUiModel>>()
    forEach { field ->
        val last = rows.lastOrNull()
        val startsRow = last == null || field.isWide() || last.size == columns || last.first().isWide()
        if (startsRow) rows += mutableListOf(field) else last += field
    }
    return rows
}

/** The issuer name in the ticket header: the creator, or the description when the pass has no creator. */
fun PassUiModel.issuerName(): String = creator?.takeIf(String::isNotBlank) ?: description

// Some issuers repeat the event name as the label of the main field. The header already shows it.
fun String.unlessIssuer(issuer: String): String = takeUnless { it.equals(issuer, ignoreCase = true) }.orEmpty()

/** The header row fields and the grid fields of a ticket. */
data class TicketFields(val header: List<PassFieldUiModel>, val grid: List<PassFieldUiModel>)

/**
 * Splits the front fields between the header row and the grid. Fields that only repeat what the ticket already
 * shows (the date, the issuer name, or the pass type) are dropped. Header fields that do not fit open the grid.
 */
fun PassUiModel.ticketFields(layout: PassTicketLayout, typeLabel: String): TicketFields {
    val issuer = issuerName()
    val ticketDate = calendarTimeSpan?.from?.toLocalDate()
    fun PassFieldUiModel.repeatsTicket() =
        (ticketDate != null && repeatsDate(ticketDate)) ||
            value.equals(issuer, ignoreCase = true) ||
            (hint == null && (value.equals(typeLabel, ignoreCase = true) || value.equals(type.name, ignoreCase = true)))
    val allHeaderFields = headerFields.filterNot { it.repeatsTicket() }
    val header = allHeaderFields.takeIf { layout !is PassTicketLayout.Simple && it.fitsTicketHeader() }.orEmpty()
    val grid = (allHeaderFields - header.toSet()) + ticketGridFields(layout).filterNot { it.repeatsTicket() }
    return TicketFields(header, grid)
}

/** Header fields share the row with the issuer name, so only a few short ones fit there. */
fun List<PassFieldUiModel>.fitsTicketHeader(): Boolean =
    size <= MAX_HEADER_FIELDS && all { it.value.length <= HEADER_VALUE_LENGTH && it.label.length <= HEADER_LABEL_LENGTH }

private fun seatOf(fields: List<PassFieldUiModel>): PassSeat? {
    // One field can match several labels, for example "Fila - Asiento". Each field fills one cell only.
    fun find(label: Regex, vararg taken: PassFieldUiModel?) =
        fields.firstOrNull { it.hint != PRIMARY && it !in taken && label.containsMatchIn(it.label) }
    val seat = find(SEAT_LABEL)
    val row = find(ROW_LABEL, seat)
    if (row == null && seat == null) return null
    return PassSeat(find(SECTION_LABEL, row, seat), row, seat)
}

private fun progressOf(value: String): PassProgress? {
    val match = PROGRESS_VALUE.matchEntire(value) ?: return null
    val current = match.groupValues[1].toInt()
    val total = match.groupValues[2].toInt()
    return if (total in 1..MAX_PROGRESS_TOTAL && current <= total) PassProgress(current, total) else null
}
