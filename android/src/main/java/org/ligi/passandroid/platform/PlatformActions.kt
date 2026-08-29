package org.ligi.passandroid.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.ligi.passandroid.functions.createIntent
import org.ligi.passandroid.maps.PassbookMapsFacade
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassLocation
import org.ligi.passandroid.printing.doPrint

interface PlatformActions {
    fun addToCalendar(pass: Pass, timeSpan: PassImpl.TimeSpan)
    fun share(uri: Uri, mimeType: String)
    fun print(pass: Pass)
    fun openLocation(location: PassLocation)
}

class AndroidPlatformActions(private val context: Context) : PlatformActions {
    override fun addToCalendar(pass: Pass, timeSpan: PassImpl.TimeSpan) {
        context.startActivity(createIntent(pass, timeSpan).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun share(uri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun print(pass: Pass) = doPrint(context, pass)

    override fun openLocation(location: PassLocation) = PassbookMapsFacade.openLocation(context, location)
}
