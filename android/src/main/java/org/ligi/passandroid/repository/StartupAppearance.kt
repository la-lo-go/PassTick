package org.ligi.passandroid.repository

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.ColorInt
import androidx.core.content.edit

data class StartupAppearance(
    val themeMode: ThemeMode,
    val amoledBlackBackground: Boolean,
    val dynamicColors: Boolean = true,
    val accentColor: Long? = null,
    val colorStyle: ColorStyle = ColorStyle.TONAL_SPOT,
) {
    @ColorInt
    fun backgroundColor(context: Context): Int = when {
        isDark(context) && amoledBlackBackground -> 0xFF000000.toInt()
        isDark(context) -> 0xFF101116.toInt()
        else -> 0xFFFFFBFE.toInt()
    }

    private fun isDark(context: Context): Boolean = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    }
}

object StartupAppearanceStore {
    private const val FILE_NAME = "startup_appearance"
    private const val THEME_MODE = "theme_mode"
    private const val AMOLED = "amoled"
    private const val DYNAMIC_COLORS = "dynamic_colors"
    private const val ACCENT_COLOR = "accent_color"
    private const val COLOR_STYLE = "color_style"

    // Legacy key that held the removed AccentPalette enum name. Read-only for migration.
    private const val LEGACY_ACCENT = "accent_palette"

    fun read(context: Context): StartupAppearance {
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        val themeMode = preferences.getString(THEME_MODE, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
        val accent = resolveAccentSettings(
            storedDynamicColors = preferences.getBoolean(DYNAMIC_COLORS, true)
                .takeIf { preferences.contains(DYNAMIC_COLORS) },
            storedAccentColor = preferences.getLong(ACCENT_COLOR, DEFAULT_ACCENT_COLOR)
                .takeIf { preferences.contains(ACCENT_COLOR) },
            legacyPalette = preferences.getString(LEGACY_ACCENT, null),
        )
        val colorStyle = preferences.getString(COLOR_STYLE, null)
            ?.let { runCatching { ColorStyle.valueOf(it) }.getOrNull() }
            ?: ColorStyle.TONAL_SPOT
        return StartupAppearance(
            themeMode = themeMode,
            amoledBlackBackground = preferences.getBoolean(AMOLED, false),
            dynamicColors = accent.dynamicColors,
            accentColor = accent.accentColor,
            colorStyle = colorStyle,
        )
    }

    fun write(
        context: Context,
        themeMode: ThemeMode,
        amoledBlackBackground: Boolean,
        dynamicColors: Boolean = true,
        accentColor: Long? = null,
        colorStyle: ColorStyle = ColorStyle.TONAL_SPOT,
    ) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit {
            putString(THEME_MODE, themeMode.name)
            putBoolean(AMOLED, amoledBlackBackground)
            putBoolean(DYNAMIC_COLORS, dynamicColors)
            putString(COLOR_STYLE, colorStyle.name)
            if (accentColor == null) remove(ACCENT_COLOR) else putLong(ACCENT_COLOR, accentColor)
            remove(LEGACY_ACCENT)
        }
    }
}
