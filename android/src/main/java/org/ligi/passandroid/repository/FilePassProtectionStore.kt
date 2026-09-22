package org.ligi.passandroid.repository

import org.json.JSONArray
import java.io.File

class FilePassProtectionStore(private val backingFile: File) {
    private val protectedPassIds = load(backingFile)

    @Synchronized
    fun isProtected(passId: String): Boolean = passId in protectedPassIds

    @Synchronized
    fun setProtected(passId: String, isProtected: Boolean) {
        require(passId.isNotBlank()) { "Pass ID cannot be empty" }
        val updatedIds = protectedPassIds.toMutableSet()
        val changed = if (isProtected) updatedIds.add(passId) else updatedIds.remove(passId)
        if (changed) {
            persist(updatedIds)
            protectedPassIds.clear()
            protectedPassIds.addAll(updatedIds)
        }
    }

    fun remove(passId: String) = setProtected(passId, false)

    private fun persist(ids: Set<String>) {
        val parent = requireNotNull(backingFile.absoluteFile.parentFile) { "Protection file needs a parent directory" }
        check(parent.isDirectory || (!parent.exists() && parent.mkdirs())) {
            "Cannot create protection metadata directory"
        }

        val temporaryFile = File.createTempFile("${backingFile.name}.", ".tmp", parent)
        try {
            temporaryFile.writeText(JSONArray(ids.sorted()).toString())
            replaceFileAtomically(temporaryFile, backingFile)
        } finally {
            temporaryFile.delete()
        }
    }

    private companion object {
        fun load(file: File): MutableSet<String> {
            if (!file.isFile) return mutableSetOf()
            return runCatching {
                val array = JSONArray(file.readText())
                buildSet {
                    repeat(array.length()) { index ->
                        array.optString(index).takeIf(String::isNotBlank)?.let(::add)
                    }
                }.toMutableSet()
            }.getOrDefault(mutableSetOf())
        }

    }
}
