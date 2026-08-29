package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.createPassMoshi
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassLocation
import org.ligi.passandroid.model.pass.PassType

class PassSerializationTest {

    private val adapter = createPassMoshi().adapter(PassImpl::class.java)

    @Test
    fun preservesStoredPassFields() {
        val original = PassImpl("pass-id").apply {
            creator = "Pass creator"
            type = PassType.BOARDING
            barCode = BarCode(PassBarCodeFormat.AZTEC, "barcode-message").apply {
                alternativeText = "A1"
            }
            accentColor = 0xFF336699.toInt()
            description = "Boarding pass"
            fields = mutableListOf(PassField("gate", "Gate", "12", false, "Departure gate"))
            locations = listOf(PassLocation().apply {
                name = "Airport"
                lat = 41.2974
                lon = 2.0833
            })
            serial = "serial-1"
        }

        val json = adapter.toJson(original)
        val restored = requireNotNull(adapter.fromJson(json))

        assertThat(json).contains("\"id\":\"pass-id\"")
        assertThat(json).contains("\"accentColor\":\"#ff336699\"")
        assertThat(restored.id).isEqualTo(original.id)
        assertThat(restored.creator).isEqualTo(original.creator)
        assertThat(restored.type).isEqualTo(original.type)
        assertThat(restored.barCode?.format).isEqualTo(original.barCode?.format)
        assertThat(restored.barCode?.message).isEqualTo(original.barCode?.message)
        assertThat(restored.barCode?.alternativeText).isEqualTo(original.barCode?.alternativeText)
        assertThat(restored.accentColor).isEqualTo(original.accentColor)
        assertThat(restored.description).isEqualTo(original.description)
        assertThat(restored.fields.single().value).isEqualTo(original.fields.single().value)
        assertThat(restored.locations.single().lat).isEqualTo(original.locations.single().lat)
        assertThat(restored.serial).isEqualTo(original.serial)
    }
}
