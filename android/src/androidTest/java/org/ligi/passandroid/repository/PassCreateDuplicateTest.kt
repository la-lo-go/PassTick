package org.ligi.passandroid.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.ligi.passandroid.R
import org.ligi.passandroid.TestApp
import org.ligi.passandroid.model.AndroidFileSystemPassStore
import org.ligi.passandroid.model.createPassMoshi
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import java.io.File

@RunWith(AndroidJUnit4::class)
class PassCreateDuplicateTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val createdPassIds = mutableListOf<String>()

    @Test
    fun createPersistsPassWithFields() {
        runBlocking {
            val repository = realRepository()
            val created = repository.create(
                PassUpdate(
                    description = "Created loyalty card",
                    creator = "PassTick Market",
                    type = PassType.LOYALTY,
                    accentColor = CREATED_ACCENT_COLOR,
                    barcodeFormat = PassBarCodeFormat.QR_CODE,
                    barcodeMessage = "created-message",
                    barcodeAlternativeText = "created alternative text",
                    fields = listOf(
                        PassFieldSnapshot(key = "member", label = "Member", value = "12345", hidden = false, hint = null),
                        PassFieldSnapshot(key = "points", label = "Points", value = "42", hidden = true, hint = "Since 2020"),
                    ),
                ),
            )
            createdPassIds += created.id

            assertThat(created.id).isNotBlank()
            assertThat(created.description).isEqualTo("Created loyalty card")
            assertThat(created.creator).isEqualTo("PassTick Market")
            assertThat(created.type).isEqualTo(PassType.LOYALTY)
            assertThat(created.accentColor).isEqualTo(CREATED_ACCENT_COLOR)
            assertThat(created.barcodeFormat).isEqualTo(PassBarCodeFormat.QR_CODE)
            assertThat(created.barcodeMessage).isEqualTo("created-message")
            assertThat(created.barcodeAlternativeText).isEqualTo("created alternative text")
            assertThat(created.fields).containsExactly(
                PassFieldSnapshot(key = "member", label = "Member", value = "12345", hidden = false, hint = null),
                PassFieldSnapshot(key = "points", label = "Points", value = "42", hidden = true, hint = "Since 2020"),
            )
            assertThat(File(passDirectory(created.id), "main.json")).exists()

            val firstEmission = repository.observePasses().first()

            assertThat(firstEmission.map { it.id }).contains(created.id)
        }
    }

    @Test
    fun createWritesArtworkFromContentUri() {
        runBlocking {
            val source = sharedFile("created-logo.png")
            val original = Bitmap.createBitmap(SOURCE_WIDTH, SOURCE_HEIGHT, Bitmap.Config.ARGB_8888)
            source.outputStream().use { output ->
                original.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            original.recycle()

            val repository = realRepository()
            val created = repository.create(
                PassUpdate(
                    description = "Pass with artwork",
                    creator = "Issuer",
                    type = PassType.EVENT,
                    accentColor = 0,
                    barcodeFormat = null,
                    barcodeMessage = "",
                    barcodeAlternativeText = "",
                    fields = emptyList(),
                    artworkUpdates = listOf(PassArtworkUpdate(PassArtworkKind.LOGO, contentUri(source))),
                ),
            )
            createdPassIds += created.id

            val saved = File(passDirectory(created.id), "logo.png")
            assertThat(saved).exists()
            val decoded = BitmapFactory.decodeFile(saved.path)
            assertThat(decoded).isNotNull
            assertThat(decoded!!.width).isEqualTo(SOURCE_WIDTH)
            assertThat(decoded.height).isEqualTo(SOURCE_HEIGHT)
            decoded.recycle()
        }
    }

    @Test
    fun duplicateCopiesContentAndMetadata() {
        runBlocking {
            val logo = pngFile("duplicate-logo.png")
            val repository = realRepository()
            val source = repository.create(
                PassUpdate(
                    description = "Source pass",
                    creator = "Issuer",
                    type = PassType.EVENT,
                    accentColor = CREATED_ACCENT_COLOR,
                    barcodeFormat = PassBarCodeFormat.QR_CODE,
                    barcodeMessage = "source-code",
                    barcodeAlternativeText = "",
                    fields = listOf(
                        PassFieldSnapshot(key = "gate", label = "Gate", value = "A1", hidden = false, hint = null),
                    ),
                    artworkUpdates = listOf(PassArtworkUpdate(PassArtworkKind.LOGO, contentUri(logo))),
                ),
            )
            createdPassIds += source.id
            repository.setTags(source.id, setOf("travel"))
            repository.setNotes(source.id, "window seat")
            repository.recordUse(source.id)
            repository.recordUse(source.id)

            val copy = repository.duplicate(source.id)
            createdPassIds += copy.id

            assertThat(copy.id).isNotEqualTo(source.id)
            assertThat(copy.description).isEqualTo("Source pass")
            assertThat(copy.barcodeMessage).isEqualTo("source-code")
            assertThat(copy.fields).hasSize(1)
            assertThat(copy.tagIds).containsExactly("travel")
            assertThat(copy.notes).isEqualTo("window seat")
            assertThat(copy.useCount).isEqualTo(2)
            assertThat(File(passDirectory(copy.id), "main.json")).exists()
            assertThat(File(passDirectory(copy.id), "logo.png")).exists()
        }
    }

    @After
    fun removeCreatedPassDirectories() {
        createdPassIds.forEach { id -> passDirectory(id).deleteRecursively() }
    }

    private fun pngFile(name: String): File {
        val file = sharedFile(name)
        val bitmap = Bitmap.createBitmap(SOURCE_WIDTH, SOURCE_HEIGHT, Bitmap.Config.ARGB_8888)
        file.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
        bitmap.recycle()
        return file
    }

    private fun realRepository(): FilePassRepository {
        val passStore = AndroidFileSystemPassStore(context, createPassMoshi())
        return FilePassRepository(context, passStore, TestApp.tracker)
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
        const val SOURCE_WIDTH = 6
        const val SOURCE_HEIGHT = 4
        val CREATED_ACCENT_COLOR = 0xFF006C4C.toInt()
    }
}
