package org.ligi.passandroid.maps

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassLocation

class PassbookMapsFacadeTest {
    @Test
    fun `location is encoded as a geo URI`() {
        val location = PassLocation().apply {
            lat = 41.3874
            lon = 2.1686
        }

        assertThat(geoUri(location)).isEqualTo("geo:41.3874,2.1686?q=41.3874,2.1686")
    }
}
