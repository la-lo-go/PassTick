package org.ligi.passandroid.functions

import android.content.Context
import android.net.Uri
import org.ligi.passandroid.Tracker
import org.ligi.passandroid.model.InputStreamWithSource

fun fromURI(context: Context, uri: Uri, tracker: Tracker): InputStreamWithSource? {
    tracker.trackEvent("protocol", "to_inputstream", uri.scheme, null)
    if (uri.scheme != "content") {
        tracker.trackException("Unsupported import URI scheme: ${uri.scheme}", false)
        return null
    }
    return context.contentResolver.openInputStream(uri)?.let {
        InputStreamWithSource(uri.toString(), it)
    }
}
