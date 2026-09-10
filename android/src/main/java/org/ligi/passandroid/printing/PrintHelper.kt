package org.ligi.passandroid.printing

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.print.PrintManager
import androidx.core.content.getSystemService

@TargetApi(Build.VERSION_CODES.KITKAT)
fun doPrint(context: Context, jobName: String, bitmap: Bitmap) {
    val printManager = context.getSystemService<PrintManager>()!!
    printManager.print(jobName, PassImagePrintDocumentAdapter(context, bitmap, jobName), null)
}
