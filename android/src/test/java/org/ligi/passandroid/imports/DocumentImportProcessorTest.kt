package org.ligi.passandroid.imports

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class DocumentImportProcessorTest {

    private val now = ZonedDateTime.of(2026, 9, 18, 14, 30, 0, 0, ZoneId.of("Europe/Madrid"))
    private val fallbackLabel = "Photo"
    private val fallback =
        "$fallbackLabel · ${now.format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm"))}"

    @Test
    fun `keeps a meaningful display name`() {
        assertThat(DocumentImportProcessor.suggestTitle("boarding-pass.pdf", fallbackLabel, now))
            .isEqualTo("boarding-pass")
    }

    @Test
    fun `keeps a name with letters and digits`() {
        assertThat(DocumentImportProcessor.suggestTitle("Ticket-42.pdf", fallbackLabel, now))
            .isEqualTo("Ticket-42")
    }

    @Test
    fun `falls back for camera style names`() {
        assertThat(DocumentImportProcessor.suggestTitle("IMG_1234.jpg", fallbackLabel, now)).isEqualTo(fallback)
        assertThat(DocumentImportProcessor.suggestTitle("dsc-42.png", fallbackLabel, now)).isEqualTo(fallback)
        assertThat(DocumentImportProcessor.suggestTitle("PXL 20260918.jpeg", fallbackLabel, now)).isEqualTo(fallback)
        assertThat(DocumentImportProcessor.suggestTitle("photo_1.jpg", fallbackLabel, now)).isEqualTo(fallback)
    }

    @Test
    fun `falls back for a null display name`() {
        assertThat(DocumentImportProcessor.suggestTitle(null, fallbackLabel, now)).isEqualTo(fallback)
    }

    @Test
    fun `falls back for an all digit name`() {
        assertThat(DocumentImportProcessor.suggestTitle("123456.pdf", fallbackLabel, now)).isEqualTo(fallback)
    }

    @Test
    fun `falls back for a UUID name`() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000.pdf"

        assertThat(DocumentImportProcessor.suggestTitle(uuid, fallbackLabel, now)).isEqualTo(fallback)
    }

    @Test
    fun `falls back for a blank stem`() {
        assertThat(DocumentImportProcessor.suggestTitle(".pdf", fallbackLabel, now)).isEqualTo(fallback)
    }

    @Test
    fun `normalized rect width and height never go negative`() {
        val rect = NormalizedRect(0.8f, 0.9f, 0.2f, 0.3f)

        assertThat(rect.width).isEqualTo(0f)
        assertThat(rect.height).isEqualTo(0f)
    }

    @Test
    fun `normalized rect rejects a thin strip`() {
        assertThat(NormalizedRect(0f, 0f, 0.01f, 0.5f).isValid()).isFalse()
        assertThat(NormalizedRect(0f, 0f, 0.5f, 0.01f).isValid()).isFalse()
    }

    @Test
    fun `normalized rect accepts a usable area`() {
        assertThat(NormalizedRect(0.1f, 0.1f, 0.9f, 0.9f).isValid()).isTrue()
        assertThat(NormalizedRect.Full.isValid()).isTrue()
    }
}
