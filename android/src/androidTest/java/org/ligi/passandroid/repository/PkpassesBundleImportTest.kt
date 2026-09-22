package org.ligi.passandroid.repository

import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.ligi.passandroid.R
import org.ligi.passandroid.TestApp
import org.ligi.passandroid.model.AndroidFileSystemPassStore
import org.ligi.passandroid.model.createPassMoshi
import java.io.File
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class PkpassesBundleImportTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val passRoot = File(context.filesDir, "passes")
    private val preexistingPassIds = passRoot.list().orEmpty().toSet()

    @Test
    fun importsEveryPassInABundle() {
        runBlocking {
            val passOne = pkpassFile("bundle-pass-one.pkpass", "Bundle pass one")
            val passTwo = pkpassFile("bundle-pass-two.pkpass", "Bundle pass two")
            val bundle = sharedFile("two-passes.pkpasses").apply { delete() }
            ZipFile(bundle).apply {
                addFile(passOne, ZipParameters().apply { fileNameInZip = passOne.name })
                addFile(passTwo, ZipParameters().apply { fileNameInZip = passTwo.name })
            }
            val repository = FilePassRepository(
                context,
                AndroidFileSystemPassStore(context, createPassMoshi()),
                TestApp.tracker,
            )

            repository.import(contentUri(bundle)).getOrThrow()

            val passes = repository.observePasses().first()

            assertThat(passes).hasSize(2)
            assertThat(passes.map { it.description })
                .containsExactlyInAnyOrder("Bundle pass one", "Bundle pass two")
            assertRetainedOriginal(passes, "Bundle pass one", passOne)
            assertRetainedOriginal(passes, "Bundle pass two", passTwo)
        }
    }

    @After
    fun removeCreatedPassDirectories() {
        passRoot.list().orEmpty()
            .filterNot(preexistingPassIds::contains)
            .forEach { File(passRoot, it).deleteRecursively() }
    }

    private fun assertRetainedOriginal(passes: List<PassSnapshot>, description: String, entry: File) {
        val pass = passes.first { it.description == description }
        val original = File(passDirectory(pass.id), "source.pkpass")
        assertThat(original).exists()
        assertThat(original.readBytes()).isEqualTo(entry.readBytes())
    }

    private fun pkpassFile(name: String, description: String): File {
        val source = File(context.cacheDir, "pkpass-source/$name").apply {
            deleteRecursively()
            mkdirs()
        }
        val passJson = """
            {
              "formatVersion": 1,
              "passTypeIdentifier": "pass.dev.lalogo.passtick",
              "serialNumber": "$name",
              "teamIdentifier": "TEAM",
              "organizationName": "PassTick Test",
              "description": "$description",
              "eventTicket": {"primaryFields": []}
            }
        """.trimIndent().toByteArray()
        File(source, "pass.json").writeBytes(passJson)
        File(source, "manifest.json").writeText("""{"pass.json":"${sha1(passJson)}"}""")
        File(source, "signature").writeText("signature")
        return sharedFile(name).apply {
            delete()
            ZipFile(this).apply {
                PASS_ENTRY_NAMES.forEach { entry ->
                    addFile(File(source, entry), ZipParameters().apply { fileNameInZip = entry })
                }
            }
        }.also { source.deleteRecursively() }
    }

    private fun sha1(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private fun passDirectory(id: String) = File(passRoot, id)

    private fun sharedFile(name: String) = File(context.cacheDir, "share/$name").apply {
        parentFile?.mkdirs()
    }

    private fun contentUri(file: File) = FileProvider.getUriForFile(
        context,
        context.getString(R.string.authority_fileprovider),
        file,
    )

    private companion object {
        val PASS_ENTRY_NAMES = listOf("pass.json", "manifest.json", "signature")
    }
}
