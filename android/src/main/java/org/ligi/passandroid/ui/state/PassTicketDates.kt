package org.ligi.passandroid.ui.state

import org.threeten.bp.LocalDate

private const val TWO_DIGIT_YEAR_BASE = 2000
private const val MONTH_PREFIX_LENGTH = 3

private val NUMERIC_DATE = Regex("\\b(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4}|\\d{2})\\b")
private val ISO_DATE = Regex("(?<!\\d)(\\d{4})-(\\d{2})-(\\d{2})(?!\\d)")
private val NAMED_DATE = Regex("\\b(\\d{1,2})\\.?\\s+(?:de\\s+)?(\\p{L}{3,})\\.?\\s+(?:de\\s+)?(\\d{4})\\b", RegexOption.IGNORE_CASE)

// English, Spanish, and German month prefixes. Index + 1 is the month number.
private val MONTH_PREFIXES = listOf(
    setOf("jan", "ene"),
    setOf("feb"),
    setOf("mar", "mär"),
    setOf("apr", "abr"),
    setOf("may", "mai"),
    setOf("jun"),
    setOf("jul"),
    setOf("aug", "ago"),
    setOf("sep", "set"),
    setOf("oct", "okt"),
    setOf("nov"),
    setOf("dec", "dic", "dez"),
)

// Labels that only name the date itself. A field with such a label adds nothing once the ticket shows the date.
private val DATE_LABEL = Regex(
    "\\b(fecha|date|datum|data|d[ií]a|day|tag|viaje|trip|journey|travel|fahrt|reise|sesi[oó]n|session|funci[oó]n)\\b",
    RegexOption.IGNORE_CASE,
)

/** True when the text names [date] in a numeric, ISO, or month-name format. Numeric dates match day-first or month-first. */
internal fun String.mentionsDate(date: LocalDate): Boolean {
    fun year(value: String) = value.toInt().let { if (value.length == 2) TWO_DIGIT_YEAR_BASE + it else it }
    if (ISO_DATE.findAll(this).any { it.groupValues.let { (_, y, m, d) -> date.matches(y.toInt(), m.toInt(), d.toInt()) } }) return true
    if (NUMERIC_DATE.findAll(this).any { match ->
            val (_, first, second, y) = match.groupValues
            date.matches(year(y), second.toInt(), first.toInt()) || date.matches(year(y), first.toInt(), second.toInt())
        }
    ) {
        return true
    }
    return NAMED_DATE.findAll(this).any { match ->
        val (_, day, name, y) = match.groupValues
        val prefix = name.take(MONTH_PREFIX_LENGTH).lowercase()
        val month = MONTH_PREFIXES.indexOfFirst { prefix in it } + 1
        month > 0 && date.matches(y.toInt(), month, day.toInt())
    }
}

private fun LocalDate.matches(year: Int, month: Int, day: Int) = this.year == year && monthValue == month && dayOfMonth == day

/** True when the field only repeats the pass date: it names the date, and its label is empty, a date, or a date word. */
fun PassFieldUiModel.repeatsDate(date: LocalDate): Boolean {
    if (!label.mentionsDate(date) && !value.mentionsDate(date)) return false
    return label.isBlank() || label.mentionsDate(date) || DATE_LABEL.containsMatchIn(label)
}

/** The layouts that print the pass date inside the ticket. */
fun PassTicketLayout.showsDate(): Boolean = this is PassTicketLayout.Event || this is PassTicketLayout.Boarding

/** True when the ticket already shows the pass date, so a separate date card would repeat it. */
fun PassUiModel.ticketShowsCalendarDate(layout: PassTicketLayout): Boolean {
    val date = calendarTimeSpan?.from?.toLocalDate() ?: return false
    return layout.showsDate() || fields.any { !it.hidden && (it.label.mentionsDate(date) || it.value.mentionsDate(date)) }
}
