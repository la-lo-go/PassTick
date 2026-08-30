package org.ligi.passandroid.widget

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter

class PassWidgetSnapshotStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val file = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))

    suspend fun read(): PassWidgetSnapshot = withContext(ioDispatcher) {
        runCatching {
            file.openRead().bufferedReader().use { reader -> decode(JSONObject(reader.readText())) }
        }.getOrDefault(PassWidgetSnapshot())
    }

    suspend fun replace(snapshot: PassWidgetSnapshot) = withContext(ioDispatcher) {
        val output = file.startWrite()
        try {
            OutputStreamWriter(output).apply {
                write(encode(snapshot).toString())
                flush()
            }
            file.finishWrite(output)
        } catch (failure: Throwable) {
            file.failWrite(output)
            throw failure
        }
    }

    private fun encode(snapshot: PassWidgetSnapshot) = JSONObject().apply {
        put("version", FORMAT_VERSION)
        put("generatedAt", snapshot.generatedAtEpochMillis)
        put("passes", JSONArray().apply {
            snapshot.passes.forEach { pass ->
                put(JSONObject().apply {
                    put("id", pass.id)
                    put("title", pass.title)
                    put("startsAt", pass.startsAtEpochMillis)
                    put("endsAt", pass.endsAtEpochMillis)
                    put("location", pass.location)
                    put("supportingText", pass.supportingText)
                })
            }
        })
    }

    private fun decode(json: JSONObject): PassWidgetSnapshot {
        if (json.optInt("version") != FORMAT_VERSION) return PassWidgetSnapshot()
        val passesJson = json.optJSONArray("passes") ?: JSONArray()
        val passes = buildList {
            for (index in 0 until passesJson.length()) {
                val pass = passesJson.optJSONObject(index) ?: continue
                val id = pass.optString("id").takeIf(String::isNotBlank) ?: continue
                add(
                    WidgetPass(
                        id = id,
                        title = pass.optString("title").ifBlank { "Pass" },
                        startsAtEpochMillis = pass.optionalLong("startsAt"),
                        endsAtEpochMillis = pass.optionalLong("endsAt"),
                        location = pass.optionalString("location"),
                        supportingText = pass.optionalString("supportingText"),
                    ),
                )
            }
        }
        return PassWidgetSnapshot(
            passes = passes,
            generatedAtEpochMillis = json.optLong("generatedAt"),
        )
    }

    private fun JSONObject.optionalString(key: String): String? =
        takeUnless { isNull(key) }?.optString(key)?.takeIf(String::isNotBlank)

    private fun JSONObject.optionalLong(key: String): Long? =
        takeUnless { isNull(key) }?.optLong(key)

    private companion object {
        const val FILE_NAME = "pass-widget-snapshot-v1.json"
        const val FORMAT_VERSION = 2
    }
}
