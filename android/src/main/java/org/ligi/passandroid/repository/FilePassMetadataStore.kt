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
    private val trashedAtByPass = loadTrashedAt(backingFile)
    private val notesByPass = loadNotes(backingFile)

    @Synchronized fun tags(passId: String): Set<String> = tagsByPass[passId].orEmpty()

    @Synchronized fun setTags(passId: String, tags: Set<String>) {
        require(passId.isNotBlank()) { "Pass ID cannot be empty" }
        val normalized = tags.map(String::trim).filter(String::isNotEmpty).toSet()
        if (normalized == tagsByPass[passId].orEmpty()) return
        val updated = tagsByPass.toMutableMap().apply {
            if (normalized.isEmpty()) remove(passId) else put(passId, normalized)
        }
        persist(updated, archivedPassIds, preferredArtworkByPass, trashedAtByPass, notesByPass)
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
        persist(tagsByPass, updated, preferredArtworkByPass, trashedAtByPass, notesByPass)
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
        persist(tagsByPass, archivedPassIds, updated, trashedAtByPass, notesByPass)
        preferredArtworkByPass.clear()
        preferredArtworkByPass.putAll(updated)
    }

    @Synchronized fun trashedAt(passId: String): Long? = trashedAtByPass[passId]

    @Synchronized fun setTrashedAt(passId: String, epochMillis: Long?) {
        require(passId.isNotBlank()) { "Pass ID cannot be empty" }
        if (epochMillis == trashedAtByPass[passId]) return
        val updated = trashedAtByPass.toMutableMap().apply {
            if (epochMillis == null) remove(passId) else put(passId, epochMillis)
        }
        persist(tagsByPass, archivedPassIds, preferredArtworkByPass, updated, notesByPass)
        trashedAtByPass.clear()
        trashedAtByPass.putAll(updated)
    }

    @Synchronized fun notes(passId: String): String = notesByPass[passId].orEmpty()

    @Synchronized fun setNotes(passId: String, notes: String) {
        require(passId.isNotBlank()) { "Pass ID cannot be empty" }
        val normalized = notes.trim()
        if (normalized == notesByPass[passId].orEmpty()) return
        val updated = notesByPass.toMutableMap().apply {
            if (normalized.isEmpty()) remove(passId) else put(passId, normalized)
        }
        persist(tagsByPass, archivedPassIds, preferredArtworkByPass, trashedAtByPass, updated)
        notesByPass.clear()
        notesByPass.putAll(updated)
    }

    @Synchronized fun remove(passId: String) {
        val updatedTags = tagsByPass.toMutableMap().apply { remove(passId) }
        val updatedArchived = archivedPassIds.toMutableSet().apply { remove(passId) }
        val updatedArtwork = preferredArtworkByPass.toMutableMap().apply { remove(passId) }
        val updatedTrashedAt = trashedAtByPass.toMutableMap().apply { remove(passId) }
        val updatedNotes = notesByPass.toMutableMap().apply { remove(passId) }
        val changed = listOf(
            updatedTags != tagsByPass,
            updatedArchived != archivedPassIds,
            updatedArtwork != preferredArtworkByPass,
            updatedTrashedAt != trashedAtByPass,
            updatedNotes != notesByPass,
        ).any()
        if (!changed) return
        persist(updatedTags, updatedArchived, updatedArtwork, updatedTrashedAt, updatedNotes)
        tagsByPass.clear()
        tagsByPass.putAll(updatedTags)
        archivedPassIds.clear()
        archivedPassIds.addAll(updatedArchived)
        preferredArtworkByPass.clear()
        preferredArtworkByPass.putAll(updatedArtwork)
        trashedAtByPass.clear()
        trashedAtByPass.putAll(updatedTrashedAt)
        notesByPass.clear()
        notesByPass.putAll(updatedNotes)
    }

    private fun persist(
        tags: Map<String, Set<String>>,
        archived: Set<String>,
        preferredArtwork: Map<String, PassArtworkKind>,
        trashedAt: Map<String, Long>,
        notes: Map<String, String>,
    ) {
        val parent = requireNotNull(backingFile.absoluteFile.parentFile) { "Metadata file needs a parent directory" }
        check(parent.isDirectory || (!parent.exists() && parent.mkdirs())) { "Cannot create metadata directory" }
        val temporary = File.createTempFile("${backingFile.name}.", ".tmp", parent)
        try {
            val json = JSONObject().put("tags", JSONObject().apply {
                tags.toSortedMap().forEach { (id, values) -> put(id, JSONArray(values.sorted())) }
            }).put("archived", JSONArray(archived.sorted())).put("preferredArtwork", JSONObject().apply {
                preferredArtwork.toSortedMap().forEach { (id, kind) -> put(id, kind.name) }
            }).put("trashedAt", JSONObject().apply {
                trashedAt.toSortedMap().forEach { (id, millis) -> put(id, millis) }
            }).put("notes", JSONObject().apply {
                notes.toSortedMap().forEach { (id, value) -> put(id, value) }
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

        fun loadTrashedAt(file: File): MutableMap<String, Long> = runCatching {
            val values = JSONObject(file.readText()).optJSONObject("trashedAt") ?: return@runCatching mutableMapOf()
            buildMap {
                values.keys().forEach { id -> values.optLong(id, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let { put(id, it) } }
            }.toMutableMap()
        }.getOrDefault(mutableMapOf())

        fun loadNotes(file: File): MutableMap<String, String> = runCatching {
            val values = JSONObject(file.readText()).optJSONObject("notes") ?: return@runCatching mutableMapOf()
            buildMap {
                values.keys().forEach { id -> values.optString(id).takeIf(String::isNotEmpty)?.let { put(id, it) } }
            }.toMutableMap()
        }.getOrDefault(mutableMapOf())
    }
}

/** Pinned passes use the old favorite file as a one-time compatible data source. */
class FilePinnedStore(backingFile: File, legacyFavoriteFile: File? = null) : FileFavoriteStore(
    backingFile,
    legacyFavoriteFile,
)
