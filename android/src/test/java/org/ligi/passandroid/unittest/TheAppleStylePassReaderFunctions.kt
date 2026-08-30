package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.ligi.passandroid.reader.getBarcodeJson

class TheAppleStylePassReaderFunctions {


    @Test
    fun testEmptyWorks() {
        assertThat(JSONObject("{ }").getBarcodeJson()).isNull()
    }

    @Test
    fun testArrayWorks() {
        val json = "{ \"barcodes\":[{\"format\":\"PKBarcodeFormatQR\",\"message\":\"YO!\"}] }"
        val tested = JSONObject(json).getBarcodeJson()

        assertThat(tested).isNotNull
        assertThat(tested!!.has("message")).isTrue
    }

    @Test
    fun testSingleWorks() {
        val json = "{ \"barcode\":{\"format\":\"PKBarcodeFormatQR\",\"message\":\"YO!\"} }"
        val tested = JSONObject(json).getBarcodeJson()

        assertThat(tested).isNotNull
        assertThat(tested!!.has("message")).isTrue
    }

    @Test
    fun testSingleIsPreferred() {
        val json = "{ \"barcodes\":[{\"format\":\"PKBarcodeFormatQR\",\"message\":\"NO!\"}] ,\"barcode\":{\"format\":\"PKBarcodeFormatQR\",\"message\":\"YO!\"} }"
        val tested = JSONObject(json).getBarcodeJson()

        assertThat(tested).isNotNull
        assertThat(tested!!.getString("message")).isEqualTo("YO!")
    }
}
