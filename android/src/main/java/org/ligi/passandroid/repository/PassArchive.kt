package org.ligi.passandroid.repository

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import org.json.JSONObject
import org.ligi.passandroid.functions.safePassIdOrNull
import java.io.Closeable
import java.io.File
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal const val ARCHIVE_FORMAT_VERSION = 1
internal const val ARCHIVE_INDEX_FILE_NAME = "backup.json"
internal const val ARCHIVE_PASSES_DIRECTORY = "passes"
internal const val ARCHIVE_METADATA_DIRECTORY = "metadata"

/** A pass directory without this file is not restorable. */
internal const val ARCHIVE_PASS_FILE_NAME = "main.json"

/** One archive entry classified by name. One [Unsafe] entry rejects the whole archive. */
internal sealed interface ArchiveEntry {
    /** backup.json, the archive index. */
    object Index : ArchiveEntry

    data class PassFile(val entryName: String, val passId: String, val relativePath: String) : ArchiveEntry

    data class MetadataFile(val entryName: String, val fileName: String) : ArchiveEntry

    /** Entries the format does not define, for example directory markers. */
    object Other : ArchiveEntry

    data class Unsafe(val entryName: String) : ArchiveEntry
}

internal fun classifyArchiveEntry(entryName: String): ArchiveEntry {
    // Directory markers end with a separator and carry no content.
    val name = entryName.removeSuffix("/")
    if (name == ARCHIVE_INDEX_FILE_NAME) return ArchiveEntry.Index
    if (name.startsWith("$ARCHIVE_METADATA_DIRECTORY/")) {
        val fileName = name.removePrefix("$ARCHIVE_METADATA_DIRECTORY/")
        return if (isSafeArchiveRelativePath(fileName)) ArchiveEntry.MetadataFile(entryName, fileName)
        else ArchiveEntry.Unsafe(entryName)
    }
    if (!name.startsWith("$ARCHIVE_PASSES_DIRECTORY/")) return ArchiveEntry.Other
    val parts = name.split('/')
    val passId = safePassIdOrNull(parts.getOrNull(1)) ?: return ArchiveEntry.Unsafe(entryName)
    val relativePath = parts.drop(2).joinToString("/")
    if (relativePath.isEmpty()) return ArchiveEntry.Other
    return if (isSafeArchiveRelativePath(relativePath)) ArchiveEntry.PassFile(entryName, passId, relativePath)
    else ArchiveEntry.Unsafe(entryName)
}

/** Rejects absolute, empty, traversal, and control-character path segments inside an entry. */
internal fun isSafeArchiveRelativePath(path: String): Boolean {
    if (path.isEmpty() || File(path).isAbsolute) return false
    return path.split('/').all { segment ->
        segment.isNotEmpty() && segment != "." && segment != ".." &&
            segment.none { it == '\\' || Character.isISOControl(it) }
    }
}

/** Maps an app metadata file to its archive entry name. */
internal fun archiveMetadataEntryName(fileName: String) = "$ARCHIVE_METADATA_DIRECTORY/${File(fileName).name}"

/** Counts the outcomes of one restore run. */
internal fun archiveRestoreSummary(
    restored: Collection<String>,
    skipped: Collection<String>,
    failed: Collection<String>,
) = ArchiveRestoreSummary(restored.size, skipped.size, failed.size)

/** Moves an extracted pass into place. The pass root is created lazily, so a restore can fill an empty store. */
internal fun movePassIntoPlace(source: File, target: File) {
    target.parentFile?.mkdirs()
    try {
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
    } catch (_: AtomicMoveNotSupportedException) {
        source.copyRecursively(target, overwrite = false)
        source.deleteRecursively()
    }
}

/** Writes the backup archive to [output]. Pass files are copied verbatim. */
internal class PassArchiveWriter(output: OutputStream) : Closeable {
    private val zip = ZipOutputStream(output)
    private val parameters = ZipParameters().apply {
        compressionMethod = CompressionMethod.DEFLATE
        compressionLevel = CompressionLevel.NORMAL
    }

    fun addIndex(passCount: Int, exportedAtEpochMillis: Long) {
        val json = JSONObject()
            .put("formatVersion", ARCHIVE_FORMAT_VERSION)
            .put("exportedAtEpochMillis", exportedAtEpochMillis)
            .put("passCount", passCount)
        addBytes(ARCHIVE_INDEX_FILE_NAME, json.toString().toByteArray())
    }

    fun addFile(entryName: String, source: File) {
        parameters.fileNameInZip = entryName
        zip.putNextEntry(parameters)
        source.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    /** Adds every file below [directory] and keeps the relative layout. */
    fun addDirectory(entryName: String, directory: File) {
        directory.listFiles().orEmpty().sortedBy(File::getName).forEach { file ->
            if (file.isDirectory) addDirectory("$entryName/${file.name}", file)
            else addFile("$entryName/${file.name}", file)
        }
    }

    private fun addBytes(entryName: String, bytes: ByteArray) {
        parameters.fileNameInZip = entryName
        zip.putNextEntry(parameters)
        zip.write(bytes)
        zip.closeEntry()
    }

    override fun close() = zip.close()
}

/** Reads a backup archive. Every pass entry is validated before the caller extracts anything. */
internal class PassArchiveReader(private val archiveFile: File) : Closeable {
    private val zip = ZipFile(archiveFile)
    private val entries = zip.fileHeaders.map { it.fileName to classifyArchiveEntry(it.fileName) }
    private val passFiles = entries.mapNotNull { (_, entry) -> entry as? ArchiveEntry.PassFile }
    private val unsafeEntries = entries.mapNotNull { (_, entry) -> entry as? ArchiveEntry.Unsafe }

    /** Validates the index and returns the pass ids in archive order. */
    fun passIds(): List<String> {
        require(unsafeEntries.isEmpty()) { "Unsafe archive entry: ${unsafeEntries.first().entryName}" }
        validateIndex()
        return passFiles.map(ArchiveEntry.PassFile::passId).distinct()
    }

    /** True when the archive holds the pass main.json. */
    fun hasPassFile(passId: String): Boolean = passFiles.any {
        it.passId == passId && it.relativePath == ARCHIVE_PASS_FILE_NAME
    }

    fun extractPass(passId: String, targetDirectory: File) {
        val files = passFiles.filter { it.passId == passId }
        require(files.isNotEmpty()) { "Pass $passId has no files in the archive" }
        targetDirectory.mkdirs()
        files.forEach { entry -> extractEntry(entry.entryName, File(targetDirectory, entry.relativePath)) }
    }

    /** Extracts the metadata files into [targetDirectory] under their archive names. */
    fun extractMetadata(targetDirectory: File) {
        val metadataEntries = entries.mapNotNull { (_, entry) -> entry as? ArchiveEntry.MetadataFile }
        if (metadataEntries.isEmpty()) return
        targetDirectory.mkdirs()
        metadataEntries.forEach { entry -> extractEntry(entry.entryName, File(targetDirectory, entry.fileName)) }
    }

    private fun extractEntry(entryName: String, target: File) {
        val header = requireNotNull(zip.getFileHeader(entryName)) { "Missing archive entry $entryName" }
        target.parentFile?.mkdirs()
        zip.getInputStream(header).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun validateIndex() {
        val header = zip.getFileHeader(ARCHIVE_INDEX_FILE_NAME)
            ?: throw IllegalArgumentException("Not a PassTick backup archive")
        val index = runCatching { JSONObject(zip.getInputStream(header).use { it.readBytes().decodeToString() }) }
            .getOrNull()
            ?: throw IllegalArgumentException("Not a PassTick backup archive")
        require(index.optInt("formatVersion", 0) == ARCHIVE_FORMAT_VERSION) {
            "Unsupported backup format version"
        }
    }

    override fun close() = zip.close()
}
