package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ImageExportOptionsTest {
    private val nothingStored = StoredPassExportOptions(null, null, null, null)

    private fun roundTrip(template: PassExportTemplate, options: PassImageExportOptions) = decodePassExportOptions(
        template,
        StoredPassExportOptions(
            options.aspectRatio.name,
            options.orientation.name,
            encodePassImageContent(options.content),
            encodePassExportStyle(options),
        ),
    )

    @Test
    fun `nothing stored decodes to the template defaults`() {
        PassExportTemplate.entries.forEach { template ->
            assertThat(decodePassExportOptions(template, nothingStored)).isEqualTo(template.defaultOptions)
        }
    }

    @Test
    fun `legacy ticket values without a style keep png and color`() {
        val legacy = StoredPassExportOptions("SQUARE", "LANDSCAPE", setOf("artwork", "barcode"), null)

        val decoded = decodePassExportOptions(PassExportTemplate.TICKET, legacy)

        assertThat(decoded.aspectRatio).isEqualTo(PassImageAspectRatio.SQUARE)
        assertThat(decoded.orientation).isEqualTo(PassImageOrientation.LANDSCAPE)
        assertThat(decoded.content).isEqualTo(PassImageContent(details = false, dateTime = false, location = false))
        assertThat(decoded.format).isEqualTo(PassExportFormat.PNG)
        assertThat(decoded.monochrome).isFalse()
        assertThat(decoded.opaqueBackground).isFalse()
    }

    @Test
    fun `every option round trips`() {
        val options = PassImageExportOptions(
            aspectRatio = PassImageAspectRatio.LETTER,
            orientation = PassImageOrientation.LANDSCAPE,
            content = PassImageContent(notes = false, hiddenFields = true),
            format = PassExportFormat.PDF,
            monochrome = true,
            opaqueBackground = true,
        )

        assertThat(roundTrip(PassExportTemplate.SHEET, options)).isEqualTo(options)
    }

    @Test
    fun `an empty stored style means png without styles`() {
        val options = PassExportTemplate.SHEET.defaultOptions.copy(format = PassExportFormat.PNG)

        assertThat(roundTrip(PassExportTemplate.SHEET, options).format).isEqualTo(PassExportFormat.PNG)
    }

    @Test
    fun `a ratio that the template does not offer falls back to its default`() {
        val stored = StoredPassExportOptions("RATIO_16_9", null, null, null)

        assertThat(decodePassExportOptions(PassExportTemplate.SHEET, stored).aspectRatio).isEqualTo(PassImageAspectRatio.A4)
    }

    @Test
    fun `unknown stored values fall back to the defaults`() {
        val stored = StoredPassExportOptions("HUGE", "DIAGONAL", null, setOf("unknown"))

        val decoded = decodePassExportOptions(PassExportTemplate.TICKET, stored)

        assertThat(decoded.aspectRatio).isEqualTo(PassImageAspectRatio.AUTO_HEIGHT)
        assertThat(decoded.orientation).isEqualTo(PassImageOrientation.PORTRAIT)
    }

    @Test
    fun `the code template exports only the code by default`() {
        assertThat(PassExportTemplate.CODE.defaultOptions.content).isEqualTo(CODE_ONLY_CONTENT)
    }

    @Test
    fun `settings without stored options use the template defaults`() {
        val settings = PassExportSettings(template = PassExportTemplate.SHEET)

        assertThat(settings.selectedOptions).isEqualTo(PassExportTemplate.SHEET.defaultOptions)
    }

    @Test
    fun `content and style accessors match their copies`() {
        PassExportContent.entries.forEach { item ->
            assertThat(PassImageContent().with(item, false)[item]).isFalse()
            assertThat(PassImageContent().with(item, true)[item]).isTrue()
        }
        PassExportStyle.entries.forEach { style ->
            assertThat(PassImageExportOptions().with(style, true)[style]).isTrue()
        }
    }

    @Test
    fun `orientation applies only to fixed ratios that are not square`() {
        assertThat(PassExportTemplate.TICKET.hasOrientation(PassImageAspectRatio.RATIO_16_9)).isTrue()
        assertThat(PassExportTemplate.TICKET.hasOrientation(PassImageAspectRatio.AUTO_HEIGHT)).isFalse()
        assertThat(PassExportTemplate.TICKET.hasOrientation(PassImageAspectRatio.SQUARE)).isFalse()
        assertThat(PassExportTemplate.STORY.hasOrientation(PassImageAspectRatio.RATIO_9_16)).isFalse()
    }
}
