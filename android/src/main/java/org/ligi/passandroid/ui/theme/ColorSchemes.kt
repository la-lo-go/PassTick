package org.ligi.passandroid.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import org.ligi.passandroid.repository.ColorStyle
import org.ligi.passandroid.repository.DEFAULT_ACCENT_COLOR

internal val brandAccentColor: Color = Color(DEFAULT_ACCENT_COLOR)

// Seed ramp for the appearance settings: sixteen hue families at a light and a dark tone each.
// The generator derives every other role from the seed.
internal val accentSeedColors: List<Long> = listOf(
    0xFF48C9B0L, 0xFF17A589L, 0xFF45B39DL, 0xFF138D75L,
    0xFF58D68DL, 0xFF28B463L, 0xFF52BE80L, 0xFF229954L,
    0xFF5DADE2L, 0xFF2E86C1L, 0xFF5499C7L, 0xFF2471A3L,
    0xFFAF7AC5L, 0xFF884EA0L, 0xFFA569BDL, 0xFF7D3C98L,
    0xFF5D6D7EL, 0xFF2E4053L, 0xFF566573L, 0xFF273746L,
    0xFFF4D03FL, 0xFFD4AC0DL, 0xFFF5B041L, 0xFFD68910L,
    0xFFEB984EL, 0xFFCA6F1EL, 0xFFDC7633L, 0xFFBA4A00L,
    0xFFEC7063L, 0xFFCB4335L, 0xFFCD6155L, 0xFFA93226L,
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

// AMOLED darkens the tinted roles toward black and keeps text roles readable on the black surface.
internal fun ColorScheme.toAmoled(): ColorScheme = copy(
    primary = primary.darken(),
    primaryContainer = primaryContainer.darken(),
    inversePrimary = inversePrimary.darken(),
    secondary = secondary.darken(),
    secondaryContainer = secondaryContainer.darken(),
    tertiary = tertiary.darken(),
    tertiaryContainer = tertiaryContainer.darken(),
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = surfaceVariant.darken(),
    surfaceTint = surfaceTint.darken(),
    inverseSurface = inverseSurface.darken(),
    error = error.darken(),
    errorContainer = errorContainer.darken(),
    outline = outline.darken(),
    outlineVariant = outlineVariant.darken(),
    scrim = scrim.darken(),
    surfaceBright = surfaceBright.darken(),
    surfaceDim = surfaceDim.darken(),
    surfaceContainer = Color.Black,
    surfaceContainerHigh = surfaceContainerHigh.darken(),
    surfaceContainerHighest = surfaceContainerHighest.darken(),
    surfaceContainerLow = surfaceContainerLow.darken(),
    surfaceContainerLowest = surfaceContainerLowest.darken(),
    primaryFixed = primaryFixed.darken(),
    primaryFixedDim = primaryFixedDim.darken(),
    secondaryFixed = secondaryFixed.darken(),
    secondaryFixedDim = secondaryFixedDim.darken(),
    tertiaryFixed = tertiaryFixed.darken(),
    tertiaryFixedDim = tertiaryFixedDim.darken(),
)

private fun Color.darken() = copy(alpha = 0.8f).compositeOver(Color.Black)
