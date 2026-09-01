package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.functions.createCalendarEvent
import org.ligi.passandroid.functions.DEFAULT_EVENT_LENGTH_IN_HOURS
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassLocation
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.threeten.bp.ZonedDateTime

const val DESCRIPTIONPROBE = "descriptionprobe"
const val LOCATIONPROBE = "locationprobe"

class TheAddToCalendar {

    val pass: Pass = mock(Pass::class.java).apply {
        `when`(description).thenReturn(DESCRIPTIONPROBE)
    }

    private val validTimeSpan = mock(PassImpl.TimeSpan::class.java).apply {
        `when`(from).thenReturn(ZonedDateTime.now())
        `when`(to).thenReturn(ZonedDateTime.now().plusHours(5))

    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowIllegalArgumentWhenNoFromOrTo() {
        createCalendarEvent(mock(Pass::class.java), mock(PassImpl.TimeSpan::class.java))
    }

    @Test
    fun descriptionShouldShow() {
        val tested = createCalendarEvent(pass, validTimeSpan)
        assertThat(tested.title).isEqualTo(DESCRIPTIONPROBE)
    }

    @Test
    fun timesAreCorrect() {
        val tested = createCalendarEvent(pass, validTimeSpan)
        assertThat(tested.endTimeMillis).isGreaterThan(tested.beginTimeMillis)
    }

    @Test
    fun missingEndUsesTwoHoursOnlyForTheCalendarEvent() {
        val start = ZonedDateTime.parse("2026-09-01T10:15:00+02:00[Europe/Madrid]")
        val source = PassImpl.TimeSpan(from = start, to = null)

        val tested = createCalendarEvent(pass, source)

        assertThat(source.to).isNull()
        assertThat(tested.endTimeMillis - tested.beginTimeMillis)
            .isEqualTo(DEFAULT_EVENT_LENGTH_IN_HOURS * 60 * 60 * 1000)
        assertThat(DEFAULT_EVENT_LENGTH_IN_HOURS).isEqualTo(2L)
    }

    @Test
    fun locationIsCorrect() {
        `when`(pass.locations).thenReturn(listOf(PassLocation().apply { name = LOCATIONPROBE }))

        val tested = createCalendarEvent(pass, validTimeSpan)
        assertThat(tested.location).isEqualTo(LOCATIONPROBE)
    }


}
