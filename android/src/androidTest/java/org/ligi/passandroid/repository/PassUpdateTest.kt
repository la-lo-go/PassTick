package org.ligi.passandroid.repository

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.TestApp
import org.ligi.passandroid.injections.FixedPassListPassStore
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassType
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.io.File

/** An update writes the editor draft into the stored pass. The stores live in the test cache, not in the app data. */
class PassUpdateTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun update(pass: PassImpl, directoryName: String, update: PassUpdate): File {
        val directory = File(context.cacheDir, directoryName).apply {
            deleteRecursively()
            mkdirs()
        }
        val store = FixedPassListPassStore(listOf(pass)).apply { pathForId = directory }
        runBlocking { FilePassRepository(context, store, TestApp.tracker).update(pass.id, update) }
        return directory
    }

    private fun basicUpdate() = PassUpdate(
        description = "Concert",
        creator = "Hall",
        type = PassType.EVENT,
        accentColor = 0xFF7B1E4A.toInt(),
        fields = emptyList(),
    )

    @Test
    fun updateWritesTheTextFieldsDatesAndPlaces() {
        val pass = PassImpl("update-basics")
        val start = ZonedDateTime.of(2026, 11, 20, 20, 0, 0, 0, ZoneId.of("Europe/Madrid"))

        update(
            pass,
            "update-basics",
            basicUpdate().copy(
                fields = listOf(
                    PassFieldSnapshot("row", "Row", "3", false, "auxiliaryFields"),
                    PassFieldSnapshot("terms", "Terms", "No refunds", true, "backFields"),
                ),
                calendarTimeSpan = PassTimeSpanSnapshot(start, start.plusHours(2)),
                locations = listOf(PassLocationSnapshot("Puerta del Sol", 40.4168, -3.7038)),
            ),
        )

        assertThat(pass.description).isEqualTo("Concert")
        assertThat(pass.creator).isEqualTo("Hall")
        assertThat(pass.type).isEqualTo(PassType.EVENT)
        assertThat(pass.fields.map { Triple(it.label, it.value, it.hide) })
            .containsExactly(Triple("Row", "3", false), Triple("Terms", "No refunds", true))
        assertThat(pass.calendarTimespan?.from).isEqualTo(start)
        assertThat(pass.locations.single().name).isEqualTo("Puerta del Sol")
    }

    @Test
    fun updateKeepsEveryCodeInOrder() {
        val pass = PassImpl("update-codes")

        update(
            pass,
            "update-codes",
            basicUpdate().copy(
                codes = listOf(
                    PassBarcodeSnapshot(PassBarCodeFormat.QR_CODE, "first", "Ticket 1"),
                    PassBarcodeSnapshot(PassBarCodeFormat.CODE_128, "second", ""),
                    PassBarcodeSnapshot(PassBarCodeFormat.AZTEC, "third", null),
                ),
            ),
        )

        assertThat(pass.barCode?.message).isEqualTo("first")
        assertThat(pass.barCode?.alternativeText).isEqualTo("Ticket 1")
        assertThat(pass.barCodes.map { it.format to it.message })
            .containsExactly(PassBarCodeFormat.CODE_128 to "second", PassBarCodeFormat.AZTEC to "third")
        assertThat(pass.barCodes.first().alternativeText).isNull()
    }

    @Test
    fun updateRemovesTheSelectedImages() {
        val pass = PassImpl("update-images")
        val directory = File(context.cacheDir, "update-images").apply {
            deleteRecursively()
            mkdirs()
        }
        File(directory, "logo.png").writeBytes(byteArrayOf(1))
        File(directory, "logo@2x.png").writeBytes(byteArrayOf(1))
        File(directory, "icon.png").writeBytes(byteArrayOf(1))
        val store = FixedPassListPassStore(listOf(pass)).apply { pathForId = directory }

        runBlocking {
            FilePassRepository(context, store, TestApp.tracker)
                .update(pass.id, basicUpdate().copy(artworkRemovals = setOf(PassArtworkKind.LOGO)))
        }

        assertThat(File(directory, "logo.png")).doesNotExist()
        assertThat(File(directory, "logo@2x.png")).doesNotExist()
        assertThat(File(directory, "icon.png")).exists()
    }

    @Test
    fun updateWithoutACodeRemovesTheCodes() {
        val pass = PassImpl("update-no-code").apply {
            barCode = BarCode(PassBarCodeFormat.QR_CODE, "old")
            barCodes = mutableListOf(BarCode(PassBarCodeFormat.QR_CODE, "old-2"))
        }

        update(pass, "update-no-code", basicUpdate())

        assertThat(pass.barCode).isNull()
        assertThat(pass.barCodes).isEmpty()
    }
}
