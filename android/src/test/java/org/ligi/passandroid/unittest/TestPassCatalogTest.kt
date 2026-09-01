package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.junit.Test
import org.ligi.passandroid.model.createPassMoshi
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassImpl

class TestPassCatalogTest {

    private val adapter = createPassMoshi().adapter(PassImpl::class.java)

    @Test
    fun catalogContainsAReadablePassForEveryBarcodeFormat() {
        val catalog = JSONArray(readCatalog())

        val passes = (0 until catalog.length()).map { index ->
            val fixture = catalog.getJSONObject(index)
            fixture.getString("fileName") to requireNotNull(adapter.fromJson(fixture.getJSONObject("pass").toString()))
        }

        assertThat(passes.map { it.first }).allMatch { it.endsWith(".espass") }
        assertThat(passes.mapNotNull { it.second.barCode?.format }).containsExactlyInAnyOrder(*PassBarCodeFormat.entries.toTypedArray())
        assertThat(passes.map { it.second.id }).doesNotHaveDuplicates()
        assertThat(passes.map { it.second.type }.distinct()).hasSizeGreaterThanOrEqualTo(5)
        assertThat(passes).allSatisfy { (_, pass) ->
            assertThat(pass.description).isNotBlank()
            assertThat(pass.creator).isNotBlank()
            assertThat(pass.barCode?.message).isNotBlank()
            assertThat(pass.fields).isNotEmpty()
        }
        assertThat(passes.map { it.second }).anySatisfy { pass -> assertThat(pass.locations).isNotEmpty() }
        assertThat(passes.map { it.second }).anySatisfy { pass -> assertThat(pass.calendarTimespan?.to).isNull() }
        assertThat(passes.map { it.second }).anySatisfy { pass -> assertThat(pass.fields).anyMatch { it.hide } }
    }

    @Test
    fun catalogUsesReadableUtf8Text() {
        val catalog = readCatalog()

        assertThat(catalog).contains("Row A · Seat 7")
        assertThat(catalog).contains("MAD → BCN")
        assertThat(catalog).contains("Adolfo Suárez Madrid–Barajas Airport")
        assertThat(catalog).contains("€25")
        assertThat(catalog).contains("North Café")
        assertThat(catalog).doesNotContain("Â", "Ã", "â")
    }

    private fun readCatalog() = javaClass.classLoader!!
        .getResourceAsStream("test-passes/catalog.json")!!
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }
}
