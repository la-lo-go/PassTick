package org.ligi.passandroid.platform

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PlatformLocationTest {
    @Test
    fun `opens a text address without coordinates`() {
        assertThat(geoUri("Calle de Alcala 42, Madrid", null, null))
            .isEqualTo("geo:0,0?q=Calle%20de%20Alcala%2042%2C%20Madrid")
    }

    @Test
    fun `keeps coordinates while using the address as the map query`() {
        assertThat(geoUri("Atocha station", 40.4066, -3.689))
            .isEqualTo("geo:40.4066,-3.689?q=Atocha%20station")
    }
}
