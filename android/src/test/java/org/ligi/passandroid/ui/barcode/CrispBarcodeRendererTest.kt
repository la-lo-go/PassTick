package org.ligi.passandroid.ui.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassBarCodeFormat

class CrispBarcodeRendererTest {
    @Test
    fun `uses whole pixel modules within the requested bounds`() {
        val bounds = 720 to 420

        listOf(
            PassBarCodeFormat.QR_CODE,
            PassBarCodeFormat.AZTEC,
            PassBarCodeFormat.PDF_417,
            PassBarCodeFormat.CODE_128,
        ).forEach { format ->
            val rendered = CrispBarcodeRenderer.renderMatrix(
                data = "PASS-1234-EXAMPLE",
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
