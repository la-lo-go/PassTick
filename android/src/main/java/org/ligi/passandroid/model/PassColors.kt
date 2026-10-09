package org.ligi.passandroid.model

import kotlin.math.roundToInt

private const val OPAQUE = 0xFF shl 24
private const val CHANNEL_MAX = 255f
private const val HUE_BUCKETS = 12
private const val FULL_TURN = 360f
private const val MIN_OPAQUE_ALPHA = 128
// A background below this chroma reads as grey, white, or black and carries no brand color.
private const val MIN_BRAND_CHROMA = 0.18f
// Artwork pixels need a clear hue and must not be near white or black to count as brand color.
private const val MIN_PIXEL_CHROMA = 0.25f
private const val MIN_PIXEL_LIGHTNESS = 0.12f
private const val MAX_PIXEL_LIGHTNESS = 0.88f
// The brand hue must cover this share of the opaque artwork, so a small colored detail does not win.
private const val MIN_BRAND_COVERAGE = 0.2f

private val RGB_FUNCTION = Regex("^rgba?\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*(?:,\\s*([\\d.]+)\\s*)?\\)$", RegexOption.IGNORE_CASE)
private val HEX_COLOR = Regex("^#([0-9a-f]{3}|[0-9a-f]{6}|[0-9a-f]{8})$", RegexOption.IGNORE_CASE)

private val NAMED_COLORS = mapOf(
    "black" to 0x000000, "white" to 0xFFFFFF, "red" to 0xFF0000, "green" to 0x00FF00, "blue" to 0x0000FF,
    "yellow" to 0xFFFF00, "cyan" to 0x00FFFF, "magenta" to 0xFF00FF, "gray" to 0x888888, "grey" to 0x888888,
)

/**
 * Parses a pass.json color. Apple documents the `rgb(r, g, b)` form; issuers also use hex and color names.
 * Returns an opaque ARGB color, or null when the value is not a color.
 */
fun parsePassColor(value: String): Int? {
    val text = value.trim()
    RGB_FUNCTION.matchEntire(text)?.let { match ->
        val (r, g, b) = match.groupValues.drop(1).take(3).map { it.toInt().coerceIn(0, 255) }
        return OPAQUE or (r shl 16) or (g shl 8) or b
    }
    HEX_COLOR.matchEntire(text)?.let { match ->
        val hex = match.groupValues[1]
        return when (hex.length) {
            3 -> OPAQUE or hex.map { "$it$it" }.joinToString("").toInt(16)
            6 -> OPAQUE or hex.toInt(16)
            else -> hex.toLong(16).toInt()
        }
    }
    return NAMED_COLORS[text.lowercase()]?.let { OPAQUE or it }
}

private class Rgb(color: Int) {
    val r = (color shr 16 and 0xFF) / CHANNEL_MAX
    val g = (color shr 8 and 0xFF) / CHANNEL_MAX
    val b = (color and 0xFF) / CHANNEL_MAX
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val chroma = max - min
    val lightness = (max + min) / 2f

    fun hue(): Float {
        if (chroma == 0f) return 0f
        val sector = when (max) {
            r -> ((g - b) / chroma).mod(6f)
            g -> (b - r) / chroma + 2f
            else -> (r - g) / chroma + 4f
        }
        return sector * 60f
    }
}

/** True when the color has a visible hue, so it can stand for a brand. */
fun isBrandColor(color: Int): Boolean = Rgb(color).chroma >= MIN_BRAND_CHROMA

/**
 * The main hue of pass artwork, averaged over the pixels of that hue. [pixels] holds ARGB values.
 * Returns null when no hue covers enough of the opaque artwork.
 */
fun dominantArtworkColor(pixels: IntArray): Int? {
    var opaque = 0
    val buckets = Array(HUE_BUCKETS) { mutableListOf<Rgb>() }
    for (pixel in pixels) {
        if (pixel ushr 24 < MIN_OPAQUE_ALPHA) continue
        opaque++
        val rgb = Rgb(pixel)
        if (rgb.chroma < MIN_PIXEL_CHROMA || rgb.lightness !in MIN_PIXEL_LIGHTNESS..MAX_PIXEL_LIGHTNESS) continue
        buckets[(rgb.hue() / FULL_TURN * HUE_BUCKETS).toInt().coerceIn(0, HUE_BUCKETS - 1)] += rgb
    }
    val best = buckets.maxBy { it.size }
    if (opaque == 0 || best.size < opaque * MIN_BRAND_COVERAGE) return null
    fun channel(value: (Rgb) -> Float) = (best.map(value).average() * CHANNEL_MAX).roundToInt()
    return OPAQUE or (channel { it.r } shl 16) or (channel { it.g } shl 8) or channel { it.b }
}

/**
 * The pass color. A colored issuer background wins. A grey, white, or black background gives way to the
 * artwork hue, then to a colored label or foreground. [artworkColor] is read only when it is needed.
 */
fun resolvePassAccentColor(background: Int?, label: Int?, foreground: Int?, artworkColor: () -> Int?): Int? {
    if (background != null && isBrandColor(background)) return background
    return artworkColor()
        ?: label?.takeIf(::isBrandColor)
        ?: foreground?.takeIf(::isBrandColor)
        ?: background
}
