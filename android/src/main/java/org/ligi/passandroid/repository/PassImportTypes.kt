package org.ligi.passandroid.repository

const val backupFileExtension = ".passtick"
const val backupMimeType = "application/vnd.passtick+zip"

fun isBackupFileName(name: String?): Boolean =
    name?.endsWith(backupFileExtension, ignoreCase = true) == true

/**
 * Formats offered when the user picks a pass file. Generic binaries are included because many
 * file providers report `.pkpass` files as `application/octet-stream`.
 */
val passFileImportMimeTypes = listOf(
    "application/vnd.apple.pkpass",
    "application/vnd.apple.pkpasses",
    "application/pkpass",
    "application/vnd.espass-espass",
    "application/vnd.espass-espass+zip",
    "application/zip",
    backupMimeType,
    "application/octet-stream",
)

val supportedPassImportMimeTypes = passFileImportMimeTypes + listOf(
    "application/pdf",
    "image/*",
)
