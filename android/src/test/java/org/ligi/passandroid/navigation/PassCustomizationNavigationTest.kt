package org.ligi.passandroid.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import org.ligi.passandroid.repository.PassArtworkKind

class PassCustomizationNavigationTest {
    @Test
    fun `pass without artwork opens layout customization`() {
        assertEquals(AppDestination.PassDetailLayoutSettings, passCustomizationDestination("pass", emptyList()))
    }

    @Test
    fun `pass with one distinct artwork kind opens layout customization`() {
        assertEquals(
            AppDestination.PassDetailLayoutSettings,
            passCustomizationDestination("pass", listOf(PassArtworkKind.ICON, PassArtworkKind.ICON)),
        )
    }

    @Test
    fun `pass with multiple artwork kinds opens image chooser`() {
        assertEquals(
            AppDestination.PassCustomization("pass"),
            passCustomizationDestination("pass", listOf(PassArtworkKind.ICON, PassArtworkKind.LOGO)),
        )
    }
}
