package org.ligi.passandroid.widget

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import java.time.Instant
import java.time.ZoneId

class CodePassResolverTest {

    private val now = Instant.parse("2026-09-21T12:00:00Z")
    private val zoneId = ZoneId.of("Europe/Madrid")

    @Test
    fun `the chosen pass wins when it is present in the snapshot`() {
        val passes = listOf(pass("a", "A"), pass("b", "B"))

        val resolved = resolveCodePass(passes, codePassId = "b", now = now, zoneId = zoneId)

        assertThat(resolved?.id).isEqualTo("b")
    }

    @Test
    fun `a missing pass id falls back to the first pinned pass in the sort order`() {
        val passes = listOf(
            pass("a", "A", pinned = true, startsAt = "2026-12-01T10:00:00Z"),
            pass("b", "B", pinned = true, startsAt = "2026-09-22T10:00:00Z"),
        )

        val resolved = resolveCodePass(
            passes,
            codePassId = "missing",
            sortOrder = PassSortOrder.DATE_ASC,
            now = now,
            zoneId = zoneId,
        )

        assertThat(resolved?.id).isEqualTo("b")
    }

    @Test
    fun `manual order decides the first pinned pass`() {
        val passes = listOf(
            pass("a", "A", pinned = true),
            pass("b", "B", pinned = true),
        )

        val resolved = resolveCodePass(
            passes,
            codePassId = null,
            sortOrder = PassSortOrder.MANUAL,
            passOrder = listOf("b", "a"),
            now = now,
            zoneId = zoneId,
        )

        assertThat(resolved?.id).isEqualTo("b")
    }

    @Test
    fun `type order sorts pinned passes by type before date`() {
        val passes = listOf(
            pass("coupon", "Coupon", pinned = true, type = PassType.COUPON, startsAt = "2026-09-22T10:00:00Z"),
            pass("event", "Event", pinned = true, type = PassType.EVENT, startsAt = "2026-09-21T10:00:00Z"),
        )

        val resolved = resolveCodePass(
            passes,
            codePassId = null,
            sortOrder = PassSortOrder.TYPE,
            now = now,
            zoneId = zoneId,
        )

        // PassType orders by enum identity, and EVENT precedes COUPON in the declaration order.
        assertThat(resolved?.id).isEqualTo("event")
    }

    @Test
    fun `without pins the next upcoming pass from the snapshot is chosen`() {
        val passes = listOf(
            pass("ended", "Ended", startsAt = "2026-09-01T10:00:00Z", endsAt = "2026-09-01T12:00:00Z"),
            pass("later", "Later", startsAt = "2026-09-30T10:00:00Z"),
        )

        val resolved = resolveCodePass(passes, codePassId = null, now = now, zoneId = zoneId)

        assertThat(resolved?.id).isEqualTo("later")
    }

    @Test
    fun `protected and locked passes never resolve because they stay out of the snapshot`() {
        val resolved = resolveCodePass(
            passes = emptyList(),
            codePassId = "protected-or-locked",
            sortOrder = PassSortOrder.DATE_ASC,
            now = now,
            zoneId = zoneId,
        )

        assertThat(resolved).isNull()
    }

    @Test
    fun `an empty snapshot resolves to null`() {
        val resolved = resolveCodePass(emptyList(), codePassId = null, now = now, zoneId = zoneId)

        assertThat(resolved).isNull()
    }

    private fun pass(
        id: String,
        title: String,
        pinned: Boolean = false,
        type: PassType = PassType.EVENT,
        startsAt: String = "2026-10-01T10:00:00Z",
        endsAt: String? = null,
    ) = WidgetPass(
        id = id,
        title = title,
        issuer = "Studio",
        type = type,
        isPinned = pinned,
        startsAtEpochMillis = Instant.parse(startsAt).toEpochMilli(),
        endsAtEpochMillis = endsAt?.let(Instant::parse)?.toEpochMilli(),
        location = null,
        supportingText = null,
        barcodeFormat = PassBarCodeFormat.QR_CODE,
        barcodeMessage = "https://example.test/$id",
    )
}