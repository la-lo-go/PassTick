package org.ligi.passandroid.model

import androidx.annotation.VisibleForTesting
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

class AppleStylePassTranslation : HashMap<String, String>() {
    fun translate(key: String?): String? = key?.let { get(it) ?: it }

    fun loadFromFile(file: File) {
        loadFromString(readFileAsStringGuessEncoding(file))
    }

    @VisibleForTesting
    fun loadFromString(input: String?) {
        input ?: return
        input.split("\";").forEach { pair ->
            val keyValue = pair.split(Regex("\" ?= ?\""))
            if (keyValue.size == 2) {
                put(removeLeadingClutter(keyValue[0]), keyValue[1])
            }
        }
    }

    private fun removeLeadingClutter(value: String): String =
        value.dropWhile { it == '"' || it == '\n' || it == '\r' || it == ' ' || it == '\uFEFF' }

    companion object {
        fun readFileAsStringGuessEncoding(file: File): String? = runCatching {
            decode(file.readBytes())
        }.getOrNull()

        private fun decode(bytes: ByteArray): String {
            if (bytes.isEmpty()) return ""

            return when {
                bytes.startsWith(UTF8_BOM) -> String(bytes, UTF8_BOM.size, bytes.size - UTF8_BOM.size, StandardCharsets.UTF_8)
                bytes.startsWith(UTF16_LE_BOM) -> String(bytes, UTF16_LE_BOM.size, bytes.size - UTF16_LE_BOM.size, StandardCharsets.UTF_16LE)
                bytes.startsWith(UTF16_BE_BOM) -> String(bytes, UTF16_BE_BOM.size, bytes.size - UTF16_BE_BOM.size, StandardCharsets.UTF_16BE)
                else -> decodeUtf8(bytes)
            }
        }

        private fun decodeUtf8(bytes: ByteArray): String {
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            return try {
                decoder.decode(ByteBuffer.wrap(bytes)).toString()
            } catch (_: CharacterCodingException) {
                String(bytes, StandardCharsets.ISO_8859_1)
            }
        }

        private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        private val UTF16_LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        private val UTF16_BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())

        private fun ByteArray.startsWith(prefix: ByteArray) =
            size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
    }
}
