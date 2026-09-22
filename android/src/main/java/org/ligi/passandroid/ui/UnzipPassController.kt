package org.ligi.passandroid.repository.io

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import okio.buffer
import okio.source
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.functions.APP
import org.ligi.passandroid.functions.readJSONSafely
import org.ligi.passandroid.functions.safePassIdOrNull
import org.ligi.passandroid.imports.DocumentImportProcessor
import org.ligi.passandroid.imports.DetectedCode
import org.ligi.passandroid.imports.ImportSource
import org.ligi.passandroid.model.InputStreamWithSource
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.repository.DOCUMENT_FILE_NAME
import org.ligi.passandroid.repository.THUMBNAIL_MAX_DIMENSION
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*

object UnzipPassController : KoinComponent {

    private const val PKPASS_EXTENSION = ".pkpass"
    private const val PKPASSES_EXTENSION = ".pkpasses"
    private const val PKPASSES_MIME_TYPE = "application/vnd.apple.pkpasses"
    private const val ORIGINAL_FILE_NAME = "source.pkpass"
    private const val NOT_A_PASS_REASON = "Pass is not espass or pkpass format :-("

    val tracker :Tracker by inject()

    interface SuccessCallback {
        fun call(uuid: String)
    }

    interface FailCallback {
        fun fail(reason: String)
    }

    fun processInputStream(spec: InputStreamUnzipControllerSpec) {
        try {
            spec.inputStreamWithSource.inputStream.use {
                val tempFile = File.createTempFile("ins", "pass")
                it.copyTo(FileOutputStream(tempFile))
                if (isPkpassesBundle(spec)) {
                    processPkpassesBundle(spec, tempFile)
                } else {
                    processFile(FileUnzipControllerSpec(tempFile.absolutePath, spec))
                }
                tempFile.delete()
            }
        } catch (e: Exception) {
            tracker.trackException("problem processing InputStream", e, false)
            spec.failCallback?.fail("problem with temp file: $e")
        }

    }

    private fun isPkpassesBundle(spec: InputStreamUnzipControllerSpec): Boolean {
        val uri = Uri.parse(spec.inputStreamWithSource.source)
        return hasPkpassesName(uri.lastPathSegment) ||
            hasPkpassesName(displayName(spec.context, uri)) ||
            runCatching { spec.context.contentResolver.getType(uri) }.getOrNull() == PKPASSES_MIME_TYPE
    }

    private fun hasPkpassesName(name: String?): Boolean =
        name?.endsWith(PKPASSES_EXTENSION, ignoreCase = true) == true

    /** Providers such as Downloads hide the file name from the URI, so ask for the display name. */
    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
        }
    }.getOrNull()

    /**
     * A .pkpasses bundle is a ZIP of .pkpass files and has no manifest at the top level.
     * Every entry imports through the single-pass path; one broken entry must not stop the others.
     */
    private fun processPkpassesBundle(spec: InputStreamUnzipControllerSpec, bundleFile: File) {
        val bundlePath = File(spec.context.cacheDir, "temp/${UUID.randomUUID()}")
        bundlePath.mkdirs()
        try {
            extractArchive(bundleFile, bundlePath)
            val entries = bundlePath.listFiles().orEmpty()
                .filter { it.isFile && it.name.endsWith(PKPASS_EXTENSION, ignoreCase = true) }
                .sortedBy { it.name }
            var importedCount = 0
            var firstFailure: String? = null
            entries.forEach { entry ->
                var importedId: String? = null
                var entryFailure: String? = null
                val entrySpec = UnzipControllerSpec(
                    targetPath = spec.targetPath,
                    context = spec.context,
                    passStore = spec.passStore,
                    onSuccessCallback = object : SuccessCallback {
                        override fun call(uuid: String) {
                            importedId = uuid
                            spec.onSuccessCallback?.call(uuid)
                        }
                    },
                    failCallback = object : FailCallback {
                        override fun fail(reason: String) {
                            entryFailure = reason
                        }
                    },
                ).apply { overwrite = spec.overwrite }
                try {
                    processFile(FileUnzipControllerSpec(entry.absolutePath, spec.inputStreamWithSource.source, entrySpec))
                } catch (e: Exception) {
                    tracker.trackException("problem importing a pkpasses entry", e, false)
                    entryFailure = e.message
                }
                val id = importedId
                // The repository reports one pass per import, so load every entry here to keep it visible.
                if (id != null && spec.passStore.getPassbookForId(id) != null) {
                    importedCount++
                } else {
                    firstFailure = firstFailure ?: entryFailure ?: NOT_A_PASS_REASON
                }
            }
            if (importedCount == 0) {
                spec.failCallback?.fail(firstFailure ?: NOT_A_PASS_REASON)
            }
        } finally {
            bundlePath.deleteRecursively()
        }
    }

    private fun processFile(spec: FileUnzipControllerSpec) {

        val generatedUuid = UUID.randomUUID().toString()
        val path = File(spec.context.cacheDir, "temp/$generatedUuid")

        path.mkdirs()

        if (!path.exists()) {
            spec.failCallback?.fail("Problem creating the temp dir: $path")
            return
        }

        File(path, "source.obj").bufferedWriter().write(spec.source)

        extractArchive(File(spec.zipFileString), path)

        val manifestPassId = try {
            readManifestPassId(path)
        } catch (e: Exception) {
            spec.failCallback?.fail("Problem with manifest.json: $e")
            return
        }

        if (manifestPassId == null) {
            if (importImagePass(spec)) return
            if (importPdfPass(spec)) return
            spec.failCallback?.fail(NOT_A_PASS_REASON)
            return
        }

        val uuid = safePassIdOrNull(manifestPassId) ?: generatedUuid
        retainOriginalFile(spec, path)
        moveExtractedPass(spec, path, uuid)
        spec.onSuccessCallback?.call(uuid)
    }

    /** Keep the received archive so a later share returns the original bytes instead of a re-zip. */
    private fun retainOriginalFile(spec: FileUnzipControllerSpec, path: File) {
        val original = File(spec.zipFileString)
        if (original.isFile) {
            original.copyTo(File(path, ORIGINAL_FILE_NAME), overwrite = true)
        }
    }

    private fun extractArchive(archive: File, path: File) {
        try {
            val zipFile = ZipFile(archive)
            zipFile.extractAll(path.absolutePath)
        } catch (e: ZipException) {
            e.printStackTrace()
        }
    }

    private fun readManifestPassId(path: File): String? {
        val manifestFile = File(path, "manifest.json")
        val espassFile = File(path, "main.json")
        return when {
            manifestFile.exists() -> {
                val readToString = manifestFile.bufferedReader().readText()
                readJSONSafely(readToString)!!.getString("pass.json")
            }
            espassFile.exists() -> {
                val readToString = espassFile.bufferedReader().readText()
                readJSONSafely(readToString)!!.getString("id")
            }
            else -> null
        }
    }

    private fun importImagePass(spec: FileUnzipControllerSpec): Boolean {
        val bitmap = DocumentImportProcessor.decodeNormalizedBitmap(File(spec.zipFileString)) ?: return false
        val resources = spec.context.resources
        val imagePass = createDocumentPass(
            source = ImportSource.IMAGE,
            title = DocumentImportProcessor.suggestTitle(
                displayName = null,
                fallbackLabel = resources.getString(R.string.import_title_photo),
                now = ZonedDateTime.now(),
            ),
            pageCount = 1,
            codes = DocumentImportProcessor.detectCodes(bitmap),
            accentColor = DocumentImportProcessor.suggestAccentColor(bitmap),
        )
        val pathForID = spec.passStore.getPathForID(imagePass.id)
        pathForID.mkdirs()

        writeArtwork(pathForID, bitmap, jpeg = true)

        spec.passStore.save(imagePass)
        spec.passStore.classifier.moveToTopic(imagePass, "new")
        spec.onSuccessCallback?.call(imagePass.id)
        return true
    }

    private fun importPdfPass(spec: FileUnzipControllerSpec): Boolean {
        return try {
            val file = File(spec.zipFileString)
            val readUtf8 = file.source().buffer().readUtf8(4)
            if (readUtf8 != "%PDF") return false

            val pageCount = DocumentImportProcessor.pdfPageCount(file)
            if (pageCount <= 0) return false
            val pageZero = DocumentImportProcessor.renderPdfPage(
                file,
                0,
                DocumentImportProcessor.MAX_DIMENSION,
            ) ?: return false

            val resources = spec.context.resources
            val pdfPass = createDocumentPass(
                source = ImportSource.PDF,
                title = DocumentImportProcessor.suggestTitle(
                    displayName = null,
                    fallbackLabel = resources.getString(R.string.import_title_pdf),
                    now = ZonedDateTime.now(),
                ),
                pageCount = pageCount,
                codes = detectPdfCodes(file, pageZero, pageCount),
                accentColor = DocumentImportProcessor.suggestAccentColor(pageZero),
            )
            val pathForID = spec.passStore.getPathForID(pdfPass.id)
            pathForID.mkdirs()

            writeArtwork(pathForID, pageZero, jpeg = false)
            file.copyTo(File(pathForID, DOCUMENT_FILE_NAME), overwrite = true)

            spec.passStore.save(pdfPass)
            spec.passStore.classifier.moveToTopic(pdfPass, "new")
            spec.onSuccessCallback?.call(pdfPass.id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun detectPdfCodes(file: File, pageZero: android.graphics.Bitmap, pageCount: Int): List<DetectedCode> {
        val found = LinkedHashMap<Pair<PassBarCodeFormat, String>, DetectedCode>()
        DocumentImportProcessor.detectCodes(pageZero).forEach { found.putIfAbsent(it.format to it.message, it) }
        for (index in 1 until minOf(pageCount, DocumentImportProcessor.MAX_DETECTION_PAGES)) {
            val page = DocumentImportProcessor.renderPdfPage(
                file,
                index,
                DocumentImportProcessor.DETECTION_WIDTH_PX,
            ) ?: continue
            DocumentImportProcessor.detectCodes(page).forEach { found.putIfAbsent(it.format to it.message, it) }
        }
        return found.values.toList()
    }

    private fun createDocumentPass(
        source: ImportSource,
        title: String,
        pageCount: Int,
        codes: List<DetectedCode>,
        accentColor: Int,
    ) = PassImpl(UUID.randomUUID().toString()).apply {
        description = title
        this.accentColor = accentColor
        app = APP
        type = PassType.EVENT
        importSource = source
        documentPageCount = pageCount
        barCode = codes.firstOrNull()?.let { BarCode(it.format, it.message) }
        barCodes = codes.drop(1).mapTo(mutableListOf()) { BarCode(it.format, it.message) }
    }

    private fun moveExtractedPass(spec: FileUnzipControllerSpec, path: File, uuid: String) {
        spec.targetPath.mkdirs()
        val renamedFile = File(spec.targetPath, uuid)

        if (spec.overwrite && renamedFile.exists()) {
            renamedFile.deleteRecursively()
        }

        if (!renamedFile.exists()) {
            path.renameTo(renamedFile)
        } else {
            Timber.i("Pass with same ID exists")
        }
    }

    private fun writeArtwork(pathForID: File, bitmap: android.graphics.Bitmap, jpeg: Boolean) {
        val extension = if (jpeg) {
            PassImpl.FILETYPE_IMAGES_JPEG
        } else {
            PassImpl.FILETYPE_IMAGES
        }
        val bytes = if (jpeg) DocumentImportProcessor.encodeJpeg(bitmap) else DocumentImportProcessor.encodePng(bitmap)
        File(pathForID, PassBitmapDefinitions.BITMAP_STRIP + extension).writeBytes(bytes)
        val thumbnail = DocumentImportProcessor.scaleToMaxDimension(bitmap, THUMBNAIL_MAX_DIMENSION)
        File(pathForID, PassBitmapDefinitions.BITMAP_THUMBNAIL + PassImpl.FILETYPE_IMAGES)
            .writeBytes(DocumentImportProcessor.encodePng(thumbnail))
    }

    class InputStreamUnzipControllerSpec(internal val inputStreamWithSource: InputStreamWithSource, context: Context, passStore: PassStore,
                                         onSuccessCallback: SuccessCallback?, failCallback: FailCallback?) : UnzipControllerSpec(context, passStore, onSuccessCallback, failCallback)

}
