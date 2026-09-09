package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.file.Files

class FilePassMetadataStoreTest {
    @Test
    fun `stores multiple tags and archive state across reopen`() {
        val file = Files.createTempFile("pass-metadata", ".json").toFile().apply { delete() }
        try {
            FilePassMetadataStore(file).apply {
                setTags("pass-1", setOf("travel", "important"))
                setArchived("pass-1", true)
            }

            FilePassMetadataStore(file).apply {
                assertThat(tags("pass-1")).containsExactlyInAnyOrder("travel", "important")
                assertThat(isArchived("pass-1")).isTrue()
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `stores preferred artwork independently from pass contents`() {
        val file = Files.createTempFile("pass-metadata", ".json").toFile().apply { delete() }
        try {
            FilePassMetadataStore(file).setPreferredArtwork("pass-1", PassArtworkKind.LOGO)

            assertThat(FilePassMetadataStore(file).preferredArtwork("pass-1")).isEqualTo(PassArtworkKind.LOGO)

            FilePassMetadataStore(file).setPreferredArtwork("pass-1", null)

            assertThat(FilePassMetadataStore(file).preferredArtwork("pass-1")).isNull()
        } finally {
            file.delete()
        }
    }
}
