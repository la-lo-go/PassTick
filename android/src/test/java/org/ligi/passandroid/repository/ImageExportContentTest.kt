package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ImageExportContentTest {
    @Test
    fun `all enabled content round trips through the stored encoding`() {
        val content = PassImageContent(
            artwork = true,
            details = true,
            barcode = true,
            dateTime = true,
            location = true,
            hiddenFields = true,
        )

        assertThat(decodePassImageContent(encodePassImageContent(content))).isEqualTo(content)
    }

    @Test
    fun `all disabled content round trips through the stored encoding`() {
        val content = PassImageContent(
            artwork = false,
            details = false,
            barcode = false,
            dateTime = false,
            location = false,
            hiddenFields = false,
        )

        assertThat(decodePassImageContent(encodePassImageContent(content))).isEqualTo(content)
    }

    @Test
    fun `missing stored content decodes to the defaults`() {
        assertThat(decodePassImageContent(null)).isEqualTo(PassImageContent())
    }
}
