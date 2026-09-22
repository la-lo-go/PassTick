package org.ligi.passandroid.repository

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
    "application/octet-stream",
)

val supportedPassImportMimeTypes = passFileImportMimeTypes + listOf(
    "application/pdf",
    "image/*",
)
