package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.nio.file.Files
import org.json.JSONArray

class FileFavoriteStoreTest {
    @Test
    fun `passes are not favorite until explicitly marked`() {
        val store = FileFavoriteStore(temporaryFile())

        assertThat(store.isFavorite("pass-1")).isFalse()
    }

    @Test
    fun `favorite state survives reopening the store`() {
        val file = temporaryFile()
        FileFavoriteStore(file).setFavorite("pass-1", true)

        assertThat(FileFavoriteStore(file).isFavorite("pass-1")).isTrue()
    }

    @Test
    fun `unmarking a pass removes its persisted marker`() {
        val file = temporaryFile()
        FileFavoriteStore(file).setFavorite("pass-1", true)
        FileFavoriteStore(file).setFavorite("pass-1", false)

        assertThat(FileFavoriteStore(file).isFavorite("pass-1")).isFalse()
    }

    @Test
    fun `removing a pass clears its persisted favorite`() {
        val file = temporaryFile()
        FileFavoriteStore(file).setFavorite("pass-1", true)
        FileFavoriteStore(file).remove("pass-1")

        assertThat(FileFavoriteStore(file).isFavorite("pass-1")).isFalse()
    }

    @Test
    fun `corrupt metadata does not make passes favorite`() {
        val file = temporaryFile().apply { writeText("not-json") }

        assertThat(FileFavoriteStore(file).isFavorite("pass-1")).isFalse()
    }

    @Test
    fun `failed persistence does not change favorites in memory`() {
        val parentFile = temporaryFile().apply { writeText("not-a-directory") }
        val store = FileFavoriteStore(parentFile.resolve("pass-favorites.json"))

        assertThatThrownBy { store.setFavorite("pass-1", true) }
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(store.isFavorite("pass-1")).isFalse()
    }

    @Test
    fun `pinned store imports legacy favorites once and keeps an explicit unpin`() {
        val legacy = temporaryFile().apply { writeText(JSONArray(listOf("pass-1")).toString()) }
        val pinned = temporaryFile()
        FilePinnedStore(pinned, legacy).setFavorite("pass-1", false)

        assertThat(FilePinnedStore(pinned, legacy).isFavorite("pass-1")).isFalse()
    }

    private fun temporaryFile() = Files.createTempFile("pass-favorites", ".json").toFile().apply { delete() }
}
