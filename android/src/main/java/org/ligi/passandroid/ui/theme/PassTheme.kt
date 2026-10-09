package org.ligi.passandroid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.repository.ColorStyle
import org.ligi.passandroid.repository.DEFAULT_ACCENT_COLOR
import org.ligi.passandroid.repository.ThemeMode

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
    dynamicColors: Boolean = true,
    accentColor: Long? = null,
    colorStyle: ColorStyle = ColorStyle.TONAL_SPOT,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    // Wallpaper colors win while dynamic colors are on. The stored seed stays untouched so
    // switching dynamic colors off restores the previous scheme. Wallpaper colors need API 31.
    val baseColors =
        if (dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            generateAccentColorScheme(Color(accentColor ?: DEFAULT_ACCENT_COLOR), dark, colorStyle)
        }
    val colors = if (dark && amoledBlackBackground) baseColors.toAmoled() else baseColors
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        shapes = PassShapes,
        content = content,
    )
}

/**
 * Colors a pass screen from the pass color, so the screen matches the ticket. The Content style keeps the scheme
 * close to the brand color. Light or dark follows the surrounding theme.
 */
@Composable
fun PassColorTheme(
    passColor: Int?,
    enabled: Boolean,
    amoledBlackBackground: Boolean,
    content: @Composable () -> Unit,
) {
    if (!enabled || passColor == null) {
        content()
        return
    }
    val dark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE
    val colors = remember(passColor, dark, amoledBlackBackground) {
        val scheme = generateAccentColorScheme(Color(passColor).copy(alpha = 1f), dark, ColorStyle.CONTENT)
        if (dark && amoledBlackBackground) scheme.toAmoled() else scheme
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        shapes = PassShapes,
        content = content,
    )
}

private const val DARK_SURFACE_LUMINANCE = 0.5f
