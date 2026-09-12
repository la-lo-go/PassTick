package org.ligi.passandroid.unittest

import android.content.Context
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.reader.AppleStylePassReader
import org.mockito.Mockito
import org.threeten.bp.ZonedDateTime
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class TheAppleStylePassReader {
    private val context = Mockito.mock(Context::class.java)
    private val tracker = Mockito.mock(Tracker::class.java)

    @Test
    fun `reads an event pass with barcode dates and locations`() {
        val passFile = passDirectory(
            """
            {
              "organizationName": "Test Transit",
              "serialNumber": "SN-1",
              "authenticationToken": "token",
              "webServiceURL": "https://example.test",
              "passTypeIdentifier": "pass.test",
              "description": "Train ticket",
              "eventTicket": {
                "primaryFields": [{ "key": "depart", "label": "Departure", "value": "10:00" }]
              },
              "barcode": { "format": "PKBarcodeFormatQR", "message": "PAYLOAD", "altText": "Show this" },
              "relevantDate": "2026-09-01T10:00:00+02:00",
              "expirationDate": "2026-09-02T10:00:00+02:00",
              "locations": [{ "latitude": 40.4, "longitude": -3.7, "relevantText": "Station" }]
            }
            """.trimIndent(),
        )

        val loaded = AppleStylePassReader.read(passFile, "en", context, tracker)!!

        assertThat(loaded.serial).isEqualTo("SN-1")
        assertThat(loaded.authToken).isEqualTo("token")
        assertThat(loaded.webServiceURL).isEqualTo("https://example.test")
        assertThat(loaded.passIdent).isEqualTo("pass.test")
        assertThat(loaded.description).isEqualTo("Train ticket")
        assertThat(loaded.type).isEqualTo(PassType.EVENT)
        assertThat(loaded.creator).isEqualTo("Test Transit")
        assertThat(loaded.barCode!!.format).isEqualTo(PassBarCodeFormat.QR_CODE)
        assertThat(loaded.barCode!!.message).isEqualTo("PAYLOAD")
        assertThat(loaded.barCode!!.alternativeText).isEqualTo("Show this")
        assertThat(loaded.calendarTimespan!!.from).isEqualTo(ZonedDateTime.parse("2026-09-01T10:00:00+02:00"))
        assertThat(loaded.validTimespans!!.single().to).isEqualTo(ZonedDateTime.parse("2026-09-02T10:00:00+02:00"))
        assertThat(loaded.locations.single().name).isEqualTo("Station")
        assertThat(loaded.locations.single().lat).isEqualTo(40.4)
        assertThat(loaded.locations.single().lon).isEqualTo(-3.7)
        assertThat(loaded.fields.map { it.key }).contains("depart")
    }

    @Test
    fun `returns null without a pass json`() {
        val directory = Files.createTempDirectory("pass-reader").toFile()

        val pass = AppleStylePassReader.read(directory, "en", context, tracker)

        assertThat(pass).isNull()
    }

    private fun passDirectory(json: String): File {
        val directory = Files.createTempDirectory("pass-reader").toFile()
        File(directory, "pass.json").writeText(json, StandardCharsets.UTF_8)
        return directory
    }
}
