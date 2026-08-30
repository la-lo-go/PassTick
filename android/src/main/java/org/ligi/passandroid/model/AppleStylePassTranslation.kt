package org.ligi.passandroid.model

import androidx.annotation.VisibleForTesting
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
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
                else -> decodeWithoutBom(bytes)
            }
        }

        private fun decodeWithoutBom(bytes: ByteArray): String {
            strictDecode(bytes, StandardCharsets.UTF_8)?.let { return it }
            return listOf("Shift_JIS", "EUC-JP", "Big5", "windows-1252", "ISO-8859-1")
                .mapNotNull { name ->
                    val charset = Charset.forName(name)
                    strictDecode(bytes, charset)?.let { DecodedText(it, score(it, name)) }
                }
                .maxByOrNull(DecodedText::score)
                ?.text
                ?: String(bytes, StandardCharsets.ISO_8859_1)
        }

        private fun strictDecode(bytes: ByteArray, charset: Charset): String? {
            val decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            return try {
                decoder.decode(ByteBuffer.wrap(bytes)).toString()
            } catch (_: CharacterCodingException) {
                null
            }
        }

        private fun score(text: String, charsetName: String): Int {
            val translationPairs = TRANSLATION_PAIR.findAll(text).count()
            val letters = text.count(Char::isLetter)
            val controls = text.count { Character.isISOControl(it) && it !in "\r\n\t" }
            val halfWidthKatakana = text.count { it in '\uFF61'..'\uFF9F' }
            val japaneseKana = text.count { it in '\u3040'..'\u30FF' }
            val legacyPreference = when (charsetName) {
                "Shift_JIS", "EUC-JP", "Big5" -> 5
                else -> 0
            }
            return translationPairs * 1_000 + letters * 2 + japaneseKana * 10 - controls * 100 -
                halfWidthKatakana * 8 + legacyPreference
        }

        private data class DecodedText(val text: String, val score: Int)

        private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        private val UTF16_LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        private val UTF16_BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        private val TRANSLATION_PAIR = Regex("\\\"[^\\\"]+\\\"\\s*=\\s*\\\"[^\\\"]*\\\"\\s*;")

        private fun ByteArray.startsWith(prefix: ByteArray) =
            size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
    }
}
