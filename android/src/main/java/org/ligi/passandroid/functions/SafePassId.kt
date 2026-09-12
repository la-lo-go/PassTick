package org.ligi.passandroid.functions

import java.io.File

private const val MAX_PASS_ID_LENGTH = 128

internal fun safePassIdOrNull(raw: String?): String? {
    val id = raw?.trim() ?: return null
    if (id.isEmpty() || id.length > MAX_PASS_ID_LENGTH) {
        return null
    }
    // A leading dot rejects the traversal tokens "." and ".." and hidden names.
    if (id.startsWith(".")) {
        return null
    }
    if (File(id).isAbsolute) {
        return null
    }
    if (id.any { it == '/' || it == '\\' || Character.isISOControl(it) }) {
        return null
    }
    return id
}
