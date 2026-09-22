package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PassImportTypesTest {
    @Test
    fun `supports pass image pdf and generic document providers`() {
        assertThat(supportedPassImportMimeTypes).contains(
            "application/vnd.apple.pkpass",
            "application/vnd.espass-espass+zip",
            "image/*",
            "application/pdf",
            "application/octet-stream",
        )
    }

    @Test
    fun `pass file import types contain archive formats only`() {
        assertThat(passFileImportMimeTypes).contains(
            "application/vnd.apple.pkpass",
            "application/pkpass",
            "application/vnd.espass-espass",
            "application/vnd.espass-espass+zip",
            "application/zip",
            "application/octet-stream",
        )
        assertThat(passFileImportMimeTypes).doesNotContain("image/*", "application/pdf")
    }

    @Test
    fun `pass file import types offer the backup format`() {
        assertThat(passFileImportMimeTypes).contains(backupMimeType)
        assertThat(supportedPassImportMimeTypes).contains(backupMimeType)
    }

    @Test
    fun `backup file names end with the passtick extension`() {
        assertThat(isBackupFileName("passtick-backup-2026-09-22.passtick")).isTrue()
        assertThat(isBackupFileName("BACKUP.PASSTICK")).isTrue()
        assertThat(isBackupFileName("passtick-backup-2026-09-22")).isFalse()
        assertThat(isBackupFileName("backup.zip")).isFalse()
        assertThat(isBackupFileName(null)).isFalse()
    }
}
