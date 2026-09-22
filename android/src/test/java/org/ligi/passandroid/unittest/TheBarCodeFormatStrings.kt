package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat

class TheBarCodeFormatStrings {

    @Test
    fun readsTheNewFormatsInsteadOfFallingBackToQr() {
        assertThat(BarCode.getFormatFromString("CODABAR")).isEqualTo(PassBarCodeFormat.CODABAR)
        assertThat(BarCode.getFormatFromString("CODE_93")).isEqualTo(PassBarCodeFormat.CODE_93)
        assertThat(BarCode.getFormatFromString("UPC_A")).isEqualTo(PassBarCodeFormat.UPC_A)
        assertThat(BarCode.getFormatFromString("UPC_E")).isEqualTo(PassBarCodeFormat.UPC_E)
    }

    @Test
    fun readsCompactAndHyphenatedUpcNames() {
        assertThat(BarCode.getFormatFromString("UPCA")).isEqualTo(PassBarCodeFormat.UPC_A)
        assertThat(BarCode.getFormatFromString("UPC-E")).isEqualTo(PassBarCodeFormat.UPC_E)
    }

    @Test
    fun keepsTheExistingAppleAndEsPassNames() {
        assertThat(BarCode.getFormatFromString("PKBarcodeFormatQR")).isEqualTo(PassBarCodeFormat.QR_CODE)
        assertThat(BarCode.getFormatFromString("PKBarcodeFormatPDF417")).isEqualTo(PassBarCodeFormat.PDF_417)
        assertThat(BarCode.getFormatFromString("PKBarcodeFormatAztec")).isEqualTo(PassBarCodeFormat.AZTEC)
        assertThat(BarCode.getFormatFromString("PKBarcodeFormatCode128")).isEqualTo(PassBarCodeFormat.CODE_128)
        assertThat(BarCode.getFormatFromString("CODE_39")).isEqualTo(PassBarCodeFormat.CODE_39)
    }
}
