package org.ligi.passandroid.repository.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import okio.buffer
import okio.source
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.functions.createPassForImageImport
import org.ligi.passandroid.functions.createPassForPDFImport
import org.ligi.passandroid.functions.readJSONSafely
import org.ligi.passandroid.functions.safePassIdOrNull
import org.ligi.passandroid.model.InputStreamWithSource
import org.ligi.passandroid.model.PassStore
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*

object UnzipPassController : KoinComponent {

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
                processFile(FileUnzipControllerSpec(tempFile.absolutePath, spec))
                tempFile.delete()
            }
        } catch (e: Exception) {
            tracker.trackException("problem processing InputStream", e, false)
            spec.failCallback?.fail("problem with temp file: $e")
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

        extractArchive(spec, path)

        val manifestPassId = try {
            readManifestPassId(path)
        } catch (e: Exception) {
            spec.failCallback?.fail("Problem with manifest.json: $e")
            return
        }

        if (manifestPassId == null) {
            if (importImagePass(spec)) return
            if (importPdfPass(spec)) return
            spec.failCallback?.fail("Pass is not espass or pkpass format :-(")
            return
        }

        val uuid = safePassIdOrNull(manifestPassId) ?: generatedUuid
        moveExtractedPass(spec, path, uuid)
        spec.onSuccessCallback?.call(uuid)
    }

    private fun extractArchive(spec: FileUnzipControllerSpec, path: File) {
        try {
            val zipFile = ZipFile(spec.zipFileString)
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
        val bitmap = BitmapFactory.decodeFile(spec.zipFileString)
        val resources = spec.context.resources

        if (bitmap == null) return false

        val imagePass = createPassForImageImport(resources)
        val pathForID = spec.passStore.getPathForID(imagePass.id)
        pathForID.mkdirs()

        File(spec.zipFileString).copyTo(File(pathForID, "strip.png"))

        spec.passStore.save(imagePass)
        spec.passStore.classifier.moveToTopic(imagePass, "new")
        spec.onSuccessCallback?.call(imagePass.id)
        return true
    }

    private fun importPdfPass(spec: FileUnzipControllerSpec): Boolean {
        if (Build.VERSION.SDK_INT < 21) return false
        return try {
            val file = File(spec.zipFileString)
            val readUtf8 = file.source().buffer().readUtf8(4)
            if (readUtf8 != "%PDF") return false

            val open = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(open)

            val page = pdfRenderer.openPage(0)
            val ratio = page.height.toFloat() / page.width

            val resources = spec.context.resources
            val widthPixels = resources.displayMetrics.widthPixels
            val createBitmap = Bitmap.createBitmap(widthPixels, (widthPixels * ratio).toInt(), Bitmap.Config.ARGB_8888)
            page.render(createBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val imagePass = createPassForPDFImport(resources)
            val pathForID = spec.passStore.getPathForID(imagePass.id)
            pathForID.mkdirs()

            createBitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(File(pathForID, "strip.png")))

            spec.passStore.save(imagePass)
            spec.passStore.classifier.moveToTopic(imagePass, "new")
            spec.onSuccessCallback?.call(imagePass.id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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

    class InputStreamUnzipControllerSpec(internal val inputStreamWithSource: InputStreamWithSource, context: Context, passStore: PassStore,
                                         onSuccessCallback: SuccessCallback?, failCallback: FailCallback?) : UnzipControllerSpec(context, passStore, onSuccessCallback, failCallback)

}
