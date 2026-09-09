package org.ligi.passandroid.platform

import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.ligi.passandroid.functions.decodeBarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.ui.state.PassArtworkUiModel
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationUiModel
import org.ligi.passandroid.ui.state.PassUiModel
import org.ligi.passandroid.repository.PassArtworkKind
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class PassImageExporterTest {
    private val pass = PassUiModel(
        id = "export-test",
        description = "A very long pass title used to verify wrapped output",
        creator = "PassTick",
        type = org.ligi.passandroid.model.pass.PassType.EVENT,
        accentColor = 0xff102f4f.toInt(),
        barcodeFormat = PassBarCodeFormat.QR_CODE,
        barcodeMessage = "export-round-trip",
        barcodeAlternativeText = null,
        fields = (1..12).map { PassFieldUiModel("key$it", "Field $it", "Long value $it ".repeat(12), false, null) },
        locations = listOf(PassLocationUiModel("Madrid", 40.4, -3.7)),
        calendarEvent = null,
    )

    @Test
    fun barcodePngRoundTripsThroughDecoder() {
        val shareDir = File(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir, "share").apply { mkdirs() }
        val output = File(shareDir, "export-test-${System.currentTimeMillis()}.png")
        val uri = FileProvider.getUriForFile(ApplicationProvider.getApplicationContext<android.content.Context>(), "dev.lalogo.passtick.fileprovider", output)
        try {
            PassImageExporter.write(ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver, uri, pass, PassImageExportMode.BARCODE)
            val bitmap = android.graphics.BitmapFactory.decodeFile(output.absolutePath)
            assertThat(bitmap.decodeBarCode()).isEqualTo("export-round-trip")
            bitmap.recycle()
        } finally { output.delete() }
    }

    @Test
    fun fullExportIncludesAllFieldsWithoutFixedEightLimit() {
        val bitmap = PassImageExporter.renderBitmap(pass, PassImageExportMode.FULL)
        assertThat(bitmap.height).isGreaterThan(12 * 52)
        bitmap.recycle()
    }

    @Test
    fun fullExportWithArtworkKeepsBarcodeAndAspectRatio() {
        val artwork = android.graphics.Bitmap.createBitmap(300, 100, android.graphics.Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().also { artwork.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it); artwork.recycle() }.toByteArray()
        val bitmap = PassImageExporter.renderBitmap(pass.copy(artwork = listOf(PassArtworkUiModel(PassArtworkKind.LOGO, bytes))), PassImageExportMode.FULL)
        assertThat(bitmap.width).isEqualTo(1200)
        assertThat(bitmap.height).isGreaterThan(500)
        bitmap.recycle()
    }

    @Test(expected = IllegalArgumentException::class)
    fun customExportRejectsEmptySelection() {
        PassImageExporter.renderBitmap(
            pass,
            PassImageExportMode.CUSTOM,
            PassImageExportSelection(artwork = false, text = false, barcode = false),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun barcodeExportRequiresBarcode() {
        PassImageExporter.renderBitmap(pass.copy(barcodeMessage = null), PassImageExportMode.BARCODE)
    }
}
