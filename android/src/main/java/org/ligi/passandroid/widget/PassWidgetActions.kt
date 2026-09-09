package org.ligi.passandroid.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.ligi.passandroid.MainActivity

internal fun openPassIntent(context: Context, passId: String, showCode: Boolean = false): Intent {
    val uri = Uri.Builder()
        .scheme("passtick")
        .authority("pass")
        .appendPath(passId)
        .apply { if (showCode) appendQueryParameter("view", "code") }
        .build()
    return Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
}
