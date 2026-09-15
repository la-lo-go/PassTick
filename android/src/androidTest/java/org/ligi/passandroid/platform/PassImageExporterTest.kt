package org.ligi.passandroid.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.ligi.passandroid.functions.decodeBarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassImageAspectRatio
import org.ligi.passandroid.repository.PassImageContent
import org.ligi.passandroid.repository.PassImageExportOptions
import org.ligi.passandroid.repository.PassImageOrientation
import org.ligi.passandroid.ui.state.PassArtworkUiModel
import org.ligi.passandroid.ui.state.PassFieldUiModel
import org.ligi.passandroid.ui.state.PassLocationUiModel
import org.ligi.passandroid.ui.state.PassTimeSpanUiModel
import org.ligi.passandroid.ui.state.PassUiModel
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class PassImageExporterTest {
    private val pass = PassUiModel(
        id = "export-test",
        description = "A very long pass title used to verify wrapped output",
        creator = "PassTick",
        type = PassType.EVENT,
        accentColor = 0xff102f4f.toInt(),
        barcodeFormat = PassBarCodeFormat.QR_CODE,
        barcodeMessage = "export-round-trip",
        barcodeAlternativeText = null,
        fields = (1..12).map { PassFieldUiModel("key$it", "Field $it", "Long value $it ".repeat(12), false, null) },
        locations = listOf(PassLocationUiModel("Madrid", 40.4, -3.7)),
        calendarEvent = null,
        calendarTimeSpan = PassTimeSpanUiModel(
            from = ZonedDateTime.of(2026, 9, 10, 10, 15, 0, 0, ZoneId.of("Europe/Madrid")),
            to = ZonedDateTime.of(2026, 9, 10, 12, 15, 0, 0, ZoneId.of("Europe/Madrid")),
        ),
    )

    private val barcodeOnly = PassImageContent(
        artwork = false,
        details = false,
        barcode = true,
        dateTime = false,
        location = false,
    )

    @Test
    fun barcodePngRoundTripsThroughDecoder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
        val output = File(shareDir, "export-test-${System.currentTimeMillis()}.png")
        val uri = FileProvider.getUriForFile(context, "dev.lalogo.passtick.fileprovider", output)
        try {
            PassImageExporter.write(
                context.contentResolver,
                uri,
                pass,
                PassImageExportOptions(content = barcodeOnly),
            )
            val bitmap = BitmapFactory.decodeFile(output.absolutePath)
            assertThat(bitmap.decodeBarCode()).isEqualTo("export-round-trip")
            bitmap.recycle()
        } finally {
            output.delete()
        }
    }

    @Test
    fun fullOptionsRenderTransparentBackgroundAndWhiteCardCenter() {
        val bitmap = PassImageExporter.renderBitmap(pass, PassImageExportOptions())
        assertThat(alphaAt(bitmap, 0, 0)).isEqualTo(0)
        assertThat(alphaAt(bitmap, bitmap.width - 1, 0)).isEqualTo(0)
        assertThat(hasOpaqueWhiteInCenter(bitmap)).isTrue()
        bitmap.recycle()
    }

    @Test
    fun artworkRoundTripsWhenPreferredArtworkIsPresent() {
        val artwork = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().also {
            artwork.compress(Bitmap.CompressFormat.PNG, 100, it)
            artwork.recycle()
        }.toByteArray()
        val bitmap = PassImageExporter.renderBitmap(
            pass.copy(artwork = listOf(PassArtworkUiModel(PassArtworkKind.LOGO, bytes))),
            PassImageExportOptions(),
        )
        assertThat(bitmap.width).isEqualTo(PassImageExporter.EXPORT_WIDTH)
        bitmap.recycle()
    }

    @Test
    fun autoHeightGrowsWithContent() {
        val sparse = pass.copy(
            fields = listOf(PassFieldUiModel("key", "Field", "value", false, null)),
            locations = emptyList(),
            calendarTimeSpan = null,
            barcodeMessage = null,
        )
        val sparseBitmap = PassImageExporter.renderBitmap(sparse, PassImageExportOptions())
        val richBitmap = PassImageExporter.renderBitmap(pass, PassImageExportOptions())
        val squareBitmap = PassImageExporter.renderBitmap(
            pass,
            PassImageExportOptions(aspectRatio = PassImageAspectRatio.SQUARE, orientation = PassImageOrientation.PORTRAIT),
        )
        assertThat(richBitmap.height).isGreaterThan(sparseBitmap.height)
        assertThat(richBitmap.height).isGreaterThan(squareBitmap.height)
        sparseBitmap.recycle()
        richBitmap.recycle()
        squareBitmap.recycle()
    }

    @Test
    fun squarePortraitProducesEqualWidthAndHeight() {
        val bitmap = PassImageExporter.renderBitmap(
            pass,
            PassImageExportOptions(
                aspectRatio = PassImageAspectRatio.SQUARE,
                orientation = PassImageOrientation.PORTRAIT,
            ),
        )
        assertThat(bitmap.width).isEqualTo(bitmap.height)
        bitmap.recycle()
    }

    @Test
    fun landscapeFlipsSixteenNineToWiderThanTall() {
        val landscape = PassImageExporter.renderBitmap(
            pass,
            PassImageExportOptions(
                aspectRatio = PassImageAspectRatio.RATIO_16_9,
                orientation = PassImageOrientation.LANDSCAPE,
            ),
        )
        val portrait = PassImageExporter.renderBitmap(
            pass,
            PassImageExportOptions(
                aspectRatio = PassImageAspectRatio.RATIO_16_9,
                orientation = PassImageOrientation.PORTRAIT,
            ),
        )
        assertThat(landscape.width).isGreaterThan(landscape.height)
        assertThat(portrait.height).isGreaterThan(portrait.width)
        landscape.recycle()
        portrait.recycle()
    }

    @Test
    fun barcodeOnlyContentWithNoTextStillDecodes() {
        val bitmap = PassImageExporter.renderBitmap(pass, PassImageExportOptions(content = barcodeOnly))
        assertThat(bitmap.decodeBarCode()).isEqualTo("export-round-trip")
        bitmap.recycle()
    }

    @Test
    fun notesRenderAsTheirOwnBlockWhenEnabled() {
        val options = PassImageExportOptions(
            content = PassImageContent(artwork = false, details = false, barcode = false, dateTime = false, location = false),
        )
        val withoutNotes = PassImageExporter.renderBitmap(pass, options)
        val withNotes = PassImageExporter.renderBitmap(pass.copy(notes = "Gate opens at six\nRow 1, seat 12A"), options)

        assertThat(withNotes.height).isGreaterThan(withoutNotes.height)
        withoutNotes.recycle()
        withNotes.recycle()
    }

    @Test
    fun emptyContentStillRendersACard() {
        val bitmap = PassImageExporter.renderBitmap(
            pass,
            PassImageExportOptions(
                content = PassImageContent(
                    artwork = false,
                    details = false,
                    barcode = false,
                    dateTime = false,
                    location = false,
                ),
            ),
        )
        assertThat(bitmap.width).isEqualTo(PassImageExporter.EXPORT_WIDTH)
        assertThat(bitmap.height).isGreaterThan(0)
        assertThat(hasOpaqueWhiteInCenter(bitmap)).isTrue()
        bitmap.recycle()
    }

    @Test
    fun writesPngIntoTheGallery() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = PassImageExporter.writeToGallery(
            context.contentResolver,
            "pass-tick-export-test.png",
            pass,
            PassImageExportOptions(content = barcodeOnly),
        )
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            assertThat(bitmap).isNotNull()
            bitmap?.recycle()
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }

    private fun alphaAt(bitmap: Bitmap, x: Int, y: Int): Int = Color.alpha(bitmap.getPixel(x, y))

    private fun hasOpaqueWhiteInCenter(bitmap: Bitmap): Boolean {
        val left = bitmap.width / 4
        val right = bitmap.width * 3 / 4
        val top = bitmap.height / 4
        val bottom = bitmap.height * 3 / 4
        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                if (bitmap.getPixel(x, y) == Color.WHITE) return true
                x += 8
            }
            y += 8
        }
        return false
    }
}
