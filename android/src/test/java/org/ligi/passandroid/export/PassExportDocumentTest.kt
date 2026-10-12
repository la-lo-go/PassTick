package org.ligi.passandroid.export

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.CODE_ONLY_CONTENT
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassImageContent
import org.ligi.passandroid.ui.state.DisplayableCode
import org.ligi.passandroid.ui.state.PassArtworkUiModel
import org.ligi.passandroid.ui.state.PassBarcodeUiModel
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationUiModel
import org.ligi.passandroid.ui.state.PassTimeSpanUiModel
import org.ligi.passandroid.ui.state.PassUiModel
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.Locale

class PassExportDocumentTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val departure = ZonedDateTime.of(2026, 9, 21, 17, 0, 0, 0, zone)
    private val today = LocalDate.of(2026, 9, 1)

    private val labels = PassExportLabels(
        typeLabel = "Boarding pass",
        date = "Date",
        time = "Time",
        starts = "Starts",
        ends = "Ends",
        expires = "Expires",
        location = "Location",
        notes = "Notes",
        moreInformation = "More information",
        pass = "Pass",
        createdBy = "Created by",
        footer = "PassTick",
    )

    private fun field(label: String, value: String, hint: String? = null, hidden: Boolean = false, key: String? = null) =
        PassFieldUiModel(key, label, value, hidden, hint)

    private val train = PassUiModel(
        id = "train",
        description = "Coastal Express",
        creator = "Iberian Rail",
        type = PassType.BOARDING,
        accentColor = 0xFF0C447C.toInt(),
        barcodeFormat = PassBarCodeFormat.QR_CODE,
        barcodeMessage = "IR-12A",
        barcodeAlternativeText = "12A",
        fields = listOf(
            field("From", "BCN", "primaryFields"),
            field("To", "VLC", "primaryFields"),
            field("Coach", "3", "headerFields"),
            field("Seat", "12A", "auxiliaryFields", key = "seat"),
            field("Platform", "7", "secondaryFields", key = "platform"),
            field("Terms", "No refunds", "backFields", hidden = true),
        ),
        locations = listOf(PassLocationUiModel("Barcelona Sants", 41.379, 2.140), PassLocationUiModel(null, 39.465, -0.377)),
        calendarEvent = null,
        calendarTimeSpan = PassTimeSpanUiModel(departure, departure.plusHours(3)),
        expiresAt = departure.plusDays(1),
        notes = "  Platform may change  ",
    )

    private fun build(
        pass: PassUiModel = train,
        content: PassImageContent = PassImageContent(),
        excluded: Set<String> = emptySet(),
    ) = buildPassExportDocument(pass, content, labels, Locale.US, codeOnTop = false, excludedFieldIds = excluded, today = today)

    @Test
    fun `a boarding pass shows its route and a date cell without a time`() {
        val document = build()

        assertThat(document.main).isEqualTo(ExportMain.Route(train.fields[0], train.fields[1]))
        assertThat(document.dateCells).extracting<String> { it.label }.containsExactly("Date")
        assertThat(document.dateCells.single().value).isEqualTo("Mon 21 Sep")
        assertThat(document.headerFields).extracting<String> { it.value }.containsExactly("3")
        assertThat(document.grid).extracting<String> { it.value }.containsExactly("12A", "7")
        assertThat(document.neutral).isFalse()
    }

    @Test
    fun `an event shows the title, the date with its time, and the seat cells`() {
        val event = train.copy(
            type = PassType.EVENT,
            fields = listOf(
                field("Event", "Final", "primaryFields"),
                field("Row", "4", "auxiliaryFields"),
                field("Seat", "12", "auxiliaryFields"),
                field("Gate", "B", "secondaryFields"),
            ),
        )

        val document = build(event)

        assertThat(document.main).isEqualTo(ExportMain.Value("Event", "Final", large = false))
        assertThat(document.dateCells).extracting<String> { it.value }.containsExactly("Mon 21 Sep", "17:00")
        assertThat(document.seatCells).extracting<String> { it.value }.containsExactly("4", "12")
        assertThat(document.grid).extracting<String> { it.value }.containsExactly("B")
        assertThat(document.gridColumns).hasSize(2)
    }

    @Test
    fun `a pass without type layout is neutral and lists its fields`() {
        val document = build(train.copy(type = PassType.GENERIC, fields = listOf(field("Ref", "A1"))))

        assertThat(document.neutral).isTrue()
        assertThat(document.main).isEqualTo(ExportMain.Value("", "Coastal Express", large = false))
        assertThat(document.gridColumns).hasSize(1)
    }

    @Test
    fun `a short loyalty value is large and a progress value fills the bar`() {
        val card = train.copy(type = PassType.LOYALTY, fields = listOf(field("Stamps", "3/10", "primaryFields")))

        val document = build(card)

        assertThat(document.main).isEqualTo(ExportMain.Value("Stamps", "3/10", large = true))
        assertThat(document.progress).isEqualTo(0.3f)
    }

    @Test
    fun `hidden fields appear only when selected`() {
        assertThat(build().backFields).isEmpty()
        assertThat(build(content = PassImageContent(hiddenFields = true)).backFields)
            .extracting<String> { it.value }.containsExactly("No refunds")
    }

    @Test
    fun `notes are trimmed and locations fall back to coordinates`() {
        val document = build()

        assertThat(document.notes).isEqualTo("Platform may change")
        assertThat(document.locations).containsExactly(
            ExportLocation("Barcelona Sants", "41.37900, 2.14000"),
            ExportLocation("39.46500, -0.37700", "39.46500, -0.37700"),
        )
    }

    @Test
    fun `every code of the pass is exported with its alternative text`() {
        val codes = listOf(
            PassBarcodeUiModel(PassBarCodeFormat.QR_CODE, "first", "One"),
            PassBarcodeUiModel(PassBarCodeFormat.CODE_128, "second", " "),
            PassBarcodeUiModel(null, "skipped", null),
        )

        val document = build(train.copy(barcodes = codes))

        assertThat(document.codes).containsExactly(
            DisplayableCode(PassBarCodeFormat.QR_CODE, "first", "One"),
            DisplayableCode(PassBarCodeFormat.CODE_128, "second", null),
        )
    }

    @Test
    fun `code only content has no details`() {
        val document = build(content = CODE_ONLY_CONTENT)

        assertThat(document.hasDetails).isFalse()
        assertThat(document.codes).containsExactly(DisplayableCode(PassBarCodeFormat.QR_CODE, "IR-12A", "12A"))
        assertThat(document.summary).isEmpty()
    }

    @Test
    fun `nothing selected leaves an empty document`() {
        val nothing = PassImageContent(
            artwork = false,
            details = false,
            notes = false,
            barcode = false,
            dateTime = false,
            location = false,
        )

        val document = build(content = nothing)

        assertThat(document.hasDetails).isFalse()
        assertThat(document.codes).isEmpty()
    }

    @Test
    fun `excluded fields are left out of every part`() {
        val document = build(excluded = setOf("seat", train.fields[2].exportId))

        assertThat(document.grid).extracting<String> { it.value }.containsExactly("7")
        assertThat(document.headerFields).isEmpty()
    }

    @Test
    fun `the summary lists the name, the issuer, and the full dates`() {
        val document = build()

        assertThat(document.summary).extracting<String> { it.label }
            .containsExactly("Pass", "Created by", "Starts", "Ends", "Expires")
        assertThat(document.summary[2].value).startsWith("Mon 21 Sep 2026, 17:00")
    }

    @Test
    fun `artwork shows an icon and a strip banner`() {
        val icon = PassArtworkUiModel(PassArtworkKind.ICON, byteArrayOf(1))
        val strip = PassArtworkUiModel(PassArtworkKind.STRIP, byteArrayOf(2))

        val document = build(train.copy(artwork = listOf(icon, strip)))

        assertThat(document.icon).isEqualTo(icon)
        assertThat(document.banner).isEqualTo(strip)
        assertThat(build(train.copy(artwork = listOf(icon, strip)), PassImageContent(artwork = false)).banner).isNull()
    }

    @Test
    fun `a single thumbnail is not repeated as a banner`() {
        val thumbnail = PassArtworkUiModel(PassArtworkKind.THUMBNAIL, byteArrayOf(3))

        val document = build(train.copy(artwork = listOf(thumbnail)))

        assertThat(document.icon).isEqualTo(thumbnail)
        assertThat(document.banner).isNull()
    }

    @Test
    fun `key facts take the seat, header, and grid fields`() {
        assertThat(build().keyFacts).extracting<String> { it.value }.containsExactly("3", "12A", "7")
    }
}
