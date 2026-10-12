package org.ligi.passandroid.printing

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Prints an exported PDF. The print system scales the vector pages to the paper, so text stays sharp. */
class PassPdfPrintDocumentAdapter(
    private val pdf: File,
    private val jobName: String,
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        layoutResultCallback: LayoutResultCallback,
        bundle: Bundle?,
    ) {
        if (cancellationSignal.isCanceled) {
            layoutResultCallback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        // The document does not depend on the paper, so a new layout never changes it.
        layoutResultCallback.onLayoutFinished(info, oldAttributes == null)
    }

    override fun onWrite(
        pageRanges: Array<PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onWriteCancelled()
            return
        }
        try {
            pdf.inputStream().use { input ->
                FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
            }
        } catch (error: IOException) {
            callback.onWriteFailed(error.message.orEmpty())
            return
        }
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }
}
