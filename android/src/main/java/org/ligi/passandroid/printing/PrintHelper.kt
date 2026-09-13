package org.ligi.passandroid.printing

import android.content.Context
import android.graphics.Bitmap
import android.print.PrintManager
import androidx.core.content.getSystemService

fun doPrint(context: Context, jobName: String, bitmap: Bitmap) {
    val printManager = context.getSystemService<PrintManager>()!!
    printManager.print(jobName, PassImagePrintDocumentAdapter(context, bitmap, jobName), null)
}
