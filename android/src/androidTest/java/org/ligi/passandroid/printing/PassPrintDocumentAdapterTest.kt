package org.ligi.passandroid.printing

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.platform.PrintableField
import org.ligi.passandroid.platform.PrintablePass
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.ArgumentMatchers.any
import java.io.File

class PassPrintDocumentAdapterTest {
    @Test
    fun writesAPassAsPdf() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pass = PrintablePass(
            description = "Train ticket",
            barcodeFormat = PassBarCodeFormat.QR_CODE,
            barcodeMessage = "ticket-123",
            barcodeAlternativeText = "Ticket 123",
            fields = listOf(PrintableField("Seat", "12A")),
        )
        val adapter = PassPrintDocumentAdapter(context, pass, "Print test")
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("test", "Test", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        val layoutCallback = mock(PrintDocumentAdapter.LayoutResultCallback::class.java)
        adapter.onLayout(attributes, attributes, CancellationSignal(), layoutCallback, Bundle())

        val output = File(context.cacheDir, "print-test.pdf")
        val descriptor = ParcelFileDescriptor.open(
            output,
            ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_READ_WRITE,
        )
        val writeCallback = mock(PrintDocumentAdapter.WriteResultCallback::class.java)
        adapter.onWrite(arrayOf(PageRange.ALL_PAGES), descriptor, CancellationSignal(), writeCallback)
        descriptor.close()

        verify(writeCallback).onWriteFinished(any())
        assertThat(output.readBytes().decodeToString(0, 4)).isEqualTo("%PDF")
        assertThat(output.length()).isGreaterThan(100)
    }
}
