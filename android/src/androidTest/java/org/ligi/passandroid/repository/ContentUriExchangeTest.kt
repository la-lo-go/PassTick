package org.ligi.passandroid.repository

import androidx.core.content.FileProvider
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
    fun exportsAPassToAContentUri() = runBlocking {
        val passDirectory = File(context.cacheDir, "export-source").apply { mkdirs() }
        File(passDirectory, "pass.json").writeText("{\"description\":\"Test pass\"}")
        val destination = sharedFile("export.espass").apply { writeBytes(byteArrayOf()) }
        val passStore = FixedPassListPassStore(emptyList()).apply { pathForId = passDirectory }
        val repository = FilePassRepository(context, passStore, TestApp.tracker)

        repository.export("pass-id", contentUri(destination)).getOrThrow()

        assertThat(ZipFile(destination).isValidZipFile).isTrue
        assertThat(ZipFile(destination).fileHeaders.map { it.fileName }).contains("pass.json")
    }

    @Test
    fun replacesArtworkFromAContentUri() = runBlocking {
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

    private fun sharedFile(name: String) = File(context.filesDir, "share/$name").apply {
        parentFile?.mkdirs()
    }

    private fun contentUri(file: File) = FileProvider.getUriForFile(
        context,
        context.getString(R.string.authority_fileprovider),
        file,
    )
}
