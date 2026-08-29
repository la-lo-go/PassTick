package org.ligi.passandroid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun PassTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) {
        DarkColors
    } else {
        LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
