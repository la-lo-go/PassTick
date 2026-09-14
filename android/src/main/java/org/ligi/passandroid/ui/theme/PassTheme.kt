package org.ligi.passandroid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.repository.AccentPalette
import org.ligi.passandroid.repository.ThemeMode

// Blue baseline fallback for API 29/30; shares its stops with AccentPalette.BLUE.
private val LightColors = accentColorSchemes(AccentPalette.BLUE)!!.first
private val DarkColors = accentColorSchemes(AccentPalette.BLUE)!!.second

private val PassShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    largeIncreased = RoundedCornerShape(36.dp),
)

@Composable
fun PassTheme(
    themeMode: ThemeMode,
    amoledBlackBackground: Boolean = false,
    accentPalette: AccentPalette = AccentPalette.DYNAMIC,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    // A curated accent palette wins on every API level; DYNAMIC falls back to wallpaper colors
    // from API 31 and to the blue baseline below.
    val baseColors = if (accentPalette != AccentPalette.DYNAMIC) {
        val (light, darkScheme) = accentColorSchemes(accentPalette)!!
        if (dark) darkScheme else light
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) {
        DarkColors
    } else {
        LightColors
    }
    val colors = if (dark && amoledBlackBackground) {
        baseColors.copy(
            background = Color.Black,
            surface = Color.Black,
        )
    } else {
        baseColors
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        shapes = PassShapes,
        content = content,
    )
}
