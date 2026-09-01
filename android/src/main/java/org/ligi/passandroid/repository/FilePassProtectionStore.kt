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
