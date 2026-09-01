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
import org.ligi.passandroid.functions.APP
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.io.PassExporter
import org.ligi.passandroid.repository.io.UnzipPassController
import java.io.File
import java.util.UUID
import org.threeten.bp.ZonedDateTime

const val DEFAULT_PASS_CATEGORY_ID = "new"

data class PassFieldSnapshot(
    val key: String?,
    val label: String,
    val value: String,
    val hidden: Boolean,
    val hint: String?,
)
data class PassLocationSnapshot(val name: String?, val latitude: Double, val longitude: Double)
data class PassTimeSpanSnapshot(val from: ZonedDateTime?, val to: ZonedDateTime?)
enum class PassArtworkKind(val fileName: String) {
    ICON(PassBitmapDefinitions.BITMAP_ICON),
    LOGO(PassBitmapDefinitions.BITMAP_LOGO),
    STRIP(PassBitmapDefinitions.BITMAP_STRIP),
    THUMBNAIL(PassBitmapDefinitions.BITMAP_THUMBNAIL),
    FOOTER(PassBitmapDefinitions.BITMAP_FOOTER),
}
data class PassArtworkSnapshot(val kind: PassArtworkKind, val bytes: ByteArray)
data class PassArtworkUpdate(val kind: PassArtworkKind, val uri: Uri)

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
    val categoryId: String = DEFAULT_PASS_CATEGORY_ID,
    val artwork: List<PassArtworkSnapshot> = emptyList(),
    val isProtected: Boolean = false,
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
    val artworkUpdates: List<PassArtworkUpdate> = emptyList(),
    val calendarTimeSpan: PassTimeSpanSnapshot? = null,
    val locations: List<PassLocationSnapshot> = emptyList(),
)

interface PassRepository {
    fun observePasses(): Flow<List<PassSnapshot>>

    suspend fun import(uri: Uri): Result<PassSnapshot>

    suspend fun create(update: PassUpdate): PassSnapshot

    suspend fun update(id: String, update: PassUpdate)

    suspend fun moveToCategory(id: String, categoryId: String)

    suspend fun setProtected(id: String, isProtected: Boolean)

    suspend fun delete(id: String): Boolean

    suspend fun export(id: String, destination: Uri): Result<Unit>

    suspend fun prepareShare(id: String): Result<Uri>
}

class FilePassRepository(
    private val context: Context,
    private val passStore: PassStore,
    private val tracker: Tracker,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val protectionStore: FilePassProtectionStore = FilePassProtectionStore(
        File(context.filesDir, "pass-protection.json"),
    ),
) : PassRepository {
    override fun observePasses(): Flow<List<PassSnapshot>> = flow {
        passStore.syncPassStoreWithClassifier(context.getString(R.string.topic_new))
        emit(visibleSnapshots())
        emitAll(passStore.updates.map { visibleSnapshots() })
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
            pass.toSnapshot(passStore.getPathForID(pass.id), context.getString(R.string.topic_new))
        }
    }

    override suspend fun create(update: PassUpdate): PassSnapshot = withContext(ioDispatcher) {
        val pass = org.ligi.passandroid.model.pass.PassImpl(UUID.randomUUID().toString()).apply { app = APP }
        applyUpdate(pass, update)
        passStore.save(pass)
        writeArtwork(pass.id, update.artworkUpdates)
        passStore.classifier.moveToTopic(pass, context.getString(R.string.topic_new))
        pass.toSnapshot(passStore.getPathForID(pass.id), context.getString(R.string.topic_new))
    }

    override suspend fun update(id: String, update: PassUpdate) = withContext(ioDispatcher) {
        val pass = passStore.getPassbookForId(id) as? org.ligi.passandroid.model.pass.PassImpl
            ?: error("Pass not found")
        applyUpdate(pass, update)
        passStore.save(pass)
        writeArtwork(id, update.artworkUpdates)
        passStore.notifyChange()
    }

    override suspend fun moveToCategory(id: String, categoryId: String) = withContext(ioDispatcher) {
        val targetCategoryId = categoryId.trim()
        require(targetCategoryId.isNotEmpty()) { "Category cannot be empty" }
        val pass = passStore.getPassbookForId(id) ?: error("Pass not found")
        passStore.classifier.moveToTopic(pass, targetCategoryId)
    }

    override suspend fun setProtected(id: String, isProtected: Boolean) = withContext(ioDispatcher) {
        checkNotNull(passStore.getPassbookForId(id)) { "Pass not found" }
        protectionStore.setProtected(id, isProtected)
        passStore.notifyChange()
    }

    private fun applyUpdate(pass: org.ligi.passandroid.model.pass.PassImpl, update: PassUpdate) {
        pass.description = update.description
        pass.creator = update.creator
        pass.type = update.type
        pass.accentColor = update.accentColor
        pass.fields = update.fields.mapTo(mutableListOf()) {
            org.ligi.passandroid.model.pass.PassField(it.key, it.label, it.value, it.hidden, it.hint)
        }
        pass.calendarTimespan = update.calendarTimeSpan?.let {
            org.ligi.passandroid.model.pass.PassImpl.TimeSpan(from = it.from, to = it.to)
        }
        pass.locations = update.locations.map { location ->
            org.ligi.passandroid.model.pass.PassLocation().apply {
                name = location.name
                lat = location.latitude
                lon = location.longitude
            }
        }
        pass.barCode = update.barcodeFormat?.let { format ->
            org.ligi.passandroid.model.pass.BarCode(format, update.barcodeMessage).apply {
                alternativeText = update.barcodeAlternativeText.ifBlank { null }
            }
        }
    }

    private fun writeArtwork(id: String, updates: List<PassArtworkUpdate>) {
        updates.forEach { artwork ->
            val target = File(passStore.getPathForID(id), artwork.kind.fileName + org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES)
            val bitmap = context.contentResolver.openInputStream(artwork.uri)?.use(android.graphics.BitmapFactory::decodeStream)
                ?: error("Cannot decode the selected image")
            target.outputStream().use { output ->
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output))
            }
        }
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        passStore.deletePassWithId(id).also { deleted ->
            if (deleted) protectionStore.remove(id)
        }
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

    private fun snapshot() = passStore.passMap.values.map { pass ->
        pass.toSnapshot(
            passStore.getPathForID(pass.id),
            passStore.classifier.getTopic(pass.id, context.getString(R.string.topic_new)),
            protectionStore.isProtected(pass.id),
        )
    }

    private fun visibleSnapshots() = snapshot().filterNot { it.categoryId == "trash" }
}

private fun Pass.toSnapshot(path: File, categoryId: String, isProtected: Boolean = false) = PassSnapshot(
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
    categoryId = categoryId,
    artwork = PassArtworkKind.entries.mapNotNull { kind ->
        File(path, kind.fileName + org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES)
            .takeIf(File::isFile)
            ?.readBytes()
            ?.let { PassArtworkSnapshot(kind, it) }
    },
    isProtected = isProtected,
)
