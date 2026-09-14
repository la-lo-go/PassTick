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
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime

const val DEFAULT_PASS_CATEGORY_ID = "new"

/** Trash expiry is enforced lazily at the next app open, which fits the offline-first app. */
val TRASH_RETENTION: Duration = Duration.ofDays(7)

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
    val isFavorite: Boolean = false,
    /** User labels. categoryId remains for compatibility with old classifier data. */
    val tagIds: Set<String> = emptySet(),
    val isArchived: Boolean = false,
    val preferredArtworkKind: PassArtworkKind? = null,
    val trashedAtEpochMillis: Long? = null,
    val notes: String = "",
) {
    val isPinned: Boolean get() = isFavorite
    val isTrashed: Boolean get() = trashedAtEpochMillis != null
}

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

    suspend fun setFavorite(id: String, isFavorite: Boolean)

    suspend fun setPinned(id: String, isPinned: Boolean) = setFavorite(id, isPinned)

    suspend fun setTags(id: String, tagIds: Set<String>)

    suspend fun setNotes(id: String, notes: String)

    suspend fun setArchived(id: String, isArchived: Boolean)

    suspend fun setPreferredArtwork(id: String, kind: PassArtworkKind?)

    suspend fun delete(id: String): Boolean

    suspend fun trashPass(id: String)

    suspend fun restoreFromTrash(id: String)

    fun observeTrashedPasses(): Flow<List<PassSnapshot>>

    suspend fun purgeExpiredTrash(retention: Duration)

    suspend fun emptyTrash()

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
    private val favoriteStore: FileFavoriteStore = FilePinnedStore(
        File(context.filesDir, "pass-pinned.json"),
        File(context.filesDir, "pass-favorites.json"),
    ),
    private val metadataStore: FilePassMetadataStore = FilePassMetadataStore(
        File(context.filesDir, "pass-metadata.json"),
    ),
) : PassRepository {
    private val migratedLegacyPassIds = mutableSetOf<String>()
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

    override suspend fun setFavorite(id: String, isFavorite: Boolean) = withContext(ioDispatcher) {
        checkNotNull(passStore.getPassbookForId(id)) { "Pass not found" }
        favoriteStore.setFavorite(id, isFavorite)
        passStore.notifyChange()
    }

    override suspend fun setTags(id: String, tagIds: Set<String>) = withContext(ioDispatcher) {
        checkNotNull(passStore.getPassbookForId(id)) { "Pass not found" }
        metadataStore.setTags(id, tagIds)
        passStore.notifyChange()
    }

    override suspend fun setNotes(id: String, notes: String) = withContext(ioDispatcher) {
        checkNotNull(passStore.getPassbookForId(id)) { "Pass not found" }
        metadataStore.setNotes(id, notes)
        passStore.notifyChange()
    }

    override suspend fun setArchived(id: String, isArchived: Boolean) = withContext(ioDispatcher) {
        checkNotNull(passStore.getPassbookForId(id)) { "Pass not found" }
        metadataStore.setArchived(id, isArchived)
        passStore.notifyChange()
    }

    override suspend fun setPreferredArtwork(id: String, kind: PassArtworkKind?) = withContext(ioDispatcher) {
        checkNotNull(passStore.getPassbookForId(id)) { "Pass not found" }
        metadataStore.setPreferredArtwork(id, kind)
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
            if (deleted) {
                protectionStore.remove(id)
                favoriteStore.remove(id)
                metadataStore.remove(id)
            }
        }
    }

    override suspend fun trashPass(id: String) = withContext(ioDispatcher) {
        checkNotNull(passStore.getPassbookForId(id)) { "Pass not found" }
        metadataStore.setTrashedAt(id, Instant.now().toEpochMilli())
        passStore.notifyChange()
    }

    override suspend fun restoreFromTrash(id: String) = withContext(ioDispatcher) {
        checkNotNull(passStore.getPassbookForId(id)) { "Pass not found" }
        metadataStore.setTrashedAt(id, null)
        passStore.notifyChange()
    }

    override fun observeTrashedPasses(): Flow<List<PassSnapshot>> = flow {
        emit(trashedSnapshots())
        emitAll(passStore.updates.map { trashedSnapshots() })
    }

    override suspend fun purgeExpiredTrash(retention: Duration) = withContext(ioDispatcher) {
        val now = Instant.now().toEpochMilli()
        trashedSnapshots().filter { snapshot ->
            snapshot.trashedAtEpochMillis?.let { trashedAt -> now - trashedAt > retention.toMillis() } == true
        }.forEach { delete(it.id) }
    }

    override suspend fun emptyTrash() = withContext(ioDispatcher) {
        trashedSnapshots().forEach { delete(it.id) }
    }

    private fun trashedSnapshots() = snapshot().filter(PassSnapshot::isTrashed)

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
            val target = File(context.cacheDir, "share/$id.espass")
            val exporter = PassExporter(passStore.getPathForID(id), target)
            exporter.export()
            exporter.exception?.let { throw it }
            FileProvider.getUriForFile(context, context.getString(R.string.authority_fileprovider), target)
        }
    }

    private fun snapshot() = passStore.passMap.values.map { pass ->
        val topic = passStore.classifier.getTopic(pass.id, context.getString(R.string.topic_new))
        val legacyTag = topic.takeUnless { it in setOf("new", "trash", "archive", "favorites", "past") }
        val isLegacy = pass.id !in migratedLegacyPassIds &&
            (topic in setOf("archive", "favorites", "past") || legacyTag != null)
        if (isLegacy) {
            if (topic == "favorites") favoriteStore.setFavorite(pass.id, true)
            if (topic == "archive") metadataStore.setArchived(pass.id, true)
            legacyTag?.let { metadataStore.setTags(pass.id, metadataStore.tags(pass.id) + it) }
            passStore.classifier.moveToTopic(pass, context.getString(R.string.topic_new))
            migratedLegacyPassIds += pass.id
        }
        val currentTopic = passStore.classifier.getTopic(pass.id, context.getString(R.string.topic_new))
        // Legacy classifier data keeps "trash" as a topic; it moves into trashedAt metadata on first read.
        val trashedAt = metadataStore.trashedAt(pass.id) ?: if (currentTopic == "trash") {
            Instant.now().toEpochMilli().also { metadataStore.setTrashedAt(pass.id, it) }
        } else {
            null
        }
        pass.toSnapshot(
            passStore.getPathForID(pass.id),
            currentTopic,
            protectionStore.isProtected(pass.id),
            favoriteStore.isFavorite(pass.id),
            metadataStore.tags(pass.id),
            metadataStore.isArchived(pass.id),
            metadataStore.preferredArtwork(pass.id),
            trashedAt,
            metadataStore.notes(pass.id),
        )
    }

    private fun visibleSnapshots() = snapshot().filterNot { it.isTrashed || it.categoryId == "trash" }
}

private fun Pass.toSnapshot(
    path: File,
    categoryId: String,
    isProtected: Boolean = false,
    isFavorite: Boolean = false,
    tagIds: Set<String> = emptySet(),
    isArchived: Boolean = false,
    preferredArtworkKind: PassArtworkKind? = null,
    trashedAtEpochMillis: Long? = null,
    notes: String = "",
) = PassSnapshot(
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
        bestArtworkFile(path, kind)
            ?.readBytes()
            ?.let { PassArtworkSnapshot(kind, it) }
    },
    isProtected = isProtected,
    isFavorite = isFavorite,
    tagIds = tagIds,
    isArchived = isArchived,
    preferredArtworkKind = preferredArtworkKind,
    trashedAtEpochMillis = trashedAtEpochMillis,
    notes = notes,
)

internal fun bestArtworkFile(path: File, kind: PassArtworkKind): File? {
    val candidates = listOf(
        File(path, "${kind.fileName}@3x${org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES}"),
        File(path, "${kind.fileName}@2x${org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES}"),
        File(path, kind.fileName + org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES),
    ).filter { it.isFile && it.length() > 0L }
    return candidates.mapNotNull { file -> pngPixelArea(file)?.let { area -> file to area } }
        .maxByOrNull { it.second }
        ?.first
        ?: candidates.firstOrNull()
}

private fun pngPixelArea(file: File): Long? = runCatching {
    val header = ByteArray(24)
    file.inputStream().use { input ->
        var offset = 0
        while (offset < header.size) {
            val count = input.read(header, offset, header.size - offset)
            if (count < 0) return null
            offset += count
        }
    }
    if (!header.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE) ||
        !header.copyOfRange(12, 16).contentEquals(PNG_IHDR)
    ) return null
    val width = header.readPositiveInt(16) ?: return null
    val height = header.readPositiveInt(20) ?: return null
    width.toLong() * height
}.getOrNull()

private fun ByteArray.readPositiveInt(offset: Int): Int? {
    val value = (this[offset].toInt() and 0xFF shl 24) or
        (this[offset + 1].toInt() and 0xFF shl 16) or
        (this[offset + 2].toInt() and 0xFF shl 8) or
        (this[offset + 3].toInt() and 0xFF)
    return value.takeIf { it > 0 }
}

private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
private val PNG_IHDR = byteArrayOf(0x49, 0x48, 0x44, 0x52)
