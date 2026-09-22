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
import org.threeten.bp.ZonedDateTime
import java.io.File
import java.security.MessageDigest

/** Locks how the pass validity end reaches the snapshot. */
@RunWith(AndroidJUnit4::class)
class PassSnapshotExpirationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val passRoot = File(context.filesDir, "passes")
    private val preexistingPassIds = passRoot.list().orEmpty().toSet()

    @Test
    fun expirationDateFillsTheSnapshotExpiryAndKeepsTheCalendarFallback() {
        runBlocking {
            val pass = importPass(
                "expiring.pkpass",
                passJson("\"expirationDate\": \"2026-10-04T18:30:00+02:00\","),
            )

            assertThat(pass.expiresAt).isEqualTo(ZonedDateTime.parse("2026-10-04T18:30:00+02:00"))
            assertThat(pass.calendarTimeSpan?.from).isNull()
            assertThat(pass.calendarTimeSpan?.to).isEqualTo(ZonedDateTime.parse("2026-10-04T18:30:00+02:00"))
        }
    }

    @Test
    fun anExplicitCalendarSpanWinsForTheCalendarFallbackOnly() {
        runBlocking {
            val pass = importPass(
                "spanned.pkpass",
                passJson(
                    "\"relevantDate\": \"2026-10-04T16:00:00+02:00\",",
                    "\"expirationDate\": \"2026-10-04T18:30:00+02:00\",",
                ),
            )

            assertThat(pass.calendarTimeSpan?.from).isEqualTo(ZonedDateTime.parse("2026-10-04T16:00:00+02:00"))
            assertThat(pass.calendarTimeSpan?.to).isNull()
            assertThat(pass.expiresAt).isEqualTo(ZonedDateTime.parse("2026-10-04T18:30:00+02:00"))
        }
    }

    @Test
    fun aPassWithoutAnExpirationHasNoSnapshotExpiry() {
        runBlocking {
            val pass = importPass("open.pkpass", passJson())

            assertThat(pass.expiresAt).isNull()
            assertThat(pass.calendarTimeSpan).isNull()
        }
    }

    @After
    fun removeCreatedPassDirectories() {
        passRoot.list().orEmpty()
            .filterNot(preexistingPassIds::contains)
            .forEach { File(passRoot, it).deleteRecursively() }
    }

    private suspend fun importPass(fileName: String, passJson: String): PassSnapshot {
        val source = File(context.cacheDir, "expiration-source/$fileName").apply {
            deleteRecursively()
            mkdirs()
        }
        val passJsonBytes = passJson.toByteArray()
        File(source, "pass.json").writeBytes(passJsonBytes)
        File(source, "manifest.json").writeText("""{"pass.json":"${sha1(passJsonBytes)}"}""")
        File(source, "signature").writeText("signature")
        val archive = sharedFile(fileName).apply {
            delete()
            ZipFile(this).apply {
                PASS_ENTRY_NAMES.forEach { entry ->
                    addFile(File(source, entry), ZipParameters().apply { fileNameInZip = entry })
                }
            }
        }
        source.deleteRecursively()
        val repository = FilePassRepository(
            context,
            AndroidFileSystemPassStore(context, createPassMoshi()),
            TestApp.tracker,
        )
        repository.import(contentUri(archive)).getOrThrow()
        return repository.observePasses().first().single()
    }

    private fun passJson(vararg extraFields: String) = """
        {
          "formatVersion": 1,
          "passTypeIdentifier": "pass.dev.lalogo.passtick",
          "serialNumber": "expiration",
          "teamIdentifier": "TEAM",
          "organizationName": "PassTick Test",
          "description": "Expiring ticket",
          ${extraFields.joinToString("\n          ")}
          "eventTicket": {"primaryFields": []}
        }
    """.trimIndent()

    private fun sha1(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

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
