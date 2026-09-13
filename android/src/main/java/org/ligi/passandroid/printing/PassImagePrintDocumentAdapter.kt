package org.ligi.passandroid.printing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.pdf.PrintedPdfDocument
import java.io.FileOutputStream
import java.io.IOException

class PassImagePrintDocumentAdapter(
    private val context: Context,
    private val bitmap: Bitmap,
    private val jobName: String,
) : PrintDocumentAdapter() {

    private var document: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        layoutResultCallback: LayoutResultCallback,
        bundle: Bundle,
    ) {
        if (cancellationSignal.isCanceled) {
            layoutResultCallback.onLayoutCancelled()
            return
        }
        document = PrintedPdfDocument(context, newAttributes)
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
            .setPageCount(1)
            .build()
        layoutResultCallback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pageRanges: Array<PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
    ) {
        val pdf = document
        if (pdf == null || cancellationSignal.isCanceled) {
            callback.onWriteCancelled()
            return
        }
        val page = pdf.startPage(0)
        drawBitmap(page.canvas)
        pdf.finishPage(page)
        try {
            pdf.writeTo(FileOutputStream(destination.fileDescriptor))
        } catch (error: IOException) {
            callback.onWriteFailed(error.message.orEmpty())
            return
        } finally {
            pdf.close()
            document = null
        }
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }

    private fun drawBitmap(canvas: Canvas) {
        val source = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val scale = minOf(canvas.width / source.width(), canvas.height / source.height())
        val width = source.width() * scale
        val height = source.height() * scale
        val left = (canvas.width - width) / 2f
        val top = (canvas.height - height) / 2f
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(left, top, left + width, top + height),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
    }
}
