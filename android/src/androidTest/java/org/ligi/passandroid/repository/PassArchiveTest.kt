package org.ligi.passandroid.repository

import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.ligi.passandroid.R
import org.ligi.passandroid.TestApp
import org.ligi.passandroid.model.AndroidFileSystemPassStore
import org.ligi.passandroid.model.createPassMoshi
import org.ligi.passandroid.model.pass.PassType
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class PassArchiveTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val createdPassIds = mutableListOf<String>()

    @Test
    fun exportWritesTheDocumentedArchiveLayout() {
        runBlocking {
            val repository = realRepository()
            val existing = existingPassIds()
            val pass = repository.create(passUpdate("Layout pass"))
            createdPassIds += pass.id
            repository.setTags(pass.id, setOf("travel"))
            val destination = sharedFile("archive-layout.zip")

            val exported = repository.exportArchive(contentUri(destination)).getOrThrow()

            assertThat(exported).isEqualTo(existing.size + 1)
            val zip = ZipFile(destination)
            assertThat(zip.fileHeaders.map { it.fileName }).contains(
                "backup.json",
                "metadata/pass-metadata.json",
                "passes/${pass.id}/main.json",
            )
            val index = JSONObject(
                zip.getInputStream(requireNotNull(zip.getFileHeader("backup.json")))
                    .use { it.readBytes().decodeToString() },
            )
            assertThat(index.getInt("formatVersion")).isEqualTo(1)
            assertThat(index.getInt("passCount")).isEqualTo(existing.size + 1)
        }
    }

    @Test
    fun archiveRoundTripRestoresPassesAndMetadata() {
        runBlocking {
            val repository = realRepository()
            val existing = existingPassIds()
            val withArtwork = repository.create(
                passUpdate(
                    "Round trip",
                    listOf(PassArtworkUpdate(PassArtworkKind.LOGO, contentUri(pngFile("round-trip-logo.png")))),
                ),
            )
            val plain = repository.create(passUpdate("Plain pass"))
            createdPassIds += listOf(withArtwork.id, plain.id)
            repository.setTags(withArtwork.id, setOf("travel"))
            repository.setNotes(withArtwork.id, "window seat")
            repository.setFavorite(withArtwork.id, true)
            repository.setProtected(withArtwork.id, true)
            repository.trashPass(plain.id)
            val destination = sharedFile("round-trip.zip")
            repository.exportArchive(contentUri(destination)).getOrThrow()

            cleanStore()
            val restoredRepository = realRepository()
            val summary = restoredRepository.importArchive(contentUri(destination)).getOrThrow()

            assertThat(summary).isEqualTo(
                ArchiveRestoreSummary(restored = 2, skipped = existing.size, failed = 0),
            )
            assertThat(File(passDirectory(withArtwork.id), "main.json")).exists()
            assertThat(File(passDirectory(withArtwork.id), "logo.png")).exists()
            val restored = restoredRepository.observePasses().first().single { it.id == withArtwork.id }
            assertThat(restored.tagIds).containsExactly("travel")
            assertThat(restored.notes).isEqualTo("window seat")
            assertThat(restored.isFavorite).isTrue
            assertThat(restored.isProtected).isTrue
        }
    }

    @Test
    fun importSkipsAnExistingPassId() {
        runBlocking {
            val repository = realRepository()
            val existing = existingPassIds()
            val pass = repository.create(passUpdate("Existing pass"))
            createdPassIds += pass.id
            val destination = sharedFile("merge.zip")
            repository.exportArchive(contentUri(destination)).getOrThrow()
            val mainJson = File(passDirectory(pass.id), "main.json").readBytes()
            val marker = File(passDirectory(pass.id), "marker.txt").apply { writeText("keep") }

            val summary = realRepository().importArchive(contentUri(destination)).getOrThrow()

            assertThat(summary).isEqualTo(
                ArchiveRestoreSummary(restored = 0, skipped = existing.size + 1, failed = 0),
            )
            assertThat(marker.readText()).isEqualTo("keep")
            assertThat(File(passDirectory(pass.id), "main.json").readBytes()).isEqualTo(mainJson)
        }
    }

    @Test
    fun unsafePassIdRejectsTheWholeArchive() {
        runBlocking {
            val archive = sharedFile("unsafe.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.writeEntry("backup.json", "{\"formatVersion\":1,\"passCount\":2}")
                zip.writeEntry("passes/good-pass/main.json", "{}")
                zip.writeEntry("passes/../evil/main.json", "{}")
            }

            val result = realRepository().importArchive(contentUri(archive))

            assertThat(result.isFailure).isTrue
            assertThat(File(File(context.filesDir, "passes"), "good-pass")).doesNotExist()
        }
    }

    @Test
    fun trashedPassKeepsItsTimestamp() {
        runBlocking {
            val repository = realRepository()
            val existing = existingPassIds()
            val pass = repository.create(passUpdate("Trashed pass"))
            createdPassIds += pass.id
            repository.trashPass(pass.id)
            val trashedAt = repository.observeTrashedPasses().first()
                .single { it.id == pass.id }
                .trashedAtEpochMillis
            val destination = sharedFile("trashed.zip")
            repository.exportArchive(contentUri(destination)).getOrThrow()

            cleanStore()
            val restoredRepository = realRepository()
            val summary = restoredRepository.importArchive(contentUri(destination)).getOrThrow()

            assertThat(summary).isEqualTo(
                ArchiveRestoreSummary(restored = 1, skipped = existing.size, failed = 0),
            )
            val restored = restoredRepository.observeTrashedPasses().first().single { it.id == pass.id }
            assertThat(restored.trashedAtEpochMillis).isEqualTo(trashedAt)
        }
    }

    @Test
    fun prepareShareReturnsTheRetainedOriginal() {
        runBlocking {
            val repository = realRepository()
            val pass = repository.create(passUpdate("Original pass"))
            createdPassIds += pass.id
            val original = "original pkpass bytes".toByteArray()
            File(passDirectory(pass.id), "source.pkpass").writeBytes(original)

            val uri = repository.prepareShare(pass.id).getOrThrow()

            assertThat(context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }).isEqualTo(original)
        }
    }

    @Test
    fun prepareShareRezipsWhenTheOriginalIsMissing() {
        runBlocking {
            val repository = realRepository()
            val pass = repository.create(passUpdate("Re-zipped pass"))
            createdPassIds += pass.id

            val uri = repository.prepareShare(pass.id).getOrThrow()
            val shared = sharedFile("shared-copy.espass")
            context.contentResolver.openInputStream(uri)!!.use { input ->
                shared.outputStream().use { output -> input.copyTo(output) }
            }

            assertThat(ZipFile(shared).fileHeaders.map { it.fileName }).contains("main.json")
        }
    }

    /** Removes the pass directories and the metadata entries of this test, so a restore starts clean. */
    private fun cleanStore() {
        createdPassIds.forEach { passDirectory(it).deleteRecursively() }
        FilePassMetadataStore(File(context.filesDir, "pass-metadata.json")).apply {
            createdPassIds.forEach { id -> remove(id) }
        }
        FileFavoriteStore(File(context.filesDir, "pass-pinned.json")).apply {
            createdPassIds.forEach { id -> remove(id) }
        }
        FilePassProtectionStore(File(context.filesDir, "pass-protection.json")).apply {
            createdPassIds.forEach { id -> remove(id) }
        }
    }

    /** Pass directories from other tests; the archive counts them, so the assertions must too. */
    private fun existingPassIds(): Set<String> = File(context.filesDir, "passes").listFiles().orEmpty()
        .filter { it.isDirectory }
        .map { it.name }
        .toSet()

    @After
    fun removeCreatedPassDirectoriesAndMetadata() {
        val passStore = AndroidFileSystemPassStore(context, createPassMoshi())
        createdPassIds.forEach { passStore.deletePassWithId(it) }
        cleanStore()
    }

    private fun passUpdate(description: String, artworkUpdates: List<PassArtworkUpdate> = emptyList()) = PassUpdate(
        description = description,
        creator = "Issuer",
        type = PassType.EVENT,
        accentColor = 0,
        barcodeFormat = null,
        barcodeMessage = "",
        barcodeAlternativeText = "",
        fields = emptyList(),
        artworkUpdates = artworkUpdates,
    )

    private fun realRepository(): FilePassRepository =
        FilePassRepository(context, AndroidFileSystemPassStore(context, createPassMoshi()), TestApp.tracker)

    private fun pngFile(name: String): File {
        val file = sharedFile(name)
        val bitmap = Bitmap.createBitmap(6, 4, Bitmap.Config.ARGB_8888)
        file.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
        bitmap.recycle()
        return file
    }

    private fun passDirectory(id: String) = File(File(context.filesDir, "passes"), id)

    private fun sharedFile(name: String) = File(context.cacheDir, "share/$name").apply {
        parentFile?.mkdirs()
    }

    private fun contentUri(file: File) = FileProvider.getUriForFile(
        context,
        context.getString(R.string.authority_fileprovider),
        file,
    )

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray())
        closeEntry()
    }
}
