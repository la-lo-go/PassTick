package org.ligi.passandroid.repository

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Persists user metadata that is independent from the imported pass file. */
class FilePassMetadataStore(private val backingFile: File) {
    private val tagsByPass = loadTags(backingFile)
    private val archivedPassIds = loadSet(backingFile, "archived")
    private val preferredArtworkByPass = loadPreferredArtwork(backingFile)

    @Synchronized fun tags(passId: String): Set<String> = tagsByPass[passId].orEmpty()

    @Synchronized fun setTags(passId: String, tags: Set<String>) {
        require(passId.isNotBlank()) { "Pass ID cannot be empty" }
        val normalized = tags.map(String::trim).filter(String::isNotEmpty).toSet()
        if (normalized == tagsByPass[passId].orEmpty()) return
        val updated = tagsByPass.toMutableMap().apply {
            if (normalized.isEmpty()) remove(passId) else put(passId, normalized)
        }
        persist(updated, archivedPassIds, preferredArtworkByPass)
        tagsByPass.clear()
        tagsByPass.putAll(updated)
    }

    @Synchronized fun isArchived(passId: String): Boolean = passId in archivedPassIds

    @Synchronized fun setArchived(passId: String, archived: Boolean) {
        require(passId.isNotBlank()) { "Pass ID cannot be empty" }
        if (archived == isArchived(passId)) return
        val updated = archivedPassIds.toMutableSet().apply {
            if (archived) add(passId) else remove(passId)
        }
        persist(tagsByPass, updated, preferredArtworkByPass)
        archivedPassIds.clear()
        archivedPassIds.addAll(updated)
    }

    @Synchronized fun preferredArtwork(passId: String): PassArtworkKind? = preferredArtworkByPass[passId]

    @Synchronized fun setPreferredArtwork(passId: String, kind: PassArtworkKind?) {
        require(passId.isNotBlank()) { "Pass ID cannot be empty" }
        if (kind == preferredArtworkByPass[passId]) return
        val updated = preferredArtworkByPass.toMutableMap().apply {
            if (kind == null) remove(passId) else put(passId, kind)
        }
        persist(tagsByPass, archivedPassIds, updated)
        preferredArtworkByPass.clear()
        preferredArtworkByPass.putAll(updated)
    }

    @Synchronized fun remove(passId: String) {
        val updatedTags = tagsByPass.toMutableMap().apply { remove(passId) }
        val updatedArchived = archivedPassIds.toMutableSet().apply { remove(passId) }
        val updatedArtwork = preferredArtworkByPass.toMutableMap().apply { remove(passId) }
        if (updatedTags == tagsByPass && updatedArchived == archivedPassIds && updatedArtwork == preferredArtworkByPass) return
        persist(updatedTags, updatedArchived, updatedArtwork)
        tagsByPass.clear()
        tagsByPass.putAll(updatedTags)
        archivedPassIds.clear()
        archivedPassIds.addAll(updatedArchived)
        preferredArtworkByPass.clear()
        preferredArtworkByPass.putAll(updatedArtwork)
    }

    private fun persist(
        tags: Map<String, Set<String>>,
        archived: Set<String>,
        preferredArtwork: Map<String, PassArtworkKind>,
    ) {
        val parent = requireNotNull(backingFile.absoluteFile.parentFile) { "Metadata file needs a parent directory" }
        check(parent.isDirectory || (!parent.exists() && parent.mkdirs())) { "Cannot create metadata directory" }
        val temporary = File.createTempFile("${backingFile.name}.", ".tmp", parent)
        try {
            val json = JSONObject().put("tags", JSONObject().apply {
                tags.toSortedMap().forEach { (id, values) -> put(id, JSONArray(values.sorted())) }
            }).put("archived", JSONArray(archived.sorted())).put("preferredArtwork", JSONObject().apply {
                preferredArtwork.toSortedMap().forEach { (id, kind) -> put(id, kind.name) }
            })
            temporary.writeText(json.toString())
            try {
                Files.move(temporary.toPath(), backingFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), backingFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            temporary.delete()
        }
    }

    private companion object {
        fun loadTags(file: File): MutableMap<String, Set<String>> = runCatching {
            val tags = JSONObject(file.readText()).optJSONObject("tags") ?: return@runCatching mutableMapOf()
            buildMap {
                tags.keys().forEach { id ->
                    val values = tags.optJSONArray(id) ?: return@forEach
                    put(id, buildSet { repeat(values.length()) { values.optString(it).trim().takeIf(String::isNotEmpty)?.let(::add) } })
                }
            }.toMutableMap()
        }.getOrDefault(mutableMapOf())

        fun loadSet(file: File, key: String): MutableSet<String> = runCatching {
            val values = JSONObject(file.readText()).optJSONArray(key) ?: return@runCatching mutableSetOf()
            buildSet { repeat(values.length()) { values.optString(it).trim().takeIf(String::isNotEmpty)?.let(::add) } }.toMutableSet()
        }.getOrDefault(mutableSetOf())

        fun loadPreferredArtwork(file: File): MutableMap<String, PassArtworkKind> = runCatching {
            val values = JSONObject(file.readText()).optJSONObject("preferredArtwork") ?: return@runCatching mutableMapOf()
            buildMap {
                values.keys().forEach { id ->
                    runCatching { PassArtworkKind.valueOf(values.getString(id)) }.getOrNull()?.let { put(id, it) }
                }
            }.toMutableMap()
        }.getOrDefault(mutableMapOf())
    }
}

/** Pinned passes use the old favorite file as a one-time compatible data source. */
class FilePinnedStore(backingFile: File, legacyFavoriteFile: File? = null) : FileFavoriteStore(
    backingFile,
    legacyFavoriteFile,
)
