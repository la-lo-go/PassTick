package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.nio.file.Files

class FilePassProtectionStoreTest {
    @Test
    fun `passes are public until explicitly protected`() {
        val store = FilePassProtectionStore(temporaryFile())

        assertThat(store.isProtected("pass-1")).isFalse()
    }

    @Test
    fun `protected state survives reopening the store`() {
        val file = temporaryFile()
        FilePassProtectionStore(file).setProtected("pass-1", true)

        assertThat(FilePassProtectionStore(file).isProtected("pass-1")).isTrue()
    }

    @Test
    fun `unprotecting a pass removes its persisted marker`() {
        val file = temporaryFile()
        FilePassProtectionStore(file).setProtected("pass-1", true)
        FilePassProtectionStore(file).setProtected("pass-1", false)

        assertThat(FilePassProtectionStore(file).isProtected("pass-1")).isFalse()
    }

    @Test
    fun `removing a pass clears its persisted protection`() {
        val file = temporaryFile()
        FilePassProtectionStore(file).setProtected("pass-1", true)
        FilePassProtectionStore(file).remove("pass-1")

        assertThat(FilePassProtectionStore(file).isProtected("pass-1")).isFalse()
    }

    @Test
    fun `corrupt metadata does not make passes protected`() {
        val file = temporaryFile().apply { writeText("not-json") }

        assertThat(FilePassProtectionStore(file).isProtected("pass-1")).isFalse()
    }

    @Test
    fun `failed persistence does not change protection in memory`() {
        val parentFile = temporaryFile().apply { writeText("not-a-directory") }
        val store = FilePassProtectionStore(parentFile.resolve("pass-protection.json"))

        assertThatThrownBy { store.setProtected("pass-1", true) }
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(store.isProtected("pass-1")).isFalse()
    }

    private fun temporaryFile() = Files.createTempFile("pass-protection", ".json").toFile().apply { delete() }
}
