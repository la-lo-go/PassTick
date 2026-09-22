package org.ligi.passandroid.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** A provider can report a generic MIME type or none, so the file name is the primary signal. */
fun isBackupUri(context: Context, uri: Uri): Boolean =
    isBackupFileName(uri.lastPathSegment) ||
        isBackupFileName(displayName(context, uri)) ||
        runCatching { context.contentResolver.getType(uri) }.getOrNull() == backupMimeType

/** Providers such as Downloads hide the file name from the URI, so ask for the display name. */
private fun displayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
    }
}.getOrNull()
