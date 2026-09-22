package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PassArchiveFormatTest {

    @Test
    fun `classifies the index entry`() {
        assertThat(classifyArchiveEntry("backup.json")).isEqualTo(ArchiveEntry.Index)
    }

    @Test
    fun `classifies a pass file and keeps the relative path`() {
        assertThat(classifyArchiveEntry("passes/boarding-pass/main.json")).isEqualTo(
            ArchiveEntry.PassFile("passes/boarding-pass/main.json", "boarding-pass", "main.json"),
        )
    }

    @Test
    fun `classifies nested pass files`() {
        assertThat(classifyArchiveEntry("passes/boarding-pass/artwork/logo.png")).isEqualTo(
            ArchiveEntry.PassFile("passes/boarding-pass/artwork/logo.png", "boarding-pass", "artwork/logo.png"),
        )
    }

    @Test
    fun `classifies a metadata file`() {
        assertThat(classifyArchiveEntry("metadata/pass-metadata.json")).isEqualTo(
            ArchiveEntry.MetadataFile("metadata/pass-metadata.json", "pass-metadata.json"),
        )
    }

    @Test
    fun `ignores entries outside passes and metadata`() {
        assertThat(classifyArchiveEntry("readme.txt")).isEqualTo(ArchiveEntry.Other)
        assertThat(classifyArchiveEntry("passes")).isEqualTo(ArchiveEntry.Other)
        assertThat(classifyArchiveEntry("passes/boarding-pass")).isEqualTo(ArchiveEntry.Other)
        assertThat(classifyArchiveEntry("passes/boarding-pass/")).isEqualTo(ArchiveEntry.Other)
    }

    @Test
    fun `rejects a traversal pass id`() {
        assertThat(classifyArchiveEntry("passes/../evil/main.json"))
            .isEqualTo(ArchiveEntry.Unsafe("passes/../evil/main.json"))
    }

    @Test
    fun `rejects an empty pass id`() {
        assertThat(classifyArchiveEntry("passes//main.json"))
            .isEqualTo(ArchiveEntry.Unsafe("passes//main.json"))
    }

    @Test
    fun `rejects a traversal path inside a pass directory`() {
        assertThat(classifyArchiveEntry("passes/boarding-pass/../../evil.txt"))
            .isEqualTo(ArchiveEntry.Unsafe("passes/boarding-pass/../../evil.txt"))
    }

    @Test
    fun `rejects a backslash inside a pass directory`() {
        assertThat(classifyArchiveEntry("passes/boarding-pass/..\\evil.txt"))
            .isEqualTo(ArchiveEntry.Unsafe("passes/boarding-pass/..\\evil.txt"))
    }

    @Test
    fun `rejects a traversal metadata name`() {
        assertThat(classifyArchiveEntry("metadata/../pass-metadata.json"))
            .isEqualTo(ArchiveEntry.Unsafe("metadata/../pass-metadata.json"))
    }

    @Test
    fun `maps a metadata file to its archive entry name`() {
        assertThat(archiveMetadataEntryName("pass-metadata.json")).isEqualTo("metadata/pass-metadata.json")
        assertThat(archiveMetadataEntryName("state/classifier_state.json")).isEqualTo("metadata/classifier_state.json")
    }

    @Test
    fun `counts restore outcomes`() {
        val summary = archiveRestoreSummary(
            restored = listOf("a", "b"),
            skipped = listOf("c"),
            failed = listOf("d", "e", "f"),
        )

        assertThat(summary).isEqualTo(ArchiveRestoreSummary(restored = 2, skipped = 1, failed = 3))
    }

    @Test
    fun `counts empty restore outcomes as zero`() {
        assertThat(archiveRestoreSummary(emptyList(), emptyList(), emptyList()))
            .isEqualTo(ArchiveRestoreSummary(restored = 0, skipped = 0, failed = 0))
    }

    @Test
    fun `writes and reads back a pass directory`() {
        withTempDirectory { work ->
            val passDirectory = File(work, "passes/boarding-pass").apply { mkdirs() }
            File(passDirectory, "main.json").writeText("{\"description\":\"Pass\"}")
            File(passDirectory, "logo.png").writeBytes(byteArrayOf(1, 2, 3))
            val archive = File(work, "backup.zip")

            PassArchiveWriter(archive.outputStream()).use { writer ->
                writer.addIndex(passCount = 1, exportedAtEpochMillis = 1_700_000_000_000)
                writer.addDirectory("passes/boarding-pass", passDirectory)
            }

            PassArchiveReader(archive).use { reader ->
                assertThat(reader.passIds()).containsExactly("boarding-pass")
                assertThat(reader.hasPassFile("boarding-pass")).isTrue
                val target = File(work, "restored/boarding-pass")
                reader.extractPass("boarding-pass", target)
                assertThat(File(target, "main.json").readText()).isEqualTo("{\"description\":\"Pass\"}")
                assertThat(File(target, "logo.png").readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
            }
        }
    }

    @Test
    fun `rejects an archive without an index`() {
        withTempDirectory { work ->
            val passDirectory = File(work, "passes/boarding-pass").apply { mkdirs() }
            File(passDirectory, "main.json").writeText("{}")
            val archive = File(work, "backup.zip")
            PassArchiveWriter(archive.outputStream()).use { writer ->
                writer.addDirectory("passes/boarding-pass", passDirectory)
            }

            val result = runCatching { PassArchiveReader(archive).use { it.passIds() } }

            assertThat(result.isFailure).isTrue
        }
    }

    @Test
    fun `rejects an archive with an unsafe entry`() {
        withTempDirectory { work ->
            val archive = File(work, "backup.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.writeEntry("backup.json", "{\"formatVersion\":1,\"passCount\":1}")
                zip.writeEntry("passes/../evil/main.json", "{}")
            }

            val result = runCatching { PassArchiveReader(archive).use { it.passIds() } }

            assertThat(result.isFailure).isTrue
        }
    }

    @Test
    fun `moves an extracted pass into a missing pass root`() {
        withTempDirectory { work ->
            val staging = File(work, "staging/boarding-pass").apply { mkdirs() }
            File(staging, "main.json").writeText("{}")
            val target = File(work, "files/passes/boarding-pass")

            movePassIntoPlace(staging, target)

            assertThat(File(target, "main.json")).exists()
            assertThat(staging).doesNotExist()
        }
    }

    private fun withTempDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("pass-archive").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray())
        closeEntry()
    }
}
