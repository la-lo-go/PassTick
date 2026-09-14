package org.ligi.passandroid.repository

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.ColorInt
import androidx.core.content.edit

data class StartupAppearance(
    val themeMode: ThemeMode,
    val amoledBlackBackground: Boolean,
    val accentPalette: AccentPalette = AccentPalette.DYNAMIC,
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
    private const val ACCENT = "accent_palette"

    fun read(context: Context): StartupAppearance {
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        val themeMode = preferences.getString(THEME_MODE, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
        val accentPalette = preferences.getString(ACCENT, null)
            ?.let { runCatching { AccentPalette.valueOf(it) }.getOrNull() }
            ?: AccentPalette.DYNAMIC
        return StartupAppearance(themeMode, preferences.getBoolean(AMOLED, false), accentPalette)
    }

    fun write(
        context: Context,
        themeMode: ThemeMode,
        amoledBlackBackground: Boolean,
        accentPalette: AccentPalette = AccentPalette.DYNAMIC,
    ) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit {
            putString(THEME_MODE, themeMode.name)
            putBoolean(AMOLED, amoledBlackBackground)
            putString(ACCENT, accentPalette.name)
        }
    }
}
