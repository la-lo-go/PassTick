package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LicensesTest {

    @Test
    fun `every licensed component is complete`() {
        assertThat(licensedComponents).isNotEmpty
        licensedComponents.forEach { component ->
            assertThat(component.name).isNotBlank()
            assertThat(component.copyright).isNotBlank()
        }
    }

    @Test
    fun `every license kind has components`() {
        LicenseKind.entries.forEach { kind ->
            assertThat(licensedComponents.filter { it.kind == kind }).isNotEmpty()
        }
    }

    @Test
    fun `component names are unique`() {
        assertThat(licensedComponents.map { it.name }).doesNotHaveDuplicates()
    }

    @Test
    fun `every license kind points at license resources`() {
        LicenseKind.entries.forEach { kind ->
            assertThat(kind.textRes).isNotZero()
            assertThat(kind.labelRes).isNotZero()
        }
    }
}
