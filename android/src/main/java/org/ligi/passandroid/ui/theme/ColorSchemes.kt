package org.ligi.passandroid.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import org.ligi.passandroid.repository.ColorStyle
import org.ligi.passandroid.repository.DEFAULT_ACCENT_COLOR

internal val brandAccentColor: Color = Color(DEFAULT_ACCENT_COLOR)

// Seed ramp for the appearance settings: thirteen hues at three tones. The generator derives
// every other role from the seed.
internal val accentSeedColors: List<Long> = listOf(
    0xFFEF9A9AL, 0xFFE53935L, 0xFFB71C1CL,
    0xFFF48FB1L, 0xFFD81B60L, 0xFF880E4FL,
    0xFFCE93D8L, 0xFF8E24AAL, 0xFF4A148CL,
    0xFF9FA8DAL, 0xFF3949ABL, 0xFF1A237EL,
    0xFF90CAF9L, 0xFF1E88E5L, 0xFF0D47A1L,
    0xFF80DEEAL, 0xFF00ACC1L, 0xFF006064L,
    0xFF80CBC4L, 0xFF00897BL, 0xFF004D40L,
    0xFFA5D6A7L, 0xFF43A047L, 0xFF1B5E20L,
    0xFFE6EE9CL, 0xFFC0CA33L, 0xFF827717L,
    0xFFFFE082L, 0xFFFFB300L, 0xFFFF6F00L,
    0xFFFFCC80L, 0xFFFB8C00L, 0xFFE65100L,
    0xFFBCAAA4L, 0xFF6D4C41L, 0xFF3E2723L,
    0xFFB0BEC5L, 0xFF546E7AL, 0xFF263238L,
)

internal fun generateAccentColorScheme(seed: Color, dark: Boolean, style: ColorStyle): ColorScheme =
    dynamicColorScheme(seedColor = seed, isDark = dark, style = style.toPaletteStyle())

internal fun ColorStyle.toPaletteStyle(): PaletteStyle = when (this) {
    ColorStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
    ColorStyle.VIBRANT -> PaletteStyle.Vibrant
    ColorStyle.EXPRESSIVE -> PaletteStyle.Expressive
    ColorStyle.RAINBOW -> PaletteStyle.Rainbow
    ColorStyle.FRUIT_SALAD -> PaletteStyle.FruitSalad
    ColorStyle.CONTENT -> PaletteStyle.Content
    ColorStyle.FIDELITY -> PaletteStyle.Fidelity
    ColorStyle.NEUTRAL -> PaletteStyle.Neutral
    ColorStyle.MONOCHROME -> PaletteStyle.Monochrome
}
