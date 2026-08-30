package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.AppleStylePassTranslation
import java.nio.charset.StandardCharsets
import java.nio.charset.Charset
import java.nio.file.Files

class TheAppleStylePassTranslation {

    @Test
    fun testThatNullTranslationWorks() {
        val tested = AppleStylePassTranslation()
        tested.loadFromString("")
        assertThat(tested.translate(null)).isNull()
    }

    @Test
    fun testThatBasicParsingWorks() {
        val tested = AppleStylePassTranslation()
        tested.loadFromString("\"foo\"=\"bar\";")
        assertThat(tested.translate("foo")).isEqualTo("bar")
    }

    @Test
    fun `reads UTF-8 without a byte order mark`() {
        assertDecoded("Grüße aus Köln".toByteArray(StandardCharsets.UTF_8), "Grüße aus Köln")
    }

    @Test
    fun `reads UTF-8 with a byte order mark`() {
        val content = "Crème brûlée".toByteArray(StandardCharsets.UTF_8)
        assertDecoded(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + content, "Crème brûlée")
    }

    @Test
    fun `reads little-endian UTF-16`() {
        val content = "日本語の搭乗券".toByteArray(StandardCharsets.UTF_16LE)
        assertDecoded(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + content, "日本語の搭乗券")
    }

    @Test
    fun `reads big-endian UTF-16`() {
        val content = "Billet à Genève".toByteArray(StandardCharsets.UTF_16BE)
        assertDecoded(byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + content, "Billet à Genève")
    }

    @Test
    fun `reads Shift-JIS translations`() {
        assertLegacyTranslation("Shift_JIS", "boarding", "\u642d\u4e57\u5238")
    }

    @Test
    fun `reads EUC-JP translations`() {
        assertLegacyTranslation("EUC-JP", "gate", "\u642d\u4e57\u53e3")
    }

    @Test
    fun `reads Big5 translations`() {
        assertLegacyTranslation("Big5", "ticket", "\u8eca\u7968")
    }

    private fun assertLegacyTranslation(charsetName: String, key: String, value: String) {
        val tested = AppleStylePassTranslation()
        val file = Files.createTempFile("pass-translation", ".strings").toFile()
        try {
            file.writeBytes("\"$key\"=\"$value\";".toByteArray(Charset.forName(charsetName)))
            tested.loadFromFile(file)
            assertThat(tested.translate(key)).isEqualTo(value)
        } finally {
            file.delete()
        }
    }

    private fun assertDecoded(bytes: ByteArray, expected: String) {
        val file = Files.createTempFile("pass-translation", ".strings").toFile()
        try {
            file.writeBytes(bytes)
            assertThat(AppleStylePassTranslation.readFileAsStringGuessEncoding(file)).isEqualTo(expected)
        } finally {
            file.delete()
        }
    }
}
