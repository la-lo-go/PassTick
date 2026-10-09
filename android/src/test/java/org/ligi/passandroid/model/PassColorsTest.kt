package org.ligi.passandroid.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PassColorsTest {

    @Test
    fun `hex colors keep the forms the old parser accepted`() {
        assertThat(parsePassColor("#F2600F")).isEqualTo(0xFFF2600F.toInt())
        assertThat(parsePassColor("#80F2600F")).isEqualTo(0x80F2600F.toInt())
        assertThat(parsePassColor("red")).isEqualTo(0xFFFF0000.toInt())
    }

    @Test
    fun `apple rgb colors parse with and without spaces`() {
        assertThat(parsePassColor("rgb(198, 0, 0)")).isEqualTo(0xFFC60000.toInt())
        assertThat(parsePassColor("rgb(255,204,0)")).isEqualTo(0xFFFFCC00.toInt())
        assertThat(parsePassColor(" RGBA(1, 2, 3, 0.5) ")).isEqualTo(0xFF010203.toInt())
        assertThat(parsePassColor("#ABC")).isEqualTo(0xFFAABBCC.toInt())
    }

    @Test
    fun `invalid colors return null`() {
        assertThat(parsePassColor("rgb(1, 2)")).isNull()
        assertThat(parsePassColor("#12345")).isNull()
        assertThat(parsePassColor("purple-ish")).isNull()
    }

    @Test
    fun `a colored background wins over the artwork`() {
        val red = 0xFFC60000.toInt()

        assertThat(resolvePassAccentColor(red, null, null) { error("artwork must not be read") }).isEqualTo(red)
    }

    @Test
    fun `a grey background gives way to the artwork hue, then to the label color`() {
        val grey = 0xFFD4D4D4.toInt()
        val magenta = 0xFFCC0099.toInt()
        val purple = 0xFF7A1F6B.toInt()

        assertThat(resolvePassAccentColor(grey, magenta, null) { purple }).isEqualTo(purple)
        assertThat(resolvePassAccentColor(grey, magenta, null) { null }).isEqualTo(magenta)
        assertThat(resolvePassAccentColor(grey, 0xFF000000.toInt(), null) { null }).isEqualTo(grey)
        assertThat(resolvePassAccentColor(null, null, null) { null }).isNull()
    }

    @Test
    fun `artwork color follows the main hue and ignores white text and transparency`() {
        val purple = 0xFF7A1F6B.toInt()
        val pixels = IntArray(100) { index ->
            when {
                index < 70 -> purple
                index < 90 -> 0xFFFFFFFF.toInt()
                else -> 0x00000000
            }
        }

        assertThat(dominantArtworkColor(pixels)).isEqualTo(purple)
    }

    @Test
    fun `a small colored detail is not a brand color`() {
        val pixels = IntArray(100) { index -> if (index < 10) 0xFFFF0000.toInt() else 0xFF222222.toInt() }

        assertThat(dominantArtworkColor(pixels)).isNull()
    }
}
