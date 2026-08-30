package org.ligi.passandroid.maps

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PassbookMapsFacadeTest {
    @Test
    fun `location is encoded as a geo URI`() {
        assertThat(geoUri(41.3874, 2.1686)).isEqualTo("geo:41.3874,2.1686?q=41.3874,2.1686")
    }
}
