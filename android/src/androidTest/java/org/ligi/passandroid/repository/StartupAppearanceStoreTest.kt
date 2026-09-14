package org.ligi.passandroid.repository

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

class StartupAppearanceStoreTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun missingKeyReadsDefaults() {
        StartupAppearanceStore.write(context, ThemeMode.SYSTEM, false, AccentPalette.DYNAMIC)
        context.getSharedPreferences("startup_appearance", Context.MODE_PRIVATE).edit().clear().commit()

        val appearance = StartupAppearanceStore.read(context)

        assertThat(appearance.themeMode).isEqualTo(ThemeMode.SYSTEM)
        assertThat(appearance.amoledBlackBackground).isFalse()
        assertThat(appearance.accentPalette).isEqualTo(AccentPalette.DYNAMIC)
    }

    @Test
    fun unknownAccentValueFallsBackToDynamic() {
        StartupAppearanceStore.write(context, ThemeMode.DARK, true, AccentPalette.GREEN)
        context.getSharedPreferences("startup_appearance", Context.MODE_PRIVATE)
            .edit().putString("accent_palette", "NOT_A_PALETTE").commit()

        assertThat(StartupAppearanceStore.read(context).accentPalette).isEqualTo(AccentPalette.DYNAMIC)
    }

    @Test
    fun writePersistsAllAppearanceValues() {
        StartupAppearanceStore.write(context, ThemeMode.LIGHT, false, AccentPalette.TEAL)

        val appearance = StartupAppearanceStore.read(context)

        assertThat(appearance.themeMode).isEqualTo(ThemeMode.LIGHT)
        assertThat(appearance.amoledBlackBackground).isFalse()
        assertThat(appearance.accentPalette).isEqualTo(AccentPalette.TEAL)
    }

    @After
    fun resetAppearance() {
        StartupAppearanceStore.write(context, ThemeMode.SYSTEM, false, AccentPalette.DYNAMIC)
    }
}
