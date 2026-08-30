package org.ligi.passandroid.model

import org.ligi.passandroid.Tracker
import org.ligi.passandroid.model.pass.PassImpl
import org.threeten.bp.ZonedDateTime

class ApplePassbookQuirkCorrector(private val tracker: Tracker) {
    fun correctQuirks(pass: PassImpl) {
        correctWestbahnDescription(pass)
        recoverCalendarDate(pass)
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
}
