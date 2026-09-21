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
import org.ligi.passandroid.imports.DocumentImportProcessor
import org.ligi.passandroid.imports.ImportDraft
import org.ligi.passandroid.imports.ImportEdits
import org.ligi.passandroid.imports.ImportSource
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassLocation
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

internal const val DOCUMENT_FILE_NAME = "source.pdf"
private const val MAIN_JSON_FILE_NAME = "main.json"
private const val IMPORT_CACHE_DIR = "import"
private const val PAGE_ZERO_FILE_NAME = "page0.png"
internal const val THUMBNAIL_MAX_DIMENSION = 384
private val STRIP_ARTWORK_FILE_NAMES = listOf(
    PassBitmapDefinitions.BITMAP_STRIP + org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES,
    PassBitmapDefinitions.BITMAP_STRIP + org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES_JPEG,
)
private val ARTWORK_EXTENSIONS = listOf(
    org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES,
    org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES_JPEG,
)

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
data class PassBarcodeSnapshot(
    val format: PassBarCodeFormat?,
    val message: String?,
    val alternativeText: String?,
)

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
    val importSource: ImportSource? = null,
    val hasDocument: Boolean = false,
    val documentPageCount: Int = 0,
    val barcodes: List<PassBarcodeSnapshot> = emptyList(),
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

    suspend fun prepareDocumentImport(uri: Uri, source: ImportSource): Result<ImportDraft>

    suspend fun commitDocumentImport(draft: ImportDraft, edits: ImportEdits): Result<PassSnapshot>

    suspend fun discardDocumentImport(draftId: String)

    suspend fun renderDocumentPage(id: String, pageIndex: Int, targetWidthPx: Int): Result<ByteArray>

    suspend fun create(update: PassUpdate): PassSnapshot

    suspend fun update(id: String, update: PassUpdate)

    suspend fun duplicate(id: String): PassSnapshot

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

    override suspend fun prepareDocumentImport(uri: Uri, source: ImportSource): Result<ImportDraft> =
        withContext(ioDispatcher) {
            runCatching {
                val id = UUID.randomUUID().toString()
                val staging = stagingDirectory(id).apply { mkdirs() }
                try {
                    when (source) {
                        ImportSource.IMAGE, ImportSource.CAMERA ->
                            prepareImageDraft(id, staging, uri, source)
                        ImportSource.PDF -> preparePdfDraft(id, staging, uri)
                    }
                } catch (error: Throwable) {
                    staging.deleteRecursively()
                    throw error
                }
            }
        }

    private fun prepareImageDraft(id: String, staging: File, uri: Uri, source: ImportSource): ImportDraft {
        val bitmap = DocumentImportProcessor.decodeNormalizedBitmap(context, uri)
            ?: error(context.getString(R.string.import_review_unreadable))
        val previewPng = DocumentImportProcessor.encodePng(bitmap)
        File(staging, PAGE_ZERO_FILE_NAME).writeBytes(previewPng)
        val displayName = DocumentImportProcessor.displayName(context, uri)
            .takeUnless { source == ImportSource.CAMERA }
        return ImportDraft(
            id = id,
            source = source,
            suggestedTitle = DocumentImportProcessor.suggestTitle(
                displayName = displayName,
                fallbackLabel = context.getString(R.string.import_title_photo),
                now = ZonedDateTime.now(),
            ),
            suggestedAccentColor = DocumentImportProcessor.suggestAccentColor(bitmap),
            pageCount = 1,
            previewPng = previewPng,
            detectedCodes = DocumentImportProcessor.detectCodes(bitmap),
        )
    }

    private fun preparePdfDraft(id: String, staging: File, uri: Uri): ImportDraft {
        val pdf = File(staging, DOCUMENT_FILE_NAME)
        DocumentImportProcessor.copyToFile(context, uri, pdf)
        val pageCount = DocumentImportProcessor.pdfPageCount(pdf)
        require(pageCount > 0) { context.getString(R.string.import_review_unreadable) }
        val pageZero = DocumentImportProcessor.renderPdfPage(pdf, 0, DocumentImportProcessor.MAX_DIMENSION)
            ?: error(context.getString(R.string.import_review_unreadable))
        val previewPng = DocumentImportProcessor.encodePng(pageZero)
        File(staging, PAGE_ZERO_FILE_NAME).writeBytes(previewPng)
        val detectedCodes = LinkedHashMap<Pair<PassBarCodeFormat, String>, org.ligi.passandroid.imports.DetectedCode>()
        DocumentImportProcessor.detectCodes(pageZero).forEach { code ->
            detectedCodes.putIfAbsent(code.format to code.message, code)
        }
        for (index in 1 until minOf(pageCount, DocumentImportProcessor.MAX_DETECTION_PAGES)) {
            val page = DocumentImportProcessor.renderPdfPage(
                pdf,
                index,
                DocumentImportProcessor.DETECTION_WIDTH_PX,
            ) ?: continue
            DocumentImportProcessor.detectCodes(page).forEach { code ->
                detectedCodes.putIfAbsent(code.format to code.message, code)
            }
        }
        return ImportDraft(
            id = id,
            source = ImportSource.PDF,
            suggestedTitle = DocumentImportProcessor.suggestTitle(
                displayName = DocumentImportProcessor.displayName(context, uri),
                fallbackLabel = context.getString(R.string.import_title_pdf),
                now = ZonedDateTime.now(),
            ),
            suggestedAccentColor = DocumentImportProcessor.suggestAccentColor(pageZero),
            pageCount = pageCount,
            previewPng = previewPng,
            detectedCodes = detectedCodes.values.toList(),
        )
    }

    override suspend fun commitDocumentImport(draft: ImportDraft, edits: ImportEdits): Result<PassSnapshot> =
        withContext(ioDispatcher) {
            runCatching {
                val staging = stagingDirectory(draft.id)
                val pageZero = File(staging, PAGE_ZERO_FILE_NAME)
                    .takeIf(File::isFile)
                    ?.readBytes()
                    ?.let(DocumentImportProcessor::decodePng)
                    ?: error(context.getString(R.string.import_review_unreadable))
                val rotation = ((edits.rotationDegrees % 360) + 360) % 360
                val artwork = DocumentImportProcessor.crop(
                    DocumentImportProcessor.rotate(pageZero, rotation),
                    edits.crop,
                )
                val pass = org.ligi.passandroid.model.pass.PassImpl(UUID.randomUUID().toString()).apply {
                    description = edits.title.trim().ifBlank { draft.suggestedTitle }
                    accentColor = edits.accentColor
                    type = PassType.EVENT
                    app = APP
                    importSource = draft.source
                    documentPageCount = draft.pageCount
                    val selectedCodes = edits.selectedCodeIndices.sorted()
                        .mapNotNull(draft.detectedCodes::getOrNull)
                    barCode = selectedCodes.firstOrNull()
                        ?.let { org.ligi.passandroid.model.pass.BarCode(it.format, it.message) }
                    barCodes = selectedCodes.drop(1).mapTo(mutableListOf()) {
                        org.ligi.passandroid.model.pass.BarCode(it.format, it.message)
                    }
                }
                val path = passStore.getPathForID(pass.id).apply { mkdirs() }
                val artworkExtension = if (draft.source == ImportSource.PDF) {
                    org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES
                } else {
                    org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES_JPEG
                }
                val artworkBytes = if (draft.source == ImportSource.PDF) {
                    DocumentImportProcessor.encodePng(artwork)
                } else {
                    DocumentImportProcessor.encodeJpeg(artwork)
                }
                File(path, PassBitmapDefinitions.BITMAP_STRIP + artworkExtension).writeBytes(artworkBytes)
                val thumbnail = DocumentImportProcessor.scaleToMaxDimension(artwork, THUMBNAIL_MAX_DIMENSION)
                File(
                    path,
                    PassBitmapDefinitions.BITMAP_THUMBNAIL + org.ligi.passandroid.model.pass.PassImpl.FILETYPE_IMAGES,
                ).writeBytes(DocumentImportProcessor.encodePng(thumbnail))
                if (draft.source == ImportSource.PDF) {
                    File(staging, DOCUMENT_FILE_NAME).copyTo(File(path, DOCUMENT_FILE_NAME), overwrite = true)
                }
                passStore.save(pass)
                passStore.classifier.moveToTopic(pass, context.getString(R.string.topic_new))
                staging.deleteRecursively()
                pass.toSnapshot(path, context.getString(R.string.topic_new))
            }
        }

    override suspend fun discardDocumentImport(draftId: String) = withContext(ioDispatcher) {
        stagingDirectory(draftId).deleteRecursively()
        Unit
    }

    override suspend fun renderDocumentPage(id: String, pageIndex: Int, targetWidthPx: Int): Result<ByteArray> =
        withContext(ioDispatcher) {
            runCatching {
                val file = File(passStore.getPathForID(id), DOCUMENT_FILE_NAME)
                require(file.isFile) { "Pass has no document" }
                val bitmap = DocumentImportProcessor.renderPdfPage(file, pageIndex, targetWidthPx)
                    ?: error("Cannot render the document page")
                DocumentImportProcessor.encodePng(bitmap)
            }
        }

    private fun stagingDirectory(draftId: String) = File(File(context.cacheDir, IMPORT_CACHE_DIR), draftId)

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

    override suspend fun duplicate(id: String): PassSnapshot = withContext(ioDispatcher) {
        val source = passStore.getPassbookForId(id) as? PassImpl ?: error("Pass not found")
        val copy = copyOf(source, UUID.randomUUID().toString())
        val sourceDirectory = passStore.getPathForID(source.id)
        val targetDirectory = passStore.getPathForID(copy.id).apply { mkdirs() }
        sourceDirectory.listFiles().orEmpty()
            .filter { it.isFile && it.name != MAIN_JSON_FILE_NAME }
            .forEach { it.copyTo(File(targetDirectory, it.name), overwrite = true) }
        passStore.save(copy)
        favoriteStore.setFavorite(copy.id, favoriteStore.isFavorite(source.id))
        protectionStore.setProtected(copy.id, protectionStore.isProtected(source.id))
        metadataStore.setTags(copy.id, metadataStore.tags(source.id))
        metadataStore.setNotes(copy.id, metadataStore.notes(source.id))
        metadataStore.setArchived(copy.id, metadataStore.isArchived(source.id))
        metadataStore.setPreferredArtwork(copy.id, metadataStore.preferredArtwork(source.id))
        // moveToTopic notifies observers, so the copy appears without an extra notifyChange.
        passStore.classifier.moveToTopic(copy, context.getString(R.string.topic_new))
        copy.toSnapshot(
            passStore.getPathForID(copy.id),
            context.getString(R.string.topic_new),
            protectionStore.isProtected(copy.id),
            favoriteStore.isFavorite(copy.id),
            metadataStore.tags(copy.id),
            metadataStore.isArchived(copy.id),
            metadataStore.preferredArtwork(copy.id),
            null,
            metadataStore.notes(copy.id),
        )
    }

    private fun copyOf(source: PassImpl, newId: String): PassImpl = PassImpl(newId).apply {
        accentColor = source.accentColor
        creator = source.creator
        type = source.type
        description = source.description
        app = source.app
        importSource = source.importSource
        documentPageCount = source.documentPageCount
        serial = source.serial
        passIdent = source.passIdent
        authToken = source.authToken
        webServiceURL = source.webServiceURL
        barCode = source.barCode?.let { code ->
            BarCode(code.format, code.message).apply { alternativeText = code.alternativeText }
        }
        barCodes = source.barCodes.mapTo(mutableListOf()) { code ->
            BarCode(code.format, code.message).apply { alternativeText = code.alternativeText }
        }
        fields = source.fields.mapTo(mutableListOf()) { field ->
            PassField(field.key, field.label, field.value, field.hide, field.hint)
        }
        locations = source.locations.map { location ->
            PassLocation().apply {
                name = location.name
                lat = location.lat
                lon = location.lon
            }
        }
        calendarTimespan = source.calendarTimespan?.let { PassImpl.TimeSpan(it.from, it.to, it.repeat) }
        validTimespans = source.validTimespans.map { PassImpl.TimeSpan(it.from, it.to, it.repeat) }
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
        // The editor manages a single code; imported extra codes would otherwise stay stale.
        pass.barCodes = mutableListOf()
    }

    private fun writeArtwork(id: String, updates: List<PassArtworkUpdate>) {
        updates.forEach { artwork ->
            val directory = passStore.getPathForID(id)
            val decoded = DocumentImportProcessor.decodeNormalizedBitmap(context, artwork.uri)
                ?: error("Cannot decode the selected image")
            val bitmap = if (artwork.kind == PassArtworkKind.THUMBNAIL) {
                DocumentImportProcessor.scaleToMaxDimension(decoded, THUMBNAIL_MAX_DIMENSION)
            } else {
                decoded
            }
            // A stale density variant outranks the replacement in bestArtworkFile.
            staleArtworkFiles(directory, artwork.kind).forEach { it.delete() }
            File(directory, artwork.kind.fileName + PassImpl.FILETYPE_IMAGES)
                .writeBytes(DocumentImportProcessor.encodePng(bitmap))
        }
    }

    private fun staleArtworkFiles(directory: File, kind: PassArtworkKind): List<File> =
        listOf("@3x", "@2x", "").flatMap { suffix ->
            ARTWORK_EXTENSIONS.map { extension -> File(directory, kind.fileName + suffix + extension) }
        }.filter { it.isFile }

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
    importSource = importSource ?: inferredImportSource(path),
    hasDocument = File(path, DOCUMENT_FILE_NAME).isFile,
    documentPageCount = documentPageCount,
    barcodes = (listOfNotNull(barCode) + barCodes)
        .distinctBy { it.format to it.message }
        .map { PassBarcodeSnapshot(it.format, it.message, it.alternativeText) },
)

/**
 * Passes imported as photo or PDF before document metadata existed carry only strip artwork
 * and no barcode. Treating them as document passes keeps them zoomable and visible on cards.
 */
private fun Pass.inferredImportSource(path: File): ImportSource? {
    val impl = this as? org.ligi.passandroid.model.pass.PassImpl ?: return null
    if (impl.app != APP || impl.barCode != null) return null
    if (File(path, DOCUMENT_FILE_NAME).isFile) return ImportSource.PDF
    if (STRIP_ARTWORK_FILE_NAMES.none { File(path, it).isFile }) return null
    val otherArtwork = PassArtworkKind.entries
        .filterNot { it == PassArtworkKind.STRIP }
        .any { bestArtworkFile(path, it) != null }
    return if (otherArtwork) null else ImportSource.IMAGE
}

internal fun bestArtworkFile(path: File, kind: PassArtworkKind): File? {
    val candidates = buildList {
        for (suffix in listOf("@3x", "@2x", "")) {
            for (extension in ARTWORK_EXTENSIONS) {
                add(File(path, kind.fileName + suffix + extension))
            }
        }
    }.filter { it.isFile && it.length() > 0L }
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
