package org.ligi.passandroid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.repository.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF2859C5),
    secondary = Color(0xFF4C5F8A),
    tertiary = Color(0xFF79536F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB2C5FF),
    secondary = Color(0xFFB7C4EA),
    tertiary = Color(0xFFE7B9D9),
)

private val PassShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    largeIncreased = RoundedCornerShape(36.dp),
)

@Composable
fun PassTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
    }
    val context = LocalContext.current
    val baseColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) {
        DarkColors
    } else {
        LightColors
    }
    val colors = if (themeMode == ThemeMode.AMOLED) {
        baseColors.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF030303),
            surfaceContainer = Color(0xFF050505),
            surfaceContainerHigh = Color(0xFF080808),
            surfaceContainerHighest = Color(0xFF0B0B0B),
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
