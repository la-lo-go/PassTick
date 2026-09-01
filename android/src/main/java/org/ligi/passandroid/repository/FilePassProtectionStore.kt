package org.ligi.passandroid.repository

import org.json.JSONArray
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class FilePassProtectionStore(private val backingFile: File) {
    private val protectedPassIds = load(backingFile)

    @Synchronized
    fun isProtected(passId: String): Boolean = passId in protectedPassIds

    @Synchronized
    fun setProtected(passId: String, isProtected: Boolean) {
        require(passId.isNotBlank()) { "Pass ID cannot be empty" }
        val changed = if (isProtected) protectedPassIds.add(passId) else protectedPassIds.remove(passId)
        if (changed) persist()
    }

    fun remove(passId: String) = setProtected(passId, false)

    private fun persist() {
        val parent = backingFile.absoluteFile.parentFile ?: return
        if (!parent.exists() && !parent.mkdirs()) return

        val temporaryFile = File.createTempFile("${backingFile.name}.", ".tmp", parent)
        try {
            temporaryFile.writeText(JSONArray(protectedPassIds.sorted()).toString())
            replaceFile(temporaryFile, backingFile)
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

        fun replaceFile(source: File, target: File) {
            try {
                Files.move(
                    source.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
