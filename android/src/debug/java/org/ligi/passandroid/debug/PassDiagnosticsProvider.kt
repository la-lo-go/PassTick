package org.ligi.passandroid.debug

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Process
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassStore
import java.security.MessageDigest

class PassDiagnosticsProvider : ContentProvider(), KoinComponent {
    private val passStore: PassStore by inject()

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        if (Binder.getCallingUid() != Process.SHELL_UID) {
            throw SecurityException("Diagnostics are available only through ADB shell")
        }
        require(uri.path == "/passes") { "Use /passes" }
        val context = requireNotNull(context)
        passStore.syncPassStoreWithClassifier(context.getString(R.string.topic_new))
        val columns = arrayOf(
            "id", "description", "creator", "type", "category", "barcode_format", "barcode_sha256",
            "barcode_length", "fields_json", "locations_json", "starts_at", "ends_at", "artwork",
        )
        return MatrixCursor(columns).apply {
            passStore.passMap.values.forEach { pass ->
                val barcode = pass.barCode
                val timeSpan = pass.calendarTimespan ?: pass.validTimespans?.firstOrNull()
                addRow(
                    arrayOf<Any?>(
                        pass.id,
                        pass.description,
                        pass.creator,
                        pass.type.name,
                        passStore.classifier.getTopic(pass.id, context.getString(R.string.topic_new)),
                        barcode?.format?.name,
                        barcode?.message?.sha256(),
                        barcode?.message?.length,
                        pass.fields.toJson { field ->
                            JSONObject().put("key", field.key).put("label", field.label).put("value", field.value)
                        },
                        pass.locations.toJson { location ->
                            JSONObject().put("name", location.name).put("latitude", location.lat).put("longitude", location.lon)
                        },
                        timeSpan?.from?.toString(),
                        timeSpan?.to?.toString(),
                        passStore.getPathForID(pass.id).listFiles()?.filter { it.isFile }?.joinToString { it.name },
                    ),
                )
            }
        }
    }

    override fun getType(uri: Uri) = "vnd.android.cursor.dir/vnd.passandroid.diagnostics"
    override fun insert(uri: Uri, values: ContentValues?) = throw UnsupportedOperationException("Read only")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException("Read only")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException("Read only")

    private fun <T> Iterable<T>.toJson(map: (T) -> JSONObject): String = JSONArray().apply {
        forEach { put(map(it)) }
    }.toString()

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        .joinToString("") { "%02x".format(it) }
}
