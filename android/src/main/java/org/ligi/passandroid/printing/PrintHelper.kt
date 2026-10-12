package org.ligi.passandroid.printing

import android.content.Context
import android.print.PrintManager
import androidx.core.content.getSystemService
import java.io.File

fun doPrint(context: Context, jobName: String, pdf: File) {
    val printManager = context.getSystemService<PrintManager>()!!
    printManager.print(jobName, PassPdfPrintDocumentAdapter(pdf, jobName), null)
}
