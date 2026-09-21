package org.ligi.passandroid.repository

import androidx.core.content.FileProvider
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.R
import org.ligi.passandroid.TestApp
import org.ligi.passandroid.functions.fromURI
import org.ligi.passandroid.injections.FixedPassListPassStore
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassType
import java.io.File

class ContentUriExchangeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun importsBytesFromAContentUri() {
        val source = sharedFile("source.pkpass").apply { writeText("pass payload") }
        val uri = contentUri(source)

        val imported = fromURI(context, uri, TestApp.tracker)

        assertThat(imported).isNotNull
        assertThat(imported!!.inputStream.bufferedReader().use { it.readText() }).isEqualTo("pass payload")
        assertThat(imported.source).startsWith("content://")
    }

    @Test
    fun exportsAPassToAContentUri() {
        runBlocking {
            val passDirectory = File(context.cacheDir, "export-source").apply { mkdirs() }
            File(passDirectory, "pass.json").writeText("{\"description\":\"Test pass\"}")
            val destination = sharedFile("export.espass").apply { writeBytes(byteArrayOf()) }
            val passStore = FixedPassListPassStore(emptyList()).apply { pathForId = passDirectory }
            val repository = FilePassRepository(context, passStore, TestApp.tracker)

            repository.export("pass-id", contentUri(destination)).getOrThrow()

            assertThat(ZipFile(destination).isValidZipFile).isTrue
            assertThat(ZipFile(destination).fileHeaders.map { it.fileName }).contains("pass.json")
        }
    }

    @Test
    fun replacesArtworkFromAContentUri() {
        runBlocking {
            val passDirectory = File(context.cacheDir, "artwork-pass").apply { mkdirs() }
            val image = sharedFile("replacement.png")
            image.outputStream().use { output ->
                Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            val pass = PassImpl("artwork-pass")
            val passStore = FixedPassListPassStore(listOf(pass)).apply { pathForId = passDirectory }
            val repository = FilePassRepository(context, passStore, TestApp.tracker)

            repository.update(
                pass.id,
                PassUpdate(
                    description = "Pass",
                    creator = "Issuer",
                    type = PassType.EVENT,
                    accentColor = 0,
                    barcodeFormat = null,
                    barcodeMessage = "",
                    barcodeAlternativeText = "",
                    fields = emptyList(),
                    artworkUpdates = listOf(PassArtworkUpdate(PassArtworkKind.LOGO, contentUri(image))),
                ),
            )

            val saved = File(passDirectory, "logo.png")
            assertThat(saved).exists()
            assertThat(BitmapFactory.decodeFile(saved.path)).isNotNull
        }
    }

    @Test
    fun replacementAppliesExifRotation() {
        runBlocking {
            val source = sharedFile("rotated.jpg")
            writeJpegWithOrientation(source, width = 6, height = 4, orientation = ExifInterface.ORIENTATION_ROTATE_90)

            val directory = replaceArtwork("artwork-exif", PassArtworkKind.LOGO, source)

            val decoded = BitmapFactory.decodeFile(File(directory, "logo.png").path)
            assertThat(decoded.width).isEqualTo(4)
            assertThat(decoded.height).isEqualTo(6)
        }
    }

    @Test
    fun replacementScalesDownLargeSources() {
        runBlocking {
            val source = sharedFile("large.png")
            writePng(source, width = 4096, height = 2048)

            val directory = replaceArtwork("artwork-large", PassArtworkKind.LOGO, source)

            val decoded = BitmapFactory.decodeFile(File(directory, "logo.png").path)
            assertThat(maxOf(decoded.width, decoded.height)).isLessThanOrEqualTo(2048)
        }
    }

    @Test
    fun replacementDropsStaleDensityVariants() {
        runBlocking {
            val source = sharedFile("replacement.png")
            writePng(source, width = 4, height = 4)

            val directory = replaceArtwork("artwork-stale", PassArtworkKind.LOGO, source) { passDirectory ->
                writePng(File(passDirectory, "logo@3x.png"), width = 600, height = 600)
            }

            assertThat(File(directory, "logo@3x.png")).doesNotExist()
            assertThat(File(directory, "logo.png")).exists()
        }
    }

    @Test
    fun replacementCapsThumbnails() {
        runBlocking {
            val source = sharedFile("thumbnail-source.png")
            writePng(source, width = 1000, height = 800)

            val directory = replaceArtwork("artwork-thumbnail", PassArtworkKind.THUMBNAIL, source)

            val decoded = BitmapFactory.decodeFile(File(directory, "thumbnail.png").path)
            assertThat(maxOf(decoded.width, decoded.height)).isLessThanOrEqualTo(384)
        }
    }

    private suspend fun replaceArtwork(
        directoryName: String,
        kind: PassArtworkKind,
        source: File,
        beforeUpdate: (File) -> Unit = {},
    ): File {
        val passDirectory = File(context.cacheDir, directoryName).apply {
            deleteRecursively()
            mkdirs()
        }
        beforeUpdate(passDirectory)
        val pass = PassImpl(directoryName)
        val passStore = FixedPassListPassStore(listOf(pass)).apply { pathForId = passDirectory }
        FilePassRepository(context, passStore, TestApp.tracker).update(
            pass.id,
            PassUpdate(
                description = "Pass",
                creator = "Issuer",
                type = PassType.EVENT,
                accentColor = 0,
                barcodeFormat = null,
                barcodeMessage = "",
                barcodeAlternativeText = "",
                fields = emptyList(),
                artworkUpdates = listOf(PassArtworkUpdate(kind, contentUri(source))),
            ),
        )
        return passDirectory
    }

    private fun writePng(target: File, width: Int, height: Int) {
        target.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        target.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
        bitmap.recycle()
    }

    private fun writeJpegWithOrientation(target: File, width: Int, height: Int, orientation: Int) {
        target.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        target.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output) }
        bitmap.recycle()
        ExifInterface(target.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
    }

    private fun sharedFile(name: String) = File(context.cacheDir, "share/$name").apply {
        parentFile?.mkdirs()
    }

    private fun contentUri(file: File) = FileProvider.getUriForFile(
        context,
        context.getString(R.string.authority_fileprovider),
        file,
    )
}
