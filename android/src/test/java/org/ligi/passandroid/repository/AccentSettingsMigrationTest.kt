package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AccentSettingsMigrationTest {
    @Test
    fun defaultsWhenNothingWasStored() {
        assertThat(resolveAccentSettings(null, null, null)).isEqualTo(AccentSettings(true, null))
    }

    @Test
    fun dynamicLegacyValueKeepsDynamicColors() {
        assertThat(resolveAccentSettings(null, null, "DYNAMIC")).isEqualTo(AccentSettings(true, null))
    }

    @Test
    fun namedLegacyValuesMapToTheirSeedColor() {
        assertThat(resolveAccentSettings(null, null, "GREEN")).isEqualTo(AccentSettings(false, 0xFF006C4CL))
        assertThat(resolveAccentSettings(null, null, "BLUE")).isEqualTo(AccentSettings(false, 0xFF2859C5L))
        assertThat(resolveAccentSettings(null, null, "BROWN")).isEqualTo(AccentSettings(false, 0xFF765848L))
    }

    @Test
    fun unknownLegacyValueFallsBackToDefaults() {
        assertThat(resolveAccentSettings(null, null, "NOT_A_PALETTE")).isEqualTo(AccentSettings(true, null))
    }

    @Test
    fun storedValuesWinOverLegacyValue() {
        assertThat(resolveAccentSettings(true, null, "GREEN")).isEqualTo(AccentSettings(true, null))
        assertThat(resolveAccentSettings(false, 0xFF123456L, "GREEN"))
            .isEqualTo(AccentSettings(false, 0xFF123456L))
    }
}
