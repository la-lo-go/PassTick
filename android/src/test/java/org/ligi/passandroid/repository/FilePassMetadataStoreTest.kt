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
    fun `stores trashedAt across reopen and clears it with null`() {
        val file = Files.createTempFile("pass-metadata", ".json").toFile().apply { delete() }
        try {
            FilePassMetadataStore(file).setTrashedAt("pass-1", 1_700_000_000_000)

            assertThat(FilePassMetadataStore(file).trashedAt("pass-1")).isEqualTo(1_700_000_000_000)

            FilePassMetadataStore(file).setTrashedAt("pass-1", null)

            assertThat(FilePassMetadataStore(file).trashedAt("pass-1")).isNull()
        } finally {
            file.delete()
        }
    }

    @Test
    fun `trashedAt is independent from archived tags and remove clears it`() {
        val file = Files.createTempFile("pass-metadata", ".json").toFile().apply { delete() }
        try {
            FilePassMetadataStore(file).apply {
                setTags("pass-1", setOf("travel"))
                setArchived("pass-1", true)
                setTrashedAt("pass-1", 1_700_000_000_000)
            }

            FilePassMetadataStore(file).apply {
                assertThat(trashedAt("pass-1")).isEqualTo(1_700_000_000_000)
                assertThat(tags("pass-1")).containsExactly("travel")
                assertThat(isArchived("pass-1")).isTrue()

                remove("pass-1")

                assertThat(trashedAt("pass-1")).isNull()
                assertThat(tags("pass-1")).isEmpty()
                assertThat(isArchived("pass-1")).isFalse()
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `remove clears tags archive and artwork for one pass without touching others`() {
        val file = Files.createTempFile("pass-metadata", ".json").toFile().apply { delete() }
        try {
            FilePassMetadataStore(file).apply {
                setTags("pass-1", setOf("travel"))
                setArchived("pass-1", true)
                setPreferredArtwork("pass-1", PassArtworkKind.LOGO)
                setTags("pass-2", setOf("work"))
                setArchived("pass-2", true)
                setPreferredArtwork("pass-2", PassArtworkKind.ICON)
            }

            FilePassMetadataStore(file).apply {
                remove("pass-1")

                assertThat(tags("pass-1")).isEmpty()
                assertThat(isArchived("pass-1")).isFalse()
                assertThat(preferredArtwork("pass-1")).isNull()
                assertThat(tags("pass-2")).containsExactly("work")
                assertThat(isArchived("pass-2")).isTrue()
                assertThat(preferredArtwork("pass-2")).isEqualTo(PassArtworkKind.ICON)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `stores notes across reopen with escaping intact`() {
        val file = Files.createTempFile("pass-metadata", ".json").toFile().apply { delete() }
        try {
            val note = "Gate A12\nSeat \"12A\" \\ row 1"

            FilePassMetadataStore(file).setNotes("pass-1", note)

            assertThat(FilePassMetadataStore(file).notes("pass-1")).isEqualTo(note)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `blank notes remove the stored key and trim the ends`() {
        val file = Files.createTempFile("pass-metadata", ".json").toFile().apply { delete() }
        try {
            FilePassMetadataStore(file).apply {
                setNotes("pass-1", "  padded note  ")
                setNotes("pass-2", "   ")
            }

            FilePassMetadataStore(file).apply {
                assertThat(notes("pass-1")).isEqualTo("padded note")
                assertThat(notes("pass-2")).isEmpty()
                assertThat(org.json.JSONObject(file.readText()).optJSONObject("notes")?.has("pass-2")).isFalse()
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `a legacy file without notes loads empty and remove clears notes`() {
        val file = Files.createTempFile("pass-metadata", ".json").toFile()
        try {
            file.writeText("{\"tags\":{\"pass-1\":[\"travel\"]}}")

            FilePassMetadataStore(file).apply {
                assertThat(notes("pass-1")).isEmpty()
                setNotes("pass-1", "kept note")
                setNotes("pass-2", "dropped note")
            }

            FilePassMetadataStore(file).apply {
                assertThat(notes("pass-1")).isEqualTo("kept note")
                assertThat(notes("pass-2")).isEqualTo("dropped note")
                remove("pass-1")

                assertThat(notes("pass-1")).isEmpty()
                assertThat(notes("pass-2")).isEqualTo("dropped note")
                assertThat(tags("pass-1")).isEmpty()
                assertThat(tags("pass-2")).isEmpty()
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `persists exactly the current metadata key set`() {
        val file = Files.createTempFile("pass-metadata", ".json").toFile().apply { delete() }
        try {
            FilePassMetadataStore(file).apply {
                setTags("pass-1", setOf("travel"))
                setArchived("pass-1", true)
                setPreferredArtwork("pass-1", PassArtworkKind.LOGO)
                setTrashedAt("pass-1", 1_700_000_000_000)
            }

            val keys = org.json.JSONObject(file.readText()).let { json ->
                buildList { json.keys().forEach { key -> add(key) } }
            }

            assertThat(keys).containsExactlyInAnyOrder("tags", "archived", "preferredArtwork", "trashedAt", "notes")
        } finally {
            file.delete()
        }
    }

    @Test
    fun `a corrupt metadata file loads as empty defaults`() {
        val file = Files.createTempFile("pass-metadata", ".json").toFile()
        try {
            file.writeText("{ not valid json")

            FilePassMetadataStore(file).apply {
                assertThat(tags("pass-1")).isEmpty()
                assertThat(isArchived("pass-1")).isFalse()
                assertThat(preferredArtwork("pass-1")).isNull()
                assertThat(trashedAt("pass-1")).isNull()
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `remove clears every metadata map for the pass`() {
        val file = Files.createTempFile("pass-metadata", ".json").toFile().apply { delete() }
        try {
            FilePassMetadataStore(file).setPreferredArtwork("pass-1", PassArtworkKind.THUMBNAIL)

            FilePassMetadataStore(file).apply {
                remove("pass-1")
                assertThat(tags("pass-1")).isEmpty()
                assertThat(isArchived("pass-1")).isFalse()
                assertThat(preferredArtwork("pass-1")).isNull()
                assertThat(trashedAt("pass-1")).isNull()
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
