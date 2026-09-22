package org.ligi.passandroid.model.pass

import com.google.zxing.BarcodeFormat

enum class PassBarCodeFormat {

    AZTEC,
    CODABAR,
    CODE_39,
    CODE_93,
    CODE_128,
    DATA_MATRIX,
    EAN_8,
    EAN_13,
    ITF,
    PDF_417,
    QR_CODE,
    UPC_A,
    UPC_E;

    fun isQuadratic() = when (this) {
        QR_CODE, AZTEC, DATA_MATRIX -> true
        else -> false
    }

    fun zxingBarCodeFormat() = when (this) {
        AZTEC -> BarcodeFormat.AZTEC
        CODABAR -> BarcodeFormat.CODABAR
        CODE_39 -> BarcodeFormat.CODE_39
        CODE_93 -> BarcodeFormat.CODE_93
        CODE_128 -> BarcodeFormat.CODE_128
        DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
        EAN_8 -> BarcodeFormat.EAN_8
        EAN_13 -> BarcodeFormat.EAN_13
        ITF -> BarcodeFormat.ITF
        PDF_417 -> BarcodeFormat.PDF_417
        QR_CODE -> BarcodeFormat.QR_CODE
        UPC_A -> BarcodeFormat.UPC_A
        UPC_E -> BarcodeFormat.UPC_E
    }
}
