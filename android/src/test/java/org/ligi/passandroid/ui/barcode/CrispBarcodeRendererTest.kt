package org.ligi.passandroid.ui.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassBarCodeFormat

class CrispBarcodeRendererTest {
    @Test
    fun `uses whole pixel modules within the requested bounds`() {
        val bounds = 720 to 420

        sampleMessages.forEach { (format, message) ->
            val rendered = CrispBarcodeRenderer.renderMatrix(
                data = message,
                format = format,
                maxWidthPx = bounds.first,
                maxHeightPx = bounds.second,
            )

            assertThat(rendered).describedAs(format.name).isNotNull
            assertThat(rendered!!.moduleScale).isGreaterThanOrEqualTo(1)
            assertThat(rendered.pixels.width).isLessThanOrEqualTo(bounds.first)
            assertThat(rendered.pixels.height).isLessThanOrEqualTo(bounds.second)
            assertTransitionsAlignToModules(rendered)
        }
    }

    @Test
    fun `extra quiet zone keeps module alignment`() {
        sampleMessages.forEach { (format, message) ->
            val rendered = CrispBarcodeRenderer.renderMatrix(
                data = message,
                format = format,
                maxWidthPx = 720,
                maxHeightPx = 420,
                extraQuietZone = true,
            )

            assertThat(rendered).describedAs(format.name).isNotNull
            assertThat(rendered!!.pixels.width).isLessThanOrEqualTo(720)
            assertThat(rendered.pixels.height).isLessThanOrEqualTo(420)
            assertTransitionsAlignToModules(rendered)
        }
    }

    @Test
    fun `extra quiet zone adds a vertical zone to linear formats`() {
        val rendered = CrispBarcodeRenderer.renderMatrix(
            "PASS-1234-EXAMPLE",
            PassBarCodeFormat.CODE_128,
            720,
            420,
            extraQuietZone = true,
        )

        assertThat(rendered).isNotNull
        assertThat(firstInkRow(rendered!!)).isGreaterThan(0)
        assertThat(lastInkRow(rendered)).isLessThan(rendered.pixels.height - 1)
    }

    @Test
    fun `linear formats have no vertical zone by default`() {
        val rendered = CrispBarcodeRenderer.renderMatrix("PASS-1234-EXAMPLE", PassBarCodeFormat.CODE_128, 720, 420)

        assertThat(rendered).isNotNull
        assertThat(rendered!!.pixels.height).isEqualTo(420)
        assertThat(firstInkRow(rendered)).isZero()
        assertThat(lastInkRow(rendered)).isEqualTo(419)
    }

    @Test
    fun `a smaller target reduces the module scale`() {
        val message = "PASS-1234-EXAMPLE-LONGER-PAYLOAD"

        val small = CrispBarcodeRenderer.renderMatrix(message, PassBarCodeFormat.QR_CODE, 612, 612)
        val normal = CrispBarcodeRenderer.renderMatrix(message, PassBarCodeFormat.QR_CODE, 720, 720)
        val large = CrispBarcodeRenderer.renderMatrix(message, PassBarCodeFormat.QR_CODE, 900, 900)

        assertThat(small).isNotNull
        assertThat(normal).isNotNull
        assertThat(large).isNotNull
        assertThat(small!!.moduleScale).isLessThan(normal!!.moduleScale)
        assertThat(normal.moduleScale).isLessThan(large!!.moduleScale)
    }

    @Test
    fun `linear formats use the available height`() {
        mapOf(
            PassBarCodeFormat.CODABAR to "A1234567890B",
            PassBarCodeFormat.CODE_39 to "PASS-1234",
            PassBarCodeFormat.CODE_93 to "TEST93",
            PassBarCodeFormat.CODE_128 to "PASS-1234-EXAMPLE",
            PassBarCodeFormat.EAN_8 to "96385074",
            PassBarCodeFormat.EAN_13 to "5901234123457",
            PassBarCodeFormat.ITF to "12345678901234",
            PassBarCodeFormat.UPC_A to "036000291452",
            PassBarCodeFormat.UPC_E to "01234565",
        ).forEach { (format, message) ->
            val rendered = CrispBarcodeRenderer.renderMatrix(message, format, 720, 420)

            assertThat(rendered).describedAs(format.name).isNotNull
            assertThat(rendered!!.pixels.height).isEqualTo(420)
        }
    }

    @Test
    fun `codabar code93 upca and upce are linear`() {
        listOf(
            PassBarCodeFormat.CODABAR,
            PassBarCodeFormat.CODE_93,
            PassBarCodeFormat.UPC_A,
            PassBarCodeFormat.UPC_E,
        ).forEach { format ->
            assertThat(CrispBarcodeRenderer.isLinear(format)).describedAs(format.name).isTrue()
        }
    }

    @Test
    fun `rejects empty data and non positive bounds`() {
        assertThat(CrispBarcodeRenderer.renderMatrix("", PassBarCodeFormat.QR_CODE, 720, 420)).isNull()
        assertThat(CrispBarcodeRenderer.renderMatrix("PASS-1234", PassBarCodeFormat.QR_CODE, 0, 420)).isNull()
        assertThat(CrispBarcodeRenderer.renderMatrix("PASS-1234", PassBarCodeFormat.QR_CODE, 720, 0)).isNull()
    }

    private fun assertTransitionsAlignToModules(rendered: RenderedBarcodeMatrix) {
        val pixels = rendered.pixels
        val scale = rendered.moduleScale
        for (y in 0 until pixels.height) {
            for (x in 1 until pixels.width) {
                if (pixels[x, y] != pixels[x - 1, y]) assertThat(x % scale).isZero()
            }
        }
        for (x in 0 until pixels.width) {
            for (y in 1 until pixels.height) {
                if (pixels[x, y] != pixels[x, y - 1]) assertThat(y % scale).isZero()
            }
        }
    }

    private fun firstInkRow(rendered: RenderedBarcodeMatrix): Int =
        (0 until rendered.pixels.height).first { y ->
            (0 until rendered.pixels.width).any { x -> rendered.pixels[x, y] }
        }

    private fun lastInkRow(rendered: RenderedBarcodeMatrix): Int =
        (rendered.pixels.height - 1 downTo 0).first { y ->
            (0 until rendered.pixels.width).any { x -> rendered.pixels[x, y] }
        }

    private val sampleMessages = mapOf(
        PassBarCodeFormat.QR_CODE to "PASS-1234-EXAMPLE",
        PassBarCodeFormat.AZTEC to "PASS-1234-EXAMPLE",
        PassBarCodeFormat.PDF_417 to "PASS-1234-EXAMPLE",
        PassBarCodeFormat.CODABAR to "A1234567890B",
        PassBarCodeFormat.CODE_128 to "PASS-1234-EXAMPLE",
        PassBarCodeFormat.CODE_39 to "PASS-1234",
        PassBarCodeFormat.CODE_93 to "TEST93",
        PassBarCodeFormat.DATA_MATRIX to "PASS-1234-EXAMPLE",
        PassBarCodeFormat.EAN_8 to "96385074",
        PassBarCodeFormat.EAN_13 to "5901234123457",
        PassBarCodeFormat.ITF to "12345678901234",
        PassBarCodeFormat.UPC_A to "036000291452",
        PassBarCodeFormat.UPC_E to "01234565",
    )
}
