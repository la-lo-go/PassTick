package org.ligi.passandroid.repository

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PassArtworkSelectionTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `highest density artwork is selected`() {
        val directory = temporaryFolder.newFolder()
        File(directory, "icon.png").writeText("base")
        File(directory, "icon@2x.png").writeText("double")
        val triple = File(directory, "icon@3x.png").apply { writeText("triple") }

        assertThat(bestArtworkFile(directory, PassArtworkKind.ICON)).isEqualTo(triple)
    }

    @Test
    fun `empty density variant falls back to usable artwork`() {
        val directory = temporaryFolder.newFolder()
        File(directory, "icon@3x.png").createNewFile()
        val double = File(directory, "icon@2x.png").apply { writeText("double") }
        File(directory, "icon.png").writeText("base")

        assertThat(bestArtworkFile(directory, PassArtworkKind.ICON)).isEqualTo(double)
    }

    @Test
    fun `largest decodable artwork is selected when density names are misleading`() {
        val directory = temporaryFolder.newFolder()
        val triple = File(directory, "icon@3x.png").apply { writePng(24, 24) }
        val double = File(directory, "icon@2x.png").apply { writePng(96, 96) }
        File(directory, "icon.png").apply { writePng(48, 48) }

        assertThat(triple.length()).isGreaterThan(0)
        assertThat(bestArtworkFile(directory, PassArtworkKind.ICON)).isEqualTo(double)
    }

    @Test
    fun `jpeg artwork is found when no png variant exists`() {
        val directory = temporaryFolder.newFolder()
        val jpeg = File(directory, "strip.jpg").apply { writeText("photo") }

        assertThat(bestArtworkFile(directory, PassArtworkKind.STRIP)).isEqualTo(jpeg)
    }

    @Test
    fun `png artwork wins over jpeg at the same density`() {
        val directory = temporaryFolder.newFolder()
        val png = File(directory, "strip.png").apply { writePng(48, 48) }
        File(directory, "strip.jpg").writeText("photo")

        assertThat(bestArtworkFile(directory, PassArtworkKind.STRIP)).isEqualTo(png)
    }

    private fun File.writePng(width: Int, height: Int) {
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", this)
    }
}
