package org.ligi.passandroid.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import org.ligi.passandroid.repository.ColorStyle
import org.ligi.passandroid.repository.DEFAULT_ACCENT_COLOR

internal val brandAccentColor: Color = Color(DEFAULT_ACCENT_COLOR)

// Seed ramp for the appearance settings: sixteen hue families at ten tones each. The generator
// derives every other role from the seed.
internal val accentSeedColors: List<Long> = listOf(
    0xFFE8F8F5L, 0xFFD1F2EBL, 0xFFA3E4D7L, 0xFF76D7C4L, 0xFF48C9B0L,
    0xFF1ABC9CL, 0xFF17A589L, 0xFF148F77L, 0xFF117864L, 0xFF0E6251L,
    0xFFE8F6F3L, 0xFFD0ECE7L, 0xFFA2D9CEL, 0xFF73C6B6L, 0xFF45B39DL,
    0xFF16A085L, 0xFF138D75L, 0xFF117A65L, 0xFF0E6655L, 0xFF0B5345L,
    0xFFEAFAF1L, 0xFFD5F5E3L, 0xFFABEBC6L, 0xFF82E0AAL, 0xFF58D68DL,
    0xFF2ECC71L, 0xFF28B463L, 0xFF239B56L, 0xFF1D8348L, 0xFF186A3BL,
    0xFFE9F7EFL, 0xFFD4EFDFL, 0xFFA9DFBFL, 0xFF7DCEA0L, 0xFF52BE80L,
    0xFF27AE60L, 0xFF229954L, 0xFF1E8449L, 0xFF196F3DL, 0xFF145A32L,
    0xFFEBF5FBL, 0xFFD6EAF8L, 0xFFAED6F1L, 0xFF85C1E9L, 0xFF5DADE2L,
    0xFF3498DBL, 0xFF2E86C1L, 0xFF2874A6L, 0xFF21618CL, 0xFF1B4F72L,
    0xFFEAF2F8L, 0xFFD4E6F1L, 0xFFA9CCE3L, 0xFF7FB3D5L, 0xFF5499C7L,
    0xFF2980B9L, 0xFF2471A3L, 0xFF1F618DL, 0xFF1A5276L, 0xFF154360L,
    0xFFF5EEF8L, 0xFFEBDEF0L, 0xFFD7BDE2L, 0xFFC39BD3L, 0xFFAF7AC5L,
    0xFF9B59B6L, 0xFF884EA0L, 0xFF76448AL, 0xFF633974L, 0xFF512E5FL,
    0xFFF4ECF7L, 0xFFE8DAEFL, 0xFFD2B4DEL, 0xFFBB8FCEL, 0xFFA569BDL,
    0xFF8E44ADL, 0xFF7D3C98L, 0xFF6C3483L, 0xFF5B2C6FL, 0xFF4A235AL,
    0xFFEBEDEFL, 0xFFD6DBDFL, 0xFFAEB6BFL, 0xFF85929EL, 0xFF5D6D7EL,
    0xFF34495EL, 0xFF2E4053L, 0xFF283747L, 0xFF212F3CL, 0xFF1B2631L,
    0xFFEAECEEL, 0xFFD5D8DCL, 0xFFABB2B9L, 0xFF808B96L, 0xFF566573L,
    0xFF2C3E50L, 0xFF273746L, 0xFF212F3DL, 0xFF1C2833L, 0xFF17202AL,
    0xFFFEF9E7L, 0xFFFCF3CFL, 0xFFF9E79FL, 0xFFF7DC6FL, 0xFFF4D03FL,
    0xFFF1C40FL, 0xFFD4AC0DL, 0xFFB7950BL, 0xFF9A7D0AL, 0xFF7D6608L,
    0xFFFEF5E7L, 0xFFFDEBD0L, 0xFFFAD7A0L, 0xFFF8C471L, 0xFFF5B041L,
    0xFFF39C12L, 0xFFD68910L, 0xFFB9770EL, 0xFF9C640CL, 0xFF7E5109L,
    0xFFFDF2E9L, 0xFFFAE5D3L, 0xFFF5CBA7L, 0xFFF0B27AL, 0xFFEB984EL,
    0xFFE67E22L, 0xFFCA6F1EL, 0xFFAF601AL, 0xFF935116L, 0xFF784212L,
    0xFFFBEEE6L, 0xFFF6DDCCL, 0xFFEDBB99L, 0xFFE59866L, 0xFFDC7633L,
    0xFFD35400L, 0xFFBA4A00L, 0xFFA04000L, 0xFF873600L, 0xFF6E2C00L,
    0xFFFDEDECL, 0xFFFADBD8L, 0xFFF5B7B1L, 0xFFF1948AL, 0xFFEC7063L,
    0xFFE74C3CL, 0xFFCB4335L, 0xFFB03A2EL, 0xFF943126L, 0xFF78281FL,
    0xFFF9EBEAL, 0xFFF2D7D5L, 0xFFE6B0AAL, 0xFFD98880L, 0xFFCD6155L,
    0xFFC0392BL, 0xFFA93226L, 0xFF922B21L, 0xFF7B241CL, 0xFF641E16L,
)

private const val SEED_TONES_PER_FAMILY = 10

// The ramp is grouped into hue families: the first row of the selector shows one preview per
// family and the second row shows every tone of the selected family.
internal val accentSeedFamilies: List<List<Long>> = accentSeedColors.chunked(SEED_TONES_PER_FAMILY)

internal val accentSeedFamilyPreviews: List<Long> = accentSeedFamilies.map { it[SEED_TONES_PER_FAMILY / 2] }

internal fun accentSeedFamilyOf(color: Long): List<Long>? = accentSeedFamilies.firstOrNull { color in it }

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
