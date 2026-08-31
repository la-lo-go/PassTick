package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.model.ApplePassbookQuirkCorrector
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.PassImpl
import org.mockito.Mockito
import org.threeten.bp.ZonedDateTime
import java.util.*

private const val DATE_PROBE = "2016-07-06T21:00:00-04:00"

class TheAppleStyleQuirkCorrector {
    private val tested = ApplePassbookQuirkCorrector(Mockito.mock(Tracker::class.java))

    @Test
    fun testThatItDoesNothingWithNoField() {
        val pass = PassImpl(UUID.randomUUID().toString())

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan).isNull()
    }


    @Test
    fun testThatDateIsExtracted() {
        val pass = PassImpl(UUID.randomUUID().toString())

        pass.fields = mutableListOf(PassField("date", "foo", DATE_PROBE, false))

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan!!.from).isEqualTo(ZonedDateTime.parse(DATE_PROBE))
    }

    @Test
    fun testThatInvalidDateIsIgnored() {
        val pass = PassImpl(UUID.randomUUID().toString())

        pass.fields = mutableListOf(PassField("date", "foo", "invalid", false))

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan).isNull()
    }

    @Test
    fun testThatDateIsExtractedAfterWrongDatesBefore() {
        val pass = PassImpl(UUID.randomUUID().toString())

        pass.fields = mutableListOf(PassField("date", "foo", "invalid", false), PassField("date", "foo", DATE_PROBE, false))

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan!!.from).isEqualTo(ZonedDateTime.parse(DATE_PROBE))
    }

    @Test
    fun `extracts a ReservaEntradas event date`() {
        val pass = PassImpl(UUID.randomUUID().toString()).apply {
            creator = "Icenter Torrent S.L."
            fields = mutableListOf(
                PassField("date-time", "Fecha y hora", "27/08/2026 - 17:20", false),
            )
        }

        assertThat(tested.correctQuirks(pass)).isTrue()

        assertThat(pass.calendarTimespan!!.from).isEqualTo(
            ZonedDateTime.parse("2026-08-27T17:20:00+02:00[Europe/Madrid]"),
        )
    }

    @Test
    fun `extracts a local date and time from another pass field`() {
        val pass = PassImpl(UUID.randomUUID().toString()).apply {
            fields = mutableListOf(
                PassField("entry", "Doors open", "Doors open 09-09-2026 at 18:45", false),
            )
        }

        assertThat(tested.correctQuirks(pass)).isTrue()
        assertThat(pass.calendarTimespan!!.from!!.toLocalDateTime().toString()).isEqualTo("2026-09-09T18:45")
    }

    @Test
    fun `recovers a month first date when its English label makes the order explicit`() {
        val pass = PassImpl(UUID.randomUUID().toString()).apply {
            fields = mutableListOf(
                PassField("event", "US date and time", "08/09/2026 5:20 PM", false),
            )
        }

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan!!.from!!.toLocalDateTime().toString()).isEqualTo("2026-08-09T17:20")
    }

    @Test
    fun `recovers a Spanish month name date from a labelled field`() {
        val pass = PassImpl(UUID.randomUUID().toString()).apply {
            fields = mutableListOf(
                PassField("evento", "Fecha y hora", "15 septiembre 2026, 18:45", false),
            )
        }

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan!!.from!!.toLocalDateTime().toString()).isEqualTo("2026-09-15T18:45")
    }

    @Test
    fun `combines labelled date and time fields`() {
        val pass = PassImpl(UUID.randomUUID().toString()).apply {
            fields = mutableListOf(
                PassField("event_date", "Event date", "27/08/2026", false),
                PassField("event_time", "Start time", "17:20", false),
            )
        }

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan!!.from!!.toLocalDateTime().toString()).isEqualTo("2026-08-27T17:20")
    }

    @Test
    fun `does not treat unrelated numbers as a pass date`() {
        val pass = PassImpl(UUID.randomUUID().toString()).apply {
            fields = mutableListOf(
                PassField("seat", "Seat", "Car 12, seat 08, gate 27", false),
            )
        }

        tested.correctQuirks(pass)

        assertThat(pass.calendarTimespan).isNull()
    }

}
