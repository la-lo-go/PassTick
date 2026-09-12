package org.ligi.passandroid.ui.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassBarCodeFormat

class CrispBarcodeRendererTest {
    @Test
    fun `uses whole pixel modules within the requested bounds`() {
        val bounds = 720 to 420

        val messages = mapOf(
            PassBarCodeFormat.QR_CODE to "PASS-1234-EXAMPLE",
            PassBarCodeFormat.AZTEC to "PASS-1234-EXAMPLE",
            PassBarCodeFormat.PDF_417 to "PASS-1234-EXAMPLE",
            PassBarCodeFormat.CODE_128 to "PASS-1234-EXAMPLE",
            PassBarCodeFormat.CODE_39 to "PASS-1234",
            PassBarCodeFormat.DATA_MATRIX to "PASS-1234-EXAMPLE",
            PassBarCodeFormat.EAN_8 to "96385074",
            PassBarCodeFormat.EAN_13 to "5901234123457",
            PassBarCodeFormat.ITF to "12345678901234",
        )

        messages.forEach { (format, message) ->
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
    fun `linear formats use the available height`() {
        mapOf(
            PassBarCodeFormat.CODE_39 to "PASS-1234",
            PassBarCodeFormat.CODE_128 to "PASS-1234-EXAMPLE",
            PassBarCodeFormat.EAN_8 to "96385074",
            PassBarCodeFormat.EAN_13 to "5901234123457",
            PassBarCodeFormat.ITF to "12345678901234",
        ).forEach { (format, message) ->
            val rendered = CrispBarcodeRenderer.renderMatrix(message, format, 720, 420)

            assertThat(rendered).describedAs(format.name).isNotNull
            assertThat(rendered!!.pixels.height).isEqualTo(420)
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
}
