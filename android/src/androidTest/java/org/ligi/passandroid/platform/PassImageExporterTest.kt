package org.ligi.passandroid.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.ligi.passandroid.export.PassExportRequest
import org.ligi.passandroid.functions.decodeBarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.CODE_ONLY_CONTENT
import org.ligi.passandroid.repository.PassArtworkKind
import org.ligi.passandroid.repository.PassExportFormat
import org.ligi.passandroid.repository.PassExportTemplate
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
    private val context = ApplicationProvider.getApplicationContext<Context>()

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

    private val labels = exportLabels(context, pass)

    private fun request(
        template: PassExportTemplate = PassExportTemplate.TICKET,
        options: PassImageExportOptions = template.defaultOptions,
    ) = PassExportRequest(template, options)

    private fun render(request: PassExportRequest, model: PassUiModel = pass) = PassImageExporter.renderBitmap(model, request, labels)

    @Test
    fun codeTemplatePngRoundTripsThroughDecoder() {
        val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
        val output = File(shareDir, "export-test-${System.currentTimeMillis()}.png")
        val uri = FileProvider.getUriForFile(context, "dev.lalogo.passtick.fileprovider", output)
        try {
            PassImageExporter.write(context.contentResolver, uri, pass, request(PassExportTemplate.CODE), labels)
            val bitmap = BitmapFactory.decodeFile(output.absolutePath)
            assertThat(bitmap.decodeBarCode()).isEqualTo("export-round-trip")
            bitmap.recycle()
        } finally {
            output.delete()
        }
    }

    @Test
    fun everyTemplateDrawsADecodableCode() {
        PassExportTemplate.entries.forEach { template ->
            val options = template.defaultOptions.copy(opaqueBackground = true)
            val bitmap = render(request(template, options))
            assertThat(bitmap.decodeBarCode()).describedAs(template.name).isEqualTo("export-round-trip")
            bitmap.recycle()
        }
    }

    @Test
    fun ticketHasATransparentBackgroundAndOpaqueContent() {
        val bitmap = render(request())
        assertThat(alphaAt(bitmap, 0, 0)).isEqualTo(0)
        assertThat(alphaAt(bitmap, bitmap.width - 1, 0)).isEqualTo(0)
        assertThat(alphaAt(bitmap, bitmap.width / 2, bitmap.height / 2)).isEqualTo(255)
        assertThat(hasWhite(bitmap)).isTrue()
        bitmap.recycle()
    }

    @Test
    fun opaqueBackgroundFillsTheCorners() {
        val bitmap = render(request(options = PassImageExportOptions(opaqueBackground = true)))
        assertThat(alphaAt(bitmap, 0, 0)).isEqualTo(255)
        bitmap.recycle()
    }

    @Test
    fun artworkRendersAtTheExportWidth() {
        val artwork = createBitmap(300, 100)
        val bytes = ByteArrayOutputStream().also {
            artwork.compress(Bitmap.CompressFormat.PNG, 100, it)
            artwork.recycle()
        }.toByteArray()
        val bitmap = render(request(), pass.copy(artwork = listOf(PassArtworkUiModel(PassArtworkKind.LOGO, bytes))))
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
        val sparseBitmap = render(request(), sparse)
        val richBitmap = render(request())
        val squareBitmap = render(request(options = PassImageExportOptions(aspectRatio = PassImageAspectRatio.SQUARE)))
        assertThat(richBitmap.height).isGreaterThan(sparseBitmap.height)
        assertThat(richBitmap.height).isGreaterThan(squareBitmap.height)
        sparseBitmap.recycle()
        richBitmap.recycle()
        squareBitmap.recycle()
    }

    @Test
    fun squarePortraitProducesEqualWidthAndHeight() {
        val bitmap = render(request(options = PassImageExportOptions(aspectRatio = PassImageAspectRatio.SQUARE)))
        assertThat(bitmap.width).isEqualTo(bitmap.height)
        bitmap.recycle()
    }

    @Test
    fun landscapeTurnsNineSixteenWiderThanTall() {
        val landscape = render(
            request(options = PassImageExportOptions(aspectRatio = PassImageAspectRatio.RATIO_9_16, orientation = PassImageOrientation.LANDSCAPE)),
        )
        val portrait = render(
            request(options = PassImageExportOptions(aspectRatio = PassImageAspectRatio.RATIO_9_16, orientation = PassImageOrientation.PORTRAIT)),
        )
        assertThat(landscape.width).isGreaterThan(landscape.height)
        assertThat(portrait.height).isGreaterThan(portrait.width)
        landscape.recycle()
        portrait.recycle()
    }

    @Test
    fun everyTemplateRatioAndOrientationHasTheExpectedShapeInPngAndPdf() {
        PassExportTemplate.entries.forEach { template ->
            template.aspectRatios.filter { it != PassImageAspectRatio.AUTO_HEIGHT }.forEach { ratio ->
                val orientations = if (template.hasOrientation(ratio)) PassImageOrientation.entries else listOf(PassImageOrientation.PORTRAIT)
                orientations.forEach { orientation ->
                    val long = maxOf(ratio.ratioWidth, ratio.ratioHeight)
                    val short = minOf(ratio.ratioWidth, ratio.ratioHeight)
                    val expected = if (orientation == PassImageOrientation.PORTRAIT) short / long else long / short
                    val options = template.defaultOptions.copy(aspectRatio = ratio, orientation = orientation)
                    val name = "$template $ratio $orientation"

                    val bitmap = render(request(template, options.copy(format = PassExportFormat.PNG)))
                    assertThat(bitmap.width.toFloat() / bitmap.height).describedAs(name).isCloseTo(expected, org.assertj.core.data.Offset.offset(0.01f))
                    bitmap.recycle()

                    val file = File(context.cacheDir, "ratio-test.pdf")
                    file.outputStream().use { PassImageExporter.write(it, pass, request(template, options.copy(format = PassExportFormat.PDF)), labels) }
                    val (width, height) = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { renderer -> renderer.openPage(0).use { it.width to it.height } }
                    }
                    file.delete()
                    assertThat(width.toFloat() / height).describedAs("$name PDF").isCloseTo(expected, org.assertj.core.data.Offset.offset(0.01f))
                }
            }
        }
    }

    @Test
    fun storyIsNineBySixteen() {
        val bitmap = render(request(PassExportTemplate.STORY))
        assertThat(bitmap.height.toFloat() / bitmap.width).isCloseTo(16f / 9f, org.assertj.core.data.Offset.offset(0.01f))
        bitmap.recycle()
    }

    @Test
    fun sheetPngHasThePageRatio() {
        val a4 = render(request(PassExportTemplate.SHEET, PassExportTemplate.SHEET.defaultOptions.copy(format = PassExportFormat.PNG)))
        assertThat(a4.height.toFloat() / a4.width).isCloseTo(842f / 595f, org.assertj.core.data.Offset.offset(0.01f))
        a4.recycle()
    }

    @Test
    fun ticketWithOnlyTheCodeStillDecodes() {
        val bitmap = render(request(options = PassImageExportOptions(content = CODE_ONLY_CONTENT, opaqueBackground = true)))
        assertThat(bitmap.decodeBarCode()).isEqualTo("export-round-trip")
        bitmap.recycle()
    }

    @Test
    fun notesMakeTheTicketTaller() {
        val options = PassImageExportOptions(
            content = PassImageContent(artwork = false, details = false, barcode = false, dateTime = false, location = false),
        )
        val withoutNotes = render(request(options = options))
        val withNotes = render(request(options = options), pass.copy(notes = "Gate opens at six\nRow 1, seat 12A"))
        assertThat(withNotes.height).isGreaterThan(withoutNotes.height)
        withoutNotes.recycle()
        withNotes.recycle()
    }

    @Test
    fun emptyContentStillRendersATicket() {
        val nothing = PassImageContent(artwork = false, details = false, notes = false, barcode = false, dateTime = false, location = false)
        val bitmap = render(request(options = PassImageExportOptions(content = nothing)))
        assertThat(bitmap.width).isEqualTo(PassImageExporter.EXPORT_WIDTH)
        assertThat(bitmap.height).isGreaterThan(PassImageExporter.EXPORT_WIDTH / 10)
        bitmap.recycle()
    }

    @Test
    fun pdfHasADecodableVectorCode() {
        val file = File(context.cacheDir, "export-test.pdf")
        try {
            file.outputStream().use {
                PassImageExporter.write(it, pass, request(options = PassImageExportOptions(format = PassExportFormat.PDF)), labels)
            }
            assertThat(file.readBytes().decodeToString(0, 4)).isEqualTo("%PDF")
            val page = renderPdfPage(file, 0, 1600)
            assertThat(page.decodeBarCode()).isEqualTo("export-round-trip")
            page.recycle()
        } finally {
            file.delete()
        }
    }

    @Test
    fun longSheetPdfContinuesOnMorePages() {
        val longNotes = (1..60).joinToString("\n\n") { "Paragraph $it. " + "Some long note text. ".repeat(8) }
        val sheet = request(PassExportTemplate.SHEET)
        val short = pass.copy(fields = emptyList(), description = "Short pass")
        assertThat(PassImageExporter.pageCount(short, sheet, labels)).isEqualTo(1)
        assertThat(PassImageExporter.pageCount(short.copy(notes = longNotes), sheet, labels)).isGreaterThan(1)
    }

    @Test
    fun writesPngIntoTheGallery() {
        val uri = PassImageExporter.writeToGallery(
            context.contentResolver,
            "pass-tick-export-test.png",
            pass,
            request(PassExportTemplate.CODE),
            labels,
        )
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            assertThat(bitmap).isNotNull()
            bitmap?.recycle()
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }

    private fun renderPdfPage(file: File, index: Int, width: Int): Bitmap =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderer.openPage(index).use { page ->
                    val bitmap = createBitmap(width, width * page.height / page.width)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }

    private fun alphaAt(bitmap: Bitmap, x: Int, y: Int): Int = Color.alpha(bitmap.getPixel(x, y))

    private fun hasWhite(bitmap: Bitmap): Boolean {
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                if (bitmap.getPixel(x, y) == Color.WHITE) return true
                x += 16
            }
            y += 16
        }
        return false
    }
}
