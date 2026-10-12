package org.ligi.passandroid.platform

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import org.ligi.passandroid.R
import org.ligi.passandroid.export.PassExportLabels
import org.ligi.passandroid.export.PassExportRequest
import org.ligi.passandroid.export.buildPassExportDocument
import org.ligi.passandroid.functions.getHumanCategoryString
import org.ligi.passandroid.platform.export.ExportImages
import org.ligi.passandroid.platform.export.ExportPage
import org.ligi.passandroid.platform.export.PassExportRenderer
import org.ligi.passandroid.platform.export.toBitmap
import org.ligi.passandroid.platform.export.writePdf
import org.ligi.passandroid.repository.PassExportFormat
import org.ligi.passandroid.ui.state.PassUiModel
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.io.OutputStream
import java.util.Locale

/** Writes a shareable PNG or PDF without exposing the app's private pass files. */
object PassImageExporter {
    const val EXPORT_WIDTH = 2000
    private const val ARTWORK_MAX_EDGE = 1600

    fun mimeType(format: PassExportFormat): String = if (format == PassExportFormat.PDF) "application/pdf" else "image/png"

    fun extension(format: PassExportFormat): String = if (format == PassExportFormat.PDF) "pdf" else "png"

    /** Writes the export in the format of the request. */
    fun write(output: OutputStream, pass: PassUiModel, request: PassExportRequest, labels: PassExportLabels) {
        when (request.options.format) {
            PassExportFormat.PNG -> writePng(output, pass, request, labels)
            PassExportFormat.PDF -> withPages(pass, request, labels, paginate = true) { it.writePdf(output) }
        }
    }

    fun write(resolver: ContentResolver, uri: Uri, pass: PassUiModel, request: PassExportRequest, labels: PassExportLabels) {
        val output = resolver.openOutputStream(uri, "wt") ?: error("Could not open the export destination")
        output.use { write(it, pass, request, labels) }
    }

    /** Saves a PNG to Pictures/PassTick. A failed write removes the pending gallery entry. */
    fun writeToGallery(
        resolver: ContentResolver,
        displayName: String,
        pass: PassUiModel,
        request: PassExportRequest,
        labels: PassExportLabels,
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PassTick")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create a gallery entry")
        try {
            val output = resolver.openOutputStream(uri, "w") ?: error("Could not open the gallery destination")
            output.use { writePng(it, pass, request, labels) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
        return uri
    }

    /** The first page as a bitmap. A sheet bitmap fits all its content on one page. */
    fun renderBitmap(
        pass: PassUiModel,
        request: PassExportRequest,
        labels: PassExportLabels,
        width: Int = EXPORT_WIDTH,
    ): Bitmap = withPages(pass, request, labels, paginate = false) { it.first().toBitmap(width) }

    internal fun pageCount(pass: PassUiModel, request: PassExportRequest, labels: PassExportLabels): Int =
        withPages(pass, request, labels, paginate = request.options.format == PassExportFormat.PDF) { it.size }

    private fun writePng(output: OutputStream, pass: PassUiModel, request: PassExportRequest, labels: PassExportLabels) {
        val bitmap = renderBitmap(pass, request, labels)
        try {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode PNG" }
        } finally {
            bitmap.recycle()
        }
    }

    private fun <T> withPages(
        pass: PassUiModel,
        request: PassExportRequest,
        labels: PassExportLabels,
        paginate: Boolean,
        use: (List<ExportPage>) -> T,
    ): T {
        val locale = Locale.getDefault()
        val document = buildPassExportDocument(
            pass,
            request.content,
            labels,
            locale,
            request.codeOnTop,
            request.excludedFieldIds,
        )
        return ExportImages(ARTWORK_MAX_EDGE).use { images ->
            use(PassExportRenderer(document, request.template, request.options, images, locale).pages(paginate))
        }
    }
}

/** The export labels in the current language. */
fun exportLabels(context: Context, pass: PassUiModel, today: LocalDate = LocalDate.now()): PassExportLabels = PassExportLabels(
    typeLabel = context.getString(getHumanCategoryString(pass.type)),
    date = context.getString(R.string.pass_detail_ticket_date),
    time = context.getString(R.string.pass_detail_ticket_time),
    starts = context.getString(R.string.export_starts),
    ends = context.getString(R.string.export_ends),
    expires = context.getString(R.string.export_expires),
    location = context.getString(R.string.export_location),
    notes = context.getString(R.string.export_notes),
    moreInformation = context.getString(R.string.pass_detail_more_information),
    pass = context.getString(R.string.export_pass),
    createdBy = context.getString(R.string.export_created_by),
    footer = context.getString(
        R.string.export_footer,
        today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())),
    ),
)
