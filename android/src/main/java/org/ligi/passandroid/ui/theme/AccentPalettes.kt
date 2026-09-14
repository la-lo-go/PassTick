package org.ligi.passandroid.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.ligi.passandroid.repository.AccentPalette

private val White = Color(0xFFFFFFFF)

private data class PaletteStops(
    // Light: primary/container/container-on sit on tonal stops 40/90/10 of the accent hue.
    val lightPrimary: Long,
    val lightPrimaryContainer: Long,
    val lightOnPrimaryContainer: Long,
    val lightSecondary: Long,
    val lightSecondaryContainer: Long,
    val lightOnSecondaryContainer: Long,
    val lightTertiary: Long,
    val lightTertiaryContainer: Long,
    val lightOnTertiaryContainer: Long,
    // Dark: primary/on/container sit on tonal stops 80/20/30, container-on on 90.
    val darkPrimary: Long,
    val darkOnPrimary: Long,
    val darkPrimaryContainer: Long,
    val darkSecondary: Long,
    val darkOnSecondary: Long,
    val darkSecondaryContainer: Long,
    val darkTertiary: Long,
    val darkOnTertiary: Long,
    val darkTertiaryContainer: Long,
)

// Fixed curated stops, one row per palette. Tertiary shifts about +60 hue from the accent.
private val paletteStops = mapOf(
    AccentPalette.BLUE to PaletteStops(
        lightPrimary = 0xFF2859C5, lightPrimaryContainer = 0xFFD8E2FF, lightOnPrimaryContainer = 0xFF001B41,
        lightSecondary = 0xFF4C5F8A, lightSecondaryContainer = 0xFFDAE2F9, lightOnSecondaryContainer = 0xFF131C2B,
        lightTertiary = 0xFF79536F, lightTertiaryContainer = 0xFFFFD8EC, lightOnTertiaryContainer = 0xFF2D1224,
        darkPrimary = 0xFFB2C5FF, darkOnPrimary = 0xFF002E6B, darkPrimaryContainer = 0xFF00449E,
        darkSecondary = 0xFFB7C4EA, darkOnSecondary = 0xFF24303F, darkSecondaryContainer = 0xFF364563,
        darkTertiary = 0xFFE7B9D9, darkOnTertiary = 0xFF43263C, darkTertiaryContainer = 0xFF5C3A54,
    ),
    AccentPalette.INDIGO to PaletteStops(
        lightPrimary = 0xFF565FA8, lightPrimaryContainer = 0xFFDDE1FF, lightOnPrimaryContainer = 0xFF141B44,
        lightSecondary = 0xFF5C5D72, lightSecondaryContainer = 0xFFE0E1F9, lightOnSecondaryContainer = 0xFF171A2C,
        lightTertiary = 0xFF76546E, lightTertiaryContainer = 0xFFFFD7F0, lightOnTertiaryContainer = 0xFF2C1229,
        darkPrimary = 0xFFBFC4FF, darkOnPrimary = 0xFF253071, darkPrimaryContainer = 0xFF3F4587,
        darkSecondary = 0xFFC4C3DD, darkOnSecondary = 0xFF2D2F43, darkSecondaryContainer = 0xFF434559,
        darkTertiary = 0xFFE3BADA, darkOnTertiary = 0xFF432741, darkTertiaryContainer = 0xFF5B3D58,
    ),
    AccentPalette.PURPLE to PaletteStops(
        lightPrimary = 0xFF6750A4, lightPrimaryContainer = 0xFFEADDFF, lightOnPrimaryContainer = 0xFF21005D,
        lightSecondary = 0xFF625B71, lightSecondaryContainer = 0xFFE8DEF8, lightOnSecondaryContainer = 0xFF1D192B,
        lightTertiary = 0xFF7D5260, lightTertiaryContainer = 0xFFFFD8E4, lightOnTertiaryContainer = 0xFF31111D,
        darkPrimary = 0xFFD0BCFF, darkOnPrimary = 0xFF381E72, darkPrimaryContainer = 0xFF4F378B,
        darkSecondary = 0xFFCCC2DC, darkOnSecondary = 0xFF332D41, darkSecondaryContainer = 0xFF4A4458,
        darkTertiary = 0xFFEFB8C8, darkOnTertiary = 0xFF492532, darkTertiaryContainer = 0xFF633B48,
    ),
    AccentPalette.PINK to PaletteStops(
        lightPrimary = 0xFF8E4957, lightPrimaryContainer = 0xFFFFD9DD, lightOnPrimaryContainer = 0xFF3A0716,
        lightSecondary = 0xFF76565B, lightSecondaryContainer = 0xFFDDE0E4, lightOnSecondaryContainer = 0xFF201A1B,
        lightTertiary = 0xFF7B5442, lightTertiaryContainer = 0xFFFFDCC1, lightOnTertiaryContainer = 0xFF2E1500,
        darkPrimary = 0xFFFFB1C8, darkOnPrimary = 0xFF511D2E, darkPrimaryContainer = 0xFF6D3343,
        darkSecondary = 0xFFE4BDC2, darkOnSecondary = 0xFF43292D, darkSecondaryContainer = 0xFF5B3F43,
        darkTertiary = 0xFFE6C1A4, darkOnTertiary = 0xFF442B18, darkTertiaryContainer = 0xFF5D4129,
    ),
    AccentPalette.RED to PaletteStops(
        lightPrimary = 0xFFBA1A1A, lightPrimaryContainer = 0xFFFFDAD6, lightOnPrimaryContainer = 0xFF410002,
        lightSecondary = 0xFF775652, lightSecondaryContainer = 0xFFFFDAD5, lightOnSecondaryContainer = 0xFF2C1512,
        lightTertiary = 0xFF7A5738, lightTertiaryContainer = 0xFFFFDCC2, lightOnTertiaryContainer = 0xFF2E1500,
        darkPrimary = 0xFFFFB4AB, darkOnPrimary = 0xFF690005, darkPrimaryContainer = 0xFF93000A,
        darkSecondary = 0xFFE7BDB8, darkOnSecondary = 0xFF442925, darkSecondaryContainer = 0xFF5D403C,
        darkTertiary = 0xFFE7C1A0, darkOnTertiary = 0xFF452B13, darkTertiaryContainer = 0xFF603F26,
    ),
    AccentPalette.ORANGE to PaletteStops(
        lightPrimary = 0xFF964F00, lightPrimaryContainer = 0xFFFFDCC2, lightOnPrimaryContainer = 0xFF2E1500,
        lightSecondary = 0xFF77574B, lightSecondaryContainer = 0xFFF5DFCB, lightOnSecondaryContainer = 0xFF241A10,
        lightTertiary = 0xFF6C5D11, lightTertiaryContainer = 0xFFF5E586, lightOnTertiaryContainer = 0xFF211B00,
        darkPrimary = 0xFFFFB874, darkOnPrimary = 0xFF4F2500, darkPrimaryContainer = 0xFF6D3A00,
        darkSecondary = 0xFFE6C1A7, darkOnSecondary = 0xFF432C20, darkSecondaryContainer = 0xFF5B4235,
        darkTertiary = 0xFFD9C85F, darkOnTertiary = 0xFF3A2F00, darkTertiaryContainer = 0xFF544600,
    ),
    AccentPalette.AMBER to PaletteStops(
        lightPrimary = 0xFF745B00, lightPrimaryContainer = 0xFFF9E286, lightOnPrimaryContainer = 0xFF231B00,
        lightSecondary = 0xFF6C5C3E, lightSecondaryContainer = 0xFFF5E0BB, lightOnSecondaryContainer = 0xFF241A04,
        lightTertiary = 0xFF486A11, lightTertiaryContainer = 0xFFCAF199, lightOnTertiaryContainer = 0xFF142000,
        darkPrimary = 0xFFE5C34B, darkOnPrimary = 0xFF3F2E00, darkPrimaryContainer = 0xFF5A4400,
        darkSecondary = 0xFFD3C5A1, darkOnSecondary = 0xFF392F16, darkSecondaryContainer = 0xFF51452B,
        darkTertiary = 0xFFAFCC76, darkOnTertiary = 0xFF223500, darkTertiaryContainer = 0xFF324D00,
    ),
    AccentPalette.YELLOW to PaletteStops(
        lightPrimary = 0xFF6E5D00, lightPrimaryContainer = 0xFFF6E388, lightOnPrimaryContainer = 0xFF1F1D00,
        lightSecondary = 0xFF665E2F, lightSecondaryContainer = 0xFFEDE2C3, lightOnSecondaryContainer = 0xFF201C08,
        lightTertiary = 0xFF3E6B36, lightTertiaryContainer = 0xFFBFF2B4, lightOnTertiaryContainer = 0xFF002206,
        darkPrimary = 0xFFE8C74A, darkOnPrimary = 0xFF3C3000, darkPrimaryContainer = 0xFF574600,
        darkSecondary = 0xFFD0C7A5, darkOnSecondary = 0xFF363018, darkSecondaryContainer = 0xFF4E462D,
        darkTertiary = 0xFFA6D39B, darkOnTertiary = 0xFF123811, darkTertiaryContainer = 0xFF255025,
    ),
    AccentPalette.GREEN to PaletteStops(
        lightPrimary = 0xFF006C4C, lightPrimaryContainer = 0xFF86F7C5, lightOnPrimaryContainer = 0xFF002116,
        lightSecondary = 0xFF4C635A, lightSecondaryContainer = 0xFFCFE9DE, lightOnSecondaryContainer = 0xFF092018,
        lightTertiary = 0xFF47637D, lightTertiaryContainer = 0xFFCDE5F8, lightOnTertiaryContainer = 0xFF001E2E,
        darkPrimary = 0xFF4DDDB4, darkOnPrimary = 0xFF003829, darkPrimaryContainer = 0xFF00523B,
        darkSecondary = 0xFFAFCFC5, darkOnSecondary = 0xFF1A352D, darkSecondaryContainer = 0xFF354B43,
        darkTertiary = 0xFFADCBE3, darkOnTertiary = 0xFF16344A, darkTertiaryContainer = 0xFF324B62,
    ),
    AccentPalette.TEAL to PaletteStops(
        lightPrimary = 0xFF00796B, lightPrimaryContainer = 0xFF7FF5DB, lightOnPrimaryContainer = 0xFF00201B,
        lightSecondary = 0xFF4A635C, lightSecondaryContainer = 0xFFCCE8E0, lightOnSecondaryContainer = 0xFF062019,
        lightTertiary = 0xFF4B607C, lightTertiaryContainer = 0xFFD2E4F8, lightOnTertiaryContainer = 0xFF001D32,
        darkPrimary = 0xFF50DBC9, darkOnPrimary = 0xFF003731, darkPrimaryContainer = 0xFF005048,
        darkSecondary = 0xFFB0CCC4, darkOnSecondary = 0xFF1B352F, darkSecondaryContainer = 0xFF324B45,
        darkTertiary = 0xFFB3C8E8, darkOnTertiary = 0xFF1B324B, darkTertiaryContainer = 0xFF334863,
    ),
    AccentPalette.CYAN to PaletteStops(
        lightPrimary = 0xFF00696E, lightPrimaryContainer = 0xFF9CF0F4, lightOnPrimaryContainer = 0xFF002022,
        lightSecondary = 0xFF4A6364, lightSecondaryContainer = 0xFFCCE7E8, lightOnSecondaryContainer = 0xFF051F20,
        lightTertiary = 0xFF545E92, lightTertiaryContainer = 0xFFDDE1FF, lightOnTertiaryContainer = 0xFF101A3D,
        darkPrimary = 0xFF4CD9DE, darkOnPrimary = 0xFF003739, darkPrimaryContainer = 0xFF004F53,
        darkSecondary = 0xFFB1CBCC, darkOnSecondary = 0xFF1C3436, darkSecondaryContainer = 0xFF334B4C,
        darkTertiary = 0xFFBCC5FF, darkOnTertiary = 0xFF232F60, darkTertiaryContainer = 0xFF3B4578,
    ),
    AccentPalette.BROWN to PaletteStops(
        lightPrimary = 0xFF765848, lightPrimaryContainer = 0xFFFFDBCA, lightOnPrimaryContainer = 0xFF2B160A,
        lightSecondary = 0xFF75594A, lightSecondaryContainer = 0xFFEADCD8, lightOnSecondaryContainer = 0xFF2B1914,
        lightTertiary = 0xFF67633A, lightTertiaryContainer = 0xFFEFE8B9, lightOnTertiaryContainer = 0xFF201D04,
        darkPrimary = 0xFFD3BCB0, darkOnPrimary = 0xFF422B1D, darkPrimaryContainer = 0xFF5B4032,
        darkSecondary = 0xFFD3C0B9, darkOnSecondary = 0xFF382E27, darkSecondaryContainer = 0xFF4F443E,
        darkTertiary = 0xFFD2CB9F, darkOnTertiary = 0xFF36331B, darkTertiaryContainer = 0xFF4E4A2E,
    ),
)

internal fun accentColorSchemes(palette: AccentPalette): Pair<ColorScheme, ColorScheme>? {
    val stops = paletteStops[palette] ?: return null
    val light = lightColorScheme(
        primary = Color(stops.lightPrimary),
        onPrimary = White,
        primaryContainer = Color(stops.lightPrimaryContainer),
        onPrimaryContainer = Color(stops.lightOnPrimaryContainer),
        secondary = Color(stops.lightSecondary),
        onSecondary = White,
        secondaryContainer = Color(stops.lightSecondaryContainer),
        onSecondaryContainer = Color(stops.lightOnSecondaryContainer),
        tertiary = Color(stops.lightTertiary),
        onTertiary = White,
        tertiaryContainer = Color(stops.lightTertiaryContainer),
        onTertiaryContainer = Color(stops.lightOnTertiaryContainer),
    )
    val dark = darkColorScheme(
        primary = Color(stops.darkPrimary),
        onPrimary = Color(stops.darkOnPrimary),
        primaryContainer = Color(stops.darkPrimaryContainer),
        onPrimaryContainer = Color(stops.lightPrimaryContainer),
        secondary = Color(stops.darkSecondary),
        onSecondary = Color(stops.darkOnSecondary),
        secondaryContainer = Color(stops.darkSecondaryContainer),
        onSecondaryContainer = Color(stops.lightSecondaryContainer),
        tertiary = Color(stops.darkTertiary),
        onTertiary = Color(stops.darkOnTertiary),
        tertiaryContainer = Color(stops.darkTertiaryContainer),
        onTertiaryContainer = Color(stops.lightTertiaryContainer),
    )
    return light to dark
}

// Swatch shown next to each palette entry in the appearance settings.
fun accentSwatchColor(palette: AccentPalette): Color? =
    paletteStops[palette]?.let { Color(it.lightPrimary) }
