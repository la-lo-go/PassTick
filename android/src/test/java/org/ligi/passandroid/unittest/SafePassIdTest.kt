package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.functions.safePassIdOrNull

class SafePassIdTest {

    @Test
    fun `accepts a UUID`() {
        val uuid = "0e5f7a3c-1f2b-4c9d-8a7e-6b5c4d3e2f10"

        assertThat(safePassIdOrNull(uuid)).isEqualTo(uuid)
    }

    @Test
    fun `accepts a simple name`() {
        assertThat(safePassIdOrNull("boarding-pass_1")).isEqualTo("boarding-pass_1")
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertThat(safePassIdOrNull("  pass-id  ")).isEqualTo("pass-id")
    }

    @Test
    fun `rejects null`() {
        assertThat(safePassIdOrNull(null)).isNull()
    }

    @Test
    fun `rejects the parent directory traversal`() {
        assertThat(safePassIdOrNull("../foo")).isNull()
    }

    @Test
    fun `rejects a forward slash`() {
        assertThat(safePassIdOrNull("a/b")).isNull()
    }

    @Test
    fun `rejects a backslash`() {
        assertThat(safePassIdOrNull("a\\b")).isNull()
    }

    @Test
    fun `rejects the parent directory token`() {
        assertThat(safePassIdOrNull("..")).isNull()
    }

    @Test
    fun `rejects the current directory token`() {
        assertThat(safePassIdOrNull(".")).isNull()
    }

    @Test
    fun `rejects an empty string`() {
        assertThat(safePassIdOrNull("")).isNull()
    }

    @Test
    fun `rejects a blank string`() {
        assertThat(safePassIdOrNull(" ")).isNull()
    }

    @Test
    fun `rejects a name with a leading dot`() {
        assertThat(safePassIdOrNull("...")).isNull()
    }

    @Test
    fun `rejects an absolute unix path`() {
        assertThat(safePassIdOrNull("/data/x")).isNull()
    }

    @Test
    fun `rejects an absolute windows path`() {
        assertThat(safePassIdOrNull("C:\\x")).isNull()
    }

    @Test
    fun `rejects an id longer than 128 characters`() {
        assertThat(safePassIdOrNull("a".repeat(200))).isNull()
    }

    @Test
    fun `accepts an id of exactly 128 characters`() {
        val id = "a".repeat(128)

        assertThat(safePassIdOrNull(id)).isEqualTo(id)
    }

    @Test
    fun `rejects a NUL character`() {
        assertThat(safePassIdOrNull("pass\u0000id")).isNull()
    }

    @Test
    fun `rejects a newline character`() {
        assertThat(safePassIdOrNull("pass\nid")).isNull()
    }

    @Test
    fun `uses the fallback UUID when the id is unsafe`() {
        val fallbackUuid = "0e5f7a3c-1f2b-4c9d-8a7e-6b5c4d3e2f10"

        assertThat(safePassIdOrNull("safe-id") ?: fallbackUuid).isEqualTo("safe-id")
        assertThat(safePassIdOrNull("../evil") ?: fallbackUuid).isEqualTo(fallbackUuid)
    }
}
