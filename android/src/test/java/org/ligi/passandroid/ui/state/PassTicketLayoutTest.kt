package org.ligi.passandroid.ui.state

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassType
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class PassTicketLayoutTest {
    private val start = ZonedDateTime.of(2026, 11, 14, 20, 30, 0, 0, ZoneId.of("Europe/Madrid"))

    @Test
    fun `boarding pass with two primary fields uses the route layout`() {
        val pass = pass(PassType.BOARDING, primary("from", "MAD"), primary("to", "BCN"), field("gate", "J42", "headerFields"))

        val layout = pass.ticketLayout()

        assertThat(layout).isEqualTo(PassTicketLayout.Boarding(primary("from", "MAD"), primary("to", "BCN")))
        assertThat(pass.ticketGridFields(layout)).isEmpty()
    }

    @Test
    fun `boarding pass with one primary field falls back to the simple layout`() {
        val pass = pass(PassType.BOARDING, primary("Route", "MAD → BCN"))

        assertThat(pass.ticketLayout()).isEqualTo(PassTicketLayout.Simple)
        assertThat(pass.ticketGridFields(PassTicketLayout.Simple)).containsExactly(primary("Route", "MAD → BCN"))
    }

    @Test
    fun `boarding pass without primary fields falls back to the simple layout`() {
        val pass = pass(PassType.BOARDING, field("Coach", "4", "secondaryFields"))

        assertThat(pass.ticketLayout()).isEqualTo(PassTicketLayout.Simple)
    }

    @Test
    fun `event with a date finds the seat fields by label`() {
        val zone = field("Zona", "Patio de butacas", "auxiliaryFields")
        val row = field("Fila", "7", "auxiliaryFields")
        val seat = field("Butaca", "12", "auxiliaryFields")
        val door = field("Puerta", "2", "secondaryFields")
        val pass = pass(PassType.EVENT, primary("Obra", "La casa de Bernarda Alba"), zone, row, seat, door, from = start)

        val layout = pass.ticketLayout()

        assertThat(layout).isEqualTo(PassTicketLayout.Event(primary("Obra", "La casa de Bernarda Alba"), PassSeat(zone, row, seat)))
        assertThat(pass.ticketGridFields(layout)).containsExactly(door)
    }

    @Test
    fun `a field labelled with both row and seat fills one cell`() {
        val room = field("Sala - Zona", "Emb1 - General", "auxiliaryFields")
        val rowAndSeat = field("Fila - Asiento", "4-5", "auxiliaryFields")
        val pass = pass(PassType.EVENT, primary("Cine", "La bola negra"), room, rowAndSeat, from = start)

        assertThat(pass.ticketLayout()).isEqualTo(PassTicketLayout.Event(primary("Cine", "La bola negra"), PassSeat(room, null, rowAndSeat)))
    }

    @Test
    fun `event without a structured date falls back to the simple layout`() {
        val pass = pass(PassType.EVENT, primary("Event", "Concert"))

        assertThat(pass.ticketLayout()).isEqualTo(PassTicketLayout.Simple)
    }

    @Test
    fun `event without row or seat has no seat block`() {
        val pass = pass(PassType.EVENT, primary("Access", "Weekend"), field("Zone", "General admission", "secondaryFields"), from = start)

        assertThat(pass.ticketLayout()).isEqualTo(PassTicketLayout.Event(primary("Access", "Weekend"), null))
    }

    @Test
    fun `store card reads stamp progress from the primary value`() {
        val pass = pass(PassType.LOYALTY, primary("Stamps", "7 of 10"))

        assertThat(pass.ticketLayout()).isEqualTo(PassTicketLayout.StoreCard(primary("Stamps", "7 of 10"), PassProgress(7, 10)))
    }

    @Test
    fun `store card keeps text values without progress`() {
        val pass = pass(PassType.LOYALTY, primary("Member", "Alex Morgan"))

        assertThat(pass.ticketLayout()).isEqualTo(PassTicketLayout.StoreCard(primary("Member", "Alex Morgan"), null))
    }

    @Test
    fun `coupon and generic use their primary field`() {
        assertThat(pass(PassType.COUPON, primary("Offer", "20% off")).ticketLayout())
            .isEqualTo(PassTicketLayout.Coupon(primary("Offer", "20% off")))
        assertThat(pass(PassType.GENERIC, primary("Locker", "B-14")).ticketLayout())
            .isEqualTo(PassTicketLayout.Generic(primary("Locker", "B-14")))
    }

    @Test
    fun `back fields stay out of the front and appear in the back list`() {
        val back = PassFieldUiModel("terms", "Terms", "No refunds", hidden = true, hint = "backFields")
        val pass = pass(PassType.GENERIC, primary("Locker", "B-14"), back)

        assertThat(pass.ticketGridFields(pass.ticketLayout())).isEmpty()
        assertThat(pass.backFields).containsExactly(back)
    }

    @Test
    fun `long values take a full grid row`() {
        val a = field("Group", "2", "auxiliaryFields")
        val b = field("Class", "Economy", "auxiliaryFields")
        val long = field("Booking", "K7Q2LMX9TR2PAB3CDEF4GHIJ", "auxiliaryFields")
        val c = field("Zone", "3", "auxiliaryFields")

        assertThat(listOf(a, b, long, c).gridRows(3)).containsExactly(listOf(a, b), listOf(long), listOf(c))
    }

    @Test
    fun `only two short header fields fit next to the issuer`() {
        assertThat(listOf(field("Gate", "B27", "headerFields"), field("Seat", "14C", "headerFields")).fitsTicketHeader()).isTrue()
        assertThat(listOf(field("Boarding gate", "Terminal 5 Gate A10", "headerFields")).fitsTicketHeader()).isFalse()
    }

    private fun primary(label: String, value: String) = field(label, value, "primaryFields")

    private fun field(label: String, value: String, hint: String?) = PassFieldUiModel(label.lowercase(), label, value, false, hint)

    private fun pass(type: PassType, vararg fields: PassFieldUiModel, from: ZonedDateTime? = null) = PassUiModel(
        id = "id",
        description = "Pass",
        creator = null,
        type = type,
        accentColor = 0,
        barcodeFormat = null,
        barcodeMessage = null,
        barcodeAlternativeText = null,
        fields = fields.toList(),
        locations = emptyList(),
        calendarEvent = null,
        calendarTimeSpan = from?.let { PassTimeSpanUiModel(it, null) },
    )
}
