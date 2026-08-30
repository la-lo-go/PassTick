package org.ligi.passandroid.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ligi.passandroid.R
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.functions.fromURI
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.io.PassExporter
import org.ligi.passandroid.repository.io.UnzipPassController
import java.io.File
import org.threeten.bp.ZonedDateTime

data class PassFieldSnapshot(
    val key: String?,
    val label: String,
    val value: String,
    val hidden: Boolean,
    val hint: String?,
)
data class PassLocationSnapshot(val name: String?, val latitude: Double, val longitude: Double)
data class PassTimeSpanSnapshot(val from: ZonedDateTime?, val to: ZonedDateTime?)

data class PassSnapshot(
    val id: String,
    val description: String,
    val creator: String?,
    val type: PassType,
    val accentColor: Int,
    val barcodeFormat: PassBarCodeFormat?,
    val barcodeMessage: String?,
    val barcodeAlternativeText: String?,
    val fields: List<PassFieldSnapshot>,
    val locations: List<PassLocationSnapshot>,
    val calendarTimeSpan: PassTimeSpanSnapshot?,
)

data class PassUpdate(
    val description: String,
    val creator: String,
    val type: PassType,
    val accentColor: Int,
    val barcodeFormat: PassBarCodeFormat?,
    val barcodeMessage: String,
    val barcodeAlternativeText: String,
    val fields: List<PassFieldSnapshot>,
)

interface PassRepository {
    fun observePasses(): Flow<List<PassSnapshot>>

    suspend fun import(uri: Uri): Result<PassSnapshot>

    suspend fun update(id: String, update: PassUpdate)

    suspend fun delete(id: String): Boolean

    suspend fun export(id: String, destination: Uri): Result<Unit>

    suspend fun prepareShare(id: String): Result<Uri>
}

class FilePassRepository(
    private val context: Context,
    private val passStore: PassStore,
    private val tracker: Tracker,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PassRepository {
    override fun observePasses(): Flow<List<PassSnapshot>> = flow {
        passStore.syncPassStoreWithClassifier(context.getString(R.string.topic_new))
        emit(snapshot())
        emitAll(passStore.updates.map { snapshot() })
    }

    override suspend fun import(uri: Uri): Result<PassSnapshot> = withContext(ioDispatcher) {
        runCatching {
            val source = requireNotNull(fromURI(context, uri, tracker)) { "Cannot open the selected file" }
            var importedId: String? = null
            var failure: String? = null
            val spec = UnzipPassController.InputStreamUnzipControllerSpec(
                source,
                context,
                passStore,
                object : UnzipPassController.SuccessCallback {
                    override fun call(uuid: String) {
                        importedId = uuid
                    }
                },
                object : UnzipPassController.FailCallback {
                    override fun fail(reason: String) {
                        failure = reason
                    }
                },
            )
            UnzipPassController.processInputStream(spec)
            failure?.let { error(it) }
            val pass = requireNotNull(importedId?.let(passStore::getPassbookForId)) { "Imported pass is unreadable" }
            passStore.classifier.moveToTopic(pass, context.getString(R.string.topic_new))
            pass.toSnapshot()
        }
    }

    override suspend fun update(id: String, update: PassUpdate) = withContext(ioDispatcher) {
        val pass = passStore.getPassbookForId(id) as? org.ligi.passandroid.model.pass.PassImpl
            ?: error("Pass not found")
        pass.description = update.description
        pass.creator = update.creator
        pass.type = update.type
        pass.accentColor = update.accentColor
        pass.fields = update.fields.mapTo(mutableListOf()) {
            org.ligi.passandroid.model.pass.PassField(it.key, it.label, it.value, it.hidden, it.hint)
        }
        pass.barCode = update.barcodeFormat?.let { format ->
            org.ligi.passandroid.model.pass.BarCode(format, update.barcodeMessage).apply {
                alternativeText = update.barcodeAlternativeText.ifBlank { null }
            }
        }
        passStore.save(pass)
        passStore.notifyChange()
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        passStore.deletePassWithId(id)
    }

    override suspend fun export(id: String, destination: Uri): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val target = File.createTempFile("pass-export-", ".espass", context.cacheDir)
            try {
                val exporter = PassExporter(passStore.getPathForID(id), target)
                exporter.export()
                exporter.exception?.let { throw it }
                context.contentResolver.openOutputStream(destination)?.use { output ->
                    target.inputStream().use { it.copyTo(output) }
                } ?: error("Cannot open the export destination")
                Unit
            } finally {
                target.delete()
            }
        }
    }

    override suspend fun prepareShare(id: String): Result<Uri> = withContext(ioDispatcher) {
        runCatching {
            val target = File(context.filesDir, "share/$id.espass")
            val exporter = PassExporter(passStore.getPathForID(id), target)
            exporter.export()
            exporter.exception?.let { throw it }
            FileProvider.getUriForFile(context, context.getString(R.string.authority_fileprovider), target)
        }
    }

    private fun snapshot() = passStore.passMap.values.map(Pass::toSnapshot)
}

private fun Pass.toSnapshot() = PassSnapshot(
    id = id,
    description = description.orEmpty(),
    creator = creator,
    type = type,
    accentColor = accentColor,
    barcodeFormat = barCode?.format,
    barcodeMessage = barCode?.message,
    barcodeAlternativeText = barCode?.alternativeText,
    fields = fields.map { PassFieldSnapshot(it.key, it.label.orEmpty(), it.value.orEmpty(), it.hide, it.hint) },
    locations = locations.map { PassLocationSnapshot(it.name, it.lat, it.lon) },
    calendarTimeSpan = calendarTimespan?.let { PassTimeSpanSnapshot(it.from, it.to) }
        ?: validTimespans?.firstOrNull()?.let { PassTimeSpanSnapshot(it.from, it.to) },
)
