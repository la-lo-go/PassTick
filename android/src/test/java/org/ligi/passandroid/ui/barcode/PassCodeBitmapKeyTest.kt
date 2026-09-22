package org.ligi.passandroid.ui.barcode

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassBarCodeFormat

class PassCodeBitmapKeyTest {
    private val format = PassBarCodeFormat.QR_CODE
    private val message = "PASS-1234-EXAMPLE"

    @Test
    fun `extra quiet zone changes the cache key`() {
        assertThat(key(options = CodeViewOptions(extraQuietZone = true)))
            .isNotEqualTo(key(options = CodeViewOptions()))
    }

    @Test
    fun `quarter turn changes the cache key`() {
        assertThat(key(options = CodeViewOptions(rotateQuarterTurn = true)))
            .isNotEqualTo(key(options = CodeViewOptions()))
    }

    @Test
    fun `size step changes the cache key through the target size`() {
        assertThat(key(widthPx = 900, heightPx = 900, options = CodeViewOptions(sizeStep = 2)))
            .isNotEqualTo(key(widthPx = 612, heightPx = 612, options = CodeViewOptions(sizeStep = 0)))
    }

    @Test
    fun `presentation only options keep the cache key`() {
        assertThat(key(options = CodeViewOptions(whiteSurround = true, keepScreenOn = false)))
            .isEqualTo(key(options = CodeViewOptions()))
    }

    private fun key(
        widthPx: Int = 720,
        heightPx: Int = 720,
        options: CodeViewOptions,
    ) = passCodeBitmapKey(format, message, widthPx, heightPx, options)
}
