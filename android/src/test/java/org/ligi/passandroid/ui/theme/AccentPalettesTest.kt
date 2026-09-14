package org.ligi.passandroid.ui.theme

import androidx.compose.ui.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.repository.AccentPalette

class AccentPalettesTest {
    @Test
    fun dynamicPaletteYieldsNoScheme() {
        assertThat(accentColorSchemes(AccentPalette.DYNAMIC)).isNull()
        assertThat(accentSwatchColor(AccentPalette.DYNAMIC)).isNull()
    }

    @Test
    fun everyFixedPaletteYieldsLightAndDarkSchemes() {
        AccentPalette.entries.filterNot { it == AccentPalette.DYNAMIC }.forEach { palette ->
            val schemes = accentColorSchemes(palette)
            assertThat(schemes).isNotNull
            assertThat(accentSwatchColor(palette)).isNotNull
        }
    }

    @Test
    fun lightPrimariesAreDistinctAcrossPalettes() {
        val primaries = AccentPalette.entries
            .filterNot { it == AccentPalette.DYNAMIC }
            .map { accentColorSchemes(it)!!.first.primary }
        assertThat(primaries.distinct()).hasSameSizeAs(primaries)
    }

    @Test
    fun darkPrimariesAreDistinctAcrossPalettes() {
        val primaries = AccentPalette.entries
            .filterNot { it == AccentPalette.DYNAMIC }
            .map { accentColorSchemes(it)!!.second.primary }
        assertThat(primaries.distinct()).hasSameSizeAs(primaries)
    }

    @Test
    fun amoledBackgroundCopyKeepsTheCuratedPrimary() {
        val (_, dark) = accentColorSchemes(AccentPalette.GREEN)!!
        val amoled = dark.copy(background = Color.Black, surface = Color.Black)
        assertThat(amoled.background).isEqualTo(Color.Black)
        assertThat(amoled.primary).isEqualTo(dark.primary)
    }

    @Test
    fun bluePaletteKeepsTheHistoricalBaselinePrimaries() {
        val (light, dark) = accentColorSchemes(AccentPalette.BLUE)!!
        assertThat(light.primary).isEqualTo(Color(0xFF2859C5))
        assertThat(dark.primary).isEqualTo(Color(0xFFB2C5FF))
    }
}
