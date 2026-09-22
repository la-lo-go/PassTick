package org.ligi.passandroid.repository

import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.ligi.passandroid.R
import org.ligi.passandroid.TestApp
import org.ligi.passandroid.model.AndroidFileSystemPassStore
import org.ligi.passandroid.model.createPassMoshi
import org.ligi.passandroid.model.pass.PassType
import java.io.File

/** Locks the storage layout and file formats that the backup archive must preserve. */
@RunWith(AndroidJUnit4::class)
class PassStorageCharacterisationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val createdPassIds = mutableListOf<String>()

    @Test
    fun createWritesMainJsonAndArtworkIntoThePassDirectory() {
        runBlocking {
            val repository = realRepository()
            val created = repository.create(
                passUpdate(
                    "Layout pass",
                    listOf(PassArtworkUpdate(PassArtworkKind.LOGO, contentUri(pngFile("layout-logo.png")))),
                ),
            )
            createdPassIds += created.id

            assertThat(File(passDirectory(created.id), "main.json")).exists()
            assertThat(File(passDirectory(created.id), "logo.png")).exists()
        }
    }

    @Test
    fun exportRezipsThePassDirectory() {
        runBlocking {
            val repository = realRepository()
            val created = repository.create(passUpdate("Export pass", emptyList()))
            createdPassIds += created.id
            val destination = sharedFile("characterisation-export.espass")

            repository.export(created.id, contentUri(destination)).getOrThrow()

            val zip = ZipFile(destination)
            assertThat(zip.isValidZipFile).isTrue
            assertThat(zip.fileHeaders.map { it.fileName }).contains("main.json")
        }
    }

    @Test
    fun metadataStoresRoundTripThroughTheirFiles() {
        val directory = File(context.cacheDir, "characterisation-metadata").apply {
            deleteRecursively()
            mkdirs()
        }
        val metadataFile = File(directory, "pass-metadata.json")
        val pinnedFile = File(directory, "pass-pinned.json")
        val protectionFile = File(directory, "pass-protection.json")

        FilePassMetadataStore(metadataFile).apply {
            setTags("pass-1", setOf("travel"))
            setArchived("pass-1", true)
            setPreferredArtwork("pass-1", PassArtworkKind.LOGO)
            setTrashedAt("pass-1", TRASHED_AT)
            setNotes("pass-1", "window seat")
        }
        FileFavoriteStore(pinnedFile).setFavorite("pass-1", true)
        FilePassProtectionStore(protectionFile).setProtected("pass-1", true)

        FilePassMetadataStore(metadataFile).apply {
            assertThat(tags("pass-1")).containsExactly("travel")
            assertThat(isArchived("pass-1")).isTrue
            assertThat(preferredArtwork("pass-1")).isEqualTo(PassArtworkKind.LOGO)
            assertThat(trashedAt("pass-1")).isEqualTo(TRASHED_AT)
            assertThat(notes("pass-1")).isEqualTo("window seat")
        }
        assertThat(FileFavoriteStore(pinnedFile).isFavorite("pass-1")).isTrue
        assertThat(FilePassProtectionStore(protectionFile).isProtected("pass-1")).isTrue

        directory.deleteRecursively()
    }

    @After
    fun removeCreatedPassDirectories() {
        val passStore = AndroidFileSystemPassStore(context, createPassMoshi())
        createdPassIds.forEach { passStore.deletePassWithId(it) }
    }

    private fun passUpdate(description: String, artworkUpdates: List<PassArtworkUpdate>) = PassUpdate(
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

    private companion object {
        const val TRASHED_AT = 1_700_000_000_000
    }
}
