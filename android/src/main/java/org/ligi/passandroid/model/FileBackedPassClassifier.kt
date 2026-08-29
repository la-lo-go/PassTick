package org.ligi.passandroid.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class FileBackedPassClassifier(
    private val backingFile: File,
    passStore: PassStore,
    moshi: Moshi,
) : PassClassifier(loadMap(backingFile, moshi), passStore) {
    private val adapter = mapAdapter(moshi)

    override fun processDataChange() {
        super.processDataChange()
        persist()
    }

    private fun persist() {
        val parent = backingFile.absoluteFile.parentFile ?: return
        if (!parent.exists() && !parent.mkdirs()) return

        val temporaryFile = File.createTempFile("${backingFile.name}.", ".tmp", parent)
        try {
            temporaryFile.writeText(adapter.toJson(topicByIdMap))
            replaceFile(temporaryFile, backingFile)
        } finally {
            temporaryFile.delete()
        }
    }

    companion object {
        private fun mapAdapter(moshi: Moshi): JsonAdapter<Map<String, String>> {
            val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            return moshi.adapter(type)
        }

        private fun loadMap(file: File, moshi: Moshi): MutableMap<String, String> {
            if (!file.isFile) return mutableMapOf()
            return runCatching { mapAdapter(moshi).fromJson(file.readText()) }
                .getOrNull()
                ?.toMutableMap()
                ?: mutableMapOf()
        }

        private fun replaceFile(source: File, target: File) {
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
