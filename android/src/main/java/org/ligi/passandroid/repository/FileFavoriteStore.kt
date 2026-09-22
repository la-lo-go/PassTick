package org.ligi.passandroid.repository

import org.json.JSONArray
import java.io.File

open class FileFavoriteStore(
    private val backingFile: File,
    legacyBackingFile: File? = null,
) {
    private val favoritePassIds = if (backingFile.isFile) load(backingFile) else {
        legacyBackingFile?.let(::load).orEmpty().toMutableSet()
    }

    @Synchronized
    fun isFavorite(passId: String): Boolean = passId in favoritePassIds

    @Synchronized
    fun setFavorite(passId: String, isFavorite: Boolean) {
        require(passId.isNotBlank()) { "Pass ID cannot be empty" }
        val updatedIds = favoritePassIds.toMutableSet()
        val changed = if (isFavorite) updatedIds.add(passId) else updatedIds.remove(passId)
        if (changed) {
            persist(updatedIds)
            favoritePassIds.clear()
            favoritePassIds.addAll(updatedIds)
        }
    }

    fun remove(passId: String) = setFavorite(passId, false)

    private fun persist(ids: Set<String>) {
        val parent = requireNotNull(backingFile.absoluteFile.parentFile) { "Favorites file needs a parent directory" }
        check(parent.isDirectory || (!parent.exists() && parent.mkdirs())) {
            "Cannot create favorites metadata directory"
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
