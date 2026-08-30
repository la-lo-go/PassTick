package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PassImportTypesTest {
    @Test
    fun `supports pass image pdf and generic document providers`() {
        assertThat(supportedPassImportMimeTypes).contains(
            "application/vnd.apple.pkpass",
            "application/vnd.espass-espass+zip",
            "image/*",
            "application/pdf",
            "application/octet-stream",
        )
    }
}
