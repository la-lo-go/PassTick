package org.ligi.passandroid.model

import org.ligi.passandroid.Tracker
import org.ligi.passandroid.model.pass.PassImpl
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class ApplePassbookQuirkCorrector(private val tracker: Tracker) {
    fun correctQuirks(pass: PassImpl): Boolean {
        val originalDescription = pass.description
        val originalTimeSpan = pass.calendarTimespan
        correctWestbahnDescription(pass)
        recoverCalendarDate(pass)
        recoverReservaEntradasDate(pass)
        return pass.description != originalDescription || pass.calendarTimespan != originalTimeSpan
    }

    private fun recoverCalendarDate(pass: PassImpl) {
        if (pass.calendarTimespan != null) return
        val date = pass.fields
            .asSequence()
            .filter { it.key == "date" }
            .mapNotNull { field -> runCatching { ZonedDateTime.parse(field.value) }.getOrNull() }
            .firstOrNull()
            ?: return
        tracker.trackEvent("quirk_fix", "find_date", "find_date", 0L)
        pass.calendarTimespan = PassImpl.TimeSpan(from = date)
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
    }
}
