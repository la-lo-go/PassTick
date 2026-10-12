package org.ligi.passandroid.export

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.repository.CODE_ONLY_CONTENT
import org.ligi.passandroid.repository.PassExportTemplate
import org.ligi.passandroid.repository.PassImageContent
import org.ligi.passandroid.repository.PassImageExportOptions

class PassExportRequestTest {
    @Test
    fun `the ticket and the sheet keep every content toggle`() {
        val content = PassImageContent(hiddenFields = true, notes = false)

        listOf(PassExportTemplate.TICKET, PassExportTemplate.SHEET).forEach { template ->
            assertThat(PassExportRequest(template, PassImageExportOptions(content = content)).content).isEqualTo(content)
        }
    }

    @Test
    fun `the story drops notes and hidden fields`() {
        val request = PassExportRequest(PassExportTemplate.STORY, PassImageExportOptions(content = PassImageContent(hiddenFields = true)))

        assertThat(request.content.notes).isFalse()
        assertThat(request.content.hiddenFields).isFalse()
        assertThat(request.content.barcode).isTrue()
    }

    @Test
    fun `the code template always shows the code and its caption adds the date`() {
        val noCode = PassImageContent(barcode = false, details = true)

        val content = PassExportRequest(PassExportTemplate.CODE, PassImageExportOptions(content = noCode)).content

        assertThat(content).isEqualTo(CODE_ONLY_CONTENT.copy(details = true, dateTime = true))
    }
}
