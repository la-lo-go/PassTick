package org.ligi.passandroid.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProtectedPassSettingsTest {
    @Test
    fun protectedPassPrivacyOptionsAreOptIn() {
        val settings = AppSettings()

        assertThat(settings.lockAllPasses).isFalse()
        assertThat(settings.showProtectedPassLockIcon).isTrue()
        assertThat(settings.blurProtectedPassCards).isFalse()
        assertThat(settings.separateProtectedPasses).isFalse()
        assertThat(settings.blockScreenshots).isFalse()
    }
}
