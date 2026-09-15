package org.ligi.passandroid.ui.theme

import androidx.compose.ui.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.repository.ColorStyle

class ColorSchemesTest {
    private val seed = Color(0xFF1E88E5)

    @Test
    fun distinctSeedsProduceDistinctPrimaries() {
        val seeds = listOf(0xFFE53935L, 0xFF1E88E5L, 0xFF43A047L, 0xFFFFB300L, 0xFF8E24AAL)

        val primaries = seeds.map {
            generateAccentColorScheme(Color(it), dark = false, style = ColorStyle.TONAL_SPOT).primary
        }

        assertThat(primaries.distinct()).hasSameSizeAs(primaries)
    }

    @Test
    fun styleChangesTheGeneratedScheme() {
        val tonalSpot = generateAccentColorScheme(seed, dark = false, style = ColorStyle.TONAL_SPOT)
        val vibrant = generateAccentColorScheme(seed, dark = false, style = ColorStyle.VIBRANT)

        assertThat(vibrant).isNotEqualTo(tonalSpot)
    }

    @Test
    fun darkAndLightSchemesDiffer() {
        val light = generateAccentColorScheme(seed, dark = false, style = ColorStyle.TONAL_SPOT)
        val dark = generateAccentColorScheme(seed, dark = true, style = ColorStyle.TONAL_SPOT)

        assertThat(dark.background).isNotEqualTo(light.background)
        assertThat(dark.primary).isNotEqualTo(light.primary)
    }

    @Test
    fun everyStyleGeneratesAllSurfaceRoles() {
        ColorStyle.entries.forEach { style ->
            val scheme = generateAccentColorScheme(Color(0xFF006C4C), dark = false, style = style)

            assertThat(scheme.primaryContainer).isNotNull
            assertThat(scheme.surfaceContainerLow).isNotNull
            assertThat(scheme.onSurfaceVariant).isNotNull
        }
    }
}
