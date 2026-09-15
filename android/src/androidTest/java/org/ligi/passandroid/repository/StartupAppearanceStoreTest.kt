package org.ligi.passandroid.repository

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

class StartupAppearanceStoreTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun missingKeysReadDefaults() {
        StartupAppearanceStore.write(context, ThemeMode.SYSTEM, false)
        context.getSharedPreferences("startup_appearance", Context.MODE_PRIVATE).edit().clear().commit()

        val appearance = StartupAppearanceStore.read(context)

        assertThat(appearance.themeMode).isEqualTo(ThemeMode.SYSTEM)
        assertThat(appearance.amoledBlackBackground).isFalse()
        assertThat(appearance.dynamicColors).isTrue()
        assertThat(appearance.accentColor).isNull()
        assertThat(appearance.colorStyle).isEqualTo(ColorStyle.TONAL_SPOT)
    }

    @Test
    fun legacyPaletteNameMigratesToItsSeedColor() {
        context.getSharedPreferences("startup_appearance", Context.MODE_PRIVATE).edit().clear()
            .putString("theme_mode", "DARK")
            .putBoolean("amoled", true)
            .putString("accent_palette", "GREEN")
            .commit()

        val appearance = StartupAppearanceStore.read(context)

        assertThat(appearance.dynamicColors).isFalse()
        assertThat(appearance.accentColor).isEqualTo(0xFF006C4CL)
    }

    @Test
    fun unknownLegacyPaletteFallsBackToDynamicDefaults() {
        context.getSharedPreferences("startup_appearance", Context.MODE_PRIVATE).edit().clear()
            .putString("accent_palette", "NOT_A_PALETTE")
            .commit()

        val appearance = StartupAppearanceStore.read(context)

        assertThat(appearance.dynamicColors).isTrue()
        assertThat(appearance.accentColor).isNull()
    }

    @Test
    fun writePersistsAllAppearanceValues() {
        StartupAppearanceStore.write(context, ThemeMode.LIGHT, false, false, 0xFF765848L, ColorStyle.VIBRANT)

        val appearance = StartupAppearanceStore.read(context)

        assertThat(appearance.themeMode).isEqualTo(ThemeMode.LIGHT)
        assertThat(appearance.amoledBlackBackground).isFalse()
        assertThat(appearance.dynamicColors).isFalse()
        assertThat(appearance.accentColor).isEqualTo(0xFF765848L)
        assertThat(appearance.colorStyle).isEqualTo(ColorStyle.VIBRANT)
    }

    @After
    fun resetAppearance() {
        StartupAppearanceStore.write(context, ThemeMode.SYSTEM, false)
    }
}
