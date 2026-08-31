package org.ligi.passandroid.model

import org.ligi.passandroid.Tracker
import org.ligi.passandroid.model.pass.PassImpl
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.Month
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import java.util.Locale

class ApplePassbookQuirkCorrector(private val tracker: Tracker) {
    fun correctQuirks(pass: PassImpl): Boolean {
        val originalDescription = pass.description
        val originalTimeSpan = pass.calendarTimespan
        correctWestbahnDescription(pass)
        recoverReservaEntradasDate(pass)
        recoverCalendarDate(pass)
        return pass.description != originalDescription || pass.calendarTimespan != originalTimeSpan
    }

    private fun recoverCalendarDate(pass: PassImpl) {
        if (pass.calendarTimespan != null) return
        val fields = pass.fields
            .asSequence()
            .sortedByDescending(::dateFieldScore)
            .toList()
        val date = fields
            .asSequence()
            .mapNotNull { field ->
                field.value
                    ?.takeIf(::containsTime)
                    ?.let { parseDateTime(it, field.key, field.label) }
            }
            .firstOrNull()
            ?: parseSeparateDateAndTime(pass)
            ?: fields.asSequence()
                .mapNotNull { field -> field.value?.let { parseDateTime(it, field.key, field.label) } }
                .firstOrNull()
            ?: return
        tracker.trackEvent("quirk_fix", "find_date", "find_date", 0L)
        pass.calendarTimespan = PassImpl.TimeSpan(from = date)
    }

    private fun parseDateTime(value: String, key: String?, label: String?): ZonedDateTime? {
        ISO_DATE_TIME.find(value)?.value?.let { iso ->
            runCatching { ZonedDateTime.parse(iso) }.getOrNull()?.let { return it }
        }
        runCatching { ZonedDateTime.parse(value) }.getOrNull()?.let { return it }

        val hint = "$key $label".lowercase(Locale.ROOT)
        MONTH_NAME_DATE.find(value)?.let { match ->
            return localDateTime(
                year = match.groupValues[3].toInt(),
                month = monthNumber(match.groupValues[2]) ?: return null,
                day = match.groupValues[1].toInt(),
                time = match.groupValues[4],
                meridiem = match.groupValues[5],
            )
        }
        MONTH_FIRST_NAME_DATE.find(value)?.let { match ->
            return localDateTime(
                year = match.groupValues[3].toInt(),
                month = monthNumber(match.groupValues[1]) ?: return null,
                day = match.groupValues[2].toInt(),
                time = match.groupValues[4],
                meridiem = match.groupValues[5],
            )
        }
        NUMERIC_DATE.find(value)?.let { match ->
            val first = match.groupValues[1].toInt()
            val second = match.groupValues[2].toInt()
            val monthFirst = when {
                first > 12 -> false
                second > 12 -> true
                hint.containsAny(MONTH_FIRST_HINTS) -> true
                hint.containsAny(DAY_FIRST_HINTS) -> false
                else -> false
            }
            return localDateTime(
                year = match.groupValues[3].toInt(),
                month = if (monthFirst) first else second,
                day = if (monthFirst) second else first,
                time = match.groupValues[4],
                meridiem = match.groupValues[5],
            )
        }
        return null
    }

    private fun parseSeparateDateAndTime(pass: PassImpl): ZonedDateTime? {
        val dateField = pass.fields
            .asSequence()
            .filter { dateFieldScore(it) > 0 }
            .mapNotNull { field -> field.value?.let { value -> parseLocalDate(value, field.key, field.label) } }
            .firstOrNull()
            ?: return null
        val timeField = pass.fields
            .asSequence()
            .filter { timeFieldScore(it) > 0 }
            .mapNotNull { it.value?.let(::parseLocalTime) }
            .firstOrNull()
            ?: return null
        return LocalDateTime.of(dateField, timeField).atZone(ZoneId.systemDefault())
    }

    private fun parseLocalDate(value: String, key: String?, label: String?): LocalDate? =
        parseDateTime(value, key, label)?.toLocalDate()

    private fun containsTime(value: String): Boolean = TIME.containsMatchIn(value) || ISO_DATE_TIME.containsMatchIn(value)

    private fun parseLocalTime(value: String): LocalTime? = TIME.find(value)?.let { match ->
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        val meridiem = match.groupValues[3]
        val normalizedHour = when {
            meridiem.equals("pm", ignoreCase = true) && hour < 12 -> hour + 12
            meridiem.equals("am", ignoreCase = true) && hour == 12 -> 0
            else -> hour
        }
        runCatching { LocalTime.of(normalizedHour, minute) }.getOrNull()
    }

    private fun localDateTime(year: Int, month: Int, day: Int, time: String, meridiem: String): ZonedDateTime? {
        val localTime = if (time.isBlank()) LocalTime.MIDNIGHT else parseLocalTime("$time $meridiem") ?: return null
        return runCatching {
            LocalDateTime.of(year, month, day, localTime.hour, localTime.minute).atZone(ZoneId.systemDefault())
        }.getOrNull()
    }

    private fun dateFieldScore(field: org.ligi.passandroid.model.pass.PassField): Int {
        val hint = "${field.key} ${field.label}".lowercase(Locale.ROOT)
        return when {
            hint.containsAny(DATE_TIME_HINTS) -> 2
            hint.containsAny(DATE_HINTS) -> 1
            else -> 0
        }
    }

    private fun timeFieldScore(field: org.ligi.passandroid.model.pass.PassField): Int =
        if ("${field.key} ${field.label}".lowercase(Locale.ROOT).containsAny(TIME_HINTS)) 1 else 0

    private fun String.containsAny(words: Set<String>) = words.any(::contains)

    private fun monthNumber(name: String): Int? {
        val candidate = name.trim().trimEnd('.')
        return MONTH_FORMATTERS.firstNotNullOfOrNull { formatter ->
            runCatching { Month.from(formatter.parse(candidate)).value }.getOrNull()
        }
    }

    private fun correctWestbahnDescription(pass: PassImpl) {
        if (pass.calendarTimespan != null || pass.creator != "WESTbahn") return
        val origin = pass.fields.firstOrNull { it.key == "from" }?.value
        val destination = pass.fields.firstOrNull { it.key == "to" }?.value
        if (origin == null && destination == null) return

        tracker.trackEvent("quirk_fix", "description_replace", "westbahn", 0L)
        pass.description = listOfNotNull(origin, destination).joinToString("->")
    }

    private fun recoverReservaEntradasDate(pass: PassImpl) {
        if (pass.calendarTimespan != null || pass.creator != RESERVA_ENTRADAS_CREATOR) return
        val value = pass.fields.firstOrNull { it.key == RESERVA_ENTRADAS_DATE_FIELD }?.value ?: return
        val match = RESERVA_ENTRADAS_DATE.find(value) ?: return
        val date = runCatching {
            LocalDateTime.parse(
                "${match.groupValues[1]} ${match.groupValues[2]}",
                RESERVA_ENTRADAS_DATE_FORMAT,
            ).atZone(ZoneId.of("Europe/Madrid"))
        }.getOrNull() ?: return
        tracker.trackEvent("quirk_fix", "find_date", "reserva_entradas", 0L)
        pass.calendarTimespan = PassImpl.TimeSpan(from = date)
    }

    private companion object {
        const val RESERVA_ENTRADAS_CREATOR = "Icenter Torrent S.L."
        const val RESERVA_ENTRADAS_DATE_FIELD = "date-time"
        val RESERVA_ENTRADAS_DATE = Regex("(\\d{2}/\\d{2}/\\d{4}).*?(\\d{2}:\\d{2})")
        val RESERVA_ENTRADAS_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val ISO_DATE_TIME = Regex("\\b\\d{4}-\\d{2}-\\d{2}T[^\\s]+")
        val NUMERIC_DATE = Regex(
            "\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})(?:[^\\d]{1,12}(\\d{1,2}:\\d{2})(?:\\s*([AaPp][.]?[Mm][.]?))?)?",
        )
        val MONTH_NAME_DATE = Regex(
            "\\b(\\d{1,2})\\s+([\\p{L}.]+)\\s+(\\d{4})(?:[^\\d]{0,12}(\\d{1,2}:\\d{2})(?:\\s*([AaPp][.]?[Mm][.]?))?)?",
            RegexOption.IGNORE_CASE,
        )
        val MONTH_FIRST_NAME_DATE = Regex(
            "\\b([\\p{L}.]+)\\s+(\\d{1,2})(?:,)?\\s+(\\d{4})(?:[^\\d]{0,12}(\\d{1,2}:\\d{2})(?:\\s*([AaPp][.]?[Mm][.]?))?)?",
            RegexOption.IGNORE_CASE,
        )
        val TIME = Regex("\\b(\\d{1,2}):(\\d{2})(?:\\s*([AaPp][.]?[Mm][.]?))?\\b")

        val DATE_TIME_HINTS = setOf(
            "date-time", "datetime", "date and time", "fecha y hora", "fecha/hora", "datum und uhrzeit",
        )
        val DATE_HINTS = setOf("date", "fecha", "datum", "jour", "data")
        val TIME_HINTS = setOf("time", "hora", "uhrzeit", "heure", "start time", "departure time")
        val MONTH_FIRST_HINTS = setOf("en-us", "english", "us date", "month/day", "month first")
        val DAY_FIRST_HINTS = setOf("fecha", "date", "datum", "jour", "data", "dd/mm", "day/month")
        val MONTH_FORMATTERS = listOf(
            Locale.ENGLISH,
            Locale.forLanguageTag("es"),
            Locale.FRENCH,
            Locale.GERMAN,
            Locale.ITALIAN,
            Locale.forLanguageTag("pt"),
        ).flatMap { locale ->
            listOf("MMM", "MMMM").map { pattern ->
                DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(locale)
            }
        }
    }
}
